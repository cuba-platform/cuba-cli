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

package com.haulmont.cuba.cli.commands

import com.haulmont.cuba.cli.LatestVersion
import com.haulmont.cuba.cli.ProjectModel
import com.haulmont.cuba.cli.ProjectStructure
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.Prompts
import com.haulmont.cuba.cli.prompting.QuestionsList

/**
 * Base command for all commands, that generate any artifact.
 *
 * GeneratorCommand has special lifecycle.
 *
 * Prompting [GeneratorCommand.prompting] is the first phase,
 * at which user is asked with questions about ahead generated artifact.
 *
 * After that, the command creates artifact model based on the prompting phase user answers and register it in the cliContext
 * by name retrieved from [getModelName].
 *
 * At generation phase, the command gets all available models as ```Map<String, Any>``` and generates artifact.
 */
abstract class GeneratorCommand<out Model : Any> : AbstractCommand() {
    protected val projectStructure: ProjectStructure by lazy { ProjectStructure() }

    protected val projectModel: ProjectModel by lazy {
        if (context.hasModel(ProjectModel.MODEL_NAME)) {
            context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)
        } else fail("No project module found")
    }

    protected val model: Model by lazy {
        if (context.hasModel(getModelName())) {
            context.getModel<Model>(getModelName())
        } else fail("Model has not yet been created")
    }

    override fun run() {
        val questions = QuestionsList { prompting() }
        val answers = if (CommonParameters.nonInteractive.isEmpty()) {
            Prompts(questions).ask()
        } else {
            Prompts(questions).askNonInteractive()
        }
        val model = createModel(answers)
        context.addModel(getModelName(), model)

        beforeGeneration()

        generate(context.getModels())
    }

    @Throws(CommandExecutionException::class)
    open fun beforeGeneration() {

    }

    abstract fun getModelName(): String

    /**
     * Specifies question that will be asked to user.
     * User may provide answers in non-interactive mode by specifying answers as command options with syntax
     * ```
     * command -PquestionOne=answerOne -PquestionTwo=answerTwo ...
     * ```
     */
    abstract fun QuestionsList.prompting()

    abstract fun createModel(answers: Answers): Model

    abstract fun generate(bindings: Map<String, Any>)

    fun processTemplate(templateName: String, bindings: Map<String, Any>, block: TemplateProcessor.() -> Unit) {
        if (context.hasModel(ProjectModel.MODEL_NAME)) {
            TemplateProcessor(templateName, bindings, projectModel.platformVersion, block)
        } else {
            TemplateProcessor(templateName, bindings, LatestVersion, block)
        }
    }
}

/**
 * Unsafe get value from map with automatic type casting.
 */
@Suppress("UNCHECKED_CAST")
infix fun <V> String.from(map: Map<String, *>): V {
    return map[this] as V
}