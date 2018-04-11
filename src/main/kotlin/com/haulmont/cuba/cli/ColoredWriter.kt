package com.haulmont.cuba.cli

import org.fusesource.jansi.Ansi
import java.io.PrintWriter

class ColoredWriter(writer: PrintWriter) : PrintWriter(writer) {
    override fun print(s: String) {
        super.print(Ansi.ansi().render(s))
        flush()
    }

    override fun println(x: String) {
        super.println(Ansi.ansi().render(x))
        flush()
    }
}