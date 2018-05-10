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

import com.beust.jcommander.Parameters
import com.google.common.base.CaseFormat
import com.haulmont.cuba.cli.ModuleStructure.Companion.WEB_MODULE
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

@Parameters(commandDescription = "Creates new screen")
class CreateScreenCommand : GeneratorCommand<ScreenModel>() {
    private val namesUtils: NamesUtils by kodein.instance()

    override fun getModelName(): String = ScreenModel.MODEL_NAME

    override fun preExecute() = checkProjectExistence()

    override fun QuestionsList.prompting() {
        question("screenName", "Screen name") {
            default("screen")
            validate {
                checkRegex("([a-zA-Z]*[a-zA-Z0-9]+)(-[a-zA-Z]*[a-zA-Z0-9]+)*", "Invalid screen name")
            }
        }
        question("packageName", "Package name") {
            default { "${projectModel.rootPackage}.web.screens" }
            validate {
                checkIsPackage()
            }
        }
        confirmation("addToMenu", "Add screen to main menu?") {
            default(true)
        }
    }

    override fun createModel(answers: Answers): ScreenModel = ScreenModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "screen", bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(WEB_MODULE)
        val screensXml = webModule.screensXml

        addToScreensXml(screensXml, model)

        val messages = webModule.src.resolve(namesUtils.packageToDirectory(model.packageName)).resolve("messages.properties")

        PropertiesHelper(messages) {
            set("caption", model.screenName)
        }

        if (model.addToMenu) {
            updateXml(webModule.rootPackageDirectory.resolve("web-menu.xml")) {
                "menu" {
                    add("item") {
                        "screen" mustBe model.screenName
                    }
                }
            }
        }
    }

    private fun addToScreensXml(screensXml: Path, screenModel: ScreenModel) {
        updateXml(screensXml) {
            add("screen") {
                "id" mustBe screenModel.screenName
                "template" mustBe (namesUtils.packageToDirectory(screenModel.packageName) + File.separatorChar + screenModel.screenName + ".xml")
            }
        }
    }
}

class ScreenModel(answers: Answers) {
    val screenName: String by nameFrom(answers)
    val controllerName: String = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, screenName)
    val packageName: String by nameFrom(answers)
    val addToMenu: Boolean by nameFrom(answers)

    companion object {
        const val MODEL_NAME = "screen"
    }
}