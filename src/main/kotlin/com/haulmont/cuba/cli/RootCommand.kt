package com.haulmont.cuba.cli

import com.beust.jcommander.Parameter

class RootCommand : CliCommand {
    @Parameter(names = ["--help"], help = true)
    var help: Boolean = false
        private set

    override fun execute(context: CliContext) {
        if (help) {
            printHelp()
        }
    }
}

fun printHelp() {
    TODO("Help message")
}