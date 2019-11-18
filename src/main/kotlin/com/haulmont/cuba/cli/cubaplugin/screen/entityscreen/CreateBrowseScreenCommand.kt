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

package com.haulmont.cuba.cli.cubaplugin.screen.entityscreen

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.cubaplugin.model.Entity
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers

@Parameters(commandDescription = "Creates new browse screen")
class CreateBrowseScreenCommand(private val forceVersion: PlatformVersion? = null) : EntityScreenCommandBase<EntityScreenModel>(), NonInteractiveInfo {
    private val version: PlatformVersion
        get() = forceVersion ?: projectModel.platformVersion

    override fun getModelName(): String = EntityScreenModel.MODEL_NAME

    override fun createModel(answers: Answers): EntityScreenModel = EntityScreenModel(answers)

    override fun getDefaultScreenId(entityName: String) = "$entityName.browse"

    override fun getDefaultControllerName(entity: Entity) = entity.className + "Browse"

    override fun getDefaultDescriptorName(entity: Entity) = entity.className.toLowerCase() + "-browse"

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(resources.getTemplate("browseScreen"), bindings, version) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(ModuleStructure.WEB_MODULE)

        if (version < PlatformVersion.v7) {
            addToScreensXml(model.screenId, model.packageName, model.descriptorName)
        }

        val messages = webModule.resolvePackagePath(model.packageName).resolve("messages.properties")

        Properties.modify(messages) {
            set("browseCaption", model.entity.className + " browser")
        }

        addToMenu(model.screenId, "${model.entity.className}s")
    }

}