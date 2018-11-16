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

package com.haulmont.cuba.cli.cubaplugin.screen.entityscreen

import com.haulmont.cuba.cli.Resources
import com.haulmont.cuba.cli.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.screen.ScreenCommandBase
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.PrintWriter

abstract class EntityScreenCommandBase<out T : EntityScreenModel> : ScreenCommandBase<T>(), NonInteractiveInfo {

    protected val resources by Resources.fromMyPlugin()

    protected val printWriter: PrintWriter by cubaKodein.instance()

    override fun getNonInteractiveParameters(): Map<String, String> = mapOf(
            "entityName" to "Fully qualified entity name",
            "packageName" to "Package name",
            "screenId" to "Screen id",
            "descriptorName" to "Descriptor name",
            "controllerName" to "Controller name"
    )

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun QuestionsList.prompting() {
        val entitiesList = entitySearch.getAllEntities()
                .filter { !it.embeddable }
                .map { it.fqn }
        if (entitiesList.isEmpty()) {
            printWriter.println("Project does not have any suitable entities.")
            abort()
        }

        options("entityName", "Choose entity", entitiesList)

        question("packageName", "Package name") {
            validate {
                checkIsPackage()
            }

            default { answers ->
                val entityName: String by answers

                val packageParts = entityName.split('.').filter { it != "entity" }
                packageParts.take(packageParts.lastIndex).joinToString(".") + ".web." + packageParts.last().toLowerCase()
            }
        }

        question("screenId", "Screen id") {
            default { answers ->
                val entityName: String by answers

                getDefaultScreenId(entityName)
            }

            validate {
                screenIdDoesNotExists(value)
            }
        }

        question("descriptorName", "Descriptor name") {
            default { answers ->
                val entityName: String by answers

                getDefaultDescriptorName(entityName)
            }

            validate {
                checkIsScreenDescriptor()

                screenDescriptorDoesNotExists(value)
            }
        }

        question("controllerName", "Controller name") {
            default { answers ->
                val entityName: String by answers
                getDefaultControllerName(entityName)
            }

            validate {
                checkIsClass()

                screenControllerDoesNotExists(value)
            }
        }
    }

    protected abstract fun getDefaultControllerName(entityName: String): String

    protected abstract fun getDefaultDescriptorName(entityName: String): String

    protected abstract fun getDefaultScreenId(entityName: String) : String

    override fun beforeGeneration() {
        checkScreenId(model.screenId)
        checkExistence(model.packageName, descriptor = model.descriptorName, controller = model.controllerName)
    }
}