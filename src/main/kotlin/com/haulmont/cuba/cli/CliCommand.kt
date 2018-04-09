package com.haulmont.cuba.cli

interface CliCommand {

    @Throws(CommandExecutionException::class)
    fun execute(context: CliContext)
}