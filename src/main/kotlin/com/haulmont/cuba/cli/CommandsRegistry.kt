package com.haulmont.cuba.cli

class CommandsRegistry(rootCommand: RootCommand) {

    private val root: CommandsSpace = CommandsSpace(rootCommand)

    fun setup(setup: CommandsSpace.() -> Unit) {
        root.setup()
    }

    fun traverse(visitor: CommandVisitor) {
        traverse(root, visitor)
    }

    private fun traverse(commandsSpace: CommandsSpace, visitor: CommandVisitor) {
        commandsSpace.commands.forEach { name, commandsSpace ->
            visitor.enterCommand(name, commandsSpace.command)
            traverse(commandsSpace, visitor)
            visitor.exitCommand()
        }
    }
}

interface CommandVisitor {
    fun enterCommand(name: String, command: CliCommand)

    fun exitCommand()
}

class CommandsSpace internal constructor(val command: CliCommand) {

    internal val commands: MutableMap<String, CommandsSpace> = mutableMapOf()

    fun command(name: String, command: CliCommand, setup: (CommandsSpace.() -> Unit)? = null) {
        check(commands[name] == null) { TODO("Write error message") }
        check(name.isNotBlank()) { TODO("Write error message") }

        val commandsSpace = CommandsSpace(command)
        setup?.let { commandsSpace.it() }
        commands[name] = commandsSpace
    }
}