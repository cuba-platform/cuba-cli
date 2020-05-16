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

package com.haulmont.cuba.cli.cubaplugin.screen.screenextension

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.core.Resources
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.cubaplugin.screen.ScreenCommandBase
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.core.prompting.Answers
import com.haulmont.cuba.cli.core.prompting.Option
import com.haulmont.cuba.cli.core.prompting.QuestionsList
import com.haulmont.cuba.cli.getTemplate

@Parameters(commandDescription = "Extends login and main screens")
class ExtendDefaultScreenCommand : ScreenCommandBase<ScreenExtensionModel>() {
    private val resources by Resources.fromMyPlugin()

    private val screens by lazy {

        val options: MutableList<ScreenToExtend> = mutableListOf()

        if (projectModel.platformVersion < PlatformVersion.v7_1) {
            options.add(ScreenToExtend("loginWindow", "Login screen", "login", "ExtAppLoginWindow", "ext-loginWindow", Type.LOGIN))
            options.add(ScreenToExtend("mainWindow", "Main screen with top menu", "mainTop", "ExtAppMainWindow", "ext-mainwindow", Type.MAIN))
        } else {
            options.add(ScreenToExtend("login", "Login screen", "login", "ExtLoginScreen", "ext-login-screen", Type.LOGIN))
            options.add(ScreenToExtend("topMenuMainScreen", "Main screen with top menu", "mainTop", "ExtMainScreen", "ext-main-screen", Type.MAIN))
            options.add(ScreenToExtend("main", "Main screen with side menu", "mainSide", "ExtMainScreen", "ext-main-screen", Type.MAIN))
        }

        return@lazy options.map(::ScreenToExtendOption)
    }

    override fun getModelName(): String = ScreenExtensionModel.MODEL_NAME

    override fun preExecute() = checkProjectExistence()

    override fun QuestionsList.prompting() {
        options("screen", "Which screen to extend?", screens)
        question("packageName", "Package name") {
            default(projectModel.rootPackage + ".web.screens")
        }
        question("screenId", "Screen with default id already exists. Specify new id.") {
            askIf { answers ->
                if (projectModel.platformVersion >= PlatformVersion.v7_1)
                    return@askIf false

                val screen: ScreenToExtend by answers

                screenRegistrationHelper.isScreenIdExists(screen.defaultId)
            }
            validate {
                screenIdDoesNotExists(value)
            }
        }

        question("descriptorName", "Descriptor name") {
            askIf { answers ->
                val screen: ScreenToExtend by answers

                screenRegistrationHelper.isDescriptorExists(answers["packageName"] as String, screen.defaultDescriptor)
            }
            validate {
                checkIsScreenDescriptor()
                screenDescriptorDoesNotExists(value)
            }
        }

        question("controllerName", "Controller name") {
            askIf { answers ->
                val screen: ScreenToExtend by answers

                screenRegistrationHelper.isControllerExists(answers["packageName"] as String, screen.defaultController)
            }
            validate {
                checkIsClass()
                screenControllerDoesNotExists(value)
            }
        }
    }

    override fun createModel(answers: Answers): ScreenExtensionModel {
        return ScreenExtensionModel(answers)
    }

    override fun beforeGeneration() {
        checkScreenId(model.id)
        checkExistence(model.packageName, descriptor = model.descriptorName)
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(resources.getTemplate("screenExtension/" + model.screen.path), bindings) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(WEB_MODULE)

        if (projectModel.platformVersion < PlatformVersion.v7_1) {
            addToScreensXml(model.id, model.packageName, model.descriptorName)
        } else if (model.screen.type == Type.MAIN) {
            registerMainScreen()
        }

        val messages = webModule.resolvePackagePath(model.packageName).resolve("messages.properties")

        Properties(messages).save()
    }

    private fun registerMainScreen() {
        val webAppProperties = projectStructure.getModule(WEB_MODULE)
                .rootPackageDirectory
                .resolve("web-app.properties")

        Properties.modify(webAppProperties) {
            if (model.id == "main") {
                remove("cuba.web.mainScreenId")
            } else {
                this["cuba.web.mainScreenId"] = "topMenuMainScreen"
            }
        }
    }

    internal enum class Type {
        LOGIN, MAIN
    }

    internal data class ScreenToExtend(val defaultId: String, val name: String, val path: String, val defaultController: String, val defaultDescriptor: String, val type: Type)

    private class ScreenToExtendOption(screenToExtend: ScreenToExtend) : Option<ScreenToExtend>("", screenToExtend.name, screenToExtend)
}

