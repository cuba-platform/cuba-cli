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

class CommandsRegistry {
    private val builders = mutableListOf<HasSubCommand.() -> Unit>()

    operator fun invoke(builder: HasSubCommand.() -> Unit) {
        builders.add(builder)
    }

    fun traverse(visitor: CommandVisitor) {
        HasSubCommand().apply {
            builders.forEach { it() }
        }.let { traverse(it, visitor) }
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
        check(name !in commands) {
            "Command with such name is already presented by ${commands[name]!!.cliCommand::class}"
        }

        commands[name] = Command(cliCommand).apply {
            setup?.let { it() }
        }
    }
}

internal class Command(val cliCommand: CliCommand) : HasSubCommand()