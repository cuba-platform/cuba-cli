package com.haulmont.cuba.cli.cubaplugin

import com.haulmont.cuba.cli.model.ProjectModel
import java.io.File
import java.util.stream.Collectors

@Throws(ProjectScanException::class)
fun scanProject(): ProjectModel {
    val model = ProjectModel()

    val persistenceXml = findPersistenceXml()

    model.rootPackage = getRootPackage(persistenceXml)
    model.name = File("").name

    parseBuildGradle(model)
    parseAppComponents(model)
    parseDatabaseInfo(model)

    return model
}

class ProjectScanException(message: String) : Exception(message)

private fun parseBuildGradle(model: ProjectModel) {
    val artifactParseError: () -> Nothing = { throw ProjectScanException("Can not parse artifact info") }

    val buildGradle = File("build.gradle")
            .bufferedReader()
            .lines()
            .collect(Collectors.joining("\n"))

    val groupRegex = Regex("group *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
    model.group = groupRegex.findAll(buildGradle)
            .firstOrNull()?.let { matchResult -> matchResult.groupValues[1] }
            ?: artifactParseError()


    val versionRegex = Regex("version *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
    model.version = versionRegex.findAll(buildGradle)
            .firstOrNull()?.let { matchResult -> matchResult.groupValues[1] }
            ?: artifactParseError()


    val copyrightRegex = Regex("copyright *= *'''(.*)'''")
    model.copyright = copyrightRegex.findAll(buildGradle)
            .firstOrNull()?.let { matchResult -> matchResult.groupValues[1] }


    val cubaVersionRegex = Regex("ext\\.cubaVersion *= *['\"]([a-zA-Z0-9_.\\-]+)['\"]")
    model.cubaVersion = cubaVersionRegex.findAll(buildGradle)
            .firstOrNull()?.let { matchResult -> matchResult.groupValues[1] }
            ?: artifactParseError()
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
