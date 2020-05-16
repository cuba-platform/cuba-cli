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

package com.haulmont.cuba.cli.cubaplugin.project

import com.haulmont.cuba.cli.core.CliPlugin
import com.haulmont.cuba.cli.command.CommandTestBase
import com.haulmont.cuba.cli.core.commands.CommandExecutionException
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.model.*
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance

class ProjectInitCommandTest : CommandTestBase() {

    override val plugins: List<CliPlugin> = listOf(CubaPlugin())
    override val kodeinsToExtend: List<Kodein> = listOf(cubaKodein)

    override fun Kodein.MainBuilder.manageDependencies() {
        bind<Boolean>(tag = "throwValidation", overrides = true) with instance(true)

        bind<PlatformVersionsManager>(overrides = true) with instance(object : PlatformVersionsManager {
            override val supportedVersionsRange: VersionRange = SpecificVersion(6, 8, 0)..SpecificVersion(7, 2, 0)
            override val versions: List<String> = listOf("6.8.0", "7.0")
            override fun load() {}
        })
    }

    @Test
    fun testPlainExecution() {
        val command = ProjectInitCommand(kodein)

        appendInputLine("""
            1
            test-project
            tp
            com.company.tp
            3
            7.1-SNAPSHOT
            1
        """.trimIndent())

        executeCommand(command)

        assertNoErrorEvents()

        val projectStructure = ProjectStructure()
        val projectModel = ProjectModel(projectStructure)

        assertTrue(projectStructure.rootPackage == "com.company.tp")
        assertTrue(projectModel.database.type == "hsql")
        assertTrue(projectModel.namespace == "tp")
        assertTrue(projectModel.platformVersion == PlatformVersion("7.1-SNAPSHOT"))
    }

    @Test
    fun testFailOnCreatingInsideAnotherProject() {
        createProject()
        executeCommand(ProjectInitCommand(kodein))
        assertErrorEvent<CommandExecutionException>()
    }
}