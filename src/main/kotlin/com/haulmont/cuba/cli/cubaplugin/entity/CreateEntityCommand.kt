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
import com.haulmont.cuba.cli.core.*
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure.Companion.CORE_MODULE
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure.Companion.GLOBAL_MODULE
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.core.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.generation.*
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.cubaplugin.model.ProjectStructure
import com.haulmont.cuba.cli.core.prompting.Answers
import com.haulmont.cuba.cli.core.prompting.QuestionsList
import com.haulmont.cuba.cli.getTemplate
import com.haulmont.cuba.cli.registration.EntityRegistrationHelper
import org.kodein.di.Kodein
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.util.*

@Parameters(commandDescription = "Creates new entity")
class CreateEntityCommand(override val kodein: Kodein = cubaKodein) : GeneratorCommand<EntityModel>(), NonInteractiveInfo {
    private val entityTypes = listOf("Persistent", "Persistent embedded", "Not persistent")

    private val namesUtils: NamesUtils by kodein.instance<NamesUtils>()

    private val printHelper: PrintHelper by kodein.instance<PrintHelper>()

    private val resources by Resources.fromMyPlugin()

    private val snippets by lazy {
        Snippets(resources, "entity")
    }

    private val entityRegistrationHelper: EntityRegistrationHelper by cubaKodein.instance<EntityRegistrationHelper>()

    private val calendar = Calendar.getInstance()

    override fun getModelName(): String = EntityModel.MODEL_NAME

    override fun getNonInteractiveParameters(): Map<String, String> = mapOf(
            "entityName" to "Entity Name",
            "packageName" to "Package Name",
            "entityType" to "Entity type. Might be one of ${entityTypes.printOptions()}"
    )

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

        textOptions("entityType", "Entity type", entityTypes) {
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

        val sep = if(projectModel.platformVersion < PlatformVersion.v7)
            "$"
        else
            "_"

        return EntityModel(
                entityName,
                "packageName" from answers,
                "entityType" from answers,
                tableName,
                sep
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
        TemplateProcessor(resources.getTemplate("entity"), bindings) {
            transformWhole()
        }

        entityRegistrationHelper.registerEntity(model.packageName + "." + model.name, model.type != "Not persistent")

        addToMessages(projectStructure)

        if (model.type == "Persistent") {
            createSqlScripts()
        }
    }

    private fun createSqlScripts() {
        val script = snippets["${projectModel.database.type}CreateTable"].format(model.tableName)

        val dbPath = projectStructure.getModule(CORE_MODULE).path.resolve("db")

        val createDbPath = dbPath.resolve("init", projectModel.database.type, "10.create-db.sql")

        if (!Files.exists(createDbPath)) {
            createDbPath.parent.takeIf {
                !Files.exists(it) || !Files.isDirectory(it)
            }?.let { Files.createDirectories(it) }

            Files.createFile(createDbPath)

            createDbPath.toFile().appendText(script)
            printHelper.fileCreated(createDbPath)
        } else {
            createDbPath.toFile().appendText(script)
            printHelper.fileModified(createDbPath)
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

        Properties.modify(messages) {
            set(model.name, entityPrintableName)
        }
    }
}