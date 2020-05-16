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

package com.haulmont.cuba.cli.cubaplugin.gradle

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.AbstractCubaCommand
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

@Parameters(commandDescription = "Launches gradle command")
class GradleCommand : AbstractCubaCommand() {
    private val gradleRunner: GradleRunner by cubaKodein.instance<GradleRunner>()

    private val printWriter: PrintWriter by cubaKodein.instance<PrintWriter>()

    @Parameter(description = "Gradle command string", variableArity = true)
    var command: List<String> = mutableListOf()
        private set

    override fun run() {
        try {
            gradleRunner.run(*command.toTypedArray())
        } catch (e: WrapperNotFoundException) {
            printWriter.println(e.message)
            fail(e.message!!, silent = true)
        }
    }

}