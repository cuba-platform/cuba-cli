package com.haulmont.cuba.cli.di

import com.haulmont.cuba.cli.ColoredWriter
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
}