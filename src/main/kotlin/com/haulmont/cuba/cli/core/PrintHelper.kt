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

package com.haulmont.cuba.cli.core

import com.beust.jcommander.ParameterException
import com.haulmont.cuba.cli.core.commands.CommandExecutionException
import com.haulmont.cuba.cli.core.commands.CommonParameters
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path

class PrintHelper : GenerationProgressPrinter {
    private val writer: PrintWriter by kodein.instance()

    private val workingDirectoryManager: WorkingDirectoryManager by kodein.instance()

    private val messages by localMessages()

    private var lastStacktrace: String = ""

    fun unrecognizedParameters(e: ParameterException) {
        writer.println("Unrecognized parameters\n${e.message}".bgRed())
    }

    fun unrecognizedCommand() {
        writer.println("Unrecognized command".bgRed())
    }

    fun handleCommandException(e: Exception) {
        e !is CommandExecutionException || !e.silent || return

        saveStacktrace(e)

        printFailMessage(e)

        if (CommonParameters.stacktrace) {
            writer.println("".bgRed())
            printLastStacktrace()
        }
    }

    fun printLastStacktrace() {
        writer.println(lastStacktrace.bgRed())
    }

    override fun fileCreated(path: Path) {
        writer.println("\tcreated  ".green() + relativize(path))
    }

    override fun fileModified(path: Path) {
        writer.println("\tmodified ".green() + relativize(path))
    }

    private fun printFailMessage(e: Exception) {
        val message = e.message ?: e.javaClass.toString()

        writer.println(messages["errorMessage", message])
    }

    private fun relativize(path: Path): Path {
        val projectPath = workingDirectoryManager.absolutePath
        if (path.toAbsolutePath().startsWith(projectPath))
            return projectPath.relativize(path.toAbsolutePath())
        return path
    }

    fun saveStacktrace(e: Exception) {
        lastStacktrace = StringWriter().also {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (e as java.lang.Throwable).printStackTrace(PrintWriter(it))
        }.toString()
    }
}