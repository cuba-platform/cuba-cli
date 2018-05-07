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

package com.haulmont.cuba.cli.cubaplugin

import com.haulmont.cuba.cli.*
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.PropertiesHelper
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.nio.file.Path
import java.nio.file.Paths

class AppComponentCommand : GeneratorCommand<AppComponentModel>() {
    private val messages: Messages = Messages(javaClass)

    private val printHelper: PrintHelper by kodein.instance()

    override fun getModelName(): String = AppComponentModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)

        if (projectModel.modulePrefix == "app") {
            confirmation("changePrefix", messages.getMessage("appComponent.changePrefix"))

            question("modulePrefix", "New prefix") {
                askIf("changePrefix")

                validate {
                    val invalidNameRegex = Regex("[^\\w\\-]")

                    if (invalidNameRegex.find(value) != null) {
                        fail("Project name should contain only Latin letters, digits, dashes and underscores.")
                    }
                }
            }
        }
    }

    override fun createModel(answers: Answers): AppComponentModel {
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)

        val modulePrefix: String = answers["modulePrefix"] as String? ?: projectModel.modulePrefix
        val changePrefix = answers["changePrefix"] as Boolean? ?: false
        return AppComponentModel(changePrefix, modulePrefix)
    }

    override fun generate(bindings: Map<String, Any>) {
        val componentModel = context.getModel<AppComponentModel>(AppComponentModel.MODEL_NAME)

        if (componentModel.changePrefix) {
            changePrefix(componentModel.modulePrefix)
        }

        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "appComponent", bindings) {
            transformWhole()
        }

        addToManifest()
    }

    private fun addToManifest() {
        val buildGradle = Paths.get("build.gradle").toFile()
        val buildGradleText = buildGradle.readText()

        if (Regex(messages.getMessage("appComponent.addToManifest.attributePattern")).find(buildGradleText) == null) {
            buildGradleText.replace(
                    messages.getMessage("appComponent.addToManifest.searchString"),
                    messages.getMessage("appComponent.addToManifest.replaceString").replace("\t", "    ")
            ).let {
                buildGradle.writeText(it)
            }
        }
    }

    private fun changePrefix(prefix: String) {
        replacePrefix(Paths.get("build.gradle"), prefix)
        replacePrefix(Paths.get("settings.gradle"), prefix)

        val projectFiles = ProjectFiles()

        val webAppProperties = projectFiles.getModule(ModuleType.WEB)
                .rootPackageDirectory
                .resolve("web-app.properties")

        PropertiesHelper(webAppProperties) {
            update("cuba.connectionUrlList") {
                it?.replaceAfterLast('/', "$prefix-core") ?: "http://localhost:8080/$prefix-core"
            }
            set("cuba.webContextName", prefix)
        }

        val appProperties = projectFiles.getModule(ModuleType.CORE)
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

        printHelper.fileAltered(gradleScriptPath)
    }

    override fun checkPreconditions() = onlyInProject()
}

data class AppComponentModel(val changePrefix: Boolean, val modulePrefix: String) {
    companion object {
        const val MODEL_NAME = "component"
    }
}