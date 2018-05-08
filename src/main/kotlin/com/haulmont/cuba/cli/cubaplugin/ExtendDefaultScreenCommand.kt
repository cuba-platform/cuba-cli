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

import com.haulmont.cuba.cli.ModuleType
import com.haulmont.cuba.cli.ProjectFiles
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.nameFrom
import com.haulmont.cuba.cli.generation.PropertiesHelper
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.updateXml
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.File
import java.nio.file.Path

class ExtendDefaultScreenCommand : GeneratorCommand<ScreenExtensionModel>() {

    private val namesUtils: NamesUtils by kodein.instance()

    override fun getModelName(): String = ScreenExtensionModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        options("screen", "Which screen to extend?", listOf("login", "main"))
        question("packageName", "Package name") {
            default(projectModel.rootPackage + ".web.screens")
        }
    }

    override fun createModel(answers: Answers): ScreenExtensionModel = ScreenExtensionModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        val screenModel = context.getModel<ScreenExtensionModel>(ScreenExtensionModel.MODEL_NAME)

        val templatePath = CubaPlugin.TEMPLATES_BASE_PATH + "screenExtension/" + screenModel.screen
        TemplateProcessor(templatePath, bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val webModule = ProjectFiles().getModule(ModuleType.WEB)
        val screensXml = webModule.screensXml

        addToScreensXml(screensXml, screenModel)

        val messages = webModule.src.resolve(namesUtils.packageToDirectory(screenModel.packageName)).resolve("messages.properties")

        PropertiesHelper(messages) {}
    }

    private fun addToScreensXml(screensXml: Path, screenModel: ScreenExtensionModel) {
        updateXml(screensXml) {
            add("screen") {
                if (screenModel.screen == "login") {
                    "id" mustBe "loginWindow"
                    "template" mustBe (namesUtils.packageToDirectory(screenModel.packageName) + File.separatorChar + "ext-loginWindow.xml")
                } else {
                    "id" mustBe "mainWindow"
                    "template" mustBe (namesUtils.packageToDirectory(screenModel.packageName) + File.separatorChar + "ext-mainwindow.xml")
                }
            }
        }
    }

    override fun checkPreconditions() = onlyInProject()
}

class ScreenExtensionModel(answers: Answers) {
    val screen: String by nameFrom(answers)
    val packageName: String by nameFrom(answers)

    companion object {
        const val MODEL_NAME = "screen"
    }
}
