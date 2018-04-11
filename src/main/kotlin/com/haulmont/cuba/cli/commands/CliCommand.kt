package com.haulmont.cuba.cli.commands

import com.haulmont.cuba.cli.CliContext

interface CliCommand {

    @Throws(CommandExecutionException::class)
    fun execute(context: CliContext)
}

class CommandExecutionException @JvmOverloads constructor(message: String, cause: Throwable? = null) : Exception(message, cause)