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
import com.haulmont.cuba.cli.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersionsManager
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersionParseException
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersionsManagerImpl
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.Kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import java.util.logging.Level

@Parameters(commandDescription = "Creates new project")
class ProjectInitCommand(override val kodein: Kodein = cubaKodein) : GeneratorCommand<ProjectInitModel>(), NonInteractiveInfo {
    private val logger by thisClassLogger()

    private val messages by localMessages()

    private val resources by Resources.fromMyPlugin()

    private val workingDirectoryManager: WorkingDirectoryManager by kodein.instance()

    private val platformVersionsManager: PlatformVersionsManager by kodein.instance()

    private val databases by lazy {
        messages["databases"].split(',')
    }

    private val databasesAliases by lazy {
        messages["databaseAliases"].split(',')
    }

    private val writer: PrintWriter by kodein.instance()

    override fun getNonInteractiveParameters(): Map<String, String> = mapOf(
            "projectName" to "Project name",
            "namespace" to "Project namespace",
            "rootPackage" to "Root package",
            "platformVersion" to "Platform version",
            "database" to "Project database. Might be one of ${databasesAliases.printOptions()}."
    )

    override fun getModelName(): String = "project"

    override fun preExecute() {
        !context.hasModel("project") || fail("There is an existing project found in current directory.")

        try {
            (platformVersionsManager as? PlatformVersionsManagerImpl)?.loadThread?.join(20_000)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, e) { "" }
        }
    }

    override fun QuestionsList.prompting() {

        textOptions("repo", "Repository to be used in project.", REPOS) {
            default(0)
        }

        question("projectName", "Project Name") {
            if (isInteractiveMode()) {
                default(
                        ADJECTIVES.random() + "-" + ANIMALS.random()
                )
            }

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

                notAlphaNumeric
                        .replace(answers["projectName"] as String, "")
                        .replace("^[0-9]+".toRegex(), "")
                        .toLowerCase()
            }

            validate {
                checkRegex("[a-z][a-z0-9]*", "Project namespace can contain only lowercase alphanumeric characters and start with a letter.")
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

        if (isNonInteractiveMode()) {
            askCustomVersion()
            askVersion()
        } else {
            askVersion()
            askCustomVersion()
        }

        val databaseOptions = databasesAliases.takeIf { isNonInteractiveMode() } ?: databases

        textOptions("database", "Choose database", databaseOptions) {
            default(0)
        }

        confirmation("kotlinSupport", "Support kotlin?") {
            askIf { answers ->
                PlatformVersion((answers[PLATFORM_VERSION]
                        ?: answers[PREDEFINED_PLATFORM_VERSION]) as String) >= PlatformVersion.v7_2
            }
        }

        question("kotlinVersion", "Kotlin version") {
            default("1.3.41")
            askIf("kotlinSupport")
        }
    }

    private fun QuestionsList.askVersion() {
        textOptions(PREDEFINED_PLATFORM_VERSION, "Platform version", platformVersionsManager.versions + CUSTOM_VERSION) {
            askIf {
                PLATFORM_VERSION !in it
            }

            default(0)
        }
    }

    private fun QuestionsList.askCustomVersion() {
        question(PLATFORM_VERSION, "Platform version") {
            askIf {
                (it[PREDEFINED_PLATFORM_VERSION] == CUSTOM_VERSION) || isNonInteractiveMode()
            }

            validate {
                if (value.isBlank() && isInteractiveMode())
                    fail("Type platform version")

                try {
                    if (PlatformVersion(value) !in platformVersionsManager.supportedVersionsRange)
                        fail(platformVersionsManager.supportedVersionsRange.printAllowedVersionsRange())
                } catch (e: PlatformVersionParseException) {
                    fail("Unable to parse \"$value\" as platform version")
                }

            }
        }
    }

    override fun createModel(answers: Answers): ProjectInitModel = ProjectInitModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        workingDirectoryManager.workingDirectory = workingDirectoryManager.workingDirectory.resolve(model.projectName)
        val cwd = workingDirectoryManager.workingDirectory

        Files.createDirectories(cwd)

        val kotlinSupport = model.kotlinSupport

        val templateTips = TemplateProcessor(resources.getTemplate("project"), bindings, PlatformVersion(model.platformVersion)) {
            listOf("modules", "build.gradle", "settings.gradle", "\${gitignore}").forEach { it ->
                transform(it) { path ->
                    return@transform if (kotlinSupport) {
                        !path.fileName.toString().endsWith(".java")
                    } else {
                        !path.fileName.toString().endsWith(".kt")
                    }
                }
            }

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
    }

    companion object {
        private const val PREDEFINED_PLATFORM_VERSION = "predefinedPlatformVersion"
        private const val CUSTOM_VERSION = "another version"
        private const val PLATFORM_VERSION = "platformVersion"

        private val ANIMALS: List<String> = listOf("phoenix", "centaur", "mermaid", "leviathan", "dragon", "pegasus", "siren", "hydra", "sphinx", "unicorn", "wyvern", "behemoth", "griffon", "dodo", "mammoth")
        private val ADJECTIVES: List<String> = listOf("great", "cool", "ambitious", "generous", "cute", "dear", "nice", "reliable", "solid", "trusty", "simple", "pure", "brave", "manly", "fearless", "artful", "vivid", "utopic", "lucid", "radiant")

        private val REPOS = listOf("https://dl.bintray.com/cuba-platform/main", "https://repo.cuba-platform.com/content/groups/work")

        private fun <E> List<E>.random(): E = get(Random().nextInt(size))
    }
}