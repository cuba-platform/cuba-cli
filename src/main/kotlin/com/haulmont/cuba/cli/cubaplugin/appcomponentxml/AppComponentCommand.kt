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

package com.haulmont.cuba.cli.cubaplugin.appcomponentxml

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.Resources
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.prifexchange.PrefixChanger
import com.haulmont.cuba.cli.generation.Snippets
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.localMessages
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance

@Parameters(commandDescription = "Generates app-component.xml")
class AppComponentCommand : GeneratorCommand<AppComponentModel>(), NonInteractiveInfo {
    private val messages by localMessages()

    private val prefixChanger: PrefixChanger by cubaKodein.instance()

    private val resources by Resources.fromMyPlugin()

    private val snippets: Snippets by lazy {
        Snippets(resources, "appcomponentxml", projectModel.platformVersion)
    }

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun getModelName(): String = AppComponentModel.MODEL_NAME

    override fun getNonInteractiveParameters(): Map<String, String> = mapOf(
            "modulePrefix" to "If specified, changes module prefix for project"
    )

    override fun QuestionsList.prompting() {
        if (isNonInteractiveMode()) {
            askPrefix(projectModel.modulePrefix)
        } else if (projectModel.modulePrefix == "app") {
            confirmation("changePrefix", messages["changePrefix"])

            askPrefix()
        }
    }

    private fun QuestionsList.askPrefix(defaultPrefix: String? = null) {
        question("modulePrefix", "New prefix") {
            askIf("changePrefix")

            defaultPrefix?.let {
                default(it)
            }

            validate {
                if (value.isBlank())
                    fail("Empty project prefix is not allowed")

                val invalidNameRegex = Regex("[^\\w\\-]")

                if (invalidNameRegex.find(value) != null) {
                    fail("Module prefix can contain letters, digits, dashes and underscore characters.")
                }
            }
        }
    }

    override fun createModel(answers: Answers): AppComponentModel {
        val modulePrefix: String = answers["modulePrefix"] as String? ?: projectModel.modulePrefix
        val changePrefix = modulePrefix != projectModel.modulePrefix
        return AppComponentModel(changePrefix, modulePrefix)
    }

    override fun generate(bindings: Map<String, Any>) {
        if (model.changePrefix) {
            prefixChanger.changePrefix(model.modulePrefix)
        }

        TemplateProcessor(resources.getTemplate("appComponent"), bindings) {
            transformWhole()
        }

        addToManifest()
    }

    private fun addToManifest() {
        val buildGradle = projectStructure.buildGradle.toFile()
        val buildGradleText = buildGradle.readText()

        if (Regex(snippets["attributePattern"]).find(buildGradleText) == null) {
            buildGradleText.replace(
                    snippets["searchString"],
                    snippets["replaceString"]
            ).let {
                buildGradle.writeText(it)
            }
        }
    }
}

