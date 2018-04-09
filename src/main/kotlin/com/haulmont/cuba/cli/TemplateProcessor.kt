package com.haulmont.cuba.cli

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeSingleton
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val GREEN = "\u001B[32m"
const val RESET = "\u001B[0m"

class TemplateProcessor(templateFolderPath: String) {

    private val pathVariablePattern: Regex = Regex("\\$\\{[a-zA-Z][0-9a-zA-Z]+(\\.[a-zA-Z][0-9a-zA-Z]*)*}")

    private val templatePath: Path

    init {
        val classLoader = javaClass.classLoader
        val templateUri = classLoader.getResource(templateFolderPath).toURI()

        templatePath = if (templateUri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(templateUri, mutableMapOf<String, Any>(), classLoader)
            fileSystem.getPath(templateFolderPath)
        } else {
            Paths.get(templateUri)
        }
    }

    fun copyTo(path: Path, bindings: Map<String, Any>) {
        Velocity.init()
        val vc = VelocityContext()
        bindings.forEach({ k, v -> vc.put(k, v) })

        val baseTemplatePath = templatePath.toAbsolutePath().toString()
        val targetDirectoryPath = path.toAbsolutePath().toString()

        Files.walk(templatePath, Int.MAX_VALUE)
                .filter { Files.isRegularFile(it) }
                .forEach { inputPath ->
                    val outputFile = inputPath.toAbsolutePath().toString()
                            .replace(baseTemplatePath, targetDirectoryPath)
                            .let { applyPathTransform(it, bindings) }
                            .let { File(it) }

                    ensureFile(outputFile)

                    copy(inputPath, outputFile, vc)
                }
    }

    private fun copy(inputPath: Path, outputFile: File, vc: VelocityContext) {
        val template = Template()
        val runtimeServices = RuntimeSingleton.getRuntimeServices()
        template.setRuntimeServices(runtimeServices)
        template.data = runtimeServices.parse(Files.newInputStream(inputPath).bufferedReader(), inputPath.fileName.toString())
        template.initDocument()

        println("${GREEN}created$RESET ${outputFile.absoluteFile}")

        val writer = outputFile.bufferedWriter()
        template.merge(vc, writer)
        writer.close()
    }

    private fun ensureFile(outputFile: File) {
        outputFile.parentFile.takeIf { !it.exists() }?.mkdirs()
        outputFile.createNewFile()
    }

    private fun applyPathTransform(path: String, bindings: Map<String, Any>): String {
        return pathVariablePattern.replace(path) {
            val paramName = it.value.substring(2, it.value.length - 1)
            bindings[paramName] as String
        }
    }
}