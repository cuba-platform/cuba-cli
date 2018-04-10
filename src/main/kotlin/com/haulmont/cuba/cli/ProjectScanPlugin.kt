package com.haulmont.cuba.cli

import com.google.common.eventbus.Subscribe
import com.haulmont.cuba.cli.commands.CreateEntityCommand
import com.haulmont.cuba.cli.commands.ProjectInitCommand
import com.haulmont.cuba.cli.event.BeforeGenerationEvent
import com.haulmont.cuba.cli.event.CommandRegisterEvent
import com.haulmont.cuba.cli.event.LoadingEndEvent

class ProjectScanPlugin : CliPlugin {
    @Subscribe
    fun onStart(event: LoadingEndEvent) {
        val context = event.cliContext
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
    fun onRegisterCommands(event: CommandRegisterEvent) {
        event.commandsRegistry.setup {
            command("init", ProjectInitCommand())
            command("entity", CreateEntityCommand())
        }
    }

    @Subscribe
    fun onBeforeGeneration(event: BeforeGenerationEvent) {
        event.cliContext.getModels().toMap(event.bindings)
    }
}
