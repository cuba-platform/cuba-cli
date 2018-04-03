package com.haulmont.cuba.cli

import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer

class Prompts internal constructor(inputStream: InputStream, outputStream: OutputStream, private val questions: List<PromptsQuestion>) {

    private val reader: BufferedReader = inputStream.bufferedReader()
    private val writer: Writer = outputStream.writer()


    fun ask(): Map<String, String> {
        val answers = mutableMapOf<String, String>()
        questions.forEach { question ->
            answers[question.name] = ask(question, answers.toMap())
        }
        return answers.toMap()
    }

    private fun ask(question: PromptsQuestion, answers: Map<String, String>): String = when (question) {
        is InputQuestion -> {
            val defaultValue = question.defaultValue.get(answers)
            val prompt = createPrompt(question, defaultValue)
            ask(question, prompt, defaultValue)
        }
        is ListQuestion -> {
            val defaultValue = question.defaultValue.get(answers)
            val prompt = createPrompt(question, defaultValue)
            val index = ask(question, prompt, defaultValue).toInt() - 1
            question.options[index]
        }
        is SilentQuestion -> question.defaultValue.get(answers)
    }

    private fun ask(question: PromptsQuestion, prompt: String, defaultValue: String): String {
        var result: String
        do {
            write(prompt)
            result = read().takeIf { it.isNotEmpty() } ?: defaultValue
        } while (!validate(result, question.validation))
        return result
    }

    private fun createPrompt(question: PromptsQuestion, defaultValue: String): String = "> ${question.caption} " +
            defaultValue.takeIf { it.isNotEmpty() }?.let { "($it) " } +
            when (question) {
                is InputQuestion -> ""
                is ListQuestion -> printOptionsIndexed(question.options)
                else -> throw IllegalArgumentException()
            }


    private fun printOptionsIndexed(options: List<String>): String = options.foldIndexed("") { index, acc, s ->
        "$acc\n${index + 1}. $s "
    }

    private fun validate(result: String, validateFn: (String) -> Unit): Boolean =
            try {
                validateFn(result)
                true
            } catch (e: ValidationException) {
                write("${e.message}\n")
                false
            }

    private fun write(prompt: String) {
        writer.write(prompt)
        writer.flush()
    }

    private fun read(): String = reader.readLine().trim()
}


val any: (String) -> Unit = {}
val nonNull: (String) -> Unit = { if (it == "") throw ValidationException("Value is required") }

class PromptsBuilder(private val inputStream: InputStream, private val outputStream: OutputStream) {
    private val questions: MutableList<PromptsQuestion> = mutableListOf()

    fun addQuestion(name: String, caption: String = name, defaultValue: DefaultValue = None, validation: (value: String) -> Unit = nonNull): PromptsBuilder {
        questions.add(InputQuestion(name, caption, defaultValue, validation))
        return this
    }

    fun addListQuestion(name: String, caption: String = name, options: List<String>, defaultValue: DefaultValue = PlainValue("1")): PromptsBuilder {
        questions.add(ListQuestion(name, caption, defaultValue, {
            try {
                val index = it.toInt()
                if (index > 0 && index <= options.size) {
                    return@ListQuestion
                }
            } catch (e: NumberFormatException) {
            }
            throw ValidationException("Input 1-${options.size}")
        }, options))
        return this
    }

    fun addSilentQuestion(name: String, defaultValue: CalculatedValue): PromptsBuilder {
        questions.add(SilentQuestion(name, defaultValue))
        return this
    }

    fun build(): Prompts = Prompts(inputStream, outputStream, questions.toList())
}

sealed class PromptsQuestion(
        val name: String,
        val caption: String,
        open val defaultValue: DefaultValue,
        val validation: (String) -> Unit)

class SilentQuestion(name: String, override val defaultValue: CalculatedValue) :
        PromptsQuestion(name, name, defaultValue, any)

class InputQuestion(name: String, caption: String, defaultValue: DefaultValue, validation: (String) -> Unit) :
        PromptsQuestion(name, caption, defaultValue, validation)

class ListQuestion(name: String, caption: String, defaultValue: DefaultValue, validation: (String) -> Unit, val options: List<String>) :
        PromptsQuestion(name, caption, defaultValue, validation)

sealed class DefaultValue
object None : DefaultValue()
class PlainValue(val value: String) : DefaultValue()
class CalculatedValue(val function: (answers: Map<String, String>) -> String) : DefaultValue()

private fun DefaultValue.get(answers: Map<String, String>): String = when (this) {
    is None -> ""
    is PlainValue -> this.value
    is CalculatedValue -> this.function(answers)
}

data class ValidationException(override val message: String) : RuntimeException(message)
