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
import com.haulmont.cuba.cli.commands.AbstractCubaCommand
import com.haulmont.cuba.cli.core.commands.CliCommand
import com.haulmont.cuba.cli.cubaplugin.ProjectService
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.front.polymer.CreatePolymerModuleCommand
import com.haulmont.cuba.cli.cubaplugin.front.react.CreateReactModuleCommand
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.core.localMessages
import com.haulmont.cuba.cli.core.prompting.Option
import com.haulmont.cuba.cli.core.prompting.Prompts
import org.kodein.di.Kodein
import org.kodein.di.generic.instance

@Parameters(commandDescription = "Create new CUBA frontend module")
class CreateFrontCommand(override val kodein: Kodein = cubaKodein) : AbstractCubaCommand() {
    private val messages by localMessages()

    private val projectService: ProjectService by kodein.instance<ProjectService>()

    override fun run() {
        val commands = mutableListOf(
                Option<CliCommand>("", "Polymer module", CreatePolymerModuleCommand(kodein))
        )

        if (projectModel.platformVersion >= PlatformVersion.v7) {
            commands += Option<CliCommand>("", "React module", CreateReactModuleCommand(kodein))
        }

        val hasRest = projectModel.appComponents.filter { it.contains("com.haulmont.addon.restapi") }.any()

        val v7_1_plus = projectModel.platformVersion >= PlatformVersion.v7_1

        val answers = Prompts.create(kodein) {
            options("command", "Select frontend type", commands)

            if (!hasRest && v7_1_plus) {
                confirmation("addRest", "For correct work front module need rest addon added. Add rest addon?") {
                    default(true)
                }

                question("restVersion", "Specify rest addon version. You can see list of available versions on addon github page https://github.com/cuba-platform/restapi.") {
                    askIf("addRest")

                    default("0.1-SNAPSHOT")
                }
            }
        }.ask()

        val command: CliCommand by answers

        command.execute()

        if ("addRest" in answers && answers["addRest"] as Boolean) {
            val restVersion: String by answers
            projectService.registerAppComponent("com.haulmont.addon.restapi:restapi-global:$restVersion")
        }
    }

    override fun preExecute() {
        checkProjectExistence()

        val alreadyContainsModule = projectStructure.settingsGradle.toFile()
                .readText().contains("front")

        !alreadyContainsModule || fail(messages["moduleExistsError"])
    }
}