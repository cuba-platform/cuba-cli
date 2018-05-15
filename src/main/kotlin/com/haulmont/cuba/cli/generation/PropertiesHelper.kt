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
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Path

class PropertiesHelper private constructor(private var properties: String) {
    fun set(key: String, value: String) {
        val matchResult = Regex("$key *= *(.*) *\n?").find(properties)
        if (matchResult != null) {
            properties = properties.replace(Regex("$key *= *(.*) *\n?")) {
                "$key = $value\n"
            }
        } else {
            properties += "$key = $value\n"
        }
    }

    fun update(key: String, update: (String?) -> String) {
        val matchResult = Regex("$key *= *(.*) *\n?").find(properties)
        if (matchResult == null) {
            properties += "$key = ${update(null)}\n"
        } else {
            properties = properties.replace(Regex("$key *= *(.*) *\n?")) {
                "$key = ${update(it.groupValues[1])}\n"
            }
        }

    }

    companion object {

        private val printHelper: PrintHelper by kodein.instance()

        operator fun invoke(path: Path, block: PropertiesHelper.() -> Unit) {
            val created = if (!Files.exists(path)) {
                Files.createFile(path)
                true
            } else false

            val file = path.toFile()

            val trailingNewLine = file.readText()
                    .let {
                        it.isEmpty() || it.endsWith("\n")
                    }

            file.readText().let {
                if (!trailingNewLine) {
                    it + "\n"
                } else it
            }.let {
                PropertiesHelper(it).apply(block).properties
            }.let {
                file.writeText(it)
            }

            if (created)
                printHelper.fileCreated(path)
            else
                printHelper.fileModified(path)
        }
    }
}