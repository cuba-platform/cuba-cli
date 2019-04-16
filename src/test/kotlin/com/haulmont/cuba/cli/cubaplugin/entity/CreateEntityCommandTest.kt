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

import com.haulmont.cuba.cli.CliPlugin
import com.haulmont.cuba.cli.command.CommandTestBase
import com.haulmont.cuba.cli.commands.CommandExecutionException
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.model.EntitySearch
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.prompting.ValidationException
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance

internal class CreateEntityCommandTest : CommandTestBase() {

    override val plugins: List<CliPlugin> = listOf(CubaPlugin())
    override val kodeinsToExtend: List<Kodein> = listOf(cubaKodein)

    override fun Kodein.MainBuilder.manageDependencies() {
        bind<Boolean>(tag = "throwValidation", overrides = true) with instance(true)
    }

    @Test
    fun testCreateEntity() {
        createProject()

        appendInputLine("TestEntity")
        appendEmptyLine()
        appendEmptyLine()

        executeCommand(CreateEntityCommand(kodein = kodein))

        assertNoErrorEvents()

        val entitySearch = EntitySearch(kodein)

        assertTrue(entitySearch.getAllEntities()[0].className == "TestEntity")
    }

    @Test
    fun testOldSeparator() {
        createProject(namespace = "ts", version = PlatformVersion("6.10"))

        appendInputLine("TestEntity")
        appendEmptyLine()
        appendEmptyLine()

        executeCommand(CreateEntityCommand(kodein = kodein))

        assertNoErrorEvents()

        val entitySearch = EntitySearch(kodein)

        assertTrue(entitySearch.getAllEntities()[0].name == "ts\$TestEntity")
    }

    @Test
    fun testNewSeparator() {
        createProject(namespace = "ts", version = PlatformVersion.v7)

        appendInputLine("TestEntity")
        appendEmptyLine()
        appendEmptyLine()

        executeCommand(CreateEntityCommand(kodein = kodein))

        assertNoErrorEvents()

        val entitySearch = EntitySearch(kodein)

        assertTrue(entitySearch.getAllEntities()[0].name == "ts_TestEntity")
    }

    @Test
    fun testCreateNonPersistentEntity() {
        createProject()

        appendInputLine("TestEntity")
        appendEmptyLine()
        appendInputLine("3")

        executeCommand(CreateEntityCommand(kodein = kodein))

        assertNoErrorEvents()

        val entitySearch = EntitySearch(kodein)

//        now, entitySearch searches only for persistent entities
        assertTrue(entitySearch.getAllEntities().isEmpty())
    }

    @Test
    fun testCreateEntityWithoutProject() {
        appendInputLine("TestEntity")
        appendEmptyLine()
        appendEmptyLine()

        executeCommand(CreateEntityCommand(kodein = kodein))

        assertErrorEvent<CommandExecutionException>()
    }

    @Test
    fun testInvalidFqn() {
        createProject()

        appendInputLine("Tes tEntity")

        executeCommand(CreateEntityCommand(kodein = kodein))

        assertErrorEvent<ValidationException>()
    }

    @Test
    fun testDuplicatedEntity() {
        createProject()

        appendInputLine("TestEntity")
        appendEmptyLine()
        appendEmptyLine()

        executeCommand(CreateEntityCommand(kodein = kodein))

        assertNoErrorEvents()

        appendInputLine("TestEntity")
        appendEmptyLine()
        appendEmptyLine()

        executeCommand(CreateEntityCommand(kodein = kodein))

        assertErrorEvent<CommandExecutionException>()
    }
}