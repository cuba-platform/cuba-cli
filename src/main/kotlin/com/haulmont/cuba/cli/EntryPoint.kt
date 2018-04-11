package com.haulmont.cuba.cli

import com.haulmont.cuba.cli.commands.CommandsRegistry
import com.haulmont.cuba.cli.di.terminalModule
import com.haulmont.cuba.cli.event.DestroyPluginEvent
import com.haulmont.cuba.cli.event.InitPluginEvent
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import java.util.*

val kodein by lazy {
    Kodein {
        import(terminalModule)

        bind<CliContext>() with instance(context)
    }
}

private val context: CliContext = CliContext()

fun main(args: Array<String>) {

    val commandsRegistry = CommandsRegistry()

    loadPlugins(context, commandsRegistry)

    val cli = createCli(args, commandsRegistry)

    cli.run()

    context.postEvent(DestroyPluginEvent())
}

private fun loadPlugins(context: CliContext, commandsRegistry: CommandsRegistry) {
    ServiceLoader.load(CliPlugin::class.java).forEach {
        context.registerListener(it)
    }

    context.postEvent(InitPluginEvent(commandsRegistry))
}

private fun createCli(args: Array<String>, commandsRegistry: CommandsRegistry): Cli {
    if (args.isNotEmpty()) {
        if (args.first() == "shell") {
            return ShellCli(commandsRegistry)
        }
    }

    return SingleCommandCli(args, commandsRegistry)
}

const val CLI_VERSION = "CUBA CLI 1.0-SNAPSHOT"