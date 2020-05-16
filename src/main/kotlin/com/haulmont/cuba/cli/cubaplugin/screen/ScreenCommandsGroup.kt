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

package com.haulmont.cuba.cli.cubaplugin.screen

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.AbstractCubaCommand
import com.haulmont.cuba.cli.core.commands.CliCommand
import com.haulmont.cuba.cli.core.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.cubaplugin.screen.entityscreen.CreateBrowseScreenCommand
import com.haulmont.cuba.cli.cubaplugin.screen.entityscreen.CreateEditScreenCommand
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.cubaplugin.screen.blankscreen.CreateScreenCommand
import com.haulmont.cuba.cli.cubaplugin.screen.entityscreen.MasterDetailScreenCommand
import com.haulmont.cuba.cli.cubaplugin.screen.screenextension.ExtendDefaultScreenCommand
import com.haulmont.cuba.cli.core.prompting.Option
import com.haulmont.cuba.cli.core.prompting.Prompts

@Parameters(commandDescription = "Create new CUBA screen")
object ScreenCommandsGroup : AbstractCubaCommand(), NonInteractiveInfo {
    override fun getNonInteractiveParameters(): Map<String, String> = mapOf(
            "type" to "Screen type"
    )

    override fun run() {

        val answers = Prompts.create {
            options("screen", "Select screen type", getScreenTypes())
        }.ask()

        val screen: CliCommand by answers

        screen.execute()
    }

    private fun getScreenTypes(): List<ScreenOption> {
        val v7 = projectModel.platformVersion >= PlatformVersion.v7
        val legacyVersion = PlatformVersion("6.10.0")


        val result = mutableListOf(
                ScreenOption("Create new browse screen", CreateBrowseScreenCommand()),
                ScreenOption("Create new edit screen", CreateEditScreenCommand())
        )

        if (v7) {
            result.add(ScreenOption("Create new master-detail screen", MasterDetailScreenCommand()))
        }

        result.add(ScreenOption("Create new blank screen", CreateScreenCommand()))

        if (v7) {
            result.add(ScreenOption("Create new legacy browse screen", CreateBrowseScreenCommand(forceVersion = legacyVersion)))
            result.add(ScreenOption("Create new legacy edit screen", CreateEditScreenCommand(forceVersion = legacyVersion)))
            result.add(ScreenOption("Create new legacy blank screen", CreateScreenCommand(forceVersion = legacyVersion)))
        }

        result.add(ScreenOption("Extend login and main screens", ExtendDefaultScreenCommand()))

        return result
    }

    private class ScreenOption(description: String, command: CliCommand): Option<CliCommand>("", description, command)
}