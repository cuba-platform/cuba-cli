/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.cli.commands

import com.beust.jcommander.Parameter
import com.haulmont.cuba.cli.CLI_VERSION
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

class RootCommand : CliCommand {
    private val writer: PrintWriter by kodein.instance()

    @Parameter(names = ["--help", "-h"], help = true, description = "Show help message")
    var help: Boolean = false
        private set

    @Parameter(names = ["--version", "-v"], help = true, description = "Show CLI version")
    var version: Boolean = false
        private set

    override fun execute() {
        if (version) {
            writer.println(CLI_VERSION)
            return
        }
    }
}