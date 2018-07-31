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

package com.haulmont.cuba.cli.cubaplugin.enumeration

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleStructure
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.generation.PropertiesHelper
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList

@Parameters(commandDescription = "Creates new enumeration")
class CreateEnumerationCommand : GeneratorCommand<EnumerationModel>() {
    override fun getModelName(): String = EnumerationModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        question("className", "Class name") {
            validate {
                checkIsClass()
            }
        }

        question("packageName", "Package name") {
            default(projectModel.rootPackage + ".entity")

            validate {
                checkIsPackage()

                value.startsWith(projectModel.rootPackage) || fail("Package should be inside root package: ${projectModel.rootPackage}")
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
                        val values: List<Answers> by it
                        if (values.size > 1) {
                            val lastValue = values[values.lastIndex - 1]
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

    override fun beforeGeneration() {
        projectStructure.getModule(ModuleStructure.GLOBAL_MODULE)
                .resolvePackagePath(model.packageName)
                .resolve(model.className + ".java")
                .let {
                    ensureFileAbsence(it, "Enumeration \"${model.packageName}.${model.className}\" already exists")
                }
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "enumeration", bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val packageDirectory = projectStructure.getModule(ModuleStructure.GLOBAL_MODULE)
                .resolvePackagePath(model.packageName)

        val enumerationPrintableName = Regex("([A-Z][a-z0-9]*)")
                .findAll(model.className)
                .map { it.value }
                .joinToString(" ")


        val messages = packageDirectory.resolve("messages.properties")

        PropertiesHelper(messages) {
            set(model.className, enumerationPrintableName)
            model.values.forEach {
                set("${model.className}.${it.name}", it.name.replace('_', ' ').toLowerCase().capitalize().trim())
            }
        }
    }
}

