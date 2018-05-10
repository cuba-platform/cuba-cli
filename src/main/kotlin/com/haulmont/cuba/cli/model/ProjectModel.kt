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

package com.haulmont.cuba.cli.model

import com.haulmont.cuba.cli.ModuleStructure.Companion.GLOBAL_MODULE
import com.haulmont.cuba.cli.PlatformVersion
import com.haulmont.cuba.cli.ProjectFileNotFoundException
import com.haulmont.cuba.cli.ProjectStructure
import com.haulmont.cuba.cli.generation.parse
import net.sf.practicalxml.DomUtil
import java.io.File
import java.nio.file.Path

@Suppress("MemberVisibilityCanBePrivate")
class ProjectModel(projectStructure: ProjectStructure) {
    val name: String

    val rootPackage: String

    val rootPackageDirectory: String

    val namespace: String

    val group: String

    val platformVersionString: String

    val platformVersion: PlatformVersion

    val copyright: String?

    val cubaVersion: String

    val modulePrefix: String

    init {
        try {
            val global = projectStructure.getModule(GLOBAL_MODULE)

            rootPackageDirectory = projectStructure.rootPackageDirectory.toString()

            rootPackage = rootPackageDirectory.replace(File.separatorChar, '.')

            name = File("").name

            namespace = parseNamespace(global.persistenceXml)

//        build.gradle

            val buildGradle = File("build.gradle").readText()

            val groupRegex = Regex("group *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            group = groupRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

            val versionRegex = Regex("version *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            platformVersionString = versionRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

            platformVersion = PlatformVersion(platformVersionString)

            val copyrightRegex = Regex("copyright *= *'''(.*)'''")
            copyright = copyrightRegex.findAll(buildGradle) groupNOrNull 1

            val cubaVersionRegex = Regex("ext\\.cubaVersion *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            cubaVersion = cubaVersionRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

            val modulePrefixRegex = Regex("def *modulePrefix *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            modulePrefix = modulePrefixRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()
        } catch (e: ProjectFileNotFoundException) {
            throw ProjectScanException(e.message!!, e)
        }
    }

    companion object {
        const val MODEL_NAME = "project"

        fun artifactParseError(): Nothing {
            throw ProjectScanException("Can not parse artifact info")
        }
    }
}

private fun parseNamespace(persistenceXml: Path): String {
    val document = parse(persistenceXml)
    return DomUtil.getChild(document.documentElement, "persistence-unit")
            .attributes
            .getNamedItem("name")
            .nodeValue
}

private infix fun Sequence<MatchResult>.groupNOrNull(groupIndex: Int): String? =
        firstOrNull()?.groupValues?.get(groupIndex)

class ProjectScanException(message: String, cause: Throwable? = null) : Exception(message, cause)