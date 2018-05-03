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

    fun ask(): Answers = questionsList.ask { it }

    private tailrec fun CompositeQuestion.ask(rootAnswers: (Answers) -> Answers): Answers {
        try {
            return this.fold<Question, Answers>(mapOf()) { answers, question ->
                if (!question.shouldAsk(rootAnswers(answers))) {
                    return@fold answers
                }

                return@fold when (question) {
                    is PlainQuestion<*> -> {
                        val answer = question.ask(rootAnswers(answers))
                        answers + (question.name to answer)
                    }
                    is RepeatingQuestion -> {
                        val storeFn = { it: Answer ->
                            answers + (question.name to it)
                        }
                        question.ask {
                            rootAnswers(storeFn(it))
                        }.let(storeFn)
                    }
                    is CompositeQuestion -> {
                        val storeFn = { it: Answers ->
                            mergeAnswers(answers, it)
                        }
                        question.ask {
                            rootAnswers(storeFn(it))
                        }.let(storeFn)
                    }
                }
            }.also { localAnswers ->
                validation(localAnswers, rootAnswers(localAnswers))
            }
        } catch (e: ValidationException) {
            writer.println("@|red ${e.message}|@")
        }
        return this.ask(rootAnswers)
    }


    private fun RepeatingQuestion.ask(rootAnswers: (List<Answers>) -> Answers): List<Answers> {
        val answersList: MutableList<Answers> = mutableListOf()
        while (offerQuestion.ask(rootAnswers(answersList))) {
            questions.ask {
                rootAnswers(answersList + it)
            }.let(answersList::add)
        }
        return answersList
    }

    private fun CompositeQuestion.mergeAnswers(targetAnswers: Answers, toMerge: Answers): Answers =
            if (isFlat) {
                targetAnswers + toMerge
            } else {
                targetAnswers + (name to toMerge)
            }

    private tailrec fun <T : Any> PlainQuestion<T>.ask(answers: Answers, prompts: String = printPrompts(answers)): T {
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
                validation(it, answers)
            }.let {
                when {
                    this is OptionsQuestion -> this.options[it as Int] as T
                    else -> it
                }
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

        questionsList.all {
            it is PlainQuestion<*>
        } || throw CommandExecutionException("Non interactive mode unavailable for complex questions")

        questionsList
                .filterIsInstance(PlainQuestion::class.java)
                .forEach {
                    checkQuestion(it, answers)
                }

        return answers
    }

    private fun <T : Any> checkQuestion(question: PlainQuestion<T>, answers: Map<String, String>) {
        question.name in answers ||
                throw ValidationException("Parameter ${question.name} not passed")

        val value = answers[question.name] as String

        when (question) {
            is OptionsQuestion -> value in question.options ||
                    throw ValidationException("Invalid value $value for parameter ${question.name}. Available values are ${question.options}.")

            else -> question.run {
                validation(value.read(), answers)
            }
        }
    }
}

data class ValidationException(override val message: String) : RuntimeException(message)