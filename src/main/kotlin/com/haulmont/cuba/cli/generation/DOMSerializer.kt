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

import com.google.common.base.Strings
import com.google.common.xml.XmlEscapers
import org.w3c.dom.Document
import org.w3c.dom.DocumentType
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.*
import java.util.*


internal class DOMSerializer(
        numIndentSpaces: Int = 0,
        private val lineSeparator: String = "\n",
        private val encoding: String = "UTF8",
        private val displayAttributesOnSeparateLine: Boolean = true,
        private val maxLengthOfString: Int = -1
) {

    private val indent = buildString {
        for (i in 0 until numIndentSpaces)
            append(" ")
    }

    @Throws(IOException::class)
    fun serialize(doc: Document, out: OutputStream) =
            OutputStreamWriter(out, encoding).use { writer ->
                serialize(doc, writer)
            }


    @Throws(IOException::class)
    fun serialize(doc: Document, file: File) =
            FileWriter(file).use { writer -> serialize(doc, writer) }


    @Throws(IOException::class)
    fun serialize(doc: Document, writer: Writer) {
        doc.normalize()
        // Start serialization recursion with no indenting
        serializeNode(doc, writer, "")
        writer.flush()
    }

    @Throws(IOException::class)
    fun serialize(element: Element, writer: Writer) {
        // Start serialization recursion with no indenting
        serializeNode(element, writer, "")
        writer.flush()
    }

    @Throws(IOException::class)
    private fun serializeNode(node: Node, writer: Writer, indentLevel: String) {
        // Determine action based on node type
        when (node.nodeType) {
            Node.DOCUMENT_NODE -> {
                val doc = node as Document
                /**
                 * DOM Level 2 code writer.write("");
                 */
                writer.write("<?xml version=\"")
                writer.write(doc.xmlVersion)
                writer.write("\" encoding=\"UTF-8\" standalone=\"")
                if (doc.xmlStandalone)
                    writer.write("yes")
                else
                    writer.write("no")
                writer.write("\"")
                writer.write("?>")
                writer.write(lineSeparator)

                // recurse on each top-level node
                val nodes = node.getChildNodes()
                if (nodes != null)
                    for (i in 0 until nodes.length)
                        serializeNode(nodes.item(i), writer, "")
            }
            Node.ELEMENT_NODE -> {
                val name = node.nodeName
                val beginStr = "$indentLevel<$name"
                writer.write(beginStr)

                val attributes = node.attributes
                val sortedAttributes = TreeMap<String, String>(AttributeComparator())
                for (i in 0 until attributes.length) {
                    val item = attributes.item(i)
                    // Ignore xmlns for non-root elements
                    if (node.parentNode != null
                            && node.parentNode.nodeType != Node.DOCUMENT_NODE
                            && item.nodeName == "xmlns")
                        continue

                    sortedAttributes[item.nodeName] = item.nodeValue
                }

                if (displayAttributesOnSeparateLine) {
                    writeAttributesOnSeparateLine(writer, beginStr, sortedAttributes)
                } else {
                    writeAttributes(writer, beginStr, sortedAttributes)
                }

                // recurse on each child
                val children = node.childNodes
                if (children != null && children.length > 0) {
                    // close the open tag
                    writer.write(">")
                    if (children.length > 0) {
                        val child = children.item(0)
                        if (child.nodeType != Node.TEXT_NODE || child.nodeValue.trim { it <= ' ' }.length == 0)
                            writer.write(lineSeparator)
                    }

                    for (i in 0 until children.length)
                        serializeNode(children.item(i), writer, indentLevel + indent)

                    if (children.length > 0) {
                        val child = children.item(children.length - 1)
                        if (child.nodeType != Node.TEXT_NODE || child.nodeValue.trim { it <= ' ' }.length == 0)
                            writer.write(indentLevel)
                    }
                    writer.write("</$name>")
                } else {
                    //Close this element without making a frivolous full close tag
                    writer.write("/>")
                }
                writer.write(lineSeparator)
            }
            Node.TEXT_NODE -> {
                val value = node.nodeValue.trim { it <= ' ' }
                if (value.length > 0)
                    printWithContentEscape(writer, value)
            }
            Node.CDATA_SECTION_NODE -> {
                writer.write("$indentLevel<![CDATA[")
                writer.write(node.nodeValue)
                writer.write("]]>$lineSeparator")
            }
            Node.COMMENT_NODE -> {
                writer.write(indentLevel + "<!--" + node.nodeValue + "-->")
                writer.write(lineSeparator)
            }
            Node.PROCESSING_INSTRUCTION_NODE -> {
                writer.write("<?" + node.nodeName + " " + node.nodeValue
                        + "?>")
                writer.write(lineSeparator)
            }
            Node.ENTITY_REFERENCE_NODE -> writer.write("&" + node.nodeName + ";")
            Node.DOCUMENT_TYPE_NODE -> {
                val docType = node as DocumentType
                val publicId = docType.publicId
                val systemId = docType.systemId
                val internalSubset = docType.internalSubset
                writer.write("<!DOCTYPE " + docType.name)
                if (publicId != null)
                    writer.write(" PUBLIC \"$publicId\" ")
                else
                    writer.write(" SYSTEM ")
                writer.write("\"" + systemId + "\"")
                if (internalSubset != null)
                    writer.write(" [$internalSubset]")
                writer.write(">")
                writer.write(lineSeparator)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeAttributes(writer: Writer, beginLine: String, sortedAttributes: TreeMap<String, String>) {
        if (sortedAttributes.isEmpty()) {
            return
        }
        if (maxLengthOfString > 0) {
            var fullLine = beginLine
            var isAttrInLine = false
            val attributeSeparator = lineSeparator + Strings.repeat(" ", beginLine.length + 1)
            for ((key, value) in sortedAttributes) {
                val attr = createAttr(key, value)
                fullLine += " $attr"
                if (!isAttrInLine || fullLine.length <= maxLengthOfString) {
                    writer.write(" $attr")
                    isAttrInLine = true
                } else {
                    writer.write(attributeSeparator)
                    writer.write(attr)
                    fullLine = attributeSeparator + attr
                }
            }
        } else {
            for ((key, value) in sortedAttributes) {
                val attr = createAttr(key, value)
                writer.write(" $attr")
            }
        }
    }

    @Throws(IOException::class)
    private fun writeAttributesOnSeparateLine(writer: Writer, beginLine: String, sortedAttributes: TreeMap<String, String>) {
        if (sortedAttributes.size > 0) {
            val firstKey = sortedAttributes.firstKey()
            var attributeStr = " $firstKey=\""
            writer.write(attributeStr)
            printWithAttributeEscape(writer, sortedAttributes[firstKey])
            writer.write("\"")

            val attributeSeparator = lineSeparator + Strings.repeat(" ", beginLine.length + 1)
            for ((key, value) in sortedAttributes) {
                if (firstKey != key) {
                    attributeStr = "$attributeSeparator$key=\""
                    writer.write(attributeStr)
                    printWithAttributeEscape(writer, value)
                    writer.write("\"")
                }
            }
        }
    }

    private fun createAttr(name: String, value: String): String {
        return name + "=\"" + xmlAttributeEscape(value) + "\""
    }

    private fun xmlAttributeEscape(value: String?): String {
        return if (value == null) {
            ""
        } else XML_ATTRIBUTE_ESCAPER.escape(value)
    }

    @Throws(IOException::class)
    private fun printWithContentEscape(writer: Writer, s: String?) {
        if (s == null) {
            return
        }
        writer.write(XML_CONTENT_ESCAPER.escape(s))
    }

    @Throws(IOException::class)
    private fun printWithAttributeEscape(writer: Writer, s: String?) {
        if (s == null) {
            return
        }
        writer.write(XML_ATTRIBUTE_ESCAPER.escape(s))
    }

    private class AttributeComparator : Comparator<String> {

        private fun getWeight(a: String): Int {
            val weight = w[a]
            return weight ?: 1000
        }

        override fun compare(a1: String, a2: String): Int {
            val w1 = getWeight(a1)
            val w2 = getWeight(a2)

            return if (w1 == w2) a1.compareTo(a2) else w1 - w2
        }

        companion object {

            private val w = HashMap<String, Int>()

            init {
                w["xmlns"] = 10
                w["xmlns:xsi"] = 20
                w["xsi:schemaLocation"] = 30
                w["id"] = 40
            }
        }
    }

    companion object {

        private val XML_ATTRIBUTE_ESCAPER = XmlEscapers.xmlAttributeEscaper()
        private val XML_CONTENT_ESCAPER = XmlEscapers.xmlContentEscaper()
    }
}
