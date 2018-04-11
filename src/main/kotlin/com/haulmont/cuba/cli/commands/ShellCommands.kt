package com.haulmont.cuba.cli.commands

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.CLI_VERSION
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

@Parameters(commandDescription = "Exit CUBA CLI")
object ExitCommand : CliCommand {
    override fun execute() {
//        Command handled in ShellCli
    }
}

@Parameters(commandDescription = "Print help")
object HelpCommand : CliCommand {
    override fun execute() {
//        Command handled in ShellCli
    }
}

@Parameters(commandDescription = "Print version")
object VersionCommand : CliCommand {
    private val writer: PrintWriter by kodein.instance()

    override fun execute() {
        writer.println(CLI_VERSION)
    }
}