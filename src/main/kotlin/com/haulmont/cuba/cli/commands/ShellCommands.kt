package com.haulmont.cuba.cli.commands

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

@Parameters(commandDescription = "Exit CUBA CLI")
object ExitCommand : CliCommand {
    override fun execute(context: CliContext) {}
}

@Parameters(commandDescription = "Print help")
object HelpCommand : CliCommand {
    override fun execute(context: CliContext) {}
}

@Parameters(commandDescription = "Print version")
object VersionCommand : CliCommand {
    private val writer: PrintWriter by kodein.instance()

    override fun execute(context: CliContext) {
        writer.println(CLI_VERSION)
    }
}