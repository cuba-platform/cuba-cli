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
import com.haulmont.cuba.cli.*
import com.haulmont.cuba.cli.ModuleStructure.Companion.CORE_MODULE
import com.haulmont.cuba.cli.ModuleStructure.Companion.GLOBAL_MODULE
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.generation.*
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Parameters(commandDescription = "Create new entity")
class CreateEntityCommand : GeneratorCommand<EntityModel>() {
    private val entityTypes = listOf("Persistent", "Persistent embedded", "Not persistent")

    private val namesUtils: NamesUtils by kodein.instance()

    private val printHelper: PrintHelper by kodein.instance()

    private val printWriter: PrintWriter by kodein.instance()

    private val messages: Messages by localMessages()

    private val snippets by lazy {
        Snippets(CubaPlugin.SNIPPETS_BASE_PATH + "entity", javaClass, projectModel.platformVersion)
    }

    private val calendar = Calendar.getInstance()

    override fun getModelName(): String = EntityModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        question("entityName", "Entity Name") {
            validate {
                checkIsClass("Invalid entity name. Entity name should match UpperCamelCase, for example NewEntity.")
            }
        }

        question("packageName", "Package Name") {
            default { "${projectModel.rootPackage}.entity" }
            validate {
                checkIsPackage()

                value.startsWith(projectModel.rootPackage) || fail("Package should be inside root package: ${projectModel.rootPackage}")
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

    override fun beforeGeneration() {
        val metadataXml = projectStructure.getModule(GLOBAL_MODULE).metadataXml
        val persistenceXml = projectStructure.getModule(GLOBAL_MODULE).persistenceXml

        listOf(metadataXml, persistenceXml).forEach {
            val entityFullName = model.packageName + "." + model.name

            parse(it).documentElement
                    .xpath("//class[text()=\"$entityFullName\"]")
                    .firstOrNull()?.let {
                        fail("Entity $entityFullName already exists")
                    }
        }
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "entity", bindings) {
            transformWhole()
        }

        if (model.type == "Not persistent") {
            val metadataXml = projectStructure.getModule(GLOBAL_MODULE).metadataXml
            addEntityToConfig(metadataXml, "metadata-model")
        } else {
            val persistenceXml = projectStructure.getModule(GLOBAL_MODULE).persistenceXml
            addEntityToConfig(persistenceXml, "persistence-unit")
        }

        addToMessages(projectStructure)

        if (model.type == "Persistent") {
            createSqlScripts()
        }
    }

    private fun addEntityToConfig(configPath: Path, elementName: String) {
        updateXml(configPath) {
            val configElement = findFirstChild(elementName) ?: appendChild(elementName)
            configElement.appendChild("class") {
                textContent = model.packageName + "." + model.name
            }
        }
    }

    private fun createSqlScripts() {
        val script = snippets["${projectModel.database.type}CreateTable"].format(model.tableName)

        val dbPath = projectStructure.getModule(CORE_MODULE).path.resolve("db")

        val createDbPath = dbPath.resolve("init", projectModel.database.type, "10.create-db.sql")

        if (Files.exists(createDbPath)) {
            createDbPath.toFile().appendText(script)
            printHelper.fileModified(createDbPath)
        } else {
            printWriter.println(messages["createDbNotFound"])
        }

        val currentYearUpdateDir = dbPath.resolve("update", projectModel.database.type, getYear())
        if (!Files.exists(currentYearUpdateDir)) {
            Files.createDirectories(currentYearUpdateDir)
        }

        val todayPrefix = getTodayPrefix()

        val todayScriptsCount = Files.walk(currentYearUpdateDir, 1)
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".sql") && it.fileName.toString().startsWith(todayPrefix) }
                .count()

        val scriptName = "$todayPrefix${1 + todayScriptsCount}-create${model.name}.sql"

        val updateScriptPath = currentYearUpdateDir.resolve(scriptName)
        updateScriptPath.toFile().also { it.createNewFile() }.writeText(script)
        printHelper.fileCreated(updateScriptPath)
    }

    private fun getYear() = (calendar[Calendar.YEAR] - 2000).toString()

    private fun getTodayPrefix() = "%s%02d%02d-".format(getYear(), calendar[Calendar.MONTH] + 1, calendar[Calendar.DAY_OF_MONTH])


    fun addToMessages(projectStructure: ProjectStructure) {
        val packageDirectory = projectStructure.getModule(GLOBAL_MODULE)
                .resolvePackagePath(model.packageName)

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