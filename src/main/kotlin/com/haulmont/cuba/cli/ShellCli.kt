package com.haulmont.cuba.cli

import com.beust.jcommander.MissingCommandException
import com.haulmont.cuba.cli.commands.*
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import org.jline.builtins.Completers
import org.jline.builtins.Completers.TreeCompleter.Node
import org.jline.builtins.Completers.TreeCompleter.node
import org.jline.reader.*
import org.kodein.di.generic.instance
import java.io.PrintWriter

class ShellCli(commandsRegistry: CommandsRegistry) : Cli {

    private val commandParser: CommandParser

    private val context: CliContext by kodein.instance()

    private val writer: PrintWriter by kodein.instance()

    private val completer: Completer

    private val lineParser: Parser by lazy { reader.parser }

    init {
        commandsRegistry {
            command("help", HelpCommand)
            command("version", VersionCommand)
            command("exit", ExitCommand)
        }

        commandParser = CommandParser(commandsRegistry)
        completer = createCommandsCompleter(commandsRegistry)
    }

    private val reader: LineReader by kodein.instance(arg = completer)

    override fun run() {
        printWelcome()

        while (true) {
            val command = try {
                val line = reader.readLine(PROMPT).takeIf {
                    it.isNotBlank()
                } ?: continue

                val parsedLine = lineParser.parse(line, 0)
                val args = parsedLine.words().toTypedArray()
                commandParser.parseCommand(args)
            } catch (e: UserInterruptException) {
                continue
            } catch (e: EndOfFileException) {
                return
            } catch (e: MissingCommandException) {
                writer.println("@|red Unrecognized command|@")
                continue
            }

            when (command) {
                is HelpCommand -> commandParser.printHelp()
                is ExitCommand -> return
                else -> {
                    context.postEvent(BeforeCommandExecutionEvent(command))
                    command.execute()
                    context.postEvent(AfterCommandExecutionEvent(command))
                }
            }
        }
    }

    private fun printWelcome() {
        writer.println("""
              |
              |@|blue  ██████╗██╗   ██╗██████╗  █████╗      ██████╗██╗     ██╗
              |██╔════╝██║   ██║██╔══██╗██╔══██╗    ██╔════╝██║     ██║
              |██║     ██║   ██║██████╔╝███████║    ██║     ██║     ██║
              |██║     ██║   ██║██╔══██╗██╔══██║    ██║     ██║     ██║
              |╚██████╗╚██████╔╝██████╔╝██║  ██║    ╚██████╗███████╗██║
              | ╚═════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝     ╚═════╝╚══════╝╚═╝
              |
              ||@""".trimMargin())
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