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

package com.haulmont.cuba.cli.cubaplugin.front

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.Messages
import com.haulmont.cuba.cli.commands.AbstractCommand
import com.haulmont.cuba.cli.commands.CliCommand
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.front.polymer.CreatePolymerModuleCommand
import com.haulmont.cuba.cli.cubaplugin.front.react.CreateReactModuleCommand
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.localMessages
import com.haulmont.cuba.cli.prompting.Option
import com.haulmont.cuba.cli.prompting.Prompts
import org.kodein.di.Kodein

@Parameters(commandDescription = "Create new CUBA frontend module")
class CreateFrontCommand(override val kodein: Kodein = cubaKodein) : AbstractCommand() {
    private val messages by localMessages()


    override fun run() {
        val commands = mutableListOf(
                Option<CliCommand>("", "Polymer module", CreatePolymerModuleCommand(kodein))
        )

        if (projectModel.platformVersion >= PlatformVersion.v7) {
            commands += Option<CliCommand>("", "React module", CreateReactModuleCommand(kodein))
        }

        val answers = Prompts.create(kodein) {
            options("command", "Select frontend type", commands)
        }.ask()

        val command: CliCommand by answers

        command.execute()
    }

    override fun preExecute() {
        checkProjectExistence()

        val alreadyContainsModule = projectStructure.settingsGradle.toFile()
                .readText().contains("front")

        !alreadyContainsModule || fail(messages["moduleExistsError"])
    }
}