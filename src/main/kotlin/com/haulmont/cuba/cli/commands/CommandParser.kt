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

import com.beust.jcommander.JCommander
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

class CommandParser(commandsRegistry: CommandsRegistry, withRootCommand: Boolean = false) {

    private val writer: PrintWriter by kodein.instance()

    private val commander = JCommander().apply {
        programName = if (withRootCommand) "cuba" else ""
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

    fun printHelp() = buildString { commander.usage(this) }.let { writer.println(it) }
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