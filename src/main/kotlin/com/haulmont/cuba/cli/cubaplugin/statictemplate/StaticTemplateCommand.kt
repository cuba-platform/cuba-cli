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

package com.haulmont.cuba.cli.cubaplugin.statictemplate

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.LatestVersion
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import java.nio.file.Paths

@Parameters(commandDescription = "Generates artifacts from custom template")
class StaticTemplateCommand : GeneratorCommand<Answers>() {

    @Parameter(required = true, description = "Template name")
    private lateinit var templateName: String

    private val template: Template by lazy {
        parseTemplate(templateName)
    }

    override fun getModelName(): String = template.modelName

    override fun QuestionsList.prompting() {
        for (question in template.questions) {
            when (question) {
                is PlainQuestion -> question(question.name, question.caption)
                is OptionsQuestion -> options(question.name, question.caption, question.options)
            }
        }
    }

    override fun createModel(answers: Answers): Answers = answers

    override fun generate(bindings: Map<String, Any>) {
        val platformVersion = try {
            projectModel.platformVersion
        } catch (e: Exception) {
            LatestVersion
        }

        TemplateProcessor(template.path, bindings, platformVersion) {
            for (instruction in template.instructions) {
                if (instruction.transform) {
                    transform(instruction.from, Paths.get(instruction.to))
                } else {
                    copy(instruction.from, Paths.get(instruction.to))
                }
            }
        }
    }
}