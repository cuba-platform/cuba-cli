package com.haulmont.cuba.cli.cubaplugin

import com.google.common.eventbus.Subscribe
import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.CliPlugin
import com.haulmont.cuba.cli.event.AfterCommandExecutionEvent
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import com.haulmont.cuba.cli.event.InitPluginEvent

class ProjectScanPlugin : CliPlugin {
    private lateinit var context: CliContext

    @Subscribe
    fun onInit(event: InitPluginEvent) {
        context = event.cliContext

        event.commandsRegistry.setup {
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
                val projectModel = scanProject()
                context.addModel("project", projectModel)
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