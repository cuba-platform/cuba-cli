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
import com.haulmont.cuba.cli.commands.AbstractCommand
import com.haulmont.cuba.cli.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.cubaplugin.screen.entityscreen.CreateBrowseScreenCommand
import com.haulmont.cuba.cli.cubaplugin.screen.entityscreen.CreateEditScreenCommand
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.cubaplugin.screen.blankscreen.CreateScreenCommand
import com.haulmont.cuba.cli.cubaplugin.screen.entityscreen.MasterDetailScreenCommand
import com.haulmont.cuba.cli.cubaplugin.screen.screenextension.ExtendDefaultScreenCommand
import com.haulmont.cuba.cli.prompting.Option
import com.haulmont.cuba.cli.prompting.Prompts

@Parameters(commandDescription = "Create new CUBA screen")
object ScreenCommandsGroup : AbstractCommand(), NonInteractiveInfo {
    override fun getNonInteractiveParameters(): Map<String, String> = mapOf(
            "type" to "Screen type"
    )

    override fun run() {

        val answers = Prompts.create {
            optionsWithDescription("type", "Select screen type", getScreenTypes())
        }.ask()

        val type: String by answers

        val command = getCommandsMap()[type]!!

        command.execute()
    }

    private fun getScreenTypes(): List<Option> {
        val v7 = projectModel.platformVersion >= PlatformVersion.v7

        val result = mutableListOf(
                Option("browse", "Create new browse screen"),
                Option("edit", "Create new edit screen")
        )

        if (v7) {
            result.add(Option("master_detail", "Create new master-detail screen"))
        }

        result.add(Option("custom", "Create new blank screen"))

        if (v7) {
            result.add(Option("browse_legacy", "Create new legacy browse screen"))
            result.add(Option("edit_legacy", "Create new legacy edit screen"))
            result.add(Option("custom_legacy", "Create new legacy blank screen"))
        }

        result.add(Option("extend", "Extend login and main screens"))

        return result
    }

    private fun getCommandsMap(): Map<String, ScreenCommandBase<Any>> {
        val legacyVersion = PlatformVersion("6.10.0")

        return mapOf(
                "custom" to CreateScreenCommand(),
                "extend" to ExtendDefaultScreenCommand(),
                "master_detail" to MasterDetailScreenCommand(),
                "browse_legacy" to CreateBrowseScreenCommand(forceVersion = legacyVersion),
                "edit_legacy" to CreateEditScreenCommand(forceVersion = legacyVersion),
                "custom_legacy" to CreateScreenCommand(forceVersion = legacyVersion),
                "browse" to CreateBrowseScreenCommand(),
                "edit" to CreateEditScreenCommand()
        )
    }

}