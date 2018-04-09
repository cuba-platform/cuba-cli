package com.haulmont.cuba.cli

import com.google.common.eventbus.Subscribe
import com.haulmont.cuba.cli.commands.CreateEntityCommand
import com.haulmont.cuba.cli.commands.ProjectInitCommand
import com.haulmont.cuba.cli.event.BeforeGenerationEvent
import com.haulmont.cuba.cli.event.CommandRegisterEvent
import com.haulmont.cuba.cli.event.LoadEndEvent
import com.haulmont.cuba.cli.model.ProjectModel

class ProjectScanPlugin : CliPlugin {
    @Subscribe
    fun onStart(event: LoadEndEvent) {
        val currentDir = event.cliContext.currentDir
        val buildGradle = currentDir.list { _, name -> name == "build.gradle" }.firstOrNull()

        if (buildGradle != null) {
            scanProject(event.cliContext)
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

    private fun scanProject(cliContext: CliContext) {
        val model = ProjectModel()
        model.name = ""
        cliContext.addModel("project", model)
    }
}