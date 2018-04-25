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