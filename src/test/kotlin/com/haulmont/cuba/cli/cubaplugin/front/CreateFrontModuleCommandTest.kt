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

package com.haulmont.cuba.cli.cubaplugin.front

import com.haulmont.cuba.cli.CliPlugin
import com.haulmont.cuba.cli.command.CommandTestBase
import com.haulmont.cuba.cli.commands.CommandExecutionException
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.ProjectService
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.front.polymer.CreatePolymerModuleCommand
import com.haulmont.cuba.cli.cubaplugin.front.react.CreateReactModuleCommand
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.cubaplugin.model.ProjectModel
import com.haulmont.cuba.cli.cubaplugin.model.ProjectStructure
import com.haulmont.cuba.cli.prompting.ReadException
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kodein.di.Kodein
import org.kodein.di.generic.instance

class CreateFrontModuleCommandTest : CommandTestBase() {
    override val plugins: List<CliPlugin> = listOf(CubaPlugin())
    override val kodeinsToExtend: List<Kodein> = listOf(cubaKodein)

    override fun Kodein.MainBuilder.manageDependencies() {
        stopExecutionOnValidationError()
    }

    @Test
    fun testPolymerCreation() {
        createProject()
        appendInputLine("y")
        executeCommand(CreatePolymerModuleCommand(kodein))
        assertTrue(hasFrontModule())
    }

    @Test
    fun testReactCreation() {
        createProject()
        appendInputLine("y")
        executeCommand(CreateReactModuleCommand(kodein))
        assertTrue(hasFrontModule())
    }

    @Test
    fun testPolymerRejected() {
        createProject()
        appendInputLine("n")
        executeCommand(CreatePolymerModuleCommand(kodein))
        assertFalse(hasFrontModule())
    }

    @Test
    fun testReactRejected() {
        createProject()
        appendInputLine("n")
        executeCommand(CreateReactModuleCommand(kodein))
        assertFalse(hasFrontModule())
    }

    @Test
    fun testDoubleReactCreationFail() {
        createProject()
        appendInputLine("y")
        executeCommand(CreateReactModuleCommand(kodein))

        assertTrue(hasFrontModule())

        appendInputLine("y")
        executeCommand(CreateReactModuleCommand(kodein))

        assertErrorEvent<CommandExecutionException>()
    }

    @Test
    fun testDoublePolymerCreationFail() {
        createProject()
        appendInputLine("y")
        executeCommand(CreatePolymerModuleCommand(kodein))

        assertTrue(hasFrontModule())

        appendInputLine("y")
        executeCommand(CreatePolymerModuleCommand(kodein))

        assertErrorEvent<CommandExecutionException>()
    }


    @Test
    fun testReactAvailableOnlySince7() {
        createProject(version = PlatformVersion("6.10"))

        appendInputLine("2")
        executeCommand(CreateFrontCommand(kodein))

        assertErrorEvent<ReadException>()
    }

    @Test
    fun testAddRestComponent() {
        createProject(version = PlatformVersion.v7_1)

        appendInputLine("2")
        appendInputLine("y")
        appendInputLine("0.1-SNAPSHOT")
        appendInputLine("y")

        executeCommand(CreateFrontCommand(kodein))

        assertTrue(ProjectModel(ProjectStructure()).appComponents.contains("com.haulmont.addon.restapi"))

        assertTrue(hasFrontModule())

        assertNoErrorEvents()
    }

    @Test
    fun testDiscardRestComponent() {
        createProject(version = PlatformVersion.v7_1)

        appendInputLine("2")
        appendInputLine("n")
        appendInputLine("y")

        executeCommand(CreateFrontCommand(kodein))

        assertTrue(!ProjectModel(ProjectStructure()).appComponents.contains("com.haulmont.addon.restapi"))

        assertTrue(hasFrontModule())

        assertNoErrorEvents()
    }

    @Test
    fun testNotAskForRestComponentAsItIsAlreadyAdded() {
        createProject(version = PlatformVersion.v7_1)

        val projectService: ProjectService by kodein.instance()

        projectService.registerAppComponent("com.haulmont.addon.restapi:restapi-global:0.1-SNAPSHOT")

        appendInputLine("2")
        appendInputLine("y")

        executeCommand(CreateFrontCommand(kodein))

        assertTrue(ProjectModel(ProjectStructure()).appComponents.contains("com.haulmont.addon.restapi"))

        assertTrue(hasFrontModule())

        assertNoErrorEvents()
    }


    private fun hasFrontModule() = ProjectStructure().settingsGradle.toFile()
            .readText().contains("front")
}