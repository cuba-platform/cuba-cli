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

package com.haulmont.cuba.cli.di

import com.haulmont.cuba.cli.ColoredWriter
import com.haulmont.cuba.cli.ErrorsManager
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.io.PrintWriter

val terminalModule = Kodein.Module {
    bind<LineReader>() with factory { completer: Completer ->
        LineReaderBuilder.builder()
                .terminal(instance())
                .completer(completer)
                .build()
    }

    bind<Terminal>() with instance(TerminalBuilder.builder().build())

    bind<PrintWriter>() with singleton {
        ColoredWriter(instance<Terminal>().writer())
    }

    bind<ErrorsManager>() with singleton { ErrorsManager() }
}