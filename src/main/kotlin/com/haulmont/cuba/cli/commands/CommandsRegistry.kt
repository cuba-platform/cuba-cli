/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.cli.commands

import org.jline.reader.Completer
/**
 * CommandRegistry stores hierarchy of commands provided by plugins.
 *
 * To register commands use [command] or [invoke] methods.
 *
 */
class CommandsRegistry {
    private val builders = mutableListOf<CommandContainerConfiguration.() -> Unit>()

    /**
     * Registers commands in CLI by setup function.
     * Note, that all setup functions executes lazily every time before command name parsing.
     */
    fun command(setup: CommandContainerConfiguration.() -> Unit) {
        builders.add(setup)
    }

    operator fun invoke(setup: CommandContainerConfiguration.() -> Unit) {
        command(setup)
    }

    internal fun traverse(visitor: CommandVisitor) {
        BaseCommand().apply {
            builders.forEach { it() }
        }.let { traverse(it, visitor) }
    }

    private fun traverse(command: CommandContainer, visitor: CommandVisitor) {
        command.commands.forEach { name, subCommand ->
            visitor.enterCommand(CommandRecord(name, subCommand.cliCommand, subCommand.completer))
            traverse(subCommand, visitor)
            visitor.exitCommand()
        }
    }
}

interface CommandContainerConfiguration {
    fun command(name: String, cliCommand: CliCommand, setup: (CommandConfiguration.() -> Unit)? = null)
}

interface CommandConfiguration : CommandContainerConfiguration {
    fun completer(completer: Completer)
}

private open class CommandContainer : CommandContainerConfiguration {
    internal val commands: MutableMap<String, Command> = mutableMapOf()

    /**
     * Registers [cliCommand] by [name] name.
     * Optionally allows to register sub-commands with [setup] function.
     */
    override fun command(name: String, cliCommand: CliCommand, setup: (CommandConfiguration.() -> Unit)?) {
        check(name.isNotBlank()) {
            "Empty names for commands are not allowed"
        }
        check(name !in commands) {
            "Command with such name is already presented by ${commands[name]!!.cliCommand::class}"
        }

        commands[name] = Command(cliCommand, name).apply {
            setup?.let { it() }
        }
    }
}

private class BaseCommand : CommandContainer()

private class Command(val cliCommand: CliCommand, val name: String, var completer: Completer? = null) :
        CommandContainer(), CommandConfiguration {

    override fun completer(completer: Completer) {
        this.completer = completer
    }
}

interface CommandVisitor {
    fun enterCommand(command: CommandRecord)

    fun exitCommand()
}

data class CommandRecord(val name: String, val cliCommand: CliCommand, val completer: Completer?)