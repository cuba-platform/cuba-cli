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
import kotlin.reflect.full.memberProperties

class TemplateProcessor private constructor(templateBasePath: String, private val bindings: Map<String, Any>) {

    private val writer: PrintWriter by kodein.instance()

    private val pathExpressionPattern = Regex("\\$\\{[a-zA-Z][0-9a-zA-Z]*(\\.[a-zA-Z][0-9a-zA-Z]*)*}")
    private val variablePartPattern = Regex("[a-zA-Z][0-9a-zA-Z]*")

    private val velocityContext: VelocityContext = VelocityContext().apply {
        bindings.forEach { k, v -> put(k, v) }
    }

    private val templatePath: Path

    init {
        val templateUri = javaClass.getResource(templateBasePath).toURI()

        templatePath = if (templateUri.scheme == "jar") {
            val fileSystem = getFileSystem(templateUri)
            fileSystem.getPath(templateBasePath)
        } else {
            Paths.get(templateUri)
        }
    }

    private fun getFileSystem(templateUri: URI?) = try {
        FileSystems.getFileSystem(templateUri)
    } catch (e: FileSystemNotFoundException) {
        FileSystems.newFileSystem(templateUri, mutableMapOf<String, Any>())
    }

    private fun process(from: Path, to: Path, withTransform: Boolean) {
        val targetAbsolutePath = to.toAbsolutePath()

        val baseTemplatePath = templatePath.toAbsolutePath().toString()
        val targetDirectoryPath = targetAbsolutePath.toString()

        Files.walk(from)
                .filter { Files.isRegularFile(it) }
                .forEach { inputPath ->
                    val outputFile = inputPath.toAbsolutePath().toString()
                            .replace(baseTemplatePath, targetDirectoryPath)
                            .let { applyPathTransform(it, bindings) }
                            .let { Paths.get(it) }
                            .let { targetAbsolutePath.relativize(it) }

                    ensureFolders(outputFile)

                    if (withTransform) {
                        transformInternal(inputPath, outputFile, velocityContext)
                    } else {
                        copyInternal(inputPath, outputFile)
                    }

                }
    }

    private fun transformInternal(inputPath: Path, outputFile: Path, vc: VelocityContext) {
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

    private fun copyInternal(inputPath: Path, outputFile: Path) {
        Files.copy(inputPath, outputFile)

        writer.println("\t@|green created|@\t$outputFile")
    }

    private fun ensureFolders(outputFile: Path) {
        val parent = outputFile.toAbsolutePath().parent
        if (!Files.exists(parent)) {
            Files.createDirectories(parent)
        }
    }

    private fun applyPathTransform(path: String, bindings: Map<String, Any>): String {
        return pathExpressionPattern.replace(path) {
            val expression = it.value.substring(2, it.value.lastIndex)
            variablePartPattern.findAll(expression)
                    .map {
                        it.value
                    }.fold(bindings as Any) { obj, name ->
                        getChild(obj, name)
                    }.toString()
        }
    }

    private fun getChild(obj: Any, name: String): Any = when (obj) {
        is Map<*, *> -> obj[name]!!
        else -> getField(obj, name)
    }

    private fun getField(obj: Any, name: String): Any {
        val kClass = obj::class

        return kClass.memberProperties.first { it.name == name }.getter.call(obj)!!
    }

    fun copy(subPath: Path, to: Path = Paths.get("")) {
        process(templatePath.resolve(subPath), to, false)
    }

    fun copy(subPath: String, to: Path = Paths.get("")) {
        process(templatePath.resolve(subPath), to, false)
    }

    fun transform(subPath: Path, to: Path = Paths.get("")) {
        process(templatePath.resolve(subPath), to, true)
    }

    fun transform(subPath: String, to: Path = Paths.get("")) {
        process(templatePath.resolve(subPath), to, true)
    }

    companion object {

        init {
            Velocity.init()
        }

        operator fun invoke(templateBasePath: String, bindings: Map<String, Any>, block: TemplateProcessor.() -> Unit) {
            TemplateProcessor(templateBasePath, bindings).block()
        }
    }
}

