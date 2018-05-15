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

import com.haulmont.cuba.cli.*
import com.haulmont.cuba.cli.commands.CommandExecutionException
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeSingleton
import org.kodein.di.generic.instance
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.reflect.full.memberProperties

/**
 * TemplateProcessor is used to generate project files from templates.
 *
 * It uses Apache Velocity template engine.
 * You can read about how to create Apache Velocity templates on its <a href="http://velocity.apache.org/engine/1.7/user-guide.html">documentation</a>.
 *
 * By default, it accepts Map<String, Any> as velocity context, where map key represents a model name from the CliContext
 * and value represents the model itself. Models may be registered in the CliContext by {@link com.haulmont.cuba.cli.commands.GeneratorCommand}
 * or by {@link com.haulmont.cuba.cli.CliPlugin}.
 *
 * Variables are allowed in template directories names. Records like {@code `${varpath}`} will be substituted with
 * corresponding variable from Velocity Context. Records like {@code `$[packageNameVariable]`} is used to automatically
 * convert package names to directories names.
 *
 * Packages of all your models should be opened in order to Apache Velocity may access them through reflexion.
 *
 */
class TemplateProcessor {
    private val bindings: Map<String, Any>

    private val writer: PrintWriter by kodein.instance()
    private val printHelper: PrintHelper by kodein.instance()

    private val pathExpressionPattern: Regex = Regex("\\$\\{[a-zA-Z][0-9a-zA-Z]*(\\.[a-zA-Z][0-9a-zA-Z]*)*}")
    private val packageExpressionPattern: Regex = Regex("\\$\\[[a-zA-Z][0-9a-zA-Z]*(\\.[a-zA-Z][0-9a-zA-Z]*)*]")

    private val variablePartPattern: Regex = Regex("[a-zA-Z][0-9a-zA-Z]*")
    private val velocityContext: VelocityContext

    val templatePath: Path

    private constructor(templateBasePath: Path, bindings: Map<String, Any>, version: PlatformVersion) {
        templatePath = version.findMostSuitableVersionDirectory(templateBasePath)
        this.bindings = bindings
        this.velocityContext = VelocityContext().apply {
            bindings.forEach { k, v -> put(k, v) }
        }
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
                            .let { projectRoot.relativize(it) }

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

        printHelper.fileCreated(outputFile)
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
            parsePathExpression(expression, bindings)
        }.let {
            packageExpressionPattern.replace(it) {
                val expression = it.value.substring(2, it.value.lastIndex)
                parsePathExpression(expression, bindings).replace('.', File.separatorChar)
            }
        }
    }

    private fun parsePathExpression(expression: String, bindings: Map<String, Any>): String =
            variablePartPattern.findAll(expression)
                    .map {
                        it.value
                    }.fold(bindings as Any) { obj, name ->
                        getChild(obj, name)
                    }.toString()


    private fun getChild(obj: Any, name: String): Any = when (obj) {
        is Map<*, *> -> {
            if (name in obj) {
                obj[name]!!
            } else throw CommandExecutionException("Path variable $name doesn't exists")
        }
        else -> getField(obj, name)
    }

    private fun getField(obj: Any, name: String): Any {
        val kClass = obj::class

        return kClass.memberProperties.first { it.name == name }.getter.call(obj)!!
    }

    fun copy(subPath: Path, to: Path = projectRoot) {
        process(templatePath.resolve(subPath), to, false)
    }

    fun copy(subPath: String, to: Path = projectRoot) {
        process(templatePath.resolve(subPath), to, false)
    }

    fun transform(subPath: Path, to: Path = projectRoot) {
        process(templatePath.resolve(subPath), to, true)
    }

    fun transform(subPath: String, to: Path = projectRoot) {
        process(templatePath.resolve(subPath), to, true)
    }

    fun transformWhole(to: Path = projectRoot) {
        transform("", to)
    }

    fun copyWhole(to: Path = projectRoot) {
        copy("", to)
    }

    fun Path.walk(depth: Int) = Files.walk(this, depth).filter { it != this }.collect(Collectors.toList())

    companion object {
        val projectRoot: Path = Paths.get("").toAbsolutePath()

        private val resources: Resources by kodein.instance()

        private val CUSTOM_TEMPLATES_PATH = Paths.get(System.getProperty("user.home"), ".haulmont", "cli", "templates")
                .also {
                    if (!Files.exists(it)) {
                        Files.createDirectories(it)
                    }
                }

        init {
            Velocity.init()
        }

        operator fun invoke(templateName: String, bindings: Map<String, Any>, platformVersion: PlatformVersion = LatestVersion, block: TemplateProcessor.() -> Unit) {
            TemplateProcessor(findTemplate(templateName), bindings, platformVersion).block()
        }

        operator fun invoke(templateBasePath: Path, bindings: Map<String, Any>, platformVersion: PlatformVersion = LatestVersion, block: TemplateProcessor.() -> Unit) {
            TemplateProcessor(templateBasePath, bindings, platformVersion).block()
        }

        fun findTemplate(templateBasePath: String): Path =
                resources.getResourcePath(templateBasePath) ?: CUSTOM_TEMPLATES_PATH.resolve(templateBasePath)
    }
}