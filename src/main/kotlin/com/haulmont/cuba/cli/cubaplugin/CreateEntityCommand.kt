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

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleType
import com.haulmont.cuba.cli.ProjectFiles
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.PropertiesAppender
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.updateXml
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.File
import java.io.PrintWriter
import java.nio.file.Path

@Parameters(commandDescription = "Create new entity")
class CreateEntityCommand : GeneratorCommand<EntityModel>() {
    private val writer: PrintWriter by kodein.instance()

    private val namesUtils: NamesUtils by kodein.instance()

    override fun getModelName(): String = EntityModel.MODEL_NAME

    override fun QuestionsList.prompting() {

        val projectModel = context.getModel<ProjectModel>("project")

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
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)
        val entityName = answers["entityName"] as String

        val tableName = buildString {
            append(projectModel.namespace.toUpperCase())
            append("_")
            append(namesUtils.entityNameToTableName(entityName))
        }

        return EntityModel(
                entityName,
                answers["packageName"] as String,
                answers["entityType"] as String,
                tableName
        )
    }

    override fun checkPreconditions() {
        onlyInProject()
    }

    override fun generate(bindings: Map<String, Any>) {
        val entityModel = context.getModel<EntityModel>(EntityModel.MODEL_NAME)

        val projectFiles = ProjectFiles()

        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "entity", bindings) {
            transform("")
        }

        if (entityModel.type == "Not persistent") {
            val metadataXml = projectFiles.getModule(ModuleType.GLOBAL).metadataXml
            addEntityToConfig(metadataXml, "metadata-model", entityModel)
        } else {
            val persistenceXml = projectFiles.getModule(ModuleType.GLOBAL).persistenceXml
            addEntityToConfig(persistenceXml, "persistence-unit", entityModel)
        }

        addToMessages(projectFiles, entityModel)
    }

    private fun addEntityToConfig(configPath: Path, elementName: String, entityModel: EntityModel) {
        updateXml(configPath) {
            elementName {
                add("class") {
                    +(entityModel.packageName + "." + entityModel.name)
                }
            }
        }
    }

    private fun addToMessages(projectFiles: ProjectFiles, entityModel: EntityModel) {
        val packageDirectory = projectFiles.getModule(ModuleType.GLOBAL)
                .src
                .resolve(entityModel.packageName.replace('.', File.separatorChar))

        val entityPrintableName = Regex("([A-Z][a-z0-9]*)")
                .findAll(entityModel.name)
                .map { it.value }
                .joinToString(" ")


        val messages = packageDirectory.resolve("messages.properties")

        PropertiesAppender(messages, writer) {
            append(entityModel.name, entityPrintableName)
        }
    }
}

data class EntityModel(val name: String, val packageName: String, val type: String, val tableName: String) {
    companion object {
        const val MODEL_NAME = "entity"
    }
}

private val entityTypes = listOf("Persistent", "Persistent embedded", "Not persistent")