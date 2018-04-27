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

import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList

class CreateComponentBeanCommand : GeneratorCommand<ComponentBeanModel>() {
    override fun getModelName(): String = ComponentBeanModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)

        question("name", "Class name") {
            validate {
                checkIsClass()
            }
        }
        question("package", "Package name") {
            validate {
                checkIsPackage()
            }
            default(projectModel.rootPackage)
        }
        question("beanName", "Bean name") {
            default { projectModel.namespace + "_" + it["name"] }
        }
        options("module", "Target module", listOf("web", "core"))
    }

    override fun createModel(answers: Answers): ComponentBeanModel =
            ComponentBeanModel(
                    "name" from answers,
                    "module" from answers,
                    "package" from answers)

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "componentBean", bindings) {
            transformWhole()
        }
    }

    override fun checkPreconditions() {
        onlyInProject()
    }
}

data class ComponentBeanModel(val name: String, val module: String, val packageName: String) {
    companion object {
        const val MODEL_NAME = "component"
    }
}