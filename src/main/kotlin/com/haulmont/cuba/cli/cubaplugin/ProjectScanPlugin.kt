package com.haulmont.cuba.cli.cubaplugin

import com.google.common.eventbus.Subscribe
import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.CliPlugin
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import com.haulmont.cuba.cli.event.InitPluginEvent
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.model.ProjectModel
import org.kodein.di.generic.instance

class ProjectScanPlugin : CliPlugin {
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
        val currentDir = context.currentDir
        val buildGradle = currentDir.list { _, name -> name == "build.gradle" }.firstOrNull()

        if (buildGradle != null) {
            try {
                context.addModel(ProjectModel.MODEL_NAME, scanProject())
            } catch (e: ProjectScanException) {
                println(e.message)
            }
        }
    }

    @Subscribe
    fun afterCommand(event: AfterCommandExecutionEvent) {
        context.clearModels()
    }
}