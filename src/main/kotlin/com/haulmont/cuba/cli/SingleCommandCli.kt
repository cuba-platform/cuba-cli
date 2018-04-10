package com.haulmont.cuba.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import org.fusesource.jansi.Ansi

class SingleCommandCli(private val args: Array<String>) : Cli() {
    private val commander = JCommander().apply {
        programName = "cuba"
        addObject(RootCommand())
    }

    override fun buildCommands(commandsRegistry: CommandsRegistry) {
        val stack: MutableList<JCommander> = mutableListOf(commander)

        commandsRegistry.traverse(object : CommandVisitor {
            override fun enterCommand(name: String, command: CliCommand) {
                val current = stack.last()
                current.addCommand(name, command)
                stack += current.commands[name]!!
            }

            override fun exitCommand() {
                stack.removeAt(stack.size - 1)
            }
        })
    }

    override fun eval() {
        try {
            commander.parse(*args)
        } catch (e: MissingCommandException) {
            render("@|red Unrecognized command|@\n")
            return
        }

        val command = getParsedCommand(commander)
        if (command is RootCommand && command.help) {
            commander.usage()
            return
        }

        context.postEvent(BeforeCommandExecutionEvent(command))

        command.execute(context)

        context.postEvent(AfterCommandExecutionEvent(command))
    }
}

private tailrec fun getParsedCommand(command: JCommander): CliCommand {
    val alias = command.parsedAlias
    return if (alias == null)
        command.objects.first() as CliCommand
    else
        getParsedCommand(command.commands[alias]!!)
}


fun render(text: String) {
    print(Ansi.ansi().render(text))
}