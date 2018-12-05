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

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.AbstractCommand
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import org.kodein.di.generic.instance
import java.io.File
import java.io.PrintWriter

@Parameters(commandDescription = "Starts or restarts the application")
class RunCommand : AbstractCommand() {
    private val gradleRunner: GradleRunner by cubaKodein.instance()
    private val printWriter: PrintWriter by cubaKodein.instance()

    override fun run() {
        try {
            var runSetup = false

            printWriter.println("Determining tomcat directory...")
            val (_, properties) = gradleRunner.run("properties", redirectOutput = false)

            val tomcatPath = Regex("cuba: cuba\\.tomcat\\.dir: (.*)[\r\n]")
                    .find(properties)?.groupValues?.let {
                if (it.size >= 2) {
                    it[1]
                } else null
            }?.let {
                // for some reason, the path is returned with ^^ postfix, remove it
                it.replace("^", "")
            } ?: fail("Unable to determine tomcat directory")

            if (!File(tomcatPath).exists()) {
                runSetup = true
            }

            val commands: Array<String> = if (runSetup) {
                arrayOf("setupTomcat", "restart")
            } else arrayOf("restart")

            gradleRunner.run(*commands)
        } catch (e: WrapperNotFoundException) {
            printWriter.println(e.message)
            fail(e.message!!, silent = true)
        }
    }
}