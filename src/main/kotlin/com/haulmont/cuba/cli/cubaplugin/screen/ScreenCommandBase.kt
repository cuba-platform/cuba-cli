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

package com.haulmont.cuba.cli.cubaplugin.screen

import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.cubaplugin.model.EntitySearch
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.ValidationHelper
import com.haulmont.cuba.cli.registration.ScreenRegistrationHelper
import org.kodein.di.generic.instance
import java.nio.file.Path

abstract class ScreenCommandBase<out Model : Any> : GeneratorCommand<Model>() {
    protected val entitySearch: EntitySearch by cubaKodein.instance()

    protected val namesUtils: NamesUtils by kodein.instance()

    protected val screensXml: Path by lazy {
        val webModule = projectStructure.getModule(ModuleStructure.WEB_MODULE)
        webModule.screensXml
    }

    protected val screenRegistrationHelper: ScreenRegistrationHelper by cubaKodein.instance()

    protected fun addToMenu(screenId: String, caption: String) {
        screenRegistrationHelper.addToMenu(screenId, caption)
    }

    protected fun checkScreenId(screenId: String) {
        screenRegistrationHelper.checkScreenId(screenId)
    }

    protected fun checkExistence(packageName: String, descriptor: String? = null, controller: String? = null) {
        screenRegistrationHelper.checkExistence(packageName, descriptor, controller)
    }

    protected fun addToScreensXml(id: String, packageName: String, descriptorName: String) {
        screenRegistrationHelper.addToScreensXml(id, packageName, descriptorName)
    }

    protected fun ValidationHelper<String>.screenIdDoesNotExists(screenId: String) {
        if (screenRegistrationHelper.isScreenIdExists(screenId))
            fail("Screen with id $screenId already exists")
    }

    protected fun ValidationHelper<String>.screenDescriptorDoesNotExists(descriptorName: String, packageName: String = packageName()) {
        if (screenRegistrationHelper.isDescriptorExists(packageName, descriptorName)) {
            fail("Such screen descriptor already exists")
        }
    }

    protected fun ValidationHelper<String>.screenControllerDoesNotExists(controllerName: String, packageName: String = answers["packageName"] as String) {
        if (screenRegistrationHelper.isControllerExists(packageName, controllerName)) {
            fail("Such screen controller already exists")
        }
    }

    protected fun ValidationHelper<String>.packageName() =
            answers["packageName"] as String
}