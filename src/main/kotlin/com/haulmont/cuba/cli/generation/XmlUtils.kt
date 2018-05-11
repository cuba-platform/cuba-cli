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
import net.sf.practicalxml.XmlException
import net.sf.practicalxml.xpath.XPathWrapper
import org.kodein.di.generic.instance
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

private val writer: PrintWriter by kodein.instance()
private val printHelper: PrintHelper by kodein.instance()

fun parse(path: Path): Document = Files.newInputStream(path).let(::InputSource).let {
    createDocumentBuilder().parse(it)
}

private fun createDocumentBuilder(): DocumentBuilder {
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.isNamespaceAware = false
    dbf.isCoalescing = true
    dbf.isValidating = false

    try {
        return dbf.newDocumentBuilder()
    } catch (e: ParserConfigurationException) {
        throw XmlException("unable to confiure parser", e)
    }
}

fun save(document: Document, path: Path) = DOMSerializer(numIndentSpaces = 4).run {
    serialize(document, Files.newOutputStream(path))
}

fun updateXml(path: Path, block: Element.() -> Unit) {
    val document = parse(path)
    document.documentElement.block()
    save(document, path)

    printHelper.fileAltered(path)
}

fun Element.getChildElements() = (0..this.childNodes.length)
        .map(this.childNodes::item)
        .filterIsInstance(Element::class.java)

fun Element.findFirstChild(tagName: String): Element? =
        DomUtil.getChildren(this).firstOrNull { it.tagName == tagName }


fun Element.appendChild(tagName: String, setup: (Element.() -> Unit)? = null): Element =
        DomUtil.appendChild(this, tagName).apply {
            if (setup != null) setup()
        }

operator fun Element.get(attributeName: String): String = this.getAttribute(attributeName)
operator fun Element.set(attributeName: String, attributeValue: String) = this.setAttribute(attributeName, attributeValue)

fun Element.xpath(expr: String): MutableList<Node> = XPathWrapper(expr).evaluate(this)