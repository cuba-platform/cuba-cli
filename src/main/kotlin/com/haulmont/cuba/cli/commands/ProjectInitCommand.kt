package com.haulmont.cuba.cli.commands

import com.beust.jcommander.Parameter
import com.haulmont.cuba.cli.*
import java.io.File
import java.nio.file.Paths

class ProjectInitCommand : CliCommand {

    @Parameter(names = ["--help"], help = true)
    var help: Boolean = false

    override fun name(): String = "init"

    override fun run() {

        if (help) {
            printHelp()
            return
        }

        generateProject()
    }

    private fun printHelp() {
        println("New CUBA platform project generation")
    }

    private fun generateProject() {
        val notAlphaNumeric = Regex("[^a-zA-Z0-9]")
        val namespaceRegex = Regex("[a-zA-Z][a-zA-Z0-9]*")

        val answers = PromptsBuilder(System.`in`, System.out)
                .addQuestion(
                        name = "projectName",
                        caption = "Project Name",
                        defaultValue = PlainValue(getDefaultProjectName()))
                .addQuestion(
                        name = "namespace",
                        caption = "Project Namespace",
                        defaultValue = CalculatedValue { answers -> notAlphaNumeric.replace(answers["projectName"]!!, "") },
                        validation = {
                            it.takeIf { it.matches(namespaceRegex) }
                                    ?: throw ValidationException("Project namespace can contain only alphanumeric characters and start with a letter.")

                        })
                .addQuestion(
                        name = "rootPackage",
                        caption = "Root package",
                        defaultValue = CalculatedValue { answers -> "com.company.${answers["namespace"]}" })
                .addListQuestion(
                        name = "platformVersion",
                        caption = "Platform Version",
                        options = listOf("6.8.5", "6.9-SNAPSHOT"))
                .addSilentQuestion(
                        name = "packageDirectoryName",
                        defaultValue = CalculatedValue { answers ->
                            answers["rootPackage"]!!.replace('.', File.separatorChar)
                        }
                ).build()
                .ask()

        TemplateProcessor("templates/project")
                .copyTo(Paths.get(""), answers)
    }
}

fun getDefaultProjectName(): String = System.getProperty("user.dir").split(File.separatorChar).last()