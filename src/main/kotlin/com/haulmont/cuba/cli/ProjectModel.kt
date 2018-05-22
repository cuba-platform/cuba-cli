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

package com.haulmont.cuba.cli

import com.haulmont.cuba.cli.ModuleStructure.Companion.CORE_MODULE
import com.haulmont.cuba.cli.ModuleStructure.Companion.GLOBAL_MODULE
import com.haulmont.cuba.cli.generation.get
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.generation.xpath
import net.sf.practicalxml.DomUtil
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("MemberVisibilityCanBePrivate")
class ProjectModel(projectStructure: ProjectStructure) {
    val name: String

    val group: String

    val version: String

    val rootPackage: String

    val rootPackageDirectory: String

    val namespace: String

    val platformVersion: PlatformVersion

    val copyright: String?

    val modulePrefix: String

    val appComponents: List<String>

    val database: Database

    init {
        try {
            val global = projectStructure.getModule(GLOBAL_MODULE)

            rootPackageDirectory = projectStructure.rootPackageDirectory.toString()

            rootPackage = rootPackageDirectory.replace(File.separatorChar, '.')

            name = projectStructure.settingsGradle.toFile().readText().let {
                Regex("rootProject.name *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]").findAll(it) groupNOrNull 1
                        ?: artifactParseError()
            }

            namespace = parseNamespace(global.persistenceXml)

            val buildGradle = File("build.gradle").readText()

            val groupRegex = Regex("group *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            group = groupRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

            val versionRegex = Regex("version *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            version = versionRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

            val copyrightRegex = Regex("copyright *= *'''(.*)'''")
            copyright = copyrightRegex.findAll(buildGradle) groupNOrNull 1

            val platformVersionRegex = Regex("ext\\.cubaVersion *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            val platformVersionString = platformVersionRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

            platformVersion = PlatformVersion(platformVersionString)

            val modulePrefixRegex = Regex("def *modulePrefix *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            modulePrefix = modulePrefixRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

            appComponents = parseAppComponents(projectStructure)

            database = parseDatabase(projectStructure)
        } catch (e: ProjectFileNotFoundException) {
            throw ProjectScanException(e.message!!, e)
        }
    }

    private fun parseAppComponents(projectStructure: ProjectStructure): List<String> {
        val webXml = projectStructure.getModule(ModuleStructure.WEB_MODULE).path
                .resolve(Paths.get("web", "WEB-INF", "web.xml"))

        return parse(webXml).documentElement
                .xpath("//context-param[param-name[text()='appComponents']]/param-value")
                .firstOrNull()?.textContent?.split(Regex(" +")) ?: emptyList()
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

private fun parseDatabase(projectStructure: ProjectStructure): Database {
    val contextXml = projectStructure.getModule(CORE_MODULE)
            .path
            .resolve("web")
            .resolve("META-INF")
            .resolve("context.xml")

    val contextXmlRoot = parse(contextXml).documentElement
    val resourceElement = contextXmlRoot.xpath("//Resource[@name=\"jdbc/CubaDS\"]").first() as Element

    return Database(getDbTypeByDriver(resourceElement["driverClassName"]), resourceElement["url"], resourceElement["driverClassName"])
}

private fun getDbTypeByDriver(driverClass: String): String = when {
    "hsql" in driverClass -> "hsql"
    "postgres" in driverClass -> "postgres"
    "sqlserver" in driverClass -> "mssql"
    "oracle" in driverClass -> "mssql"
    "mysql" in driverClass -> "mysql"
    else -> throw ProjectScanException("Unrecognized jdbc driver class $driverClass")
}


private infix fun Sequence<MatchResult>.groupNOrNull(groupIndex: Int): String? =
        firstOrNull()?.groupValues?.get(groupIndex)

class ProjectScanException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class Database(val type: String, val connectionString: String, val driverClassName: String)