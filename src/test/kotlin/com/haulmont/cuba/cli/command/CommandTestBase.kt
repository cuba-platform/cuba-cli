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

package com.haulmont.cuba.cli.command

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.CliPlugin
import com.haulmont.cuba.cli.WorkingDirectoryManager
import com.haulmont.cuba.cli.commands.CliCommand
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.cubaplugin.project.ProjectInitCommand
import com.haulmont.cuba.cli.cubaplugin.project.ProjectInitModel
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import com.haulmont.cuba.cli.event.ErrorEvent
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

open class CommandTestBase {

    private lateinit var testDir: Path

    @Suppress("MemberVisibilityCanBePrivate")
    lateinit var context: CliContext

    @Suppress("MemberVisibilityCanBePrivate")
    lateinit var kodein: Kodein

    private lateinit var outputStream: PipedOutputStream
    private lateinit var inputStream: PipedInputStream

    val errors: MutableList<ErrorEvent> = mutableListOf()

    open val plugins: List<CliPlugin> = listOf()

    open val kodeinsToExtend: List<Kodein> = listOf()

    @Before
    fun setUp() {
        errors.clear()

        kodein = Kodein {
            kodeinsToExtend.forEach {
                extend(it)
            }

            bind<Terminal>(overrides = true) with instance(createTerminal())

            manageDependencies()
        }

        val bus: EventBus by kodein.instance()
        context = kodein.direct.instance()

        for (plugin in plugins) {
            context.registerPlugin(plugin)
            bus.register(plugin)
            bus.register(this)
        }

        val workingDirectoryManager: WorkingDirectoryManager by kodein.instance()

        workingDirectoryManager.workingDirectory = workingDirectoryManager.workingDirectory.resolve("build/test-run")
        testDir = workingDirectoryManager.workingDirectory

        Files.createDirectories(workingDirectoryManager.workingDirectory)

        doSetup()
    }

    @After
    fun tearDown() {
        val workingDirectoryManager: WorkingDirectoryManager by kodein.instance()
        workingDirectoryManager.workingDirectory = testDir.parent

        context.clearModels()
        deleteDirectoryRecursion(testDir)
    }

    protected open fun Kodein.MainBuilder.manageDependencies() {

    }

    protected fun Kodein.MainBuilder.stopExecutionOnValidationError() {
        bind<Boolean>(tag = "throwValidation", overrides = true) with instance(true)
    }

    protected open fun doSetup() {

    }

    fun createProject(name: String = "test-project", namespace: String = "test", version: PlatformVersion = PlatformVersion.v7) {
        val initModel = ProjectInitModel(mapOf(
                "projectName" to name,
                "namespace" to namespace,
                "rootPackage" to "com.haulmont.test",
                "repo" to "https://dl.bintray.com/cuba-platform/main",
                "platformVersion" to version.toString(),
                "database" to "HSQLDB"
        ))

        val projectInitCommand = ProjectInitCommand()

        context.addModel(projectInitCommand.getModelName(), initModel)
        projectInitCommand.generate(context.getModels())
        context.clearModels()
    }

    fun executeCommand(command: CliCommand) {
        val bus: EventBus by kodein.instance()

        bus.post(BeforeCommandExecutionEvent(command))
        try {
            command.execute()
        } catch (e: Exception) {
            bus.post(ErrorEvent(e))
        }
        bus.post(AfterCommandExecutionEvent(command))
        context.clearModels()
    }

    private fun deleteDirectoryRecursion(path: Path) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.newDirectoryStream(path).use { entries ->
                for (entry in entries) {
                    deleteDirectoryRecursion(entry)
                }
            }
        }
        Files.delete(path)
    }

    private fun createTerminal(): Terminal {
        outputStream = PipedOutputStream()
        inputStream = PipedInputStream()
        inputStream.connect(outputStream)

        return TerminalBuilder.builder().dumb(true)
                .system(false)
                .jna(false)
                .jansi(false)
                .streams(inputStream, System.out)
                .build()
    }

    @Subscribe
    private fun onError(errorEvent: ErrorEvent) {
        errors.add(errorEvent)
    }

    fun appendEmptyLine() = "\n".toByteArray().let(outputStream::write)
    fun appendInputLine(str: String) = "$str\n".toByteArray().let(outputStream::write)

    fun assertNoErrorEvents() {
        assertTrue(errors.isEmpty())
    }

    inline fun <reified T : Throwable> assertErrorEvent() {
        assertTrue(errors.filter { it.cause is T }.any())
    }
}
