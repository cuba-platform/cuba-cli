package com.haulmont.cuba.cli

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        if (args.first() == "shell") {
            ShellCli().run()
            return
        }
    }

    SingleCommandCli(args).run()
}
