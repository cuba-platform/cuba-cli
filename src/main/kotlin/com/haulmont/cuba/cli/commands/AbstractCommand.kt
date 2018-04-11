package com.haulmont.cuba.cli.commands

import com.beust.jcommander.Parameter
import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance

abstract class AbstractCommand : CliCommand {
    val context: CliContext by kodein.instance()

    @Parameter(names = ["--help"], help = true)
    private var help: Boolean = false

    override fun execute() {
        if (help) {
            printHelp()
            return
        }

        checkPreconditions()
    }

    @Throws(CommandExecutionException::class)
    protected open fun checkPreconditions() {
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