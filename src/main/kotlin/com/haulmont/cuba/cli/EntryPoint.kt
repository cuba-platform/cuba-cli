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

import com.haulmont.cuba.cli.commands.CommandsRegistry
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.di.terminalModule
import com.haulmont.cuba.cli.event.DestroyPluginEvent
import com.haulmont.cuba.cli.event.InitPluginEvent
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.lang.module.ModuleFinder
import java.nio.file.Paths
import java.util.*

val kodein = Kodein {
    import(terminalModule)

    bind<CliContext>() with singleton { CliContext() }

    bind<NamesUtils>() with instance(NamesUtils())

    bind<Resources>() with instance(Resources())
}

private val context: CliContext by kodein.instance()

fun main(args: Array<String>) {

    val mode = getCliMode(args)

    val commandsRegistry = CommandsRegistry()

    loadPlugins(context, commandsRegistry, mode)

    val cli: Cli = when (mode) {
        CliMode.SHELL -> ShellCli(commandsRegistry)
        CliMode.SINGLE_COMMAND -> SingleCommandCli(args, commandsRegistry)
    }

    cli.run()

    context.postEvent(DestroyPluginEvent())
}

fun getCliMode(args: Array<String>): CliMode =
        if (args.isEmpty() || args.first() == "shell") {
            CliMode.SHELL
        } else {
            CliMode.SINGLE_COMMAND
        }


private fun loadPlugins(context: CliContext, commandsRegistry: CommandsRegistry, mode: CliMode) {

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

    ServiceLoader.load(pluginsLayer, CliPlugin::class.java).forEach {
        context.registerListener(it)
    }

    context.postEvent(InitPluginEvent(commandsRegistry, mode))
}

const val CLI_VERSION = "CUBA CLI 1.0-SNAPSHOT"