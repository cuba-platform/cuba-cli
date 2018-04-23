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

package com.haulmont.cuba.cli.prompting

import com.haulmont.cuba.cli.commands.CommonParameters
import com.haulmont.cuba.cli.kodein
import org.fusesource.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.impl.completer.NullCompleter
import org.kodein.di.generic.instance
import java.io.PrintWriter

class Prompts internal constructor(private val questionsList: QuestionsList) {

    private val reader: LineReader by kodein.instance(arg = NullCompleter())
    private val writer: PrintWriter by kodein.instance()

    fun ask(): Answers = questionsList.getQuestions()
            .fold(mapOf()) { answers, question ->
                answers.toMutableMap()
                        .apply {
                            put(question.name, ask(question, answers))
                        }
            }

    fun askNonInteractive(): Answers {
        val answers = CommonParameters.nonInteractive

        questionsList.getQuestions().forEach {
            if (it.name !in answers) {
                throw ValidationException("Parameter ${it.name} not passed")
            }
            val value = answers[it.name] as String

            when (it) {
                is OptionsQuestion -> if (value !in it.options) {
                    throw ValidationException("Invalid value $value for parameter ${it.name}. Available values are ${it.options}.")
                }
                is WithValidation -> it.validation(value)
            }
        }

        return answers
    }


    private fun ask(question: Question, answers: Answers): Answer = when (question) {
        is OptionsQuestion -> {
            val defaultValue = question.defaultValue.get(answers)
            val prompt = createPrompt(question, defaultValue)
            val index = ask(question.validation, prompt, defaultValue).toInt() - 1
            question.options[index]
        }
        is PlainQuestion -> {
            val defaultValue = question.defaultValue.get(answers)
            val prompt = createPrompt(question, defaultValue)
            ask(question.validation, prompt, defaultValue)
        }
    }

    private tailrec fun ask(validation: (String) -> Unit, prompt: String, defaultValue: String): String {
        val answer = read(prompt).takeIf { it.isNotEmpty() } ?: defaultValue
        return if (answer satisfies validation)
            answer
        else
            ask(validation, prompt, defaultValue)
    }

    private fun createPrompt(question: Question, defaultValue: String): String {
        val answer = "> ${question.caption} "
        val defaultValuePostfix = when {
            defaultValue.isEmpty() -> ""
            else -> "@|red ($defaultValue) |@"
        }
        val options = when (question) {
            is OptionsQuestion -> printOptionsIndexed(question.options)
            is PlainQuestion -> ""
            else -> throw IllegalArgumentException()
        }
        return answer + defaultValuePostfix + options
    }


    private fun printOptionsIndexed(options: List<String>): String = options
            .foldIndexed("") { index, acc, s ->
                "$acc\n${index + 1}. $s "
            }

    private fun read(prompt: String): String = reader.readLine(Ansi.ansi().render(prompt).toString()).trim()

    private infix fun String.satisfies(validateFn: (String) -> Unit): Boolean =
            try {
                validateFn(this)
                true
            } catch (e: ValidationException) {
                writer.println("@|red ${e.message}|@")
                false
            }
}

private fun DefaultValue.get(answers: Answers): String = when (this) {
    is None -> ""
    is PlainValue -> this.value
    is CalculatedValue -> this.function(answers)
}

data class ValidationException(override val message: String) : RuntimeException(message)