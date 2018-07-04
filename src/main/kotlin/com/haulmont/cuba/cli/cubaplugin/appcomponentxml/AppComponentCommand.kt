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
import com.haulmont.cuba.cli.ModuleStructure.Companion.CORE_MODULE
import com.haulmont.cuba.cli.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.PrintHelper
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.generation.PropertiesHelper
import com.haulmont.cuba.cli.generation.Snippets
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.localMessages
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.nio.file.Path

@Parameters(commandDescription = "Generates app-component.xml")
class AppComponentCommand : GeneratorCommand<AppComponentModel>() {
    private val messages by localMessages()

    private val printHelper: PrintHelper by kodein.instance()

    private val snippets: Snippets by lazy {
        Snippets(CubaPlugin.SNIPPETS_BASE_PATH + "appcomponentxml", javaClass, projectModel.platformVersion)
    }

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun getModelName(): String = AppComponentModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        if (projectModel.modulePrefix == "app") {
            confirmation("changePrefix", messages["changePrefix"])

            question("modulePrefix", "New prefix") {
                askIf("changePrefix")

                validate {
                    if (value.isBlank())
                        fail("Empty project prefix is not allowed")

                    val invalidNameRegex = Regex("[^\\w\\-]")

                    if (invalidNameRegex.find(value) != null) {
                        fail("Project name should contain only Latin letters, digits, dashes and underscores.")
                    }
                }
            }
        }
    }

    override fun createModel(answers: Answers): AppComponentModel {
        val modulePrefix: String = answers["modulePrefix"] as String? ?: projectModel.modulePrefix
        val changePrefix = answers["changePrefix"] as Boolean? ?: false
        return AppComponentModel(changePrefix, modulePrefix)
    }

    override fun generate(bindings: Map<String, Any>) {
        if (model.changePrefix) {
            changePrefix(model.modulePrefix)
        }

        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "appComponent", bindings, projectModel.platformVersion) {
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

    private fun changePrefix(prefix: String) {
        replacePrefix(projectStructure.buildGradle, prefix)
        replacePrefix(projectStructure.settingsGradle, prefix)

        val webAppProperties = projectStructure.getModule(WEB_MODULE)
                .rootPackageDirectory
                .resolve("web-app.properties")

        PropertiesHelper(webAppProperties) {
            update("cuba.connectionUrlList") {
                it?.replaceAfterLast('/', "$prefix-core") ?: "http://localhost:8080/$prefix-core"
            }
            set("cuba.webContextName", prefix)
        }

        val appProperties = projectStructure.getModule(CORE_MODULE)
                .rootPackageDirectory
                .resolve("app.properties")

        PropertiesHelper(appProperties) {
            set("cuba.webContextName", "$prefix-core")
        }
    }

    private fun replacePrefix(gradleScriptPath: Path, prefix: String) {
        val modulePrefixRegex = Regex("def *modulePrefix *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")

        val scriptFile = gradleScriptPath.toFile()

        scriptFile.readText()
                .replace(modulePrefixRegex, "def modulePrefix = \"$prefix\"")
                .let { scriptFile.writeText(it) }

        printHelper.fileModified(gradleScriptPath)
    }
}

