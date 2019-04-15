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

import com.haulmont.cuba.cli.bgRed
import com.haulmont.cuba.cli.commands.CommandExecutionException
import com.haulmont.cuba.cli.commands.CommonParameters
import org.fusesource.jansi.Ansi
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.impl.completer.NullCompleter
import org.kodein.di.Kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

class Prompts internal constructor(private val questionsList: QuestionsList, private val throwValidation: Boolean = false, kodein: Kodein = com.haulmont.cuba.cli.kodein) {

    private val reader: LineReader by kodein.instance(arg = NullCompleter())
    private val writer: PrintWriter by kodein.instance()

    /**
     * Asks user in non-interactive mode if he specified any dynamic parameter.
     * Asks user in interactive mode otherwise.
     */
    fun ask(): Answers = if (CommonParameters.nonInteractive.isEmpty())
        askInteractive()
    else
        askNonInteractive()

    fun askInteractive() = questionsList.ask { it }

    @Suppress("NON_TAIL_RECURSIVE_CALL")
    private tailrec fun QuestionsList.ask(rootAnswers: (Answers) -> Answers): Answers {
        val answers: MutableMap<String, Answer> = mutableMapOf()

        for (question in this) {
            if (!question.shouldAsk(rootAnswers(answers))) {
                continue
            }

            answers[question.name] = when (question) {
                is StringQuestion -> question.ask(rootAnswers(answers))
                is ConfirmationQuestion -> question.ask(rootAnswers(answers))
                is OptionsQuestion<*> -> question.ask(rootAnswers(answers))
                is RepeatingQuestion -> question.ask {
                    rootAnswers(answers) + (question.name to it)
                }
                is QuestionsList -> question.ask {
                    rootAnswers(answers) + (question.name to it)
                }
            }
        }

        try {
            return answers.also { validation(it, rootAnswers(it)) }
        } catch (e: ValidationException) {
            writer.println(e.message.bgRed())

            if(throwValidation)
                throw e
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

    private tailrec fun StringQuestion.ask(answers: Answers, prompts: String = printPrompts(answers)): String {
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
            }
        } catch (e: Exception) {
            when (e) {
                is ValidationException, is ReadException -> e.message?.let { writer.println(it.bgRed()) }
                else -> throw e
            }

            if(throwValidation)
                throw e
        }

        return ask(answers, prompts)
    }

    private tailrec fun <T : Any> OptionsQuestion<T>.ask(answers: Answers, prompts: String = printPrompts(answers)): T {
        try {
            return read(prompts).let {
                if (it.isNotEmpty()) return@let it.read()
                val defaultValue = defaultValue
                when (defaultValue) {
                    is PlainValue -> defaultValue.value
                    is CalculatedValue -> defaultValue.function(answers)
                    else -> it.read()
                }
            }.let {
                options[it]
            }.value.also {
                validation(it, answers)
            }
        } catch (e: Exception) {
            when (e) {
                is ValidationException, is ReadException -> e.message?.let { writer.println(it.bgRed()) }
                else -> throw e
            }

            if(throwValidation)
                throw e
        }

        return ask(answers, prompts)
    }
private tailrec fun ConfirmationQuestion.ask(answers: Answers, prompts: String = printPrompts(answers)): Boolean {
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
            }
        } catch (e: Exception) {
            when (e) {
                is ValidationException, is ReadException -> e.message?.let { writer.println(it.bgRed()) }
                else -> throw e
            }

            if(throwValidation)
                throw e
        }

        return ask(answers, prompts)
    }

    private fun read(prompt: String): String = reader.readLine(Ansi.ansi().render(prompt).toString())?.trim() ?: throw EndOfFileException()

    fun askNonInteractive(): Answers {
        val answers = CommonParameters.nonInteractive.toMutableMap()

        questionsList.all {
            it is PlainQuestion<*>
        } || throw CommandExecutionException("Non interactive mode unavailable for complex questions")

        questionsList
                .filterIsInstance(PlainQuestion::class.java)
                .forEach {
                    processQuestion(it, answers)
                }

        return answers
    }

    private fun <T : Any> processQuestion(question: PlainQuestion<T>, answers: MutableMap<String, Any>) {
        if (!question.askCondition(answers))
            return

        question.name in answers || question.defaultValue !== None ||
                throw ValidationException("Parameter ${question.name} not passed")

        if (question.name in answers) {
            val value = answers[question.name] as String

            when (question) {
                is OptionsQuestion<*> -> value in question ||
                        throw ValidationException("Invalid value $value for parameter ${question.name}. Available values are ${question.options.map { it.id }}.")

                is StringQuestion -> (question as StringQuestion).run {
                    validation(value.read(), answers)
                    answers[question.name] = question.run {
                        value.read()
                    }
                }

                is ConfirmationQuestion -> (question as ConfirmationQuestion).run {
                    validation(value.read(), answers)
                    answers[question.name] = question.run {
                        value.read()
                    }
                }
            }


        } else question.defaultValue.let { defaultValue ->
            when (defaultValue) {
                None -> throw ValidationException("Parameter ${question.name} not passed")
                is PlainValue -> answers[question.name] = defaultValue.value
                is CalculatedValue -> answers[question.name] = defaultValue.function(answers)
            }

            if (question is OptionsQuestion<*>)
                answers[question.name] = question.options[answers[question.name] as Int].id
        }
    }

    companion object {
        fun create(init: QuestionsList.() -> Unit) = Prompts(QuestionsList("", init))
    }
}

data class ValidationException(override val message: String) : RuntimeException(message)