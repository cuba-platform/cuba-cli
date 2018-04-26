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
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.getChildElements
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import net.sf.practicalxml.DomUtil
import org.kodein.di.generic.instance

@Parameters(commandDescription = "Creates new entity listener")
class CreateEntityListenerCommand : GeneratorCommand<EntityListenerModel>() {
    private val namesUtils: NamesUtils by kodein.instance()

    override fun getModelName(): String = EntityListenerModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)

        val persistenceXml = ProjectFiles().getModule(ModuleType.GLOBAL).persistenceXml
        val entitiesList = parse(persistenceXml).documentElement
                .let { DomUtil.getChild(it, "persistence-unit") }
                .getChildElements()
                .filter { element -> element.tagName == "class" }
                .map { it.textContent.trim() }

        if (entitiesList.isEmpty())
            fail("Project does not have any entities.")

        question("name", "Listener name") {
            validate {
                checkIsClass()
            }
        }
        options("entityType", "Select entity", entitiesList)
        question("packageName", "Listener package") {
            default(projectModel.rootPackage + ".listener")
            validate {
                checkIsPackage()
            }
        }
        question("beanName", "Bean name") {
            default { projectModel.namespace + "_" + it["name"] }
        }

        questionList("interfaces") {
            val interfaces = listOf("beforeInsert", "beforeUpdate", "beforeDelete",
                    "afterInsert", "afterUpdate", "afterDelete",
                    "beforeAttach", "beforeDetach")

            interfaces.forEach {
                confirmation(it, "Implement ${it.capitalize()}EntityListener?") {
                    default(true)
                }
            }

            validate { answers ->
                if (interfaces.none { answers[it] as Boolean }) {
                    fail("Listener must implement at least one of the interfaces")
                }
            }
        }
    }

    override fun createModel(answers: Answers): EntityListenerModel = EntityListenerModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "entityListener", bindings) {
            transform("")
        }

        registerListener()
    }

    private fun registerListener() {
        val model = context.getModel<EntityListenerModel>(EntityListenerModel.MODEL_NAME)

        val entityPath = ProjectFiles().getModule(ModuleType.GLOBAL)
                .src
                .resolve(namesUtils.packageToDirectory(model.entityPackageName))
                .resolve(model.entityName + ".java")

        val entityLines = entityPath.toFile()
                .readLines()
                .toMutableList()

//        Add import if need
        val importListenerRegex = Regex("import +com\\.haulmont\\.cuba\\.core\\.entity\\.annotation\\.Listeners; *")

        val hasListenerImport = entityLines.find { it.matches(importListenerRegex) } != null

        if (!hasListenerImport) {
            entityLines.add(2, "import com.haulmont.cuba.core.entity.annotation.Listeners;")
        }

//        Create or alter Listeners annotation
        val listenersRegex = Regex("@Listeners\\((value *= *)?\\{?([\". a-zA-Z0-9_,]+)*}?\\)")
        val classRegex = Regex(".* +class +.*")

        val listeners: MutableList<String> = mutableListOf()
        var annotationIndex = -1
        var classIndex = -1
        entityLines.forEachIndexed { index, line ->
            listenersRegex.find(line)?.let {
                listeners += it.groupValues[2]
                        .replace("\"", " ")
                        .split(",")
                        .map { it.trim() }
                annotationIndex = index
            }
            if (line.matches(classRegex)) {
                classIndex = index
            }
        }

        listeners += model.beanName

        val annotationString = "@Listeners({${listeners.joinToString { "\"$it\"" }}})"

        if (annotationIndex != -1) {
            entityLines.removeAt(annotationIndex)
            entityLines.add(annotationIndex, annotationString)
        } else {
            entityLines.add(classIndex, annotationString)
        }

        entityPath.toFile().bufferedWriter().use {
            entityLines.forEach { line ->
                it.write(line)
                it.write("\n")
            }
        }
    }

    override fun checkPreconditions() {
        onlyInProject()
    }
}

data class EntityListenerModel(
        val className: String,
        val packageName: String,
        val beanName: String,
        val entityName: String,
        val entityPackageName: String,
        val beforeInsert: Boolean,
        val beforeUpdate: Boolean,
        val beforeDelete: Boolean,
        val afterInsert: Boolean,
        val afterUpdate: Boolean,
        val afterDelete: Boolean,
        val beforeAttach: Boolean,
        val beforeDetach: Boolean) {

    companion object {
        const val MODEL_NAME = "listener"

        operator fun invoke(answers: Answers): EntityListenerModel {
            val entity: String = answers("entityType")
            val lastDotIndex = entity.lastIndexOf('.')
            val entityName = if (lastDotIndex == -1) entity else entity.substring(lastDotIndex + 1)
            val entityPackageName = entity.removeSuffix(".$entityName")

            val interfaces: Answers = answers("interfaces")

            return EntityListenerModel(
                    answers("name"),
                    answers("packageName"),
                    answers("beanName"),
                    entityName,
                    entityPackageName,
                    interfaces("beforeInsert"),
                    interfaces("beforeUpdate"),
                    interfaces("beforeDelete"),
                    interfaces("afterInsert"),
                    interfaces("afterUpdate"),
                    interfaces("afterDelete"),
                    interfaces("beforeAttach"),
                    interfaces("beforeDetach")
            )
        }

        @Suppress("UNCHECKED_CAST")
        infix operator fun <V> Map<String, *>.invoke(key: String): V {
            return this[key] as V
        }
    }
}