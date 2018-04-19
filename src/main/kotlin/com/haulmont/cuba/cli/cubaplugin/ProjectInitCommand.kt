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

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths

@Parameters(commandDescription = "Create new project")
class ProjectInitCommand : GeneratorCommand<ProjectInitModel>() {

    private val writer: PrintWriter by kodein.instance()

    override fun getModelName(): String = "project"

    override fun checkPreconditions() {
        super.checkPreconditions()

        check(!context.hasModel("project")) { "There is an existing project found in current directory." }
    }

    override fun QuestionsList.prompting() {
        question("projectName", "Project Name") {
            default { System.getProperty("user.dir").split(File.separatorChar).last() }
        }

        question("namespace", "Project Namespace") {
            default { answers ->
                val notAlphaNumeric = Regex("[^a-zA-Z0-9]")

                notAlphaNumeric.replace(answers["projectName"] as String, "")
            }

            validate {
                checkRegex("[a-zA-Z][a-zA-Z0-9]*", "Project namespace can contain only alphanumeric characters and start with a letter.")
            }
        }

        question("rootPackage", "Root package") {
            default { answers -> "com.company.${answers["namespace"]}" }
            validate {
                checkIsPackage()
            }
        }

        options("platformVersion", "Platform version", availablePlatformVersions) {
            default(1)
        }
    }

    override fun createModel(answers: Answers): ProjectInitModel {
        val rootPackage = answers["rootPackage"] as String
        return ProjectInitModel(
                answers["projectName"] as String,
                answers["namespace"] as String,
                rootPackage,
                answers["platformVersion"] as String,
                rootPackage.replace('.', File.separatorChar)
        )
    }

    override fun beforeGeneration(bindings: MutableMap<String, Any>) {
        val model = context.getModel<ProjectInitModel>(getModelName())
        bindings["rootPackageDirectory"] = model.rootPackageDirectory
    }

    override fun generate(bindings: Map<String, Any>) {
        val cwd = Paths.get("")
        TemplateProcessor("templates/project")
                .copyTo(cwd, bindings)

        writer.println("""

            @|white Project generated at ${cwd.toAbsolutePath()} directory
            To build project execute `gradlew assemble`
            To import project into IDE execute `gradlew ide` and open generated *.iml file|@

        """.trimIndent())
    }
}

data class ProjectInitModel(val name: String, val namespace: String, val rootPackage: String, val platformVersion: String, val rootPackageDirectory: String)

private val availablePlatformVersions = listOf("6.8.5", "6.9-SNAPSHOT")