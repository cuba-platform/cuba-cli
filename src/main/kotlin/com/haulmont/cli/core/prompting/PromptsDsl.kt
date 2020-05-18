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

package com.haulmont.cli.core.prompting

import com.haulmont.cli.core.green

sealed class Question(val name: String) : Conditional {
    override var askCondition: (Answers) -> Boolean = { true }

    fun shouldAsk(answers: Answers) = askCondition(answers)
}

sealed class PlainQuestion<T : Any>(name: String, val caption: String) : Question(name), Print<T>, Read<T>, HasDefault<T> {
    open fun printPrompts(answers: Answers): String =
            "?".green() + """ $caption ${printDefaultValue(answers)}> """


    open fun printDefaultValue(answers: Answers): String {
        val defaultValue = this.defaultValue
        return when (defaultValue) {
            None -> ""
            is PlainValue -> defaultValue.value.print()
            is CalculatedValue -> defaultValue.function(answers).print()
        }.let {
            if (it.isEmpty()) it
            else "($it) "
        }
    }
}

class QuestionsList(name: String, setup: QuestionsList.() -> Unit) : Iterable<Question>, Question(name), WithValidation<Answers> {
    override var validation: (Answers, Answers) -> Unit = acceptAll

    private val questions: MutableList<Question> = mutableListOf()

    init {
        setup()

        questions.groupingBy { it.name }
                .eachCount()
                .entries
                .firstOrNull { (_, count) -> count > 1 }
                ?.let { (name, _) -> throw RuntimeException("Duplicated questions with name $name") }
    }

    fun question(name: String, caption: String, configuration: (StringQuestionConfigurationScope.() -> Unit)? = null) {
        val question = StringQuestion(name, caption)

        if (configuration != null)
            configuration(question)

        questions.add(question)
    }

    fun textOptions(name: String, caption: String, strOptions: List<String>, configuration: (DefaultValueConfigurable<Int>.() -> Unit)? = null) {
        val options = strOptions.map { StringOption(it) }

        val question = OptionsQuestion(name, caption, options)

        if (configuration != null)
            configuration(question)

        questions.add(question)
    }

    fun <T : Any> options(name: String, caption: String, options: List<Option<T>>, configuration: (DefaultValueConfigurable<Int>.() -> Unit)? = null) {
        val question = OptionsQuestion(name, caption, options)

        if (configuration != null)
            configuration(question)

        questions.add(question)
    }

    fun confirmation(name: String, caption: String, configuration: (ConfirmationQuestionConfigurationScope.() -> Unit)? = null) {
        val question = ConfirmationQuestion(name, caption)

        if (configuration != null)
            configuration(question)

        questions.add(question)
    }

    fun repeating(name: String, offer: String, configuration: QuestionsList.() -> Unit) {
        questions.add(RepeatingQuestion(name, offer, configuration))
    }

    fun questionList(name: String = "", configure: QuestionsList.() -> Unit) {
        questions.add(QuestionsList(name, configure))
    }

    override fun iterator(): Iterator<Question> = questions.iterator()
}

class RepeatingQuestion(name: String, offer: String, setup: (QuestionsList.() -> Unit)) : Question(name) {
    val offerQuestion: ConfirmationQuestion = ConfirmationQuestion("", offer)
    val questions: QuestionsList = QuestionsList(name, setup)
}

class StringQuestion(name: String, caption: String) :
        PlainQuestion<String>(name, caption),
        HasDefault<String>,
        WithValidation<String>,
        StringQuestionConfigurationScope {

    override var validation: (String, Answers) -> Unit = acceptAll

    override var defaultValue: DefaultValue<String> = None

    override fun String.read(): String = this

    override fun String.print() = this
}

interface StringQuestionConfigurationScope : DefaultValueConfigurable<String>, ValidationConfigurable<String>, Conditional

class OptionsQuestion<T : Any>(name: String, caption: String, val options: List<Option<T>>) :
        PlainQuestion<Int>(name, caption),
        HasDefault<Int>,
        WithValidation<T>,
        OptionsQuestionConfigurationScope {

    override var defaultValue: DefaultValue<Int> = None

    override var validation: (T, Answers) -> Unit = acceptAll

    operator fun contains(option: String): Boolean = option in options.map { it.id }

    init {
        check(options.isNotEmpty())
    }

    override fun String.read(): Int {
        try {
            val value = this.toInt() - 1
            if (value in (0 until options.size))
                return value
        } catch (e: NumberFormatException) {}

        throw ReadException("Input 1-${options.size}")
    }

    override fun Int.print(): String = (this + 1).toString()

    override fun printPrompts(answers: Answers): String {
        return buildString {
            append(super.printPrompts(answers))
            options.forEachIndexed { index, option ->
                append("\n${index + 1}. $option")
            }
            append('\n')
            append("> ")
        }
    }
}

