package com.haulmont.cuba.cli.commands

import com.beust.jcommander.JCommander
import com.haulmont.cuba.cli.CommandVisitor
import com.haulmont.cuba.cli.CommandsRegistry

class CommandParser(commandsRegistry: CommandsRegistry, withRootCommand: Boolean = false) {

    private val commander = JCommander().apply {
        programName = "cuba"
        if (withRootCommand) {
            addObject(RootCommand())
        }
    }

    init {
        initCommander(commander, commandsRegistry)
    }

    fun parseCommand(args: Array<String>): CliCommand {
        commander.parse(*args)
        return getParsedCommand(commander)
    }

    fun usage() = commander.usage()
}

private tailrec fun getParsedCommand(command: JCommander): CliCommand {
    val alias = command.parsedAlias
    return if (alias == null)
        command.objects.first() as CliCommand
    else
        getParsedCommand(command.commands[alias]!!)
}


private fun initCommander(commander: JCommander, commandsRegistry: CommandsRegistry) {
    val stack: MutableList<JCommander> = mutableListOf(commander)

    commandsRegistry.traverse(object : CommandVisitor {
        override fun enterCommand(name: String, command: CliCommand) {
            stack += stack.last().createCommand(name, command)
        }

        override fun exitCommand() {
            stack.removeAt(stack.lastIndex)
        }
    })
}

private fun JCommander.createCommand(name: String, command: CliCommand): JCommander {
    this.addCommand(name, command)
    return this.commands[name]!!
}