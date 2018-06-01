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

import com.haulmont.cuba.cli.LatestVersion
import com.haulmont.cuba.cli.PlatformVersion
import com.haulmont.cuba.cli.Resources
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Path

class Snippets(private val snippetsPath: Path) {
    private val snippets: MutableMap<String, String> = mutableMapOf()

    operator fun get(name: String): String {
        if (name !in snippets) {
            snippets[name] = Files.newInputStream(snippetsPath.resolve(name)).reader().readText()
        }
        return snippets[name]!!
    }

    companion object {
        private val resources: Resources by kodein.instance()

        operator fun invoke(basePath: String, clazz: Class<Any>, platformVersion: PlatformVersion = LatestVersion): Snippets {
            val baseDirectory = resources.getResourcePath(basePath, clazz)!!

            return platformVersion.findMostSuitableVersionDirectory(baseDirectory)
                    .let(::Snippets)
        }
    }
}