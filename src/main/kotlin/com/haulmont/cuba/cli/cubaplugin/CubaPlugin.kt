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
            command("init", ProjectInitCommand())
            command("entity", CreateEntityCommand())
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