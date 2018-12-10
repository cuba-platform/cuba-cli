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

package com.haulmont.cuba.cli.cubaplugin.prefixchange

import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure
import com.haulmont.cuba.cli.PrintHelper
import com.haulmont.cuba.cli.cubaplugin.model.ProjectStructure
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.nio.file.Path

class PrefixChanger {

    private val printHelper: PrintHelper by kodein.instance()

    fun changePrefix(prefix: String) {
        val projectStructure = ProjectStructure()

        replacePrefix(projectStructure.buildGradle, prefix)
        replacePrefix(projectStructure.settingsGradle, prefix)

        val webAppProperties = projectStructure.getModule(ModuleStructure.WEB_MODULE)
                .rootPackageDirectory
                .resolve("web-app.properties")

        Properties.modify(webAppProperties) {
            update("cuba.connectionUrlList") {
                it?.replaceAfterLast('/', "$prefix-core") ?: "http://localhost:8080/$prefix-core"
            }
            set("cuba.webContextName", prefix)
        }

        val appProperties = projectStructure.getModule(ModuleStructure.CORE_MODULE)
                .rootPackageDirectory
                .resolve("app.properties")

        Properties.modify(appProperties) {
            set("cuba.webContextName", "$prefix-core")
        }
    }

    private fun replacePrefix(gradleScriptPath: Path, prefix: String) {
        val modulePrefixRegex = Regex("def *modulePrefix *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")

        val scriptFile = gradleScriptPath.toFile()

        scriptFile.readText()
                .replace(modulePrefixRegex, "def modulePrefix = \"$prefix\"")
                .let { scriptFile.writeText(it) }

        printHelper.fileModified(gradleScriptPath)
    }
}