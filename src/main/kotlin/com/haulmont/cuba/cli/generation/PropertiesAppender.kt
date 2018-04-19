package com.haulmont.cuba.cli.generation

import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path

class PropertiesAppender private constructor(private val stringBuilder: StringBuilder) {
    fun append(key: String, value: String) {
        stringBuilder.append(key.trim()).append(" = ").append(value).append("\n")
    }

    companion object {
        operator fun invoke(path: Path, writer: PrintWriter, block: PropertiesAppender.() -> Unit) {
            val created = if (!Files.exists(path)) {
                Files.createFile(path)
                true
            } else false

            val file = path.toFile()

            val trailingNewLine = file.readText()
                    .let {
                        it.isEmpty() || it.endsWith("\n")
                    }

            buildString {
                if (!trailingNewLine) {
                    append("\n")
                }
                PropertiesAppender(this).block()
            }.let { file.appendText(it) }

            writer.println(buildString {
                append("\t@|green ")
                if (created)
                    append("created")
                else
                    append("altered")
                append("|@ ")
                append(path)
            })
        }
    }
}