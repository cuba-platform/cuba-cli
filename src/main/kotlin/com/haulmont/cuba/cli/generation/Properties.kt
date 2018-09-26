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
import com.haulmont.cuba.cli.generation.properties.MessagesWriter
import com.haulmont.cuba.cli.kodein
import org.apache.commons.configuration.PropertiesConfiguration
import org.kodein.di.generic.instance
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path

class Properties private constructor(private val propertiesConfiguration: PropertiesConfiguration, private val created: Boolean) {
    operator fun set(key: String, value: String) {
        propertiesConfiguration.setProperty(key, value)
    }

    fun update(key: String, update: (String?) -> String) {
        propertiesConfiguration.setProperty(key, update(propertiesConfiguration.getString(key)))
    }

    operator fun get(key: String): String? = propertiesConfiguration.getString(key)

    fun save() {
        propertiesConfiguration.save()

        val path = propertiesConfiguration.file.toPath()
        if (created) {
            printHelper.fileCreated(path)
        } else
            printHelper.fileModified(path)
    }

    companion object {
        private val printHelper: PrintHelper by kodein.instance()

        operator fun invoke(path: Path): Properties {
            val properties = PropertiesConfiguration().apply {
                encoding = "UTF-8"
                file = path.toFile()
                ioFactory = object : PropertiesConfiguration.DefaultIOFactory() {
                    override fun createPropertiesWriter(out: Writer, delimiter: Char): PropertiesConfiguration.PropertiesWriter {
                        return MessagesWriter(out, delimiter)
                    }
                }

                if (file.exists()) {
                    load()
                }
            }

            return Properties(properties, !Files.exists(path))
        }

        fun modify(path: Path, block: Properties.() -> Unit) = Properties(path).apply(block).save()
    }
}