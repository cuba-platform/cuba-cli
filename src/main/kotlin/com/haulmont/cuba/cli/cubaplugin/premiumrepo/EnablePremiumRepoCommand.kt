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

package com.haulmont.cuba.cli.cubaplugin.premiumrepo

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.core.PrintHelper
import com.haulmont.cuba.cli.core.Resources
import com.haulmont.cuba.cli.commands.AbstractCubaCommand
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.generation.Snippets
import com.haulmont.cuba.cli.core.localMessages
import com.haulmont.cuba.cli.core.prompting.Prompts
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths


@Parameters(commandDescription = "Adds premium CUBA repository to projects build script")
class EnablePremiumRepoCommand : AbstractCubaCommand() {
    private val gradlePropertiesPath = Paths.get(System.getProperty("user.home"), ".gradle", "gradle.properties")

    private val printHelper: PrintHelper by kodein.instance<PrintHelper>()
    private val printWriter: PrintWriter by kodein.instance<PrintWriter>()
    private val messages by localMessages()

    private val resources by Resources.fromMyPlugin()

    private val snippets by lazy {
        Snippets(resources, "premiumrepo")
    }

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun run() {
        if (!credentialsExists()) {
            val licenseKey = Prompts.create {
                question("licenseKey", "Input you license key") {
                    validate {
                        checkRegex("[0-9a-zA-Z]+-[0-9a-zA-Z]+", "License key should be form of ************-************")
                    }
                }
            }.ask()["licenseKey"] as String

            activate(licenseKey)
        }

        val buildGradle = Files.newInputStream(projectStructure.buildGradle).bufferedReader().use {
            it.readText()
        }

        if (!buildGradle.contains(snippets["premiumRepo"])) {

            buildGradle.replace(snippets["freeRepo"], snippets["bothRepos"]).let { text ->
                Files.newOutputStream(projectStructure.buildGradle).bufferedWriter().use {
                    it.write(text)
                }
            }

            printHelper.fileModified(projectStructure.buildGradle)
        } else {
            printWriter.println(messages["repoAlreadyAdded"])
        }
    }

    private fun activate(licenseKey: String) {
        Properties.modify(gradlePropertiesPath) {
            val userPassSplit = licenseKey.split("-")
            this[PREMIUM_REPO_USER] = userPassSplit[0]
            this[PREMIUM_REPO_PASS] = userPassSplit[1]
        }
    }


    private fun credentialsExists(): Boolean {
        val properties = Properties(gradlePropertiesPath)
        return properties[PREMIUM_REPO_USER] != null && properties[PREMIUM_REPO_PASS] != null
    }

    companion object {
        const val PREMIUM_REPO_USER = "premiumRepoUser"
        const val PREMIUM_REPO_PASS = "premiumRepoPass"
    }
}