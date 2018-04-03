package com.haulmont.cuba.cli

import com.beust.jcommander.Parameter

object CommonOptions {

    @Parameter(names = ["--help"], help = true)
    var help: Boolean = false
}