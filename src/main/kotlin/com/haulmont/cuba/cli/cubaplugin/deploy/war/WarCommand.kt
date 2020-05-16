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

package com.haulmont.cuba.cli.cubaplugin.deploy.war

import com.beust.jcommander.Parameters
import com.haulmont.cli.core.*
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.deploy.ContextXmlParams
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cli.core.prompting.Answers
import com.haulmont.cli.core.prompting.QuestionsList
import com.haulmont.cuba.cli.getTemplate
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files


@Parameters(commandDescription = "Generates buildWar gradle task")
class WarCommand : GeneratorCommand<WarModel>() {

    private val resources: Resources by Resources.fromMyPlugin()

    private val printWriter: PrintWriter by cubaKodein.instance<PrintWriter>()

    private val printHelper: PrintHelper by cubaKodein.instance<PrintHelper>()

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun getModelName(): String = "war"

    override fun QuestionsList.prompting() {

        val buildGradle = projectStructure.buildGradle.readText()
        if (buildGradle.contains("task buildWar(")) {
            printWriter.println("Your build.gradle already contains buildWar task. If you want to recreate it, manually remove it and restart the command")
            abort()
        }

        question("appHome", "Application home directory") {
            default("app_home")
        }

        confirmation("includeJdbc", "Include JDBC driver?")

        confirmation("includeTomcatContextXml", "Include Tomcat's context.xml?")

        confirmation("generateContextXml", "Generate custom context.xml?") {
            askIf("includeTomcatContextXml")
        }

        ContextXmlParams.run {
            askContextXmlParams(projectModel, questionName = "warContextParams", askCondition = "generateContextXml")
        }

        confirmation("specifyCustomContextXmlPath", "Specify custom context.xml path?") {
            askIf {
                it["generateContextXml"] == false
            }
        }
        question("customContextXmlPath", "Custom context.xml path") {
            askIf("specifyCustomContextXmlPath")
        }

        confirmation("singleWar", "Single WAR for Middleware and Web Client?")
        confirmation("generateWebXml", "Generate custom web.xml?") {
            askIf("singleWar")
        }
        question("customWebXmlPath", "Custom web.xml path") {
            askIf {
                it["generateWebXml"] == false
            }

            validate {
                checkIsNotBlank()
            }
        }

        confirmation("generateLogback", "Generate Logback configuration file?")
        confirmation("specifyLogback", "Specify Logback configuration file path?") {
            askIf {
                it["generateLogback"] == false
            }
        }
        question("customLogback", "Logback configuration file") {
            askIf("specifyLogback")
        }
    }

    override fun createModel(answers: Answers): WarModel = WarModel(answers, projectModel)

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(resources.getTemplate("war"), bindings) {
            if (model.generateContextXml) {
                transform("war-context.xml", projectStructure.path.resolve("modules", "core", "web", "META-INF"))
            }

            if (model.generateWebXml) {
                transform("single-war-web.xml", projectStructure.path.resolve("modules", "web", "web", "WEB-INF"))
            }

            if (model.generateLogback) {
                copy("war-logback.xml",
                        projectStructure.path.resolve("etc").also {
                            if (Files.notExists(it)) Files.createDirectories(it)
                        }
                )
            }

            projectStructure.buildGradle.let {
                it.appendText(transformToText("warTask"))
                printHelper.fileModified(it)
            }
        }
    }
}