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

package com.haulmont.cuba.cli.cubaplugin.installcomponent

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.Messages
import com.haulmont.cuba.cli.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.PrintHelper
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.generation.updateXml
import com.haulmont.cuba.cli.generation.xpath
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.nio.file.Paths

@Parameters(commandDescription = "Installs CUBA platform component")
class ComponentInstallCommand : GeneratorCommand<ComponentModel>() {

    private val messages = Messages(javaClass)

    private val printHelper: PrintHelper by kodein.instance()

    override fun getModelName(): String = ComponentModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        question("artifactCoordinates", messages["artifactCoordinatesQuestionCaption"]) {
            validate {
                value.split(':').size == 3 || fail(messages["invalidArtifactCoordinates"])
            }
        }
    }

    override fun createModel(answers: Answers): ComponentModel = ComponentModel("artifactCoordinates" from answers)

    override fun generate(bindings: Map<String, Any>) {
        registerInGradle()
        registerInWebXml()
    }

    private fun registerInGradle() {
        projectStructure.buildGradle.toFile().apply {
            val text = readText()
            val firstAppComponent = Regex("appComponent\\([^\n]*\\)")
                    .find(text)!!
                    .groupValues[0]

            val withNewComponent = text.replace(
                    firstAppComponent,
                    "$firstAppComponent\n    appComponent(\"${model.artifactCoordinates}\")")
            writeText(withNewComponent)
        }
        printHelper.fileModified(projectStructure.buildGradle)
    }

    private fun registerInWebXml() {
        val webXml = projectStructure.getModule(WEB_MODULE).path
                .resolve(Paths.get("web", "WEB-INF", "web.xml"))
        updateXml(webXml) {
            val registeredComponentsElement = xpath("//context-param[param-name[text()='appComponents']]/param-value").first()
            registeredComponentsElement.textContent = registeredComponentsElement.textContent + " " + model.artifactCoordinates.split(':')[0]
        }
    }
}