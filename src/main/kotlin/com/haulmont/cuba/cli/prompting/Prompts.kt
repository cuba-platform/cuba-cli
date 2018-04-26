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

import com.haulmont.cuba.cli.commands.CommandExecutionException
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

    fun ask(): Answers = questionsList.ask()

    private fun <T : Any> askSimple(question: PlainQuestion<T>, answers: Answers): Answer {
        val value = question.ask(answers)
        return if (question is OptionsQuestion) {
            question.options[value as Int]
        } else value
    }

    private tailrec fun CompositeQuestion.ask(): Answers {
        try {
            return this.fold<Question, Map<String, Answer>>(mapOf()) { doneAnswers, question ->
                when (question) {
                    is PlainQuestion<*> -> {
                        val answer = askSimple(question, doneAnswers)
                        doneAnswers + (question.name to answer)
                    }
                    is CompositeQuestion -> {
                        val answer = question.ask()
                        if (question.isFlat) {
                            doneAnswers + answer
                        } else {
                            doneAnswers + (question.name to answer)
                        }
                    }
                }
            }.also(validation)
        } catch (e: ValidationException) {
            writer.println("@|red ${e.message}|@")
        }
        return this.ask()
    }

    private tailrec fun <T : Any> PlainQuestion<T>.ask(answers: Answers, maybePrompts: String? = null): T {
        val prompts = maybePrompts ?: printPrompts(answers)
        try {
            return read(prompts).let {
                if (it.isNotEmpty()) return@let it.read()
                val defaultValue = defaultValue
                when (defaultValue) {
                    is PlainValue -> defaultValue.value
                    is CalculatedValue -> defaultValue.function(answers)
                    else -> it.read()
                }
            }.also {
                validation(it)
            }
        } catch (e: Exception) {
            when (e) {
                is ValidationException, is ReadException -> writer.println("@|red ${e.message}|@")
                else -> throw e
            }

        }

        return ask(answers, prompts)
    }

    private fun read(prompt: String): String = reader.readLine(Ansi.ansi().render(prompt).toString()).trim()

    fun askNonInteractive(): Answers {
        val answers = CommonParameters.nonInteractive

        if (!questionsList.all { it is PlainQuestion<*> }) {
            throw CommandExecutionException("Non interactive mode unavailable for complex questions")
        }

        questionsList.filterIsInstance(PlainQuestion::class.java).forEach {
            checkQuestion(it, answers)
        }

        return answers
    }

    private fun <T : Any> checkQuestion(it: PlainQuestion<T>, answers: Map<String, String>) {
        if (it.name !in answers) {
            throw ValidationException("Parameter ${it.name} not passed")
        }
        val value = answers[it.name] as String

        when (it) {
            is OptionsQuestion -> if (value !in it.options) {
                throw ValidationException("Invalid value $value for parameter ${it.name}. Available values are ${it.options}.")
            }
            else -> it.run {
                validation(value.read())
            }
        }
    }
}

data class ValidationException(override val message: String) : RuntimeException(message)