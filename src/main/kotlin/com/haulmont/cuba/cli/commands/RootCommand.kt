package com.haulmont.cuba.cli.commands

import com.beust.jcommander.Parameter
import com.haulmont.cuba.cli.CLI_VERSION
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

class RootCommand : CliCommand {
    private val writer: PrintWriter by kodein.instance()

    @Parameter(names = ["--help", "-h"], help = true, description = "Show help message")
    var help: Boolean = false
        private set

    @Parameter(names = ["--version", "-v"], help = true, description = "Show CLI version")
    var version: Boolean = false
        private set

    override fun execute() {
        if (version) {
            writer.println(CLI_VERSION)
            return
        }
    }
}