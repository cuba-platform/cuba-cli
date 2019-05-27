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

package com.haulmont.cuba.cli.cubaplugin.installcomponent

import com.haulmont.cuba.cli.CliPlugin
import com.haulmont.cuba.cli.command.CommandTestBase
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.model.ProjectModel
import com.haulmont.cuba.cli.cubaplugin.model.ProjectStructure
import org.junit.Assert
import org.junit.Test
import org.kodein.di.Kodein

class AddComponentCommandTest : CommandTestBase() {
    override val plugins: List<CliPlugin> = listOf(CubaPlugin())
    override val kodeinsToExtend: List<Kodein> = listOf(cubaKodein)

    override fun Kodein.MainBuilder.manageDependencies() {
        stopExecutionOnValidationError()
    }

    @Test
    fun testPolymerCreation() {
        createProject()

        appendInputLine("com.haulmont.test:test-addon:0.1-SNAPSHOT")
        executeCommand(AddComponentCommand(kodein))

        Assert.assertTrue(ProjectModel(ProjectStructure()).appComponents.contains("com.haulmont.test"))

        assertNoErrorEvents()
    }
}