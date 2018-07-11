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

package com.haulmont.cuba.cli.cubaplugin.project

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.*
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.*

@Parameters(commandDescription = "Creates new project")
class ProjectInitCommand : GeneratorCommand<ProjectInitModel>() {
    private val messages by localMessages()

    private val workingDirectoryManager: WorkingDirectoryManager by kodein.instance()

    private val platformVersionsManager: PlatformVersionsManager by kodein.instance()

    private val databases by lazy {
        messages["databases"].split(',')
    }

    private val writer: PrintWriter by kodein.instance()

    override fun getModelName(): String = "project"

    override fun preExecute() {
        !context.hasModel("project") || fail("There is an existing project found in current directory.")
    }

    override fun QuestionsList.prompting() {
        question("projectName", "Project Name") {
            default(
                    ADJECTIVES.random() + "-" + ANIMALS.random()
            )

            validate {
                val invalidNameRegex = Regex("[^\\w\\-]")

                if (invalidNameRegex.find(value) != null) {
                    fail("Project name should contain only Latin letters, digits, dashes and underscores.")
                }

                if (value.isBlank()) {
                    fail("Empty names not allowed")
                }

                if (Paths.get(value).let { Files.exists(it) && Files.isDirectory(it) }) {
                    fail("Directory with such name $value already exists")
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
            default { answers -> "com.company.${answers["namespace"]}".toLowerCase() }
            validate {
                checkIsPackage()

                if (value.toLowerCase() != value)
                    fail("Root package is allowed in lower case")
            }
        }

        options("platformVersion", "Platform version", platformVersionsManager.versions + CUSTOM_VERSION) {
            default(0)
        }

        question("customPlatformVersion", "Platform version") {
            askIf {
                it["platformVersion"] == CUSTOM_VERSION
            }

            validate {
                if (value.isBlank())
                    fail("Type platform version")
            }
        }

        options("database", "Choose database", databases) {
            default(0)
        }
    }

    override fun createModel(answers: Answers): ProjectInitModel = ProjectInitModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        workingDirectoryManager.workingDirectory = Paths.get(model.projectName)
        val cwd = workingDirectoryManager.workingDirectory

        Files.createDirectories(cwd)

        val templateTips = TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "project", bindings, PlatformVersion(model.platformVersion)) {
            listOf("modules", "build.gradle", "settings.gradle").forEach {
                transform(it)
            }
            copy("gitignore")
            Files.move(cwd.resolve("gitignore"), cwd.resolve(".gitignore"))

            listOf("gradle", "gradlew", "gradlew.bat").forEach {
                copy(it)
            }
            try {
                Files.setPosixFilePermissions(cwd.resolve("gradlew"), setOf(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ))
            } catch (e: Exception) {
                //todo warn if current system is *nix
            }
        }

        templateTips?.let { writer.println(it.format(cwd.toAbsolutePath().toString())) }

        val dpTipsMessageName = when (model.database.database) {
            databases[5] -> "oracleDbTips"
            databases[6] -> "mysqlDbTips"
            else -> return
        }

        writer.println(messages[dpTipsMessageName])
    }

    companion object {
        private val ANIMALS: List<String> = listOf("phoenix", "centaur", "mermaid", "leviathan", "dragon", "pegasus", "siren", "hydra", "sphinx", "unicorn", "wyvern", "behemoth", "griffon", "dodo", "mammoth")
        private val ADJECTIVES: List<String> = listOf("great", "cool", "ambitious", "generous", "cute", "dear", "nice", "reliable", "solid", "trusty", "simple", "pure", "brave", "manly", "fearless", "artful", "vivid", "utopic", "lucid", "radiant")
        private const val CUSTOM_VERSION = "another version"

        private fun <E> List<E>.random(): E = get(Random().nextInt(size))
    }
}