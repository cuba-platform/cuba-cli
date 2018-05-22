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
import org.w3c.dom.Element
import java.nio.file.Path

class Snippets(snippetPath: Path) {
    private val snippets: Map<String, String>

    init {
        val borderRegex = Regex("[\n \r]*\\|")
        snippets = parse(snippetPath).documentElement
                .xpath("//snippet")
                .filterIsInstance(Element::class.java)
                .associateBy({
                    it["name"]
                }, {
                    it.textContent
                            .replaceFirst(borderRegex, "")
                            .reversed()
                            .replaceFirst(borderRegex, "")
                            .reversed()
                })
    }

    operator fun get(name: String): String = snippets[name]!!

    companion object {
        private val resources: Resources by kodein.instance()

        operator fun invoke(basePath: String, snippetName: String, clazz: Class<Any>, platformVersion: PlatformVersion = LatestVersion): Snippets {
            val baseDirectory = resources.getResourcePath(basePath, clazz)!!

            return platformVersion.findMostSuitableVersionDirectory(baseDirectory)
                    .resolve(snippetName)
                    .let(::Snippets)
        }
    }
}