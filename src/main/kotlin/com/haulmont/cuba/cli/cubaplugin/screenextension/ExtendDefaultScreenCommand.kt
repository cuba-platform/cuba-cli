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

package com.haulmont.cuba.cli.cubaplugin.screenextension

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.generation.*
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.nio.file.Path

@Parameters(commandDescription = "Extends login and main screens")
class ExtendDefaultScreenCommand : GeneratorCommand<ScreenExtensionModel>() {

    private val namesUtils: NamesUtils by kodein.instance()

    override fun getModelName(): String = ScreenExtensionModel.MODEL_NAME

    override fun preExecute() = checkProjectExistence()

    override fun QuestionsList.prompting() {
        options("screen", "Which screen to extend?", listOf("login", "main"))
        question("packageName", "Package name") {
            default(projectModel.rootPackage + ".web.screens")
        }
    }

    override fun createModel(answers: Answers): ScreenExtensionModel = ScreenExtensionModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        val templatePath = CubaPlugin.TEMPLATES_BASE_PATH + "screenExtension/" + model.screen
        TemplateProcessor(templatePath, bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(WEB_MODULE)
        val screensXml = webModule.screensXml

        addToScreensXml(screensXml)

        val messages = webModule.src.resolve(namesUtils.packageToDirectory(model.packageName)).resolve("messages.properties")

        PropertiesHelper(messages) {}
    }

    private fun addToScreensXml(screensXml: Path) {
        updateXml(screensXml) {
            appendChild("screen") {
                if (model.screen == "login") {
                    this["id"] = "loginWindow"
                    this["template"] = (namesUtils.packageToDirectory(model.packageName) + '/' + "ext-loginWindow.xml")
                } else {
                    this["id"] = "mainWindow"
                    this["template"] = (namesUtils.packageToDirectory(model.packageName) + '/' + "ext-mainwindow.xml")
                }
            }
        }
    }
}

