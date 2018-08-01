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

package com.haulmont.cuba.cli

import com.beust.jcommander.JCommander
import com.google.common.eventbus.EventBus
import com.haulmont.cuba.cli.commands.CommandsRegistry
import com.haulmont.cuba.cli.commands.LaunchOptions
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.di.terminalModule
import com.haulmont.cuba.cli.event.DestroyPluginEvent
import com.haulmont.cuba.cli.event.ErrorEvent
import com.haulmont.cuba.cli.event.InitPluginEvent
import com.haulmont.cuba.cli.registration.registrationModule
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.io.PrintWriter
import java.lang.module.ModuleFinder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.*
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

val CLI_VERSION: String by lazy {
    val properties = Properties()

    val propertiesInputStream = Cli::class.java.getResourceAsStream("application.properties")
    propertiesInputStream.use {
        val inputStreamReader = java.io.InputStreamReader(propertiesInputStream, StandardCharsets.UTF_8)
        properties.load(inputStreamReader)
    }

    "CUBA CLI " + properties["version"]!!
}

private val bus: EventBus = EventBus { throwable: Throwable, subscriberContext ->
    if (subscriberContext.event !is ErrorEvent) {
        subscriberContext.eventBus.post(ErrorEvent(throwable))
    } else {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        (throwable as java.lang.Throwable).printStackTrace(writer)

        writer.println(
                """Exception during another exception handling.
                    Source ${subscriberContext.subscriber}#${subscriberContext.subscriberMethod}.
                    Terminating...""".bgRed().trimIndent())

        System.exit(1)
    }
}

val kodein = Kodein {
    import(terminalModule)
    import(registrationModule)

    bind<CliContext>() with singleton { CliContext() }

    bind<EventBus>() with instance(bus)

    bind<NamesUtils>() with instance(NamesUtils())

    bind<Resources>() with instance(Resources())

    bind<WorkingDirectoryManager>() with instance(WorkingDirectoryManager())

    bind<PlatformVersionsManager>() with singleton { PlatformVersionsManager() }
}

private val context: CliContext by kodein.instance()

private val writer: PrintWriter by kodein.instance()

fun main(args: Array<String>) {

    val mode = getCliMode(args)

    if (mode == CliMode.SHELL) {
        parseLaunchOptions(args)
        setupLogger()

        val versionManager = kodein.direct.instance<PlatformVersionsManager>()
        versionManager.load()
    }

    val commandsRegistry = CommandsRegistry()

    loadPlugins(commandsRegistry, mode)

    val cli: Cli = when (mode) {
        CliMode.SHELL -> ShellCli(commandsRegistry)
        CliMode.SINGLE_COMMAND -> SingleCommandCli(args, commandsRegistry)
    }

    cli.run()

    bus.post(DestroyPluginEvent())
}

private fun getCliMode(args: Array<String>): CliMode =
        if (args.isEmpty() || args.first() == "shell" || args.first().startsWith("-")) {
            CliMode.SHELL
        } else {
            CliMode.SINGLE_COMMAND
        }

private fun loadPlugins(commandsRegistry: CommandsRegistry, mode: CliMode) {

    val pluginsDir = Paths.get(System.getProperty("user.home"), ".haulmont", "cli", "plugins")

    val bootLayer = ModuleLayer.boot()

    val pluginModulesFinder = ModuleFinder.of(pluginsDir)
    val pluginModules = pluginModulesFinder.findAll().map {
        it.descriptor().name()
    }

    val configuration = bootLayer.configuration().resolve(pluginModulesFinder, ModuleFinder.of(), pluginModules)

    val pluginsLayer = ModuleLayer.defineModulesWithOneLoader(
            configuration,
            mutableListOf(bootLayer),
            ClassLoader.getSystemClassLoader()
    ).layer()

    for (plugin in ServiceLoader.load(pluginsLayer, CliPlugin::class.java)) {
        if (plugin !is CubaPlugin) {
            writer.println("Loaded plugin @|green ${plugin.javaClass.name}|@.")
        }
        bus.register(plugin)
    }

    bus.post(InitPluginEvent(commandsRegistry, mode))
}


private fun parseLaunchOptions(args: Array<String>) =
        JCommander(LaunchOptions).parseWithoutValidation(*args)

private fun setupLogger() {
    val root = Logger.getLogger("")
    root.handlers.filterIsInstance<ConsoleHandler>()
            .firstOrNull()
            ?.let {
                if (LaunchOptions.debug) {
                    it.level = Level.ALL
                } else {
                    it.level = Level.OFF
                }
            }
}
