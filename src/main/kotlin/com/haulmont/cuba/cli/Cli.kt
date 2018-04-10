package com.haulmont.cuba.cli

import com.haulmont.cuba.cli.event.DestroyPluginEvent
import com.haulmont.cuba.cli.event.InitPluginEvent
import java.util.*

abstract class Cli {
    protected val context: CliContext = CliContext()
    private val commandsRegistry: CommandsRegistry = CommandsRegistry(RootCommand())

    fun run() {
        loadPlugins()
        buildCommands(commandsRegistry)
        eval()
        tearDown()
    }

    protected open fun loadPlugins() {
        load {
            context.registerListener(it)
        }

        context.postEvent(InitPluginEvent(context, commandsRegistry))
    }

    protected abstract fun buildCommands(commandsRegistry: CommandsRegistry)

    protected abstract fun eval()

    protected open fun tearDown() {
        context.postEvent(DestroyPluginEvent())
    }
}

fun load(onLoad: (CliPlugin) -> Unit) = ServiceLoader.load(CliPlugin::class.java).forEach {
    onLoad(it)
}