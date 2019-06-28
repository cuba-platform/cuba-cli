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
import com.haulmont.cuba.cli.*
import com.haulmont.cuba.cli.commands.CdCommand
import com.haulmont.cuba.cli.cubaplugin.appcomponentxml.AppComponentCommand
import com.haulmont.cuba.cli.cubaplugin.componentbean.CreateComponentBeanCommand
import com.haulmont.cuba.cli.cubaplugin.config.ConfigCommand
import com.haulmont.cuba.cli.cubaplugin.deploy.CreateTaskCommandGroup
import com.haulmont.cuba.cli.cubaplugin.deploy.uberjar.UberJarCommand
import com.haulmont.cuba.cli.cubaplugin.deploy.war.WarCommand
import com.haulmont.cuba.cli.cubaplugin.entity.CreateEntityCommand
import com.haulmont.cuba.cli.cubaplugin.entitylistener.CreateEntityListenerCommand
import com.haulmont.cuba.cli.cubaplugin.enumeration.CreateEnumerationCommand
import com.haulmont.cuba.cli.cubaplugin.front.CreateFrontCommand
import com.haulmont.cuba.cli.cubaplugin.gradle.BuildCommand
import com.haulmont.cuba.cli.cubaplugin.gradle.GradleCommand
import com.haulmont.cuba.cli.cubaplugin.gradle.RunCommand
import com.haulmont.cuba.cli.cubaplugin.idea.IdeaOpenCommand
import com.haulmont.cuba.cli.cubaplugin.installcomponent.AddComponentCommand
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
import com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent
import com.haulmont.cuba.cli.event.InitPluginEvent
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.util.logging.Level
import java.util.logging.Logger

@Suppress("UNUSED_PARAMETER")
class CubaPlugin : CliPlugin {
    override val apiVersion: Int = API_VERSION

    override val resources: ResourcesPath = HasResources(RESOURCES_PATH)

    private val context: CliContext by kodein.instance()

    private val writer: PrintWriter by kodein.instance()

    private val namesUtils: NamesUtils by kodein.instance()

    private val printHelper: PrintHelper by kodein.instance()

    private val messages by localMessages()

    private val versionUtils: VersionUtils = VersionUtils()

    private val log: Logger = Logger.getLogger(CliPlugin::class.java.name)

    @Subscribe
    fun onInit(event: InitPluginEvent) {
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

        val projectModel = try {
            ProjectModel(projectStructure)
        } catch (e: Exception) {
            log.log(Level.WARNING, "Exception during project structure exploring", e)
        }

        try {
            context.addModel(ProjectModel.MODEL_NAME, projectModel)
        } catch (e: ProjectScanException) {
            log.log(Level.WARNING, "Exception during project model parsing", e)

            writer.println(messages["projectParsingError"].attention())

            printHelper.saveStacktrace(e)
        }
    }

    companion object {
        const val RESOURCES_PATH = "/com/haulmont/cuba/cli/cubaplugin/"
    }
}