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

package com.haulmont.cuba.cli.cubaplugin.componentbean

import com.beust.jcommander.Parameters
import com.haulmont.cli.core.Resources
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cli.core.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cli.core.prompting.Answers
import com.haulmont.cli.core.prompting.QuestionsList
import com.haulmont.cuba.cli.getTemplate

@Parameters(commandDescription = "Creates new Spring bean")
class CreateComponentBeanCommand : GeneratorCommand<ComponentBeanModel>(), NonInteractiveInfo {
    private val resources by Resources.fromMyPlugin()

    override fun getModelName(): String = ComponentBeanModel.MODEL_NAME

    override fun preExecute() = checkProjectExistence()

    override fun getNonInteractiveParameters(): Map<String, String> = mapOf(
            "name" to "Class name",
            "packageName" to "Package name",
            "beanName" to "Bean name",
            "module" to "Target module, web or core."
    )

    override fun QuestionsList.prompting() {
        question("name", "Class name") {
            validate {
                checkIsClass()
            }
        }
        question("packageName", "Package name") {
            validate {
                checkIsPackage()
            }
            default(projectModel.rootPackage)
        }
        question("beanName", "Bean name") {
            default { projectModel.namespace + "_" + it["name"] }
        }
        textOptions("module", "Target module", listOf("web", "core"))
    }

    override fun createModel(answers: Answers): ComponentBeanModel = ComponentBeanModel(answers)

    override fun beforeGeneration() {
        projectStructure.getModule(model.module)
                .resolvePackagePath(model.packageName)
                .resolve(model.name + ".java")
                .let {
                    ensureFileAbsence(it, "Bean \"${model.packageName}.${model.name}\" already exists in module ${model.module}")
                }
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(resources.getTemplate("componentBean"), bindings) {
            transformWhole()
        }
    }
}

