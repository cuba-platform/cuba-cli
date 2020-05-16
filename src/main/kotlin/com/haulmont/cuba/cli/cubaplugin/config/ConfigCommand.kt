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

package com.haulmont.cuba.cli.cubaplugin.config

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure.Companion.CORE_MODULE
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure.Companion.GLOBAL_MODULE
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.core.Resources
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.core.prompting.Answers
import com.haulmont.cuba.cli.core.prompting.QuestionsList
import com.haulmont.cuba.cli.getTemplate

@Parameters(commandDescription = "Creates new configuration interface")
class ConfigCommand : GeneratorCommand<ConfigModel>() {

    private val resources by Resources.fromMyPlugin()

    override fun getModelName(): String = ConfigModel.NAME

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun QuestionsList.prompting() {
        question("name", "Config name") {
            validate {
                checkIsClass()
            }
        }

        textOptions("module", "Module", listOf(GLOBAL_MODULE, CORE_MODULE, WEB_MODULE))

        question("packageName", "Package name") {
            default(projectModel.rootPackage + ".config")

            validate {
                checkIsPackage()
            }
        }

        textOptions("sourceType", "Source type", listOf("SYSTEM", "APP", "DATABASE"))
    }

    override fun createModel(answers: Answers): ConfigModel = ConfigModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(resources.getTemplate("config"), bindings) {
            transformWhole()
        }
    }
}