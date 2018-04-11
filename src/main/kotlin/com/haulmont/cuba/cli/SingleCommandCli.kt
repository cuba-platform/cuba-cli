package com.haulmont.cuba.cli

import com.beust.jcommander.MissingCommandException
import com.haulmont.cuba.cli.commands.CommandParser
import com.haulmont.cuba.cli.commands.RootCommand
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import org.fusesource.jansi.Ansi

class SingleCommandCli(private val args: Array<String>, val context: CliContext, commandsRegistry: CommandsRegistry) : Cli {
    private val commandParser: CommandParser = CommandParser(commandsRegistry, true)

    override fun run() {
        val command = try {
            commandParser.parseCommand(args)
        } catch (e: MissingCommandException) {
            render("@|red Unrecognized command|@\n")
            return
        }

        if (command is RootCommand && command.help) {
            commandParser.usage()
            return
        }

        context.postEvent(BeforeCommandExecutionEvent(command))
        command.execute(context)
        context.postEvent(AfterCommandExecutionEvent(command))
    }
}

fun render(text: String) {
    print(Ansi.ansi().render(text))
}