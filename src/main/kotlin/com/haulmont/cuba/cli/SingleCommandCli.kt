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
import com.haulmont.cuba.cli.commands.CliCommand
import com.haulmont.cuba.cli.commands.CommandParser
import com.haulmont.cuba.cli.commands.CommandsRegistry
import com.haulmont.cuba.cli.commands.CommonParameters
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import org.kodein.di.generic.instance

class SingleCommandCli(private val args: Array<String>, commandsRegistry: CommandsRegistry) : Cli {

    private val context: CliContext by kodein.instance()

    private val printHelper: PrintHelper by kodein.instance()

    private val commandParser: CommandParser = CommandParser(commandsRegistry, false)

    override fun run() {
        val command = try {
            commandParser.parseCommand(args)
        } catch (e: MissingCommandException) {
            printHelper.unrecognizedCommand()
            return
        } catch (e: ParameterException) {
            printHelper.unrecognizedParameters()
            return
        }

        evalCommand(command)
    }

    private fun evalCommand(command: CliCommand) {
        if (CommonParameters.help) {
            commandParser.printHelp(command)
            return
        }

        context.postEvent(BeforeCommandExecutionEvent(command))
        try {
            command.execute()
        } catch (e: Exception) {
            printHelper.handleCommandException(e)
        }
        context.postEvent(AfterCommandExecutionEvent(command))
    }
}