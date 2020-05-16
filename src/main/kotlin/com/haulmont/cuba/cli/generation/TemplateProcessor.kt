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

import com.haulmont.cli.core.*
import com.haulmont.cli.core.commands.CommandExecutionException
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.kodein.di.generic.instance
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.full.memberProperties

/**
 * TemplateProcessor is used to generate project files from templates.
 *
 * It uses Apache Velocity template engine.
 * You can read about how to create Apache Velocity templates on its [documentation](http://velocity.apache.org/engine/1.7/user-guide.html).
 *
 * By default, it accepts ```Map<String, Any>``` as velocity context, where map key represents a model name from the CliContext
 * and value represents the model itself. Models may be registered in the CliContext by [com.haulmont.cuba.cli.commands.GeneratorCommand]
 * or by [com.haulmont.cuba.cli.CliPlugin].
 *
 * Variables are allowed in template directories names. Records like ```${varpath}``` will be substituted with
 * corresponding variable from Velocity Context. Records like ```$[packageNameVariable]``` is used to automatically
 * convert package names to directories names.
 *
 * Packages of all your models should be opened in order to Apache Velocity may access them through reflexion.
 *
 */
class TemplateProcessor(templateBasePath: Path, private val bindings: Map<String, Any>, version: PlatformVersion = PlatformVersion.findVersion()) {

    private val printHelper: PrintHelper by kodein.instance<PrintHelper>()

    private val pathExpressionPattern: Regex = Regex("\\$\\{[a-zA-Z][0-9a-zA-Z]*(\\.[a-zA-Z][0-9a-zA-Z]*)*}")
    private val packageExpressionPattern: Regex = Regex("\\$\\[[a-zA-Z][0-9a-zA-Z]*(\\.[a-zA-Z][0-9a-zA-Z]*)*]")

    private val variablePartPattern: Regex = Regex("[a-zA-Z][0-9a-zA-Z]*")
    private val velocityContext: VelocityContext

    private val velocityHelper: VelocityHelper = VelocityHelper()

    val templatePath: Path = version.findMostSuitableVersionDirectory(templateBasePath)

    init {
        this.velocityContext = VelocityContext().apply {
            bindings.forEach { k, v -> put(k, v) }
        }
    }

    private fun process(from: Path, to: Path, withTransform: Boolean) {
        val targetAbsolutePath = to.toAbsolutePath()

        val baseTemplatePath = templatePath.toAbsolutePath().toString()
        val targetDirectoryPath = targetAbsolutePath.toString()

        from.walk()
                .filter { !isTemplateMetadata(it) }
                .filter { Files.isRegularFile(it) }
                .forEach { inputPath ->
                    val outputFile = inputPath.toAbsolutePath().toString()
                            .replace(baseTemplatePath, targetDirectoryPath)
                            .let { applyPathTransform(it, bindings) }
                            .let { Paths.get(it) }

                    ensureFolders(outputFile)

                    if (withTransform) {
                        transformInternal(inputPath, outputFile, velocityContext)
                    } else {
                        copyInternal(inputPath, outputFile)
                    }
                }
    }

    private fun isTemplateMetadata(path: Path): Boolean =
            path.fileName.toString() in listOf("template.xml", "tips.txt")


    private fun transformInternal(inputPath: Path, outputFile: Path, vc: VelocityContext) {
        velocityHelper.generate(inputPath, outputFile, vc)

        printHelper.fileCreated(outputFile)
    }

    private fun copyInternal(inputPath: Path, outputFile: Path) {
        Files.copy(inputPath, outputFile)

        printHelper.fileCreated(outputFile)
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

    fun transform(subPath: String, to: OutputStream) {
        val filePath = templatePath.resolve(subPath)

        check(Files.isRegularFile(filePath)) { "Only file may be saved to output stream" }

        velocityHelper.generate(filePath, velocityContext).let {
            to.write(it.toByteArray())
        }
    }

    fun transformToText(subPath: String): String = ByteArrayOutputStream().also {
        transform(subPath, it)
    }.toByteArray().let { String(it) }


    fun copy(subPath: String, to: OutputStream) {
        val filePath = templatePath.resolve(subPath)

        check(Files.isRegularFile(filePath)) { "Only file may be saved to output stream" }

        Files.newInputStream(filePath).copyTo(to)
    }

    fun transformWhole(to: Path = projectRoot) {
        transform("", to)
    }

    fun copyWhole(to: Path = projectRoot) {
        copy("", to)
    }

    companion object {
        private val workingDirectoryManager: WorkingDirectoryManager by kodein.instance<WorkingDirectoryManager>()

        val projectRoot: Path
            get() = workingDirectoryManager.workingDirectory.toAbsolutePath()


        init {
            Velocity.init()
        }

        operator fun invoke(
                templateBasePath: Path,
                bindings: Map<String, Any>,
                platformVersion: PlatformVersion = PlatformVersion.findVersion(),
                block: TemplateProcessor.() -> Unit): String? {
            TemplateProcessor(templateBasePath, bindings, platformVersion).block()

            return maybeTips(templateBasePath)
        }

        private fun maybeTips(templateBasePath: Path): String? =
                templateBasePath.resolve("tips.txt")
                        .takeIf { Files.exists(it) }
                        ?.let {
                            Files.newInputStream(it)
                                    .bufferedReader()
                                    .readText()
                        }
    }
}