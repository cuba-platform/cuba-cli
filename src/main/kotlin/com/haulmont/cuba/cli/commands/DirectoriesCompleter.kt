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

import org.jline.builtins.Completers
import org.jline.reader.Candidate
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Replacement of default directories completer for normal work on windows
 */
class DirectoriesCompleter(currentDir: Path) : Completers.DirectoriesCompleter(currentDir) {
    override fun complete(reader: LineReader, commandLine: ParsedLine, candidates: MutableList<Candidate>) {
        val buffer = commandLine.word().substring(0, commandLine.wordCursor())

        val current: Path
        val curBuf: String
        val lastSep = buffer.lastIndexOf("/")
        if (lastSep >= 0) {
            curBuf = buffer.substring(0, lastSep + 1)
            current = if (curBuf.startsWith("~")) {
                if (curBuf.startsWith("~/")) {
                    userHome.resolve(curBuf.substring(2))
                } else {
                    userHome.parent.resolve(curBuf.substring(1))
                }
            } else {
                userDir.resolve(curBuf)
            }
        } else {
            curBuf = ""
            current = userDir
        }
        try {
            Files.newDirectoryStream(current) { this.accept(it) }.forEach { p ->
                val value = curBuf + p.fileName.toString()
                if (Files.isDirectory(p)) {
                    candidates.add(Candidate(
                            value + if (reader.isSet(LineReader.Option.AUTO_PARAM_SLASH)) "/" else "",
                            getDisplay(reader.terminal, p), null, null,
                            if (reader.isSet(LineReader.Option.AUTO_REMOVE_SLASH)) "/" else null, null,
                            false))
                } else {
                    candidates.add(Candidate(value, getDisplay(reader.terminal, p), null, null, null, null, true))
                }
            }
        } catch (e: IOException) {
            // Ignore
        }

    }
}