/**
 * [id] is unique identifier of an option to be used in non interactive mode
 */
open class Option<T>(val id: String, val description: String? = null, val value: T) {
    override fun toString(): String {
        return description ?: id
    }
}

class StringOption(name: String, description: String? = null): Option<Any>(name, description, name)

interface OptionsQuestionConfigurationScope : DefaultValueConfigurable<Int>, Conditional

interface ConfirmationQuestionConfigurationScope : DefaultValueConfigurable<Boolean>, Conditional

class ConfirmationQuestion(name: String, caption: String) :
        PlainQuestion<Boolean>(name, caption),
        ConfirmationQuestionConfigurationScope,
        WithValidation<Boolean> {

    override var defaultValue: DefaultValue<Boolean> = None
    override var validation: (Boolean, Answers) -> Unit = acceptAll

    override fun String.read(): Boolean {
        val asChars = this.toLowerCase().trim().toCharArray()
        if (asChars.size != 1) throw ReadException()
        return when (asChars[0]) {
            'y' -> true
            'n' -> false
            else -> throw ReadException()
        }
    }

    override fun printDefaultValue(answers: Answers): String {
        val defaultValue = this.defaultValue
        return when (defaultValue) {
            None -> " (y/n) "
            is PlainValue -> if (defaultValue.value) " (Y/n) " else " (y/N) "
            is CalculatedValue -> if (defaultValue.function(answers)) " (Y/n) " else " (y/N) "
        }
    }

    override fun Boolean.print(): String = if (this) "y" else "n"
}

sealed class DefaultValue<out T>
object None : DefaultValue<Nothing>()
class PlainValue<out T>(val value: T) : DefaultValue<T>()
class CalculatedValue<out T>(val function: (Answers) -> T) : DefaultValue<T>()

typealias Answer = Any
typealias Answers = Map<String, Answer>

interface DefaultValueConfigurable<T> {
    fun default(function: (answers: Answers) -> T)

    fun default(plain: T)
}

interface HasDefault<T : Any> : DefaultValueConfigurable<T> {
    var defaultValue: DefaultValue<T>

    override fun default(function: (answers: Answers) -> T) {
        defaultValue = CalculatedValue { function(it) }
    }

    override fun default(plain: T) {
        defaultValue = PlainValue(plain)
    }
}

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
private val acceptAll: (Any, Answers) -> Unit = { value, answers -> }

interface ValidationConfigurable<T : Any> {
    fun validate(block: ValidationHelper<T>.() -> Unit)
}

interface WithValidation<T : Any> : ValidationConfigurable<T> {
    var validation: (T, Answers) -> Unit

    override fun validate(block: ValidationHelper<T>.() -> Unit) {
        check(validation == acceptAll)
        validation = { value, answers -> ValidationHelper(value, answers).block() }
    }
}

class ValidationHelper<T : Any>(val value: T, val answers: Answers) {
    fun checkRegex(pattern: String, failMessage: String = "Invalid value") {
        if (value !is String) {
            throw RuntimeException("Trying to validate non string value with regex")
        }
        if (!Regex(pattern).matches(value))
            fail(failMessage)
    }

    fun checkIsNotBlank(failMessage: String = "Specify value") {
        value is String && value.isBlank() && fail(failMessage)
    }

    fun checkIsPackage(failMessage: String = "Is not valid package name") =
            checkRegex("[a-zA-Z][0-9a-zA-Z]*(\\.[a-zA-Z][0-9a-zA-Z]*)*", failMessage)

    fun checkIsClass(failMessage: String = "Invalid class name. Class name should match UpperCamelCase.") =
            checkRegex("\\b[A-Z]+[\\w\\d]*", failMessage)

    fun checkIsScreenDescriptor(failMessage: String = "Screen descriptor can contain alphanumerical characters, dashes, underscores and dollar signs.") {
        checkRegex("[a-zA-Z0-9.$\\-_]+", failMessage)
    }

    fun fail(cause: String): Nothing = throw ValidationException(cause)

    fun checkIsInt(failMessage: String = "Input integer number") {
        value is Int || try {
            value.toString().toInt()
            true
        } catch (e: NumberFormatException) {
            false
        } || fail(failMessage)
    }
}

interface Print<in T : Any> {
    fun T.print() = this.toString()
}

interface Read<out T : Any> {
    fun String.read(): T
}

class ReadException(message: String = "Invalid value") : Exception(message)

interface Conditional {
    var askCondition: (Answers) -> Boolean

    fun askIf(askCondition: (Answers) -> Boolean) {
        this.askCondition = askCondition
    }

    fun askIf(confirmationQuestionName: String) {
        askIf {
            it[confirmationQuestionName] as Boolean? ?: false
        }
    }
}