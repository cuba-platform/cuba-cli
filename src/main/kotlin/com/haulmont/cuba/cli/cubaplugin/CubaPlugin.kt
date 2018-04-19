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

package com.haulmont.cuba.cli.cubaplugin

import com.google.common.eventbus.Subscribe
import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.CliPlugin
import com.haulmont.cuba.cli.ProjectFiles
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import com.haulmont.cuba.cli.event.InitPluginEvent
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.model.ProjectScanException
import org.kodein.di.generic.instance

class CubaPlugin : CliPlugin {
    private val context: CliContext by kodein.instance()

    @Subscribe
    fun onInit(event: InitPluginEvent) {
        event.commandsRegistry {
            command("create-app", ProjectInitCommand())
            command("entity", CreateEntityCommand())
            command("screen", CreateScreenCommand())
            command("service", CreateServiceCommand())
        }
    }

    @Subscribe
    fun beforeCommand(event: BeforeCommandExecutionEvent) {
        val projectFiles = try {
            ProjectFiles()
        } catch (e: Exception) {
            return
        }

        try {
            context.addModel(ProjectModel.MODEL_NAME, ProjectModel(projectFiles))
        } catch (e: ProjectScanException) {
            println(e.message)
        }
    }

    @Subscribe
    fun afterCommand(event: AfterCommandExecutionEvent) {
        context.clearModels()
    }
}