package com.haulmont.cuba.cli.prompting

class QuestionsList(setup: (QuestionsList.() -> Unit)) {
    private val questions: MutableList<Question> = mutableListOf()

    init {
        setup()

        check(questions.isNotEmpty())

        questions.groupingBy { it.name }
                .eachCount()
                .entries
                .firstOrNull { (_, v) -> v > 1 }
                ?.let { (name, _) -> throw RuntimeException("Duplicated questions with name $name") }
    }

    fun getQuestions() = questions.toList()

    @JvmOverloads
    fun question(name: String, caption: String, setup: (PlainQuestion.() -> Unit)? = null) {
        questions.add(PlainQuestion(name, caption, setup))
    }

    @JvmOverloads
    fun options(name: String, caption: String, options: List<String>, setup: (OptionsQuestion.() -> Unit)? = null) {
        questions.add(OptionsQuestion(name, caption, options, setup))
    }
}


sealed class Question(val name: String, val caption: String) {
    internal var default: DefaultValue = None

    internal var validation: (String) -> Unit = {}
}

open class PlainQuestion(name: String, caption: String, setup: (PlainQuestion.() -> Unit)? = null) : Question(name, caption) {

    init {
        setup?.let { it() }
    }

    fun default(function: (answers: Answers) -> String) {
        default = CalculatedValue(function)
    }

    fun default(value: String) {
        default = PlainValue(value)
    }

    fun validate(block: ValidationHelper.(String) -> Unit) {
        validation = { ValidationHelper(it).block(it) }
    }

}

class OptionsQuestion(
        name: String,
        caption: String,
        val options: List<String>,
        setup: (OptionsQuestion.() -> Unit)?) : Question(name, caption) {

    init {
        setup?.let { it() }
        check(options.isNotEmpty())

        validation = {
            ValidationHelper(it).run {
                try {
                    if (it.toInt() in (1..options.size))
                        return@run
                } catch (e: NumberFormatException) {
                }

                fail("Input 1-${options.size}")
            }
        }
    }


    init {
        setup?.let { it() }
    }

    fun default(function: (answers: Answers) -> Int) {
        default = CalculatedValue({ function(it).toString() })
    }

    fun default(index: Int) {
        default = PlainValue(index.toString())
    }

}

sealed class DefaultValue
object None : DefaultValue()
class PlainValue(val value: String) : DefaultValue()
class CalculatedValue(val function: (Answers) -> String) : DefaultValue()


typealias Answer = Any
typealias Answers = Map<String, Answer>

class ValidationHelper(private val value: String) {
    fun checkRegex(pattern: String, failMessage: String = "Invalid value") {
        if (!Regex(pattern).matches(value))
            fail(failMessage)
    }

    fun checkIsPackage(failMessage: String = "Is not valid package name") {
        checkRegex("[a-zA-Z][0-9a-zA-Z]*(\\.[a-zA-Z][0-9a-zA-Z]*)+", failMessage)
    }

    fun fail(cause: String) {
        throw ValidationException(cause)
    }
}