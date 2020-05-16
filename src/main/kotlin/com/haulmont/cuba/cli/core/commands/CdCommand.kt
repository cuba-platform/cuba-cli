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

package com.haulmont.cuba.cli.core.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.core.WorkingDirectoryManager
import com.haulmont.cuba.cli.core.kodein
import com.haulmont.cuba.cli.core.resolve
import org.kodein.di.generic.instance
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

@Parameters(commandDescription = "Changes current directory if specified, prints current one otherwise")
class CdCommand : AbstractCommand() {
    @Parameter(description = "Target directory")
    private var directory: String? = null

    private val workingDirectoryManager: WorkingDirectoryManager by kodein.instance()

    private val printWriter: PrintWriter by kodein.instance()

    private val isUnix = System.getProperty("os.name").toLowerCase().let {
        it.contains("nux") || it.contains("mac")
    }

    override fun run() {
        if (directory == null) {
            printWriter.println(workingDirectoryManager.absolutePath)
            return
        }

        if (isUnix) {
            directory = directory!!.replaceFirst("^~".toRegex(), System.getProperty("user.home"))
        }

        val dst = if (directory!!.startsWith('/')) {
            Paths.get(directory!!)
        } else workingDirectoryManager.absolutePath.resolve(*directory!!.split(File.separatorChar).toTypedArray())

        if (Files.exists(dst) && Files.isDirectory(dst)) {
            workingDirectoryManager.workingDirectory = dst.normalize()
        } else printWriter.println("Directory not found")
    }
}