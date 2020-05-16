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

package com.haulmont.cuba.cli.cubaplugin.entitylistener

import com.haulmont.cuba.cli.core.CliPlugin
import com.haulmont.cuba.cli.command.CommandTestBase
import com.haulmont.cuba.cli.core.commands.CommandExecutionException
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.entity.CreateEntityCommand
import com.haulmont.cuba.cli.core.prompting.ValidationException
import org.junit.Test
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance

class CreateEntityListenerCommandTest : CommandTestBase() {
    override val plugins: List<CliPlugin> = listOf(CubaPlugin())
    override val kodeinsToExtend: List<Kodein> = listOf(cubaKodein)


    override fun Kodein.MainBuilder.manageDependencies() {
        stopExecutionOnValidationError()
    }

    @Test
    fun testNoEntity() {
        createProject()

        executeCommand(CreateEntityListenerCommand(kodein))

        assertErrorEvent<CommandExecutionException>()
    }

    @Test
    fun testCommand() {
        createProject()

        appendInputLine("""
            TestEntity


        """.trimIndent())

        executeCommand(CreateEntityCommand(kodein))

        appendInputLine("""
            TestEntityListener
            1


            y
            y
            y
            y
            y
            y
            y
            y
        """.trimIndent())

        executeCommand(CreateEntityListenerCommand(kodein))

        assertNoErrorEvents()
    }

    @Test
    fun testNoInterfacesImplemented() {
        createProject()

        appendInputLine("""
            TestEntity


        """.trimIndent())

        executeCommand(CreateEntityCommand(kodein))

        appendInputLine("""
            TestEntityListener
            1


            n
            n
            n
            n
            n
            n
            n
            n
        """.trimIndent())

        executeCommand(CreateEntityListenerCommand(kodein))

        assertErrorEvent<ValidationException>()
    }
}