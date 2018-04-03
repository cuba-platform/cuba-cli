package com.haulmont.cuba.cli

interface CliCommand {
    fun run()

    fun name(): String
}