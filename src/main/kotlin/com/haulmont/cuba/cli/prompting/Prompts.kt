package com.haulmont.cuba.cli.prompting

import com.haulmont.cuba.cli.kodein
import org.fusesource.jansi.Ansi
import org.jline.reader.LineReader
import org.jline.reader.impl.completer.NullCompleter
import org.kodein.di.generic.instance
import java.io.PrintWriter

class Prompts internal constructor(private val questionsList: QuestionsList) {

    private val reader: LineReader by kodein.instance(arg = NullCompleter())
    private val writer: PrintWriter by kodein.instance()

    fun ask(): Answers {
        val answers: MutableMap<String, Answer> = mutableMapOf()

        questionsList.getQuestions()
                .forEach {
                    answers[it.name] = ask(it, answers)
                }

        return answers
    }

    private fun ask(question: Question, answers: Map<String, Any>): String = when (question) {
        is OptionsQuestion -> {
            val defaultValue = question.default.get(answers)
            val prompt = createPrompt(question, defaultValue)
            val index = ask(question.validation, prompt, defaultValue).toInt() - 1
            question.options[index]
        }
        is PlainQuestion -> {
            val defaultValue = question.default.get(answers)
            val prompt = createPrompt(question, defaultValue)
            ask(question.validation, prompt, defaultValue)
        }
    }

    private fun ask(validation: (String) -> Unit, prompt: String, defaultValue: String): String {
        var result: String
        do {
            result = read(prompt).takeIf { it.isNotEmpty() } ?: defaultValue
        } while (!validate(result, validation))
        return result
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

    private fun validate(result: String, validateFn: (String) -> Unit): Boolean =
            try {
                validateFn(result)
                true
            } catch (e: ValidationException) {
                writer.println("@|red ${e.message}|@")
                false
            }

    private fun read(prompt: String): String = reader.readLine(Ansi.ansi().render(prompt).toString()).trim()
}

private fun DefaultValue.get(answers: Answers): String = when (this) {
    is None -> ""
    is PlainValue -> this.value
    is CalculatedValue -> this.function(answers)
}

data class ValidationException(override val message: String) : RuntimeException(message)