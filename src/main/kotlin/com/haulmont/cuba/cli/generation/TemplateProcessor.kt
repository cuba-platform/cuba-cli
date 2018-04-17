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

import com.haulmont.cuba.cli.kodein
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeSingleton
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.net.URI
import java.nio.file.*

class TemplateProcessor(templateBasePath: String) {

    private val writer: PrintWriter by kodein.instance()

    private val pathVariablePattern: Regex = Regex("\\$\\{[a-zA-Z][0-9a-zA-Z]+(\\.[a-zA-Z][0-9a-zA-Z]*)*}")

    private val templatePath: Path

    init {
        val classLoader = javaClass.classLoader
        val templateUri = classLoader.getResource(templateBasePath).toURI()

        templatePath = if (templateUri.scheme == "jar") {
            val fileSystem = getFileSystem(templateUri, classLoader)
            fileSystem.getPath(templateBasePath)
        } else {
            Paths.get(templateUri)
        }
    }

    private fun getFileSystem(templateUri: URI?, classLoader: ClassLoader?) = try {
        FileSystems.getFileSystem(templateUri)
    } catch (e: FileSystemNotFoundException) {
        FileSystems.newFileSystem(templateUri, mutableMapOf<String, Any>(), classLoader)
    }

    fun copyTo(path: Path, bindings: Map<String, Any>) {
        val targetAbsolutePath = path.toAbsolutePath()

        Velocity.init()
        val vc = VelocityContext()
        bindings.forEach { k, v -> vc.put(k, v) }

        val baseTemplatePath = templatePath.toAbsolutePath().toString()
        val targetDirectoryPath = targetAbsolutePath.toString()

        Files.walk(templatePath, Int.MAX_VALUE)
                .filter { Files.isRegularFile(it) }
                .forEach { inputPath ->
                    val outputFile = inputPath.toAbsolutePath().toString()
                            .replace(baseTemplatePath, targetDirectoryPath)
                            .let { applyPathTransform(it, bindings) }
                            .let { Paths.get(it) }
                            .let { targetAbsolutePath.relativize(it) }

                    ensureFile(outputFile)

                    copy(inputPath, outputFile, vc)
                }
    }

    private fun copy(inputPath: Path, outputFile: Path, vc: VelocityContext) {
        val template = Template()
        val runtimeServices = RuntimeSingleton.getRuntimeServices()
        template.setRuntimeServices(runtimeServices)
        template.data = Files.newInputStream(inputPath).bufferedReader().use {
            runtimeServices.parse(it, inputPath.fileName.toString())
        }
        template.initDocument()

        Files.newBufferedWriter(outputFile).use { writer ->
            template.merge(vc, writer)
        }

        writer.println("\t@|green created|@\t$outputFile")
    }

    private fun ensureFile(outputFile: Path) {
        (outputFile.parent ?: outputFile.toAbsolutePath().parent).takeIf { !Files.exists(it) }?.let { Files.createDirectories(it) }
        Files.createFile(outputFile)
    }

    private fun applyPathTransform(path: String, bindings: Map<String, Any>): String {
        return pathVariablePattern.replace(path) {
            val paramName = it.value.substring(2, it.value.length - 1)
            bindings[paramName] as String
        }
    }
}