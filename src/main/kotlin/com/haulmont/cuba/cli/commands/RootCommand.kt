package com.haulmont.cuba.cli.commands

import com.beust.jcommander.Parameter
import com.haulmont.cuba.cli.CliContext

class RootCommand : CliCommand {
    @Parameter(names = ["--help", "-h"], help = true, description = "Show help message")
    var help: Boolean = false
        private set

    @Parameter(names = ["--version", "-v"], help = true, description = "Show CLI version")
    var version: Boolean = false
        private set

    override fun execute(context: CliContext) {
        if (version) {
            printVersion()
            return
        }
    }
}

private fun printVersion() {
    println("CUBA CLI 1.0-SNAPSHOT")
}