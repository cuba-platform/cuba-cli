package com.haulmont.cuba.cli

import com.beust.jcommander.MissingCommandException
import com.haulmont.cuba.cli.commands.CommandParser
import com.haulmont.cuba.cli.commands.CommandsRegistry
import com.haulmont.cuba.cli.commands.RootCommand
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import org.kodein.di.generic.instance
import java.io.PrintWriter

class SingleCommandCli(private val args: Array<String>, commandsRegistry: CommandsRegistry) : Cli {

    private val context: CliContext by kodein.instance()

    private val writer: PrintWriter by kodein.instance()

    private val commandParser: CommandParser = CommandParser(commandsRegistry, true)

    override fun run() {
        val command = try {
            commandParser.parseCommand(args)
        } catch (e: MissingCommandException) {
            writer.println("@|red Unrecognized command|@")
            return
        }

        if (command is RootCommand && command.help) {
            commandParser.printHelp()
            return
        }

        context.postEvent(BeforeCommandExecutionEvent(command))
        command.execute()
        context.postEvent(AfterCommandExecutionEvent(command))
    }
}