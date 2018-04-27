/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.cli.generation

import com.haulmont.cuba.cli.PrintHelper
import com.haulmont.cuba.cli.kodein
import net.sf.practicalxml.DomUtil
import net.sf.practicalxml.ParseUtil
import org.kodein.di.generic.instance
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path

private val writer: PrintWriter by kodein.instance()
private val printHelper: PrintHelper by kodein.instance()

fun parse(path: Path): Document =
        Files.newInputStream(path).let(::InputSource).let(ParseUtil::parse)


fun save(document: Document, path: Path) = DOMSerializer(numIndentSpaces = 4).run {
    serialize(document, Files.newOutputStream(path))
}

fun updateXml(path: Path, definition: XmlDefinition.() -> Unit) {
    val document = parse(path)
    ensureXmlStructure(document.documentElement, definition)
    save(document, path)

    printHelper.fileAltered(path)
}

fun Element.getChildElements() = (0..this.childNodes.length)
        .map(this.childNodes::item)
        .filterIsInstance(Element::class.java)

fun Element.findChild(predicate: (Element) -> Boolean): Element? =
        DomUtil.getChildren(this).firstOrNull(predicate)

interface XmlDefinition {
    infix operator fun String.invoke(definition: XmlDefinition.() -> Unit) {
        elem(this, definition)
    }

    operator fun String.unaryPlus() {
        content(this)
    }

    fun add(tagName: String, definition: XmlDefinition.() -> Unit)

    fun elem(tagName: String, definition: XmlDefinition.() -> Unit)

    fun content(value: String)

    infix fun String.mustBe(value: String)
}

class ElementPredicate(private val element: Element, private val tagName: String) : XmlDefinition {
    override fun add(tagName: String, definition: XmlDefinition.() -> Unit) {}

    override fun content(value: String) {}

    private val attrs: MutableMap<String, String> = linkedMapOf()

    override fun String.mustBe(value: String) {
        attrs[this] = value
    }

    override fun elem(tagName: String, definition: XmlDefinition.() -> Unit) {}

    fun findFirst(): Element? {
        val predicate: (Element) -> Boolean = { elem ->
            attrs.all { (attr, value) ->
                elem.hasAttribute(attr) && elem.getAttribute(attr) == value
            }
        }

        val children = DomUtil.getChildren(element).filter {
            it.tagName == tagName && predicate(it)
        }

        return children.firstOrNull()
    }
}

class ElementWrapper(private val element: Element) : XmlDefinition {
    override fun add(tagName: String, definition: XmlDefinition.() -> Unit) {
        DomUtil.appendChild(element, tagName).apply {
            ElementWrapper(this).definition()
        }
    }

    override fun content(value: String) {
        element.textContent = value
    }

    override infix fun String.mustBe(value: String) {
        element.setAttribute(this, value)
    }

    override fun elem(tagName: String, definition: XmlDefinition.() -> Unit) {
        val predicate = ElementPredicate(element, tagName)
        predicate.definition()
        (predicate.findFirst() ?: DomUtil.appendChild(element, tagName)).apply {
            ElementWrapper(this).definition()
        }
    }
}

fun ensureXmlStructure(element: Element, definition: XmlDefinition.() -> Unit) {
    val wrapper = ElementWrapper(element)
    wrapper.definition()
}