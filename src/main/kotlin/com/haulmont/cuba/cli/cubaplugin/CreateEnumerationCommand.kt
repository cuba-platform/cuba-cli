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
import com.haulmont.cuba.cli.commands.nameFrom
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList

class CreateEnumerationCommand : GeneratorCommand<EnumerationModel>() {
    override fun getModelName(): String = EnumerationModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)


        question("className", "Class name") {
            validate {
                checkIsClass()
            }
        }

        question("packageName", "Package name") {
            default(projectModel.rootPackage + ".entity")

            validate {
                checkIsPackage()
            }
        }

        options("idType", "Id type", listOf("String", "Integer"))

        repeating("values", "Add value?") {
            question("name", "Value name")
            question("id", "Id") {
                default {
                    if (it["idType"] == "String")
                        ""
                    else {
                        val filledValues: List<Answers> = "values" from it
                        if (filledValues.size > 1) {
                            val lastValue = filledValues[filledValues.lastIndex - 1]
                            val prevId = (lastValue["id"] as String).toInt()
                            (prevId + 10).toString()
                        } else 10.toString()
                    }
                }

                validate {
                    if (answers["idType"] == "Integer") {
                        checkIsInt()
                    }
                }
            }
        }
    }

    override fun createModel(answers: Answers): EnumerationModel = EnumerationModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "enumeration", bindings) {
            transformWhole()
        }
    }
}

class EnumerationModel(answers: Answers) {
    val className: String by nameFrom(answers)
    val packageName: String by nameFrom(answers)
    val idType: String by nameFrom(answers)
    val values: List<EnumValue> = (answers["values"] as List<Answers>).map(::EnumValue)

    companion object {
        const val MODEL_NAME = "enumeration"
    }
}

class EnumValue(mapRepresentation: Answers) {
    val name: String by nameFrom(mapRepresentation)
    val id: String by nameFrom(mapRepresentation)
}