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

import com.haulmont.cuba.cli.WorkingDirectoryManager
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import org.kodein.di.generic.instance

class GradleRunner {

    private val workingDirectoryManager: WorkingDirectoryManager by cubaKodein.instance()

    fun run(commands: List<String>) : Int {
        val currentDir = workingDirectoryManager.absolutePath

        val osName = System.getProperty("os.name").toLowerCase()
        val gradleScriptName = when {
            osName.indexOf("win") >= 0 -> "gradlew.bat"
            else -> "gradlew"
        }
        val gradleScriptPath = currentDir.resolve(gradleScriptName).toFile().absolutePath


        val command : List<String> = when {
            osName.indexOf("win") >= 0 -> arrayListOf("cmd", "/C", gradleScriptPath, *commands.toTypedArray())
            else -> arrayListOf(gradleScriptPath, *commands.toTypedArray())
        }

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(currentDir.toFile())
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT)

        processBuilder.environment().remove("JAVA_OPTS")

        val process = processBuilder.start()

        return process.waitFor()
    }
}