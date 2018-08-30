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
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.ScreenCommandBase
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList

@Parameters(commandDescription = "Extends login and main screens")
class ExtendDefaultScreenCommand : ScreenCommandBase<ScreenExtensionModel>() {
    override fun getModelName(): String = ScreenExtensionModel.MODEL_NAME

    override fun preExecute() = checkProjectExistence()

    override fun QuestionsList.prompting() {
        options("screen", "Which screen to extend?", listOf("login", "main"))
        question("packageName", "Package name") {
            default(projectModel.rootPackage + ".web.screens")
        }
        question("screenId", "Screen with default id already exists. Specify new id.") {
            askIf {
                if (it["screen"] == "login") {
                    screenRegistrationHelper.isScreenIdExists("loginWindow")
                } else {
                    screenRegistrationHelper.isScreenIdExists("mainWindow")
                }
            }
            validate {
                screenIdDoesNotExists(value)
            }
        }

        question("descriptorName", "Descriptor name") {
            askIf {
                if (it["screen"] == "login") {
                    screenRegistrationHelper.isDescriptorExists(it["packageName"] as String, "ext-loginWindow")
                } else {
                    screenRegistrationHelper.isDescriptorExists(it["packageName"] as String, "ext-mainwindow")
                }
            }
            validate {
                checkIsScreenDescriptor()
                screenDescriptorDoesNotExists(value)
            }
        }

        question("controllerName", "Controller name") {
            askIf {
                if (it["screen"] == "login") {
                    screenRegistrationHelper.isControllerExists(it["packageName"] as String, "ExtAppLoginWindow")
                } else {
                    screenRegistrationHelper.isDescriptorExists(it["packageName"] as String, "ExtAppMainWindow")
                }
            }
            validate {
                checkIsClass()
                screenControllerDoesNotExists(value)
            }
        }
    }

    override fun createModel(answers: Answers): ScreenExtensionModel = ScreenExtensionModel(answers)

    override fun beforeGeneration() {
        checkScreenId(model.id)
        checkExistence(model.packageName, descriptor = model.screen)
    }

    override fun generate(bindings: Map<String, Any>) {
        val templatePath = CubaPlugin.TEMPLATES_BASE_PATH + "screenExtension/" + model.screen
        TemplateProcessor(templatePath, bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(WEB_MODULE)

        addToScreensXml(model.id, model.packageName, model.descriptorName)

        val messages = webModule.resolvePackagePath(model.packageName).resolve("messages.properties")

        Properties(messages).save()
    }
}

