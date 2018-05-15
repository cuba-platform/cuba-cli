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
import com.haulmont.cuba.cli.commands.CommandExecutionException
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.getChildElements
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import net.sf.practicalxml.DomUtil
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Paths

@Parameters(commandDescription = "Generates artifacts from custom template")
class StaticTemplateCommand : GeneratorCommand<Answers>() {

    @Parameter(required = true, description = "Template name")
    private var templateName: String? = null

    private var template: Template? = null

    override fun getModelName(): String = template!!.modelName

    override fun run() {
        template = parseTemplate(templateName!!)
        super.execute()
    }

    override fun QuestionsList.prompting() {
        for (question in template!!.questions) {
            when (question) {
                is PlainQuestion -> question(question.name, question.caption)
                is OptionsQuestion -> options(question.name, question.caption, question.options)
            }
        }
    }

    override fun createModel(answers: Answers): Answers = answers

    override fun generate(bindings: Map<String, Any>) {
        val template = template!!
        TemplateProcessor(template.path, bindings, projectModel.platformVersion) {
            for (instruction in template.instructions) {
                if (instruction.transform) {
                    transform(instruction.from, Paths.get(instruction.to))
                } else {
                    copy(instruction.from, Paths.get(instruction.to))
                }
            }
        }
    }

    private fun parseTemplate(templateName: String): Template? {
        val templateBasePath = TemplateProcessor.findTemplate(templateName)
        val templateXml = templateBasePath
                .resolve("template.xml")

        if (!Files.exists(templateBasePath)) {
            throw CommandExecutionException("Unable to find template.xml")
        }

        val templateDocument = parse(templateXml).documentElement
        val questions: List<TemplateQuestion> = DomUtil.getChild(templateDocument, "quesitons").let {
            parseQuestions(it)
        }
        val instructions: List<GenerationInstruction> = DomUtil.getChild(templateDocument, "operations").let {
            parseGenerationInstructions(it)
        }
        return Template(templateBasePath, templateDocument.getAttribute("modelName"), questions, instructions)
    }

    private fun parseQuestions(questionsListElement: Element): List<TemplateQuestion> =
            questionsListElement.getChildElements()
                    .map {
                        val name = it.getAttribute("name")
                        val caption = it.getAttribute("caption")

                        when (it.tagName) {
                            "plain" -> PlainQuestion(name, caption)
                            "options" -> OptionsQuestion(name, caption, parseOptions(it))
                            else -> throw CommandExecutionException("Invalid template")
                        }
                    }.toList()


    private fun parseOptions(optionsQuestion: Element): List<String> =
            optionsQuestion.getChildElements()
                    .map {
                        when (it.tagName) {
                            "option" -> it.textContent
                            else -> throw CommandExecutionException("Invalid template")
                        }
                    }.toList()


    private fun parseGenerationInstructions(instructionsListElement: Element): List<GenerationInstruction> =
            instructionsListElement.getChildElements()
                    .map {
                        val src = it.getAttribute("src")
                        val dst = it.getAttribute("dst")
                        when (it.tagName) {
                            "transform" -> GenerationInstruction(src, dst, true)
                            "copy" -> GenerationInstruction(src, dst, false)
                            else -> throw CommandExecutionException("Invalid template")
                        }
                    }.toList()

}