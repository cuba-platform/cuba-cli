package com.haulmont.cuba.cli

import com.haulmont.cuba.cli.event.DestroyPluginEvent
import com.haulmont.cuba.cli.event.InitPluginEvent
import java.util.*

fun main(args: Array<String>) {

    val context = CliContext()
    val commandsRegistry = CommandsRegistry()

    loadPlugins(context, commandsRegistry)

    val cli = createCli(args, context, commandsRegistry)

    cli.run()

    context.postEvent(DestroyPluginEvent())
}

private fun loadPlugins(context: CliContext, commandsRegistry: CommandsRegistry) {
    ServiceLoader.load(CliPlugin::class.java).forEach {
        context.registerListener(it)
    }

    context.postEvent(InitPluginEvent(context, commandsRegistry))
}

private fun createCli(args: Array<String>, context: CliContext, commandsRegistry: CommandsRegistry): Cli {
    if (args.isNotEmpty()) {
        if (args.first() == "shell") {
            return ShellCli(context, commandsRegistry)
        }
    }

    return SingleCommandCli(args, context, commandsRegistry)
}
