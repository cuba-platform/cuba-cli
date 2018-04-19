package com.haulmont.cuba.cli

import com.haulmont.cuba.cli.commands.CommonParameters
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.io.StringWriter

class ErrorsManager {
    val writer: PrintWriter by kodein.instance()

    private var lastStacktrace: String = ""

    fun unrecognizedParameters() {
        writer.println("@|red Unrecognized parameters|@")
    }

    fun unrecognizedCommand() {
        writer.println("@|red Unrecognized command|@")
    }

    fun handleCommandException(e: Exception) {
        saveStacktrace(e)

        if (CommonParameters.stacktrace) {
            printLastStacktrace()
        } else {
            printFailMessage(e)
        }
    }

    fun printLastStacktrace() {
        writer.println("@|red $lastStacktrace|@")
    }

    private fun printFailMessage(e: Exception) {
        val message = e.message ?: e.javaClass.toString()

        """

            @|red FAILURE: Command execution failed with an exception.

            * What went wrong:
            > $message

            * Try:
            Run `stacktrace` command to get full stack trace or use --stacktrace option.

            * Visit https://www.cuba-platform.com/discuss/ to get more help.
            |@
        """.trimIndent().let {
            writer.println(it)
        }
    }

    private fun saveStacktrace(e: Exception) {
        lastStacktrace = StringWriter().also {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (e as java.lang.Throwable).printStackTrace(PrintWriter(it))
        }.toString()
    }
}