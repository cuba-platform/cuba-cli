package com.haulmont.cuba.cli.model

import com.haulmont.cuba.cli.ModuleType
import com.haulmont.cuba.cli.ProjectFiles
import com.haulmont.cuba.cli.generation.parse
import net.sf.practicalxml.DomUtil
import java.io.File
import java.nio.file.Path

@Suppress("MemberVisibilityCanBePrivate")
class ProjectModel(projectFiles: ProjectFiles) {
    val name: String

    val rootPackage: String

    val rootPackageDirectory: String

    val namespace: String

    val group: String

    val version: String

    val copyright: String?

    val cubaVersion: String

    init {
        val global = projectFiles.getModule(ModuleType.GLOBAL)

        rootPackageDirectory = projectFiles.rootPackageDirectory.toString()

        rootPackage = rootPackageDirectory.replace(File.separatorChar, '.')

        name = File("").name

        namespace = parseNamespace(global.persistenceXml)

//        build.gradle
        val artifactParseError: () -> Nothing = { throw ProjectScanException("Can not parse artifact info") }

        val buildGradle = File("build.gradle").readText()

        val groupRegex = Regex("group *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
        group = groupRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

        val versionRegex = Regex("version *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
        version = versionRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()


        val copyrightRegex = Regex("copyright *= *'''(.*)'''")
        copyright = copyrightRegex.findAll(buildGradle) groupNOrNull 1

        val cubaVersionRegex = Regex("ext\\.cubaVersion *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
        cubaVersion = cubaVersionRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()
    }

    companion object {
        const val MODEL_NAME = "project"
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

class ProjectScanException(message: String) : Exception(message)