package com.haulmont.cuba.cli

import com.haulmont.cuba.cli.commands.CliCommand

class CommandsRegistry {

    private val root: HasSubCommand = HasSubCommand()

    fun setup(setup: HasSubCommand.() -> Unit) {
        root.setup()
    }

    fun traverse(visitor: CommandVisitor) {
        traverse(root, visitor)
    }

    private fun traverse(hasSubCommand: HasSubCommand, visitor: CommandVisitor) {
        hasSubCommand.commands.forEach { name, subCommand ->
            visitor.enterCommand(name, subCommand.cliCommand)
            traverse(subCommand, visitor)
            visitor.exitCommand()
        }
    }
}

interface CommandVisitor {
    fun enterCommand(name: String, command: CliCommand)

    fun exitCommand()
}

open class HasSubCommand internal constructor() {

    internal val commands: MutableMap<String, Command> = mutableMapOf()

    fun command(name: String, cliCommand: CliCommand, setup: (HasSubCommand.() -> Unit)? = null) {
        check(name.isNotBlank()) {
            "Empty names for commands are not allowed"
        }
        check(commands[name] == null) {
            "Command with such name is already presented by ${commands[name]!!.cliCommand::class}"
        }

        commands[name] = Command(cliCommand).apply {
            setup?.let { it() }
        }
    }
}

internal class Command(val cliCommand: CliCommand) : HasSubCommand()