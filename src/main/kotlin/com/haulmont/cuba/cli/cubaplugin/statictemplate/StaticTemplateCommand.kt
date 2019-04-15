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
import com.haulmont.cuba.cli.cubaplugin.model.LatestVersion
import com.haulmont.cuba.cli.WorkingDirectoryManager
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.VelocityHelper
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import com.haulmont.cuba.cli.registration.EntityRegistrationHelper
import com.haulmont.cuba.cli.registration.ScreenRegistrationHelper
import com.haulmont.cuba.cli.registration.ServiceRegistrationHelper
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files
import java.util.stream.Collectors

@Parameters(commandDescription = "Generates artifacts from custom template")
class StaticTemplateCommand : GeneratorCommand<Answers>() {

    private val writer: PrintWriter by kodein.instance()

    private val workingDirectoryManager: WorkingDirectoryManager by kodein.instance()

    private val screenRegistrationHelper: ScreenRegistrationHelper by cubaKodein.instance()
    private val serviceRegistrationHelper: ServiceRegistrationHelper by cubaKodein.instance()
    private val entityRegistrationHelper: EntityRegistrationHelper by cubaKodein.instance()

    private val velocityHelper = VelocityHelper()

    @Parameter(description = "Template name")
    private var templateName: String? = null

    private val template: StaticTemplate by lazy {
        parseTemplate(templateName!!)
    }

    override fun run() {
        if (templateName != null) {
            super.run()
        } else {
            Files.walk(CUSTOM_TEMPLATES_PATH, 1)
                    .filter {
                        it != CUSTOM_TEMPLATES_PATH &&
                                Files.isDirectory(it) &&
                                Files.exists(it.resolve("template.xml"))
                    }
                    .map {
                        it.fileName.toString()
                    }
                    .collect(Collectors.toList())
                    .joinTo(StringBuilder(), prefix = "These templates are available: ", postfix = ".")
                    .let {
                        writer.println(it)
                    }
        }
    }

    override fun getModelName(): String = template.name

    override fun QuestionsList.prompting() {
        for (question in template.questions) {
            when (question) {
                is PlainQuestion -> question(question.name, question.caption)
                is OptionsQuestion -> textOptions(question.name, question.caption, question.options)
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

        val cwd = workingDirectoryManager.workingDirectory

        val maybeHints = TemplateProcessor(template.path, bindings, platformVersion) {
            for (instruction in template.instructions) {
                if (instruction.transform) {
                    transform(instruction.from, cwd.resolve(instruction.to))
                } else {
                    copy(instruction.from, cwd.resolve(instruction.to))
                }
            }
        }

        for (registration in template.registrations) {
            when (registration) {
                is ScreenRegistration -> {
                    val id = velocityHelper.generate(registration.id, "id", bindings)
                    val packageName = velocityHelper.generate(registration.packageName, "packageName", bindings)
                    val descriptorName = velocityHelper.generate(registration.descriptorName, "descriptorName", bindings)
                    val addToMenu = velocityHelper.generate(registration.addToMenu, "addToMenu", bindings) == "true"
                    val menuCaption = velocityHelper.generate(registration.menuCaption, "menuCaption", bindings)

                    screenRegistrationHelper.checkScreenId(id)
                    screenRegistrationHelper.addToScreensXml(id, packageName, descriptorName)

                    if (addToMenu) {
                        screenRegistrationHelper.addToMenu(id, menuCaption)
                    }

                }
                is ServiceRegistration -> {
                    val name = velocityHelper.generate(registration.name, "name", bindings)
                    val packageName = velocityHelper.generate(registration.packageName, "packageName", bindings)
                    val interfaceName = velocityHelper.generate(registration.interfaceName, "interfaceName", bindings)

                    serviceRegistrationHelper.registerService(name, packageName, interfaceName)
                }
                is EntityRegistration -> {
                    val className = velocityHelper.generate(registration.className, "className", bindings)
                    val persistent = velocityHelper.generate(registration.persistent, "persistent", bindings) == "true"

                    entityRegistrationHelper.registerEntity(className, persistent)
                }
            }
        }

        maybeHints?.let { writer.print(it) }
    }
}