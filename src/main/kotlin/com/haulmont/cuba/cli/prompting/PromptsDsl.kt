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

class QuestionsList(setup: (QuestionsList.() -> Unit)) {
    private val questions: MutableList<Question> = mutableListOf()

    init {
        setup()

        check(questions.isNotEmpty())

        questions.groupingBy { it.name }
                .eachCount()
                .entries
                .firstOrNull { (_, count) -> count > 1 }
                ?.let { (name, _) -> throw RuntimeException("Duplicated questions with name $name") }
    }

    fun getQuestions() = questions.toList()

    fun question(name: String, caption: String, configuration: (PlainQuestionConfigurationScope.() -> Unit)? = null) {
        PlainQuestion(name, caption).apply {
            configuration?.let { this.it() }
            questions.add(this)
        }
    }

    fun options(name: String, caption: String, options: List<String>, configuration: (DefaultValueConfigurable<Int>.() -> Unit)? = null) {
        OptionsQuestion(name, caption, options).apply {
            configuration?.let { this.it() }
            questions.add(this)
        }
    }
}

sealed class Question(val name: String, val caption: String)

class PlainQuestion(name: String, caption: String) :
        Question(name, caption),
        HasDefault<String>,
        WithValidation,
        PlainQuestionConfigurationScope {
    override var validation: (String) -> Unit = acceptAll
    override var defaultValue: DefaultValue = None
}

interface PlainQuestionConfigurationScope : DefaultValueConfigurable<String>, ValidationConfigurable

class OptionsQuestion(
        name: String,
        caption: String,
        val options: List<String>) : Question(name, caption), HasDefault<Int>, WithValidation {

    init {
        check(options.isNotEmpty())
    }

    override var validation: (String) -> Unit = {
        ValidationHelper(it).run {
            try {
                if (it.toInt() in (1..options.size))
                    return@run
            } catch (e: NumberFormatException) {
            }

            fail("Input 1-${options.size}")
        }
    }

    override var defaultValue: DefaultValue = None
}

sealed class DefaultValue
object None : DefaultValue()
class PlainValue(val value: String) : DefaultValue()
class CalculatedValue(val function: (Answers) -> String) : DefaultValue()

typealias Answer = Any
typealias Answers = Map<String, Answer>

interface DefaultValueConfigurable<in T> {
    fun default(function: (answers: Answers) -> T)

    fun default(plain: T)
}

interface HasDefault<in T> : DefaultValueConfigurable<T> {
    var defaultValue: DefaultValue

    override fun default(function: (answers: Answers) -> T) {
        defaultValue = CalculatedValue { function(it).toString() }
    }

    override fun default(plain: T) {
        defaultValue = PlainValue(plain.toString())
    }
}

private val acceptAll: (String) -> Unit = {}

interface ValidationConfigurable {
    fun validate(block: ValidationHelper.(String) -> Unit)
}

interface WithValidation : ValidationConfigurable {
    var validation: (String) -> Unit

    override fun validate(block: ValidationHelper.(String) -> Unit) {
        check(validation == acceptAll)
        validation = { ValidationHelper(it).block(it) }
    }
}

class ValidationHelper(private val value: String) {
    fun checkRegex(pattern: String, failMessage: String = "Invalid value") {
        if (!Regex(pattern).matches(value))
            fail(failMessage)
    }

    fun checkIsPackage(failMessage: String = "Is not valid package name") =
            checkRegex("[a-zA-Z][0-9a-zA-Z]*(\\.[a-zA-Z][0-9a-zA-Z]*)*", failMessage)

    fun checkIsClass(failMessage: String = "Invalid class name") =
            checkRegex("\\b[A-Z]+[\\w\\d]*", failMessage)

    fun fail(cause: String): Nothing = throw ValidationException(cause)
}