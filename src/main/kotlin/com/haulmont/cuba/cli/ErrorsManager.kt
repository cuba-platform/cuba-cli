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