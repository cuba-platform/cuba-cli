package com.haulmont.cuba.cli.cubaplugin

import com.haulmont.cuba.cli.model.ProjectModel
import java.io.File

@Throws(ProjectScanException::class)
fun scanProject(): ProjectModel = ProjectModel().apply {
    val persistenceXml = findPersistenceXml()

    rootPackage = getRootPackage(persistenceXml)
    rootPackageDirectory = rootPackage.replace('.', File.separatorChar)
    name = File("").name

    parsePersistenceXml(this, persistenceXml)
    parseBuildGradle(this)
    parseAppComponents(this)
    parseDatabaseInfo(this)
}

class ProjectScanException(message: String) : Exception(message)


fun parsePersistenceXml(model: ProjectModel, persistenceXml: File) {
    model.namespace = Regex("<persistence-unit +name *= *\"([a-zA-Z0-9]+)\"")
            .findAll(persistenceXml.readText()) groupNOrNull 1
            ?: throw ProjectScanException("Unable to scan persistence xml")
}

private fun parseBuildGradle(model: ProjectModel) {
    val artifactParseError: () -> Nothing = { throw ProjectScanException("Can not parse artifact info") }

    val buildGradle = File("build.gradle").readText()

    val groupRegex = Regex("group *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
    model.group = groupRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()

    val versionRegex = Regex("version *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
    model.version = versionRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()


    val copyrightRegex = Regex("copyright *= *'''(.*)'''")
    model.copyright = copyrightRegex.findAll(buildGradle) groupNOrNull 1

    val cubaVersionRegex = Regex("ext\\.cubaVersion *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
    model.cubaVersion = cubaVersionRegex.findAll(buildGradle) groupNOrNull 1 ?: artifactParseError()
}

private fun parseAppComponents(model: ProjectModel) {

}

private fun parseDatabaseInfo(model: ProjectModel) {

}

val globalModuleDir = File("modules/global/src")

private fun getRootPackage(persistenceXml: File): String =
        persistenceXml.parentFile
                .relativeTo(globalModuleDir)
                .toString()
                .replace(File.separatorChar, '.')

private fun findPersistenceXml(): File {
    return globalModuleDir.walkTopDown()
            .filter { it.name == "persistence.xml" }
            .firstOrNull()
            ?: throw ProjectScanException("Unable to find persistence.xml")
}


private infix fun Sequence<MatchResult>.groupNOrNull(groupIndex: Int): String? =
        firstOrNull()?.groupValues?.get(groupIndex)