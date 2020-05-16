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

import com.haulmont.cli.core.commands.CommandExecutionException
import com.haulmont.cuba.cli.generation.findFirstChild
import com.haulmont.cuba.cli.generation.get
import com.haulmont.cuba.cli.generation.getChildElements
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cli.core.kodein
import net.sf.practicalxml.DomUtil
import org.kodein.di.generic.instance
import org.w3c.dom.Element
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class StaticTemplate(val path: Path, val name: String, val questions: List<TemplateQuestion>, val instructions: List<GenerationInstruction>, val registrations: List<Registration>)
data class GenerationInstruction(val from: String, val to: String, val transform: Boolean)

sealed class Registration
data class ScreenRegistration(val id: String, val packageName: String, val descriptorName: String, val addToMenu: String, val menuCaption: String) : Registration()
data class ServiceRegistration(val name: String, val packageName: String, val interfaceName: String) : Registration()
data class EntityRegistration(val className: String, val persistent: String) : Registration()


sealed class TemplateQuestion(val name: String, val caption: String)
class PlainQuestion(name: String, caption: String) : TemplateQuestion(name, caption)
class OptionsQuestion(name: String, caption: String, val options: List<String>) : TemplateQuestion(name, caption)

private val printWriter: PrintWriter by kodein.instance<PrintWriter>()

fun parseTemplate(templateName: String): StaticTemplate {
    val templateBasePath = findTemplate(templateName)
    val templateXml = templateBasePath
            .resolve("template.xml")

    if (!Files.exists(templateBasePath)) {
        printWriter.println("Unable to find template.xml for template $templateName")
        throw CommandExecutionException("Unable to find template.xml for template $templateName", silent = true)
    }

    val templateDocument = parse(templateXml).documentElement
    val questions: List<TemplateQuestion> = DomUtil.getChild(templateDocument, "questions").let {
        parseQuestions(it)
    }
    val instructions: List<GenerationInstruction> = DomUtil.getChild(templateDocument, "operations").let {
        parseGenerationInstructions(it)
    }
    val registrations: List<Registration> = templateDocument.findFirstChild("register")?.let {
        parseRegistrations(it)
    } ?: emptyList()

    return StaticTemplate(templateBasePath, templateDocument["name"], questions, instructions, registrations)
}

private fun parseQuestions(questionsListElement: Element): List<TemplateQuestion> =
        questionsListElement.getChildElements()
                .map {
                    val name = it["name"]
                    val caption = it["caption"]

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
                    val src = it["src"]
                    val dst = it["dst"]
                    when (it.tagName) {
                        "transform" -> GenerationInstruction(src, dst, true)
                        "copy" -> GenerationInstruction(src, dst, false)
                        else -> throw CommandExecutionException("Invalid template")
                    }
                }.toList()


private fun parseRegistrations(registrationsListElement: Element): List<Registration> =
        registrationsListElement.getChildElements()
                .map {
                    when (it.tagName) {
                        "screen" -> ScreenRegistration(
                                it.findFirstChild("id")!!.textContent,
                                it.findFirstChild("package")!!.textContent,
                                it.findFirstChild("descriptor")!!.textContent,
                                it.findFirstChild("add-to-menu")?.textContent ?: "false",
                                it.findFirstChild("menu-caption")?.textContent ?: "")

                        "service" -> ServiceRegistration(
                                it.findFirstChild("name")!!.textContent,
                                it.findFirstChild("package")!!.textContent,
                                it.findFirstChild("interface")!!.textContent)

                        "entity" -> EntityRegistration(
                                it.findFirstChild("class")!!.textContent,
                                it.findFirstChild("persistent")?.textContent ?: "true")

                        else -> throw CommandExecutionException("Invalid registration type ${it.tagName}")
                    }
                }.toList()

val CUSTOM_TEMPLATES_PATH: Path = Paths.get(System.getProperty("user.home"), ".haulmont", "cli", "templates")
        .also {
            if (!Files.exists(it)) {
                Files.createDirectories(it)
            }
        }

private fun findTemplate(templateBasePath: String): Path = CUSTOM_TEMPLATES_PATH.resolve(templateBasePath)