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
import com.haulmont.cuba.cli.commands.nameFrom
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

            validate {
                val invalidNameRegex = Regex("[^\\w\\-]")

                if (invalidNameRegex.find(value) != null) {
                    fail("Project name should contain only Latin letters, digits, dashes and underscores.")
                }

                if (value.isBlank()) {
                    fail("Empty names not allowed")
                }
            }
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
            default(0)
        }
    }

    override fun createModel(answers: Answers): ProjectInitModel = ProjectInitModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        val cwd = Paths.get("")

        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "project", bindings) {
            listOf("modules", "build.gradle", "settings.gradle").forEach {
                transform(it)
            }
            copy("gitignore", Paths.get(".gitignore"))
            listOf("gradle", "gradlew", "gradlew.bat").forEach {
                copy(it)
            }
        }

        writer.println("""

            @|white Project generated at ${cwd.toAbsolutePath()} directory
            To build project execute `gradlew assemble`
            To import project into IDE execute `gradlew ide` and open generated *.iml file|@

        """.trimIndent())
    }
}

class ProjectInitModel(answers: Answers) {
    val projectName: String by nameFrom(answers)
    val namespace: String by nameFrom(answers)
    val rootPackage: String by nameFrom(answers)
    val platformVersion: String by nameFrom(answers)
    val rootPackageDirectory: String = rootPackage.replace('.', File.separatorChar)

}

private val availablePlatformVersions = listOf("6.8.5", "6.9-SNAPSHOT")