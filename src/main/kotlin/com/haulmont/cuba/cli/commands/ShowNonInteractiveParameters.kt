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

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.bgRed
import com.haulmont.cuba.cli.green
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

@Parameters(commandDescription = "Prints commandPath parameters for non interactive mode")
class ShowNonInteractiveParameters(private val commandsRegistry: CommandsRegistry) : AbstractCommand() {

    @Parameter(variableArity = true)
    private var commandPath: List<String> = mutableListOf()

    private val printWriter: PrintWriter by kodein.instance()

    override fun run() {
        val currentPath = mutableListOf<String>()

        commandsRegistry.traverse(object : CommandVisitor {
            override fun enterCommand(command: CommandRecord) {
                currentPath.add(command.name)

                if (currentPath == commandPath) {
                    printParameters(command.cliCommand)

                    abort()
                }
            }

            override fun exitCommand() {
                currentPath.removeAt(currentPath.lastIndex)
            }
        })

        fail("Command \"${commandPath.joinToString(" ")}\" not found")
    }

    private fun printParameters(command: CliCommand) {
        if (command is NonInteractiveInfo) {
            printWriter.println("Non interactive parameters for command ${commandPath.joinToString(" ").green()}:")
            command.getNonInteractiveParameters().forEach { param, description ->
                printWriter.println("$param\t$description")
            }
        } else printWriter.println("Command doesn't provide information about non interactive parameters".bgRed())
    }
}