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

package com.haulmont.cuba.cli.cubaplugin.model

import com.haulmont.cuba.cli.generation.get
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.generation.xpath
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure.Companion.CORE_MODULE
import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure.Companion.GLOBAL_MODULE
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.resolve
import net.sf.practicalxml.DomUtil
import org.w3c.dom.Element
import java.nio.file.Path

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

    val appComponentsStr: String

    val database: Database

    init {
        try {
            val global = projectStructure.getModule(GLOBAL_MODULE)

            rootPackageDirectory = projectStructure.rootPackageDirectory

            rootPackage = projectStructure.rootPackage

            name = projectStructure.settingsGradle.toFile().readText().let {
                Regex("rootProject.name *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]").findAll(it).getGroupValue(1)
                        ?: artifactParseError()
            }

            namespace = parseNamespace(global.metadataXml)

            val buildGradle = projectStructure.buildGradle.toFile().readText()

            val groupRegex = Regex("group *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            group = groupRegex.findAll(buildGradle).getGroupValue(1) ?: artifactParseError()

            val versionRegex = Regex("version *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            version = versionRegex.findAll(buildGradle).getGroupValue(1) ?: artifactParseError()

            val copyrightRegex = Regex("copyright *= *'''(.*)'''")
            copyright = copyrightRegex.findAll(buildGradle).getGroupValue(1)

            val platformVersionRegex = Regex("ext\\.cubaVersion *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            val platformVersionString = platformVersionRegex.findAll(buildGradle).getGroupValue(1)
                    ?: artifactParseError()

            platformVersion = PlatformVersion(platformVersionString)

            val modulePrefixRegex = Regex("def *modulePrefix *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
            modulePrefix = modulePrefixRegex.findAll(buildGradle).getGroupValue(1) ?: artifactParseError()

            appComponents = parseAppComponents(projectStructure)

            appComponentsStr = appComponents.joinToString(separator = " ")

            database = parseJndiDatabase(projectStructure) ?: parseApplicationDatabase(projectStructure)
                    ?: throw ProjectScanException("Unable to scan db properties")
        } catch (e: ProjectFileNotFoundException) {
            throw ProjectScanException(e.message!!, e)
        }
    }

    private fun parseAppComponents(projectStructure: ProjectStructure): List<String> {
        val webXml = projectStructure.getModule(ModuleStructure.WEB_MODULE).path
                .resolve("web", "WEB-INF", "web.xml")

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

private fun parseNamespace(metadataXml: Path): String {
    val document = parse(metadataXml)
    return DomUtil.getChild(document.documentElement, "metadata-model")
            ?.attributes
            ?.getNamedItem("namespace")
            ?.nodeValue ?: ""
}

private fun parseJndiDatabase(projectStructure: ProjectStructure): Database? {
    val contextXml = projectStructure.getModule(CORE_MODULE)
            .path
            .resolve("web")
            .resolve("META-INF")
            .resolve("context.xml")

    val contextXmlRoot = parse(contextXml).documentElement
    val resourceElement = contextXmlRoot.xpath("//Resource[@name=\"jdbc/CubaDS\"]").firstOrNull() as? Element
            ?: return null

    return Database(
            getDbTypeByDriver(resourceElement["driverClassName"]),
            resourceElement["url"],
            resourceElement["driverClassName"],
            getPrefixUrl(resourceElement["driverClassName"]),
            resourceElement["username"],
            resourceElement["password"],
            DataSourceProvider.JNDI
    )
}

private fun parseApplicationDatabase(projectStructure: ProjectStructure): Database? {
    val appPropertiesPath = projectStructure.getModule(CORE_MODULE)
            .path
            .resolve("src")
            .resolve(projectStructure.rootPackageDirectory)
            .resolve("app.properties")

    val properties = Properties(appPropertiesPath)

    val url = properties["cuba.dataSource.jdbcUrl"]

    val driverClassName = properties["cuba.dataSource.driverClassName"] ?: ""
    val dbType = getDbTypeByDriver(driverClassName)
    val prefixUrl = getPrefixUrl(driverClassName)

    val username = properties["cuba.dataSource.username"]!!
    val password = properties["cuba.dataSource.password"] ?: ""

    if (url == null) {
        val host = properties["cuba.dataSource.host"]!!
        val port = properties["cuba.dataSource.port"]?.let { ":$it" } ?: ""
        val dbName = properties["cuba.dataSource.dbName"]!!
        val connectionParams = properties["cuba.dataSource.connectionParams"]

        return Database(
                dbType,
                getConnectionUrl(dbType, prefixUrl, host + port, connectionParams, dbName),
                driverClassName,
                prefixUrl,
                username,
                password,
                DataSourceProvider.Application
        )
    } else {
        return Database(
                dbType,
                url,
                driverClassName,
                prefixUrl,
                username,
                password,
                DataSourceProvider.Application
        )
    }
}

private fun getDbTypeByDriver(driverClass: String): String = when {
    "hsql" in driverClass -> "hsql"
    "postgres" in driverClass -> "postgres"
    "sqlserver" in driverClass -> "mssql"
    "oracle" in driverClass -> "oracle"
    "mysql" in driverClass -> "mysql"
    else -> throw ProjectScanException("Unrecognized jdbc driver class $driverClass")
}

fun getPrefixUrl(driverClass: String): String = when {
    "hsql" in driverClass -> "jdbc:hsqldb:hsql://"
    "postgres" in driverClass -> "jdbc:postgresql://"
    "sqlserver" in driverClass -> "jdbc:sqlserver://"
    "oracle" in driverClass -> "jdbc:oracle:thin:@//"
    "mysql" in driverClass -> "jdbc:mysql://"
    else -> throw ProjectScanException("Unrecognized jdbc driver class $driverClass")
}

fun getConnectionUrl(dbType: String, prefix: String, host: String, connectionParams: String?, dbName: String): String {
    return when (dbType) {
        "hsql" -> prefix + host + "/" + dbName + (connectionParams?.let { ";$it" } ?: "")
        "postgres", "mssql", "oracle", "mysql" -> prefix + host + "/" + dbName + (connectionParams?.let { "?$it" }
                ?: "")
        else -> throw ProjectScanException("Unable to construct jdbc url")
    }
}

private fun Sequence<MatchResult>.getGroupValue(groupIndex: Int): String? =
        firstOrNull()?.groupValues?.get(groupIndex)

class ProjectScanException(message: String, cause: Throwable? = null) : Exception(message, cause)

enum class DataSourceProvider {
    JNDI, Application
}

data class Database(val type: String, val connectionString: String, val driverClassName: String, val urlPrefix: String, val username: String, val password: String, val dataSourceProvider: DataSourceProvider)