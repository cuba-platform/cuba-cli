package com.haulmont.cuba.cli

import com.beust.jcommander.Parameter

abstract class AbstractCommand : CliCommand {
    @Parameter(names = ["--help"], help = true)
    private var help: Boolean = false

    override fun execute(context: CliContext) {
        if (help) {
            printHelp()
            return
        }

        checkPreconditions(context)
    }

    @Throws(CommandExecutionException::class)
    protected open fun checkPreconditions(context: CliContext) {
    }

    protected open fun printHelp() {}

    @Throws(CommandExecutionException::class)
    protected fun onlyInProject(context: CliContext) {
        if (!context.hasModel("project")) {
            fail("Command should be started in project directory")
        }
    }

    @Throws(CommandExecutionException::class)
    protected fun fail(cause: String): Unit = throw CommandExecutionException(cause)
}