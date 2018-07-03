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

import com.haulmont.cuba.cli.ModuleStructure
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.*
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.nio.file.Path

abstract class ScreenCommandBase<out Model : Any> : GeneratorCommand<Model>() {
    protected val namesUtils: NamesUtils by kodein.instance()

    protected fun addToScreensXml(screensXml: Path, id: String, packageName: String, descriptorName: String) {
        updateXml(screensXml) {
            appendChild("screen") {
                this["id"] = id
                val template = namesUtils.packageToDirectory(packageName) + '/' + descriptorName + ".xml"
                this["template"] = template
            }
        }
    }

    protected fun addToMenu(menuXml: Path, screenId: String, caption: String) {
        updateXml(menuXml) {
            val menuItem = findFirstChild("menu") ?: appendChild("menu")

            menuItem.appendChild("item") {
                this["id"] = screenId
                this["screen"] = screenId
            }
        }

        val mainMessages = projectStructure.getModule(ModuleStructure.WEB_MODULE)
                .rootPackageDirectory
                .resolve("web")
                .resolve("messages.properties")
        PropertiesHelper(mainMessages) {
            set("menu-config.$screenId", caption)
        }
    }
}