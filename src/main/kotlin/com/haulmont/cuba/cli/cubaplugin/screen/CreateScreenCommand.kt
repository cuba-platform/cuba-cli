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

package com.haulmont.cuba.cli.cubaplugin.screen

import com.beust.jcommander.Parameters
import com.google.common.base.CaseFormat
import com.haulmont.cuba.cli.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.ScreenCommandBase
import com.haulmont.cuba.cli.generation.PropertiesHelper
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList

@Parameters(commandDescription = "Creates new screen")
class CreateScreenCommand : ScreenCommandBase<ScreenModel>() {
    override fun getModelName(): String = ScreenModel.MODEL_NAME

    override fun preExecute() = checkProjectExistence()

    override fun QuestionsList.prompting() {
        question("descriptorName", "Descriptor name") {
            default("screen")
            validate {
                checkIsScreenDescriptor()
            }
        }
        question("controllerName", "Controller name") {
            default {
                CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, it["descriptorName"] as String)
            }
            validate {
                checkIsClass()
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

    override fun beforeGeneration() {
        screenRegistrationHelper.checkScreenId(model.descriptorName)
        screenRegistrationHelper.checkExistence(model.packageName, model.descriptorName, model.controllerName)
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "screen", bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(WEB_MODULE)

        screenRegistrationHelper.addToScreensXml(model.descriptorName, model.packageName, model.descriptorName)

        val messages = webModule.resolvePackagePath(model.packageName).resolve("messages.properties")

        PropertiesHelper(messages) {
            set("caption", model.descriptorName)
        }

        if (model.addToMenu) {
            val caption = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, model.controllerName).replace('_', ' ')
            addToMenu(webModule.rootPackageDirectory.resolve("web-menu.xml"), model.descriptorName, caption)
        }
    }
}

