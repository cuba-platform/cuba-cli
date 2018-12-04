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

package com.haulmont.cuba.cli.cubaplugin.prifexchange

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.AbstractCommand
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.prompting.Prompts
import org.kodein.di.generic.instance
import java.io.PrintWriter


@Parameters(commandDescription = "Changes modules prefix")
class PrefixChangeCommand : AbstractCommand() {

    private val printWriter: PrintWriter by cubaKodein.instance()

    private val prefixChanger: PrefixChanger by cubaKodein.instance()

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun run() {
        printWriter.println("Current module prefix is ${projectModel.modulePrefix}")

        val newPrefix = Prompts.create {
            question("modulePrefix", "New prefix") {
                validate {
                    if (value.isBlank())
                        fail("Empty project prefix is not allowed")

                    if(value == projectModel.modulePrefix)
                        fail("Inputted prefix equals current project prefix")

                    val invalidNameRegex = Regex("[^\\w\\-]")

                    if (invalidNameRegex.find(value) != null) {
                        fail("Module prefix can contain letters, digits, dashes and underscore characters")
                    }
                }
            }
        }.ask()["modulePrefix"] as String

        prefixChanger.changePrefix(newPrefix)
    }
}