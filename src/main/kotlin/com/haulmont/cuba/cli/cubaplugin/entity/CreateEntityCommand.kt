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

package com.haulmont.cuba.cli.cubaplugin.entity

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleStructure.Companion.GLOBAL_MODULE
import com.haulmont.cuba.cli.ProjectStructure
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.generation.*
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.File
import java.nio.file.Path

@Parameters(commandDescription = "Create new entity")
class CreateEntityCommand : GeneratorCommand<EntityModel>() {
    private val entityTypes = listOf("Persistent", "Persistent embedded", "Not persistent")

    private val namesUtils: NamesUtils by kodein.instance()

    override fun getModelName(): String = EntityModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        question("entityName", "Entity Name") {
            validate {
                checkRegex("\\b[A-Z]+[\\w\\d_$]*", "Invalid entity name")
            }
        }

        question("packageName", "Package Name") {
            default { "${projectModel.rootPackage}.entity" }
            validate {
                checkIsPackage()
            }
        }

        options("entityType", "Entity type", entityTypes) {
            default(0)
        }
    }

    override fun createModel(answers: Answers): EntityModel {
        val entityName = answers["entityName"] as String

        val tableName = buildString {
            append(projectModel.namespace.toUpperCase())
            append("_")
            append(namesUtils.entityNameToTableName(entityName))
        }

        return EntityModel(
                entityName,
                "packageName" from answers,
                "entityType" from answers,
                tableName
        )
    }

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "entity", bindings) {
            transformWhole()
        }

        if (model.type == "Not persistent") {
            val metadataXml = projectStructure.getModule(GLOBAL_MODULE).metadataXml
            addEntityToConfig(metadataXml, "metadata-model", model)
        } else {
            val persistenceXml = projectStructure.getModule(GLOBAL_MODULE).persistenceXml
            addEntityToConfig(persistenceXml, "persistence-unit", model)
        }

        addToMessages(projectStructure)
    }

    private fun addEntityToConfig(configPath: Path, elementName: String, model: EntityModel) {
        updateXml(configPath) {
            val configElement = findFirstChild(elementName) ?: appendChild(elementName)
            configElement.appendChild("class") {
                textContent = model.packageName + "." + model.name
            }
        }
    }

    private fun addToMessages(projectStructure: ProjectStructure) {
        val packageDirectory = projectStructure.getModule(GLOBAL_MODULE)
                .src
                .resolve(model.packageName.replace('.', File.separatorChar))

        val entityPrintableName = Regex("([A-Z][a-z0-9]*)")
                .findAll(model.name)
                .map { it.value }
                .joinToString(" ")


        val messages = packageDirectory.resolve("messages.properties")

        PropertiesHelper(messages) {
            set(model.name, entityPrintableName)
        }
    }
}