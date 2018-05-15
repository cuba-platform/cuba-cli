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

import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance

abstract class AbstractCommand : CliCommand {
    val context: CliContext by kodein.instance()

    final override fun execute() {
        preExecute()

        run()

        postExecute()
    }

    protected abstract fun run()

    protected open fun preExecute() {}

    protected open fun postExecute() {}

    @Throws(CommandExecutionException::class)
    protected fun checkProjectExistence() {
        if (!context.hasModel("project")) {
            fail("Command should be started in project directory")
        }
    }

    @Throws(CommandExecutionException::class)
    protected fun fail(cause: String, silent: Boolean = false): Nothing =
            throw CommandExecutionException(cause, silent = silent)
}