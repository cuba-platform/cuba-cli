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

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.runtime.RuntimeSingleton
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path

class VelocityHelper {
    fun generate(input: String, templateName: String, vc: VelocityContext): String {
        val template = Template()
        val runtimeServices = RuntimeSingleton.getRuntimeServices()
        template.setRuntimeServices(runtimeServices)
        template.data = runtimeServices.parse(input, templateName)

        template.initDocument()

        return StringWriter().apply {
            template.merge(vc, this)
        }.toString()
    }

    fun generate(inputPath: Path, vc: VelocityContext): String {
        val templateText = Files.newInputStream(inputPath)
                .bufferedReader().readText()
        val templateName = generate(templateText, inputPath.fileName.toString(), vc)

        return generate(templateText, templateName, vc)
    }

    fun generate(inputPath: Path, outputFile: Path, vc: VelocityContext) {
        val output = generate(inputPath, vc)

        Files.newBufferedWriter(outputFile).use { writer ->
            writer.write(output)
        }
    }

    fun generate(input: String, templateName: String, bindings: Map<String, Any>): String {
        val vc = VelocityContext().apply {
            bindings.forEach { k, v -> put(k, v) }
        }
        return generate(input, templateName, vc)
    }
}