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

package com.haulmont.cli.core.di

import com.haulmont.cli.core.ColoredWriter
import com.haulmont.cli.core.GenerationProgressPrinter
import com.haulmont.cli.core.PrintHelper
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.io.PrintWriter
import java.util.logging.Level
import java.util.logging.Logger

val terminalModule = Kodein.Module {
    Logger.getLogger("org.jline").run {
        level = Level.OFF
    }

    bind<LineReader>() with factory { completer: Completer ->
        LineReaderBuilder.builder()
                .terminal(instance())
                .completer(completer)
                .build()
    }

    bind<Terminal>() with singleton {
        System.setProperty("org.jline.terminal.conemu.disable-activate", "true")
        TerminalBuilder.builder().build()
    }

    bind<PrintWriter>() with singleton {
        ColoredWriter(instance<Terminal>().writer())
    }

    bind<PrintHelper>() with singleton { PrintHelper() }

    bind<GenerationProgressPrinter>() with singleton { kodein.direct.instance<PrintHelper>() }

    bind<Boolean>(tag = "throwValidation") with instance(false)
}