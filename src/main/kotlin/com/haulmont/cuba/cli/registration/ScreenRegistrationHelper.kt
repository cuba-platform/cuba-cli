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

package com.haulmont.cuba.cli.registration

import com.haulmont.cuba.cli.ModuleStructure
import com.haulmont.cuba.cli.ProjectStructure
import com.haulmont.cuba.cli.commands.CommandExecutionException
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.generation.*
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Path

class ScreenRegistrationHelper {
    private val screensXml: Path
        get() = ProjectStructure().getModule(ModuleStructure.WEB_MODULE).screensXml

    private val namesUtils: NamesUtils by kodein.instance()

    fun addToScreensXml(id: String, packageName: String, descriptorName: String) {
        updateXml(screensXml) {
            appendChild("screen") {
                this["id"] = id
                val template = namesUtils.packageToDirectory(packageName) + '/' + descriptorName + ".xml"
                this["template"] = template
            }
        }
    }

    fun checkScreenId(screenId: String) {
        parse(screensXml).documentElement
                .xpath("//screen[@id=\"$screenId\"]")
                .firstOrNull()?.let {
                    throw CommandExecutionException("Screen with id \"$screenId\" already exists")
                }
    }

    fun checkExistence(packageName: String, descriptor: String? = null, controller: String? = null) {
        val packagePath = ProjectStructure().getModule(ModuleStructure.WEB_MODULE).resolvePackagePath(packageName)

        descriptor?.let {
            ensureFileAbsence(packagePath.resolve("$descriptor.xml"),
                    cause = "Screen descriptor $packageName.$descriptor.xml already exists")
        }
        controller?.let {
            ensureFileAbsence(packagePath.resolve("$controller.java"),
                    cause = "Screen controller $packageName.$controller already exists")
        }
    }

    fun addToMenu(screenId: String, caption: String) {
        val projectStructure = ProjectStructure()
        val webModule = projectStructure.getModule(ModuleStructure.WEB_MODULE)

        updateXml(webModule.rootPackageDirectory.resolve("web-menu.xml")) {
            val menuItem = findFirstChild("menu") ?: appendChild("menu")

            menuItem.appendChild("item") {
                this["id"] = screenId
                this["screen"] = screenId
            }
        }

        val mainMessages = webModule
                .rootPackageDirectory
                .resolve("web")
                .resolve("messages.properties")
        Properties.modify(mainMessages) {
            set("menu-config.$screenId", caption)
        }
    }

    private fun ensureFileAbsence(file: Path, cause: String, silent: Boolean = false) {
        if (Files.exists(file))
            throw CommandExecutionException(cause, silent = silent)
    }

}