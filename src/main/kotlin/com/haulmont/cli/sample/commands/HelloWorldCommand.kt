package com.haulmont.cli.sample.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.haulmont.cli.core.commands.AbstractCommand
import com.haulmont.cli.core.green
import org.kodein.di.generic.instance
import java.io.PrintWriter

@Parameters(commandDescription = "Hello command")
class HelloWorldCommand : AbstractCommand() {

    private val writer: PrintWriter by kodein.instance<PrintWriter>()

    @Parameter(
            description = "Name parameter",
            hidden = true
    )
    private var name: String? = null

    override fun run() {
        writer.println("Hello ${name ?: "World"}".green())
    }
}