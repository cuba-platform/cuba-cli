package com.haulmont.cuba.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import com.haulmont.cuba.cli.event.CommandRegisterEvent
import com.haulmont.cuba.cli.event.LoadingEndEvent
import org.fusesource.jansi.Ansi.ansi
import org.slf4j.LoggerFactory
import java.util.*

private val context = CliContext()
private val rootCommand = RootCommand()
private val commandsRegistry = CommandsRegistry(rootCommand)
private val logger = LoggerFactory.getLogger("CubaCli")

fun main(args: Array<String>) {

    logger.debug("CUBA CLI start")
    logger.debug("Loading plugins")

    loadPlugins {
        context.registerListener(it)
    }

    context.postEvent(LoadingEndEvent(context))

    context.postEvent(CommandRegisterEvent(context, commandsRegistry))

    val commander = JCommander()
    commandsRegistry.apply(commander)

    try {
        commander.parse(*args)
    } catch (e: MissingCommandException) {
        render("@|red Unrecognized command|@\n")
        return
    }

    val command = getParsedCommand(commander)
    command.execute(context)
}

fun loadPlugins(onLoad: (CliPlugin) -> Unit) = ServiceLoader.load(CliPlugin::class.java).forEach {
    onLoad(it)
}


private tailrec fun getParsedCommand(command: JCommander): CliCommand {
    val alias = command.parsedAlias
    return if (alias == null)
        command.objects.first() as CliCommand
    else
        getParsedCommand(command.commands[alias]!!)
}

fun render(text: String) {
    print(ansi().render(text))
}
