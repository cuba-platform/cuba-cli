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

package com.haulmont.cuba.cli.core.commands

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.haulmont.cuba.cli.core.kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class CommandParser(private val commandsRegistry: CommandsRegistry, private val shellMode: Boolean = false) {

    private val writer: PrintWriter by kodein.instance()

    private lateinit var commander: JCommander

    init {
        initCommander()
    }

    fun parseCommand(args: Array<String>): CliCommand {
        commander.parse(*args)
        return getParsedCommand(commander)
    }

    fun printHelp() {
        launchOptionsHelp()

        writer.println("CUBA CLI commands")
        printHelp(commander)

        writer.println("See Quick start tutorial on https://github.com/cuba-platform/cuba-cli/wiki/Quick-Start\n")
    }

    private fun launchOptionsHelp() {
        writer.println("CUBA CLI launch options")

        LaunchOptions::class.memberProperties.map {
            it.javaField!!.getAnnotation(Parameter::class.java)
        }.filter {
            !it.hidden
        }.forEach {
            writer.println(it.names.first() + " ".repeat(6) + it.description)
        }

        writer.println()
    }

    fun printHelp(command: CliCommand) {
        if (command is UsageProvider) {
            writer.println(command.printUsage())
        } else {
            val route: List<JCommander> = findRoute(command)
            route.let {
                buildString {
                    if (it.size > 1) {
                        val commandName = it.last().programName
                        val commandParent = it[it.lastIndex - 1]
                        commandParent.usage(commandName, this)
                    } else {
                        it.first().usage(this)
                    }
                }
            }.let {
                writer.println(it)
            }
        }
    }

    fun reset() {
        CommonParameters.reset()
        initCommander()
    }

    private fun initCommander() {
        commander = JCommander().apply {
            programName = if (!shellMode) "cuba" else ""
            if (!shellMode) {
                addObject(CommonParameters)
            }
        }

        val stack: MutableList<JCommander> = mutableListOf(commander)

        commandsRegistry.traverse(object : CommandVisitor {
            override fun enterCommand(command: CommandRecord) {
                stack += stack.last().createCommand(command.name, command.cliCommand)
            }

            override fun exitCommand() {
                stack.removeAt(stack.lastIndex)
            }
        })
    }

    private fun JCommander.createCommand(name: String, command: CliCommand): JCommander {
        this.addCommand(name, listOf(command, CommonParameters))
        return this.commands[name]!!
    }

    private fun printHelp(commander: JCommander) =
            buildString {
                commander.usage(this)
            }.let {
                writer.println(it)
            }

    private fun findRoute(command: CliCommand, commander: JCommander = this.commander): List<JCommander> {
        if (command in commander.objects)
            return listOf(commander)

        commander.commands.values.forEach {
            findRoute(command, it).takeIf {
                it.isNotEmpty()
            }?.let {
                return listOf(commander) + it
            }
        }

        return listOf()
    }

    private tailrec fun getParsedCommand(command: JCommander): CliCommand {
        val alias = command.parsedAlias
        return if (alias == null)
            command.objects.first { it is CliCommand } as CliCommand
        else
            getParsedCommand(command.commands[alias]!!)
    }
}