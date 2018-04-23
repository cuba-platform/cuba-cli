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

import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.Prompts
import com.haulmont.cuba.cli.prompting.QuestionsList

abstract class GeneratorCommand<out Model : Any> : AbstractCommand() {
    override fun execute() {
        super.execute()
        val questions = QuestionsList { prompting() }
        val answers = if (CommonParameters.nonInteractive.isEmpty()) {
            Prompts(questions).ask()
        } else {
            Prompts(questions).askNonInteractive()
        }
        val model = createModel(answers)
        context.addModel(getModelName(), model)

        val bindings: MutableMap<String, Any> = mutableMapOf()
        context.getModels().toMap(bindings)
        beforeGeneration(bindings)

        generate(bindings.toMap())
    }

    abstract fun getModelName(): String

    abstract fun QuestionsList.prompting()

    abstract fun createModel(answers: Answers): Model

    open fun beforeGeneration(bindings: MutableMap<String, Any>) {}

    abstract fun generate(bindings: Map<String, Any>)
}