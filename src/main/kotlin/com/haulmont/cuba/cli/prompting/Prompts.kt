package com.haulmont.cuba.cli.prompting

import org.fusesource.jansi.Ansi.ansi
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer

class Prompts internal constructor(inputStream: InputStream, outputStream: OutputStream, private val questionsList: QuestionsList) {

    private val reader: BufferedReader = inputStream.bufferedReader()
    private val writer: Writer = outputStream.writer()

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
            print(prompt)
            result = read().takeIf { it.isNotEmpty() } ?: defaultValue
        } while (!validate(result, validation))
        return result
    }

    private fun createPrompt(question: Question, defaultValue: String): String {
        val answer = "> ${question.caption} "
        val defaultValuePostfix = when {
            defaultValue.isEmpty() -> ""
            else -> ansi().render("@|red ($defaultValue) |@").toString()
        }
        val options = when (question) {
            is OptionsQuestion -> printOptionsIndexed(question.options)
            is PlainQuestion -> ""
            else -> throw IllegalArgumentException()
        }
        return answer + defaultValuePostfix + options
    }


    private fun printOptionsIndexed(options: List<String>): String = options.foldIndexed("") { index, acc, s ->
        "$acc\n${index + 1}. $s "
    }

    private fun validate(result: String, validateFn: (String) -> Unit): Boolean =
            try {
                validateFn(result)
                true
            } catch (e: ValidationException) {
                println(ansi().render("@|red ${e.message}|@").toString())
                false
            }

    private fun print(prompt: String) {
        writer.write(ansi().render(prompt).toString())
        writer.flush()
    }

    private fun println(prompt: String) {
        print(ansi().render(prompt + "\n").toString())
    }

    private fun read(): String = reader.readLine().trim()
}

private fun DefaultValue.get(answers: Answers): String = when (this) {
    is None -> ""
    is PlainValue -> this.value
    is CalculatedValue -> this.function(answers)
}

data class ValidationException(override val message: String) : RuntimeException(message)
