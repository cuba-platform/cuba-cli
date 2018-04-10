package com.haulmont.cuba.cli.commands

import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.GeneratorCommand
import com.haulmont.cuba.cli.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import java.io.File
import java.nio.file.Paths

class ProjectInitCommand : GeneratorCommand<ProjectInitModel>() {
    override fun getModelName(): String = "project"


    override fun checkPreconditions(context: CliContext) {
        super.checkPreconditions(context)

        check(!context.hasModel("project")) { "There are existing project found" }
    }

    override fun QuestionsList.prompting(context: CliContext) {
        question("projectName", "Project Name") {
            default { System.getProperty("user.dir").split(File.separatorChar).last() }
        }

        question("namespace", "Project Namespace") {
            default { answers ->
                val notAlphaNumeric = Regex("[^a-zA-Z0-9]")

                notAlphaNumeric.replace(answers["projectName"] as String, "")
            }

            validate {
                checkRegex("[a-zA-Z][a-zA-Z0-9]*", "Project namespace can contain only alphanumeric characters and start with a letter.")
            }
        }

        question("rootPackage", "Root package") {
            default { answers -> "com.company.${answers["namespace"]}" }
        }

        options("platformVersion", "Platform version", availablePlatformVersions)
    }

    override fun createModel(context: CliContext, answers: Answers): ProjectInitModel {
        val rootPackage = answers["rootPackage"] as String
        return ProjectInitModel(
                answers["projectName"] as String,
                answers["namespace"] as String,
                rootPackage,
                answers["platformVersion"] as String,
                rootPackage.replace('.', File.separatorChar)
        )
    }

    override fun beforeGeneration(context: CliContext, bindings: MutableMap<String, Any>) {
        val model = context.getModel<ProjectInitModel>(getModelName())!!
        bindings["rootPackageDirectory"] = model.rootPackageDirectory
    }

    override fun generate(context: CliContext, bindings: Map<String, Any>) {
        TemplateProcessor("templates/project")
                .copyTo(Paths.get(""), bindings)
    }
}

data class ProjectInitModel(val name: String, val namespace: String, val rootPackage: String, val platformVersion: String, val rootPackageDirectory: String)

private val availablePlatformVersions = listOf("6.8.5", "6.9-SNAPSHOT")