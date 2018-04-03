package com.haulmont.cuba.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import java.util.*

fun main(args: Array<String>) {
    val commands = ServiceLoader.load(CliCommand::class.java).toList()

    val builder = JCommander.newBuilder()

    builder.addObject(CommonOptions)

    commands.forEach {
        builder.addCommand(it.name(), it)
    }

    val commander = builder.build()

    try {
        commander.parse(*args)
    } catch (e: MissingCommandException) {
        println("Unrecognized command")
        printHelp()
        return
    }

    val commandName: String? = commander.parsedCommand

    if (commandName == null || CommonOptions.help) {
        printHelp()
        return
    }

    commands.first { it.name() == commandName }.run()
}

fun printHelp() {
    TODO("Help message")
}
