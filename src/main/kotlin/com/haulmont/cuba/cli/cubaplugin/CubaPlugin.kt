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
import com.haulmont.cli.core.*
import com.haulmont.cli.core.commands.CdCommand
import com.haulmont.cli.core.event.BeforeCommandExecutionEvent
import com.haulmont.cli.core.event.InitPluginEvent
import com.haulmont.cuba.cli.cubaplugin.appcomponentxml.AppComponentCommand
import com.haulmont.cuba.cli.cubaplugin.componentbean.CreateComponentBeanCommand
import com.haulmont.cuba.cli.cubaplugin.config.ConfigCommand
import com.haulmont.cuba.cli.cubaplugin.deploy.CreateTaskCommandGroup
import com.haulmont.cuba.cli.cubaplugin.deploy.uberjar.UberJarCommand
import com.haulmont.cuba.cli.cubaplugin.deploy.war.WarCommand
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.entity.CreateEntityCommand
import com.haulmont.cuba.cli.cubaplugin.entitylistener.CreateEntityListenerCommand
import com.haulmont.cuba.cli.cubaplugin.enumeration.CreateEnumerationCommand
import com.haulmont.cuba.cli.cubaplugin.front.CreateFrontCommand
import com.haulmont.cuba.cli.cubaplugin.gradle.BuildCommand
import com.haulmont.cuba.cli.cubaplugin.gradle.GradleCommand
import com.haulmont.cuba.cli.cubaplugin.gradle.RunCommand
import com.haulmont.cuba.cli.cubaplugin.idea.IdeaOpenCommand
import com.haulmont.cuba.cli.cubaplugin.installcomponent.AddComponentCommand
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersionsManager
import com.haulmont.cuba.cli.cubaplugin.model.ProjectModel
import com.haulmont.cuba.cli.cubaplugin.model.ProjectScanException
import com.haulmont.cuba.cli.cubaplugin.model.ProjectStructure
import com.haulmont.cuba.cli.cubaplugin.prefixchange.PrefixChangeCommand
import com.haulmont.cuba.cli.cubaplugin.premiumrepo.EnablePremiumRepoCommand
import com.haulmont.cuba.cli.cubaplugin.project.ProjectInitCommand
import com.haulmont.cuba.cli.cubaplugin.screen.ScreenCommandsGroup
import com.haulmont.cuba.cli.cubaplugin.service.CreateServiceCommand
import com.haulmont.cuba.cli.cubaplugin.statictemplate.StaticTemplateCommand
import com.haulmont.cuba.cli.cubaplugin.theme.ThemeExtensionCommand
import com.haulmont.cuba.cli.cubaplugin.updatescript.UpdateScriptCommand
import org.jline.terminal.Terminal
import org.jline.terminal.impl.DumbTerminal
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("UNUSED_PARAMETER")
class CubaPlugin : MainCliPlugin {

    override val pluginsDir: Path? = Paths.get(System.getProperty("user.home"), ".haulmont", "cli", "plugins")
    override val priority: Int = 900
    override val prompt: String = "cuba>"
    override val apiVersion: Int = API_VERSION

    override val resources: ResourcesPath = HasResources(RESOURCES_PATH)

    override fun welcome() {
        printWelcome()
    }

    private val context: CliContext by kodein.instance<CliContext>()

    private val writer: PrintWriter by kodein.instance<PrintWriter>()

    private val namesUtils: NamesUtils by kodein.instance<NamesUtils>()

    private val printHelper: PrintHelper by kodein.instance<PrintHelper>()

    private val messages by localMessages()

    private val terminal: Terminal by kodein.instance<Terminal>()

    private val versionUtils: VersionUtils = VersionUtils()

    @Subscribe
    fun onInit(event: InitPluginEvent) {

        loadVersions()

        event.commandsRegistry {
            command("create-app", ProjectInitCommand())
            command("create-entity", CreateEntityCommand())
            command("create-screen", ScreenCommandsGroup)
            command("create-service", CreateServiceCommand())
            command("use-template", StaticTemplateCommand())
            command("create-bean", CreateComponentBeanCommand())
            command("create-entity-listener", CreateEntityListenerCommand())
            command("create-app-component-xml", AppComponentCommand())
            command("create-enumeration", CreateEnumerationCommand())
            command("extend-theme", ThemeExtensionCommand())
            command("add-component", AddComponentCommand())
            command("create-front-module", CreateFrontCommand())
            command("create-config", ConfigCommand())
            command("create-update-script", UpdateScriptCommand())
            command("enable-premium-repo", EnablePremiumRepoCommand())
            command("change-modules-prefix", PrefixChangeCommand())
            command("create-task", CreateTaskCommandGroup) {
                command("war", WarCommand())
                command("uberjar", UberJarCommand())
            }
            command("gradle", GradleCommand())
            command("idea", IdeaOpenCommand())
            command("build", BuildCommand())
            command("run", RunCommand())
        }
    }

    private fun loadVersions() {
        val versionManager: PlatformVersionsManager by cubaKodein.instance<PlatformVersionsManager>()
        versionManager.load()
    }

    @Subscribe
    fun beforeCommand(event: BeforeCommandExecutionEvent) {
        when (event.command) {
            is CdCommand -> return
        }

        context.addModel("names", namesUtils)
        context.addModel("versions", versionUtils)

        val projectStructure = try {
            ProjectStructure()
        } catch (e: Exception) {
            return
        }

        try {
            context.addModel(ProjectModel.MODEL_NAME, ProjectModel(projectStructure))
        } catch (e: ProjectScanException) {
            writer.println(messages["projectParsingError"].attention())

            printHelper.saveStacktrace(e)
        }
    }

    companion object {
        const val RESOURCES_PATH = "/com/haulmont/cuba/cli/cubaplugin/"
    }

    private fun printWelcome() {
        if (terminal !is DumbTerminal) {
            writer.println(messages["welcomeMessage"].trimMargin())
        } else {
            writer.println(messages["welcomeMessageDumb"].trimMargin())
        }
    }
}