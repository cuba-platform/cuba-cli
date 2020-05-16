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

import com.haulmont.cli.core.PrintHelper
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure
import com.haulmont.cuba.cli.cubaplugin.model.ProjectStructure
import com.haulmont.cuba.cli.generation.updateXml
import com.haulmont.cuba.cli.generation.xpath
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ProjectServiceImpl : ProjectService {
    private val printHelper: PrintHelper by cubaKodein.instance<PrintHelper>()

    override fun registerAppComponent(coordinates: String) {
        val projectStructure = ProjectStructure()

        projectStructure.buildGradle.toFile().apply {
            val text = readText()
            val firstAppComponent = Regex("appComponent\\([^\n]*\\)")
                    .find(text)!!
                    .groupValues[0]

            val withNewComponent = text.replace(
                    firstAppComponent,
                    "$firstAppComponent\n    appComponent(\"$coordinates\")")
            writeText(withNewComponent)
        }
        printHelper.fileModified(projectStructure.buildGradle)

        for (module in listOf(ModuleStructure.WEB_MODULE, ModuleStructure.CORE_MODULE)) {
            val webXml = projectStructure.getModule(module).path
                    .resolve(Paths.get("web", "WEB-INF", "web.xml"))
            if (Files.exists(webXml)) {
                registerAppComponentInWebXml(webXml, coordinates)
            }
        }
    }

    private fun registerAppComponentInWebXml(webXml: Path, coordinates: String) {
        updateXml(webXml) {
            val registeredComponentsElement = xpath("//context-param[param-name[text()='appComponents']]/param-value").first()
            registeredComponentsElement.textContent = registeredComponentsElement.textContent + " " + coordinates.split(':')[0]
        }
    }
}