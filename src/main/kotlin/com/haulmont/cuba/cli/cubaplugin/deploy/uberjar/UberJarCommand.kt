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

package com.haulmont.cuba.cli.cubaplugin.deploy.uberjar

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.*
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.deploy.ContextXmlParams
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import com.haulmont.cuba.cli.prompting.ValidationHelper
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files

@Parameters(commandDescription = "Generates buildUberJar gradle task")
class UberJarCommand : GeneratorCommand<UberJarModel>() {

    private val resources: Resources by Resources.fromMyPlugin()

    private val printWriter: PrintWriter by cubaKodein.instance()

    private val printHelper: PrintHelper by cubaKodein.instance()

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun getModelName(): String = "uberJar"

    override fun QuestionsList.prompting() {
        val buildGradle = projectStructure.buildGradle.readText()
        if (buildGradle.contains("task buildUberJar(")) {
            printWriter.println("Your build.gradle already contains buildUberJar task. If you want to recreate it, manually remove it and restart the command")
            abort()
        }

        confirmation("singleUberJar", "Single Uber JAR")

        confirmation("generateLogback", "Generate Logback configuration file?")
        confirmation("specifyLogback", "Specify Logback configuration file path?") {
            askIf {
                it["generateLogback"] == false
            }
        }
        question("customLogback", "Logback configuration file") {
            askIf("specifyLogback")
        }

        confirmation("generateCustomJetty", "Generate custom Jetty environment file?")

        ContextXmlParams.run {
            askContextXmlParams(projectModel, questionName = "customJettyContextParams", askCondition = "generateCustomJetty")
        }

        question("customJettyPath", "Custom Jetty environment path") {
            askIf { it["generateCustomJetty"] == false }

            validate {
                checkIsNotBlank()
            }
        }

        question("corePort", "Core port") {
            askIf { it["singleUberJar"] == false }
            default("8079")

            validate {
                validatePort()
            }
        }

        question("webPort", "Web port") {
            default("8080")

            validate {
                validatePort()
            }
        }

        question("portalPort", "Portal port") {
            askIf { it["singleUberJar"] == false }
            default("8081")

            validate {
                validatePort()
            }
        }

    }

    private fun ValidationHelper<String>.validatePort() {
        checkRegex("[0-9]+", "Port value must be between 1 and 65535")

        if (value.toInt() < 1 || value.toInt() > 65535)
            fail("Port value must be between 1 and 65535")
    }

    override fun createModel(answers: Answers): UberJarModel = UberJarModel(answers, projectModel)

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(resources.getTemplate("uberjar"), bindings) {
            if (model.generateCustomJetty) {
                transform("jetty-env.xml", projectStructure.path.resolve("modules", "core", "web", "META-INF"))
            }

            if (model.generateLogback) {
                copy("uber-jar-logback.xml",
                        projectStructure.path.resolve("etc").also {
                            if (Files.notExists(it)) Files.createDirectories(it)
                        }
                )
            }

            projectStructure.buildGradle.let {
                it.appendText(transformToText("uberJarTask"))
                printHelper.fileModified(it)
            }
        }
    }
}