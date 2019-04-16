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

package com.haulmont.cuba.cli.cubaplugin.front.react

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.Messages
import com.haulmont.cuba.cli.PrintHelper
import com.haulmont.cuba.cli.Resources
import com.haulmont.cuba.cli.Resources.Companion.fromMyPlugin
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.front.CreateFrontCommand
import com.haulmont.cuba.cli.generation.Snippets
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import com.haulmont.cuba.cli.walk
import org.kodein.di.Kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files.isRegularFile

@Parameters(commandDescription = "Creates React module")
class CreateReactModuleCommand(override val kodein: Kodein = cubaKodein) : GeneratorCommand<ReactModuleModel>() {

    private val messages = Messages(CreateFrontCommand::class.java)

    private val resources: Resources by fromMyPlugin()

    private val writer: PrintWriter by kodein.instance()

    private val snippets: Snippets by lazy {
        Snippets(resources, "react", projectModel.platformVersion)
    }

    private val printHelper: PrintHelper by kodein.instance()

    override fun getModelName(): String = "react"

    override fun QuestionsList.prompting() {
        confirmation("confirm", messages["reactConfirmationCaption"]) {
            default(true)
        }
    }

    override fun createModel(answers: Answers): ReactModuleModel = if (answers["confirm"] as Boolean) {
        ReactModuleModel(projectModel)
    } else fail("rejected", silent = true)

    override fun generate(bindings: Map<String, Any>) {
        val destinationDir = projectStructure.path.resolve("modules/front")

        val maybeHints = TemplateProcessor(resources.getTemplate("react"), bindings) {
            templatePath.walk().filter {
                it != templatePath && isRegularFile(it)
            }.forEach {
                if (it.fileName.toString().endsWith(".png")) {
                    copy(it, to = destinationDir)
                } else {
                    transform(it, to = destinationDir)
                }
            }
        }

        projectStructure.buildGradle.toFile().apply {
            readText()
                    .replace(snippets["moduleVarSearch"], snippets["moduleVarReplace"])
                    .replace(snippets["moduleConfigureSearch"], snippets["moduleConfigureReplace"])
                    .replace(snippets["undeploySearch"], snippets["undeployReplace"])
                    .let {
                        writeText(it)
                    }
        }
        printHelper.fileModified(projectStructure.buildGradle)


        projectStructure.settingsGradle.toFile().apply {
            val lines = readLines().map {
                if (it.startsWith("include(")) {
                    it.replace(Regex("include\\((.*)\\)")) {
                        val modules = it.groupValues[1].split(",")
                        (modules + "\":\${modulePrefix}-front\"").joinToString(" ,", "include(", ")")
                    }
                } else it
            } + snippets["moduleRegistration"]
            writeText(lines.joinToString("\n"))
        }
        printHelper.fileModified(projectStructure.settingsGradle)

        maybeHints?.let { writer.println(it) }
    }

    override fun preExecute() {
        checkProjectExistence()

        val alreadyContainsModule = projectStructure.settingsGradle.toFile()
                .readText().contains("front")

        !alreadyContainsModule || fail(messages["moduleExistsError"])
    }
}