package com.haulmont.cuba.cli

import com.beust.jcommander.JCommander

class CommandsRegistry(rootCommand: RootCommand) {

    val root: CommandsSpace = CommandsSpace(rootCommand)

    fun setup(setup: CommandsSpace.() -> Unit) {
        root.setup()
    }

    fun apply(jCommander: JCommander) {
        jCommander.addObject(root.command)
        apply(jCommander, root)
    }

    private fun apply(jCommander: JCommander, currentCommandSpace: CommandsSpace) {
        currentCommandSpace.commands
                .forEach { name, commandsSpace ->
                    jCommander.addCommand(name, commandsSpace.command)
                    apply(jCommander.commands[name]!!, commandsSpace)
                }
    }
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


