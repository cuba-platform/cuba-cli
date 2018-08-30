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

package com.haulmont.cuba.cli

import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.ParameterException
import com.google.common.eventbus.EventBus
import com.haulmont.cuba.cli.commands.*
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import com.haulmont.cuba.cli.event.ErrorEvent
import org.jline.builtins.Completers
import org.jline.builtins.Completers.TreeCompleter.Node
import org.jline.builtins.Completers.TreeCompleter.node
import org.jline.reader.*
import org.jline.terminal.Terminal
import org.jline.terminal.impl.DumbTerminal
import org.kodein.di.generic.instance
import java.io.PrintWriter

class ShellCli(commandsRegistry: CommandsRegistry) : Cli {

    private val commandParser: CommandParser

    private val cliContext: CliContext by kodein.instance()

    private val writer: PrintWriter by kodein.instance()

    private val printHelper: PrintHelper by kodein.instance()

    private val terminal: Terminal by kodein.instance()

    private val bus: EventBus by kodein.instance()

    private val messages by localMessages()

    private val completer: Completer

    private val lineParser: Parser by lazy { reader.parser }

    init {
        commandsRegistry {
            command("help", HelpCommand)
            command("stacktrace", Stacktrace)
            command("version", VersionCommand)
            command("exit", ExitCommand)
            command("cd", CdCommand())
            command("parameters", ShowNonInteractiveParameters(commandsRegistry))
        }

        commandParser = CommandParser(commandsRegistry, shellMode = true)
        completer = createCommandsCompleter(commandsRegistry)
    }

    private val reader: LineReader by kodein.instance(arg = completer)

    override fun run() {
        printWelcome()

        while (true) {
            CommonParameters.reset()
            commandParser.reset()

            val command = try {
                val line = reader.readLine(PROMPT).also {
                    it != null || return
                }.takeIf {
                    it.isNotBlank()
                } ?: continue

                val parsedLine = lineParser.parse(line, 0)
                val args = parsedLine.words().toTypedArray()
                commandParser.parseCommand(args)
            } catch (e: UserInterruptException) {
                continue
            } catch (e: MissingCommandException) {
                printHelper.unrecognizedCommand()
                continue
            } catch (e: ParameterException) {
                printHelper.unrecognizedParameters(e)
                continue
            } catch (e: EndOfFileException) {
                return
            }

            if (CommonParameters.help) {
                commandParser.printHelp(command)
                continue
            }

            when (command) {
                is HelpCommand -> commandParser.printHelp()
                is Stacktrace -> printHelper.printLastStacktrace()
                is ExitCommand -> return
                else -> evalCommand(command)
            }
        }
    }

    private fun evalCommand(command: CliCommand) {
        bus.post(BeforeCommandExecutionEvent(command))
        try {
            command.execute()
        } catch (e: EndOfFileException) {
        } catch (e: UserInterruptException) {
        } catch (e: Exception) {
            printHelper.handleCommandException(e)
            bus.post(ErrorEvent(e))
        }
        bus.post(AfterCommandExecutionEvent(command))
        cliContext.clearModels()
    }

    private fun printWelcome() {
        if (terminal !is DumbTerminal) {
            writer.println(messages["welcomeMessage"].trimMargin())
        } else {
            writer.println(messages["welcomeMessageDumb"].trimMargin())
        }
        writer.println(messages["interactiveModeHint"])
    }

    companion object {
        private const val PROMPT: String = "cuba>"
    }
}

private fun createCommandsCompleter(commandsRegistry: CommandsRegistry): Completers.TreeCompleter {
    val rootBuilders = mutableListOf<NodeBuilder>()
    val stack = mutableListOf<NodeBuilder>()

    commandsRegistry.traverse(object : CommandVisitor {
        override fun enterCommand(name: String, command: CliCommand) {
            val builder = NodeBuilder(name, command)
            if (stack.isEmpty()) {
                rootBuilders += builder
            } else {
                stack.last().builders += builder
            }
            stack += builder
        }

        override fun exitCommand() {
            stack.removeAt(stack.lastIndex)
        }
    })

    return Completers.TreeCompleter(*rootBuilders.buildNodes())
}

private class NodeBuilder(val name: String, val command: CliCommand) {
    val builders: MutableList<NodeBuilder> = mutableListOf()

    fun build(): Node = when {
        builders.isEmpty() -> node(name)
        else -> node(name, *builders.buildNodes())
    }
}

private fun MutableList<NodeBuilder>.buildNodes() = map { it.build() }.toTypedArray()