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
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers

@Parameters(commandDescription = "Creates new master-detail screen")
class MasterDetailScreenCommand : EntityScreenCommandBase<EntityScreenModel>() {
    override fun getModelName(): String = EntityScreenModel.MODEL_NAME

    override fun createModel(answers: Answers): EntityScreenModel = EntityScreenModel(answers, entitySearch)

    override fun getDefaultScreenId(entityName: String) = projectModel.namespace + "$" + entityName.split('.').last() + ".browse"

    override fun getDefaultControllerName(entityName: String) = entityName.split('.').last() + "Browse"

    override fun getDefaultDescriptorName(entityName: String) = entityName.split('.').last().toLowerCase() + "-browse"

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(resources.getTemplate("masterDetailScreen"), bindings) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(ModuleStructure.WEB_MODULE)
        val messages = webModule.resolvePackagePath(model.packageName).resolve("messages.properties")

        Properties.modify(messages) {
            set("browseCaption", model.entity.className + " browser")
        }

        addToMenu(model.screenId, "${model.entity.className}s")
    }
}