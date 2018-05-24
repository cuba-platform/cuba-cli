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

package com.haulmont.cuba.cli.cubaplugin.browsescreen

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleStructure
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.generation.*
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import net.sf.practicalxml.DomUtil
import org.kodein.di.generic.instance

@Parameters(commandDescription = "Creates new browse screen")
class CreateBrowseScreenCommand : GeneratorCommand<BrowseScreenModel>() {
    private val namesUtils: NamesUtils by kodein.instance()

    override fun getModelName(): String = BrowseScreenModel.MODEL_NAME

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun QuestionsList.prompting() {
        val persistenceXml = projectStructure.getModule(ModuleStructure.GLOBAL_MODULE).persistenceXml
        val entitiesList = parse(persistenceXml).documentElement
                .let { DomUtil.getChild(it, "persistence-unit") }
                .getChildElements()
                .filter { element -> element.tagName == "class" }
                .map { it.textContent.trim() }

        if (entitiesList.isEmpty())
            fail("Project does not have any entities.")

        options("entityName", "Choose entity", entitiesList)

        question("packageName", "Package name") {
            validate {
                checkIsPackage()
            }

            default { answers ->
                val entityName: String by answers

                val packageParts = entityName.split('.')
                packageParts.take(packageParts.lastIndex).joinToString(".") + ".web." + packageParts.last().toLowerCase()
            }
        }

        question("screenName", "Screen name") {
            default { answers ->
                val entityName: String by answers

                projectModel.namespace + "$" + entityName.split('.').last() + ".browse"
            }
        }
    }

    override fun createModel(answers: Answers): BrowseScreenModel = BrowseScreenModel(answers)

    override fun beforeGeneration() {
        val webModule = projectStructure.getModule(ModuleStructure.WEB_MODULE)
        val screensXml = webModule.screensXml

        parse(screensXml).documentElement
                .xpath("//screen[id=\"${model.screenName}\"]")
                .firstOrNull()?.let {
                    fail("Screen with id ${model.screenName} already exists")
                }
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "browseScreen", bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(ModuleStructure.WEB_MODULE)
        val screensXml = webModule.screensXml

        updateXml(screensXml) {
            appendChild("screen") {
                this["id"] = model.screenName
                val template = namesUtils.packageToDirectory(model.packageName) + '/' + model.screenName + ".xml"
                this["template"] = template
            }
        }

        val messages = webModule.src.resolve(namesUtils.packageToDirectory(model.packageName)).resolve("messages.properties")

        PropertiesHelper(messages) {
            set("browseCaption", model.entityName + " browser")
        }

        updateXml(webModule.rootPackageDirectory.resolve("web-menu.xml")) {
            val menuItem = findFirstChild("menu") ?: appendChild("menu")

            menuItem.appendChild("item") {
                this["screen"] = model.screenName
            }
        }

        val menuMessages = webModule.src.resolve(namesUtils.packageToDirectory(projectModel.rootPackage)).resolve("messages.properties")
        PropertiesHelper(menuMessages) {
            set("menu-config.${model.screenName}", "${model.entityName}s")
        }
    }

}