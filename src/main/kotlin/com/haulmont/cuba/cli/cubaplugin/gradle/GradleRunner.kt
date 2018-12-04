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
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.completer.NullCompleter
import org.jline.terminal.Terminal
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

class GradleRunner {
    private val workingDirectoryManager: WorkingDirectoryManager by cubaKodein.instance()

    private val reader: LineReader by cubaKodein.instance(arg = NullCompleter())
    private val terminal: Terminal by cubaKodein.instance()

    fun run(vararg commands: String): Int {
        val currentDir = workingDirectoryManager.absolutePath

        val osName = System.getProperty("os.name").toLowerCase()
        val gradleScriptName = when {
            osName.indexOf("win") >= 0 -> "gradlew.bat"
            else -> "gradlew"
        }
        val gradleScriptPath = currentDir.resolve(gradleScriptName).toFile().absolutePath

        if (!Files.exists(Path.of(gradleScriptPath))) {
            throw WrapperNotFoundException
        }

        val command: List<String> = when {
            osName.indexOf("win") >= 0 -> arrayListOf("cmd", "/C", gradleScriptPath, *commands)
            else -> arrayListOf(gradleScriptPath, *commands)
        }

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(currentDir.toFile())
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT)

        processBuilder.environment().remove("JAVA_OPTS")

        val process = processBuilder.start()

        thread {
            process.waitFor()
            terminal.raise(Terminal.Signal.INT)
        }

        while (process.isAlive) {
            try {
                reader.readLine()
            } catch (e: UserInterruptException) {
                process.destroy()
            } catch (e: EndOfFileException) {
                process.destroy()
            }
        }

        return process.exitValue()
    }
}

object WrapperNotFoundException : Exception("Gradle wrapper not found")