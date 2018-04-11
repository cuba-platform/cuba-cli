package com.haulmont.cuba.cli.commands

interface CliCommand {

    @Throws(CommandExecutionException::class)
    fun execute()
}

class CommandExecutionException @JvmOverloads constructor(message: String, cause: Throwable? = null) : Exception(message, cause)