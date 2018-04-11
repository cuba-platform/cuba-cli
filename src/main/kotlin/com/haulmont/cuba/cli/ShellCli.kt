package com.haulmont.cuba.cli

import com.haulmont.cuba.cli.commands.CliCommand
import com.haulmont.cuba.cli.commands.CommandParser
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import org.jline.builtins.Completers
import org.jline.builtins.Completers.TreeCompleter.Node
import org.jline.builtins.Completers.TreeCompleter.node
import org.jline.reader.*
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

class ShellCli(val context: CliContext, commandsRegistry: CommandsRegistry) : Cli {

    private val commandParser: CommandParser = CommandParser(commandsRegistry)
    private val completer: Completer = createCompleter(commandsRegistry)

    private val terminal: Terminal = TerminalBuilder.builder().build()

    private val reader: LineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(completer)
            .build()

    override fun run() {
        printWelcome()

        while (true) {
            val command = try {
                val line = reader.readLine("cuba>")
                val parsedLine = reader.parser.parse(line, 0)
                val args = parsedLine.words().toTypedArray()
                commandParser.parseCommand(args)
            } catch (e: UserInterruptException) {
                continue
            } catch (e: EndOfFileException) {
                return
            }

            context.postEvent(BeforeCommandExecutionEvent(command))
            command.execute(context)
            context.postEvent(AfterCommandExecutionEvent(command))
        }
    }
}

private fun createCompleter(commandsRegistry: CommandsRegistry): Completers.TreeCompleter {
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
            stack.removeAt(stack.size - 1)
        }
    })

    return Completers.TreeCompleter(rootBuilders.map { it.build() })
}

class NodeBuilder(val name: String, val command: CliCommand) {
    val builders: MutableList<NodeBuilder> = mutableListOf()

    fun build(): Node = when {
        builders.isEmpty() -> node(name)
        else -> node(name, builders.map { it.build() })
    }
}

private fun printWelcome() {
    println("""
              |
              | ██████╗██╗   ██╗██████╗  █████╗      ██████╗██╗     ██╗
              |██╔════╝██║   ██║██╔══██╗██╔══██╗    ██╔════╝██║     ██║
              |██║     ██║   ██║██████╔╝███████║    ██║     ██║     ██║
              |██║     ██║   ██║██╔══██╗██╔══██║    ██║     ██║     ██║
              |╚██████╗╚██████╔╝██████╔╝██║  ██║    ╚██████╗███████╗██║
              | ╚═════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝     ╚═════╝╚══════╝╚═╝
              |
                                                        """.trimMargin())
}