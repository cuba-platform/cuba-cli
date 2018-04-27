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
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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

        generate(context.getModels())
    }

    abstract fun getModelName(): String

    abstract fun QuestionsList.prompting()

    abstract fun createModel(answers: Answers): Model

    abstract fun generate(bindings: Map<String, Any>)
}

fun <R, T> nameFrom(answers: Answers): ReadOnlyProperty<R, T> = object : ReadOnlyProperty<R, T> {
    override fun getValue(thisRef: R, property: KProperty<*>): T = answers[property.name] as T
}

/**
 * Unsafe get value from map
 */
@Suppress("UNCHECKED_CAST")
infix fun <V> String.from(map: Map<String, *>): V {
    return map[this] as V
}