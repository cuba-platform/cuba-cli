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

package com.haulmont.cuba.cli.generation

import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersion
import com.haulmont.cuba.cli.Resources
import java.nio.file.Files
import java.nio.file.Path

class Snippets(private val snippetsPath: Path) {
    private val snippets: MutableMap<String, String> = mutableMapOf()

    operator fun get(name: String): String {
        if (name !in snippets) {
            snippets[name] = Files.newInputStream(snippetsPath.resolve(name)).use { stream -> stream.reader().readText() }
        }
        return snippets[name]!!
    }

    companion object {
        operator fun invoke(resources: Resources, snippetsPath: String, platformVersion: PlatformVersion = PlatformVersion.findVersion()): Snippets {
            val baseDirectory = resources.getSnippets(snippetsPath)

            return platformVersion.findMostSuitableVersionDirectory(baseDirectory)
                    .let(::Snippets)
        }
    }
}