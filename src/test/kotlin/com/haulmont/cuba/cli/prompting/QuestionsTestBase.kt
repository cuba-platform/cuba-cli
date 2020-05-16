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

import com.haulmont.cuba.cli.core.ColoredWriter
import com.haulmont.cuba.cli.core.PrintHelper
import com.haulmont.cuba.cli.core.prompting.Answers
import com.haulmont.cuba.cli.core.prompting.Prompts
import com.haulmont.cuba.cli.core.prompting.QuestionsList
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.junit.Before
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintWriter

open class QuestionsTestBase {
    lateinit var kodein: Kodein

    private lateinit var outputStream: PipedOutputStream
    private lateinit var inputStream: PipedInputStream

    @Before
    internal fun setUp() {
        reset()
    }

    fun reset() {
        kodein = Kodein {
            bind<Terminal>() with instance(createTerminal())

            bind<LineReader>() with factory { completer: Completer ->
                LineReaderBuilder.builder()
                        .terminal(instance())
                        .completer(completer)
                        .build()
            }

            bind<PrintWriter>() with singleton {
                ColoredWriter(instance<Terminal>().writer())
            }

            bind<PrintHelper>() with singleton { PrintHelper() }

            bind<Boolean>(tag = "throwValidation") with instance(throwValidation())
        }
    }

    protected open fun throwValidation(): Boolean = true

    fun appendEmptyLine() = "\n".toByteArray().let(outputStream::write)
    fun appendInputLine(str: String) = "$str\n".toByteArray().let(outputStream::write)

    fun interactivePrompts(setup: QuestionsList.() -> Unit): Answers =
            Prompts.create(kodein) {
                QuestionsList("test", setup)
            }.askInteractive()

    private fun createTerminal(): Terminal {
        outputStream = PipedOutputStream()
        inputStream = PipedInputStream()
        inputStream.connect(outputStream)

        return TerminalBuilder.builder().dumb(true)
                .system(false)
                .jna(false)
                .jansi(false)
                .streams(inputStream, ByteArrayOutputStream())
                .build()
    }
}
