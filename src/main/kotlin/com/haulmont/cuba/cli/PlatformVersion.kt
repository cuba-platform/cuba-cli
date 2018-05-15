package com.haulmont.cuba.cli

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

/**
 * Represents CUBA Platform version.
 *
 * May be {@link com.haulmont.cuba.cli.SpecifiedVersion} or {@link com.haulmont.cuba.cli.LatestVersion}.
 *
 * It is used to choose most appropriate artifact template for current project CUBA Platform version.
 *
 * Choice rule is as follows:
 *
 * All first level child directories names in template root are tried to parse as version string.
 * If at least one of the directories doesn't conforms version naming rule, all template is considered
 * as not supporting versions, and the root template directory is been choosing.
 *
 * Parsed versions sorted in ascent order and directory with first equal or greater version is been choosing.
 *
 */
sealed class PlatformVersion : Comparable<PlatformVersion> {
    override fun compareTo(other: PlatformVersion): Int {
        if (other == this) return 0

        if (other == LatestVersion)
            return -1

        if (this == LatestVersion)
            return 1

        this as SpecificVersion
        other as SpecificVersion

        versionNumbers.zip(other.versionNumbers)
                .forEach { (it, that) ->
                    if (it < that)
                        return -1
                    if (it > that)
                        return 1
                }

        return versionNumbers.size.compareTo(other.versionNumbers.size)
    }

    fun findMostSuitableVersionDirectory(baseDirectory: Path): Path {
        Files.walk(baseDirectory, 1)
                .filter { Files.isDirectory(it) && it != baseDirectory }
                .collect(Collectors.toList())
                .associateBy {
                    try {
                        PlatformVersion(it.fileName.toString())
                    } catch (e: Exception) {
//                        found directory that doesn't conform any version naming rules
                        return baseDirectory
                    }
                }.toSortedMap()
                .onEach { (dirVersion, directory) ->
                    if (dirVersion >= this)
                        return directory
                }.let {
                    if (it.isNotEmpty())
                        return it[it.lastKey()]!!
                }

        return baseDirectory
    }

    companion object {
        fun parse(versionStr: String): PlatformVersion {
            if (versionStr.isEmpty() || versionStr == "latest") {
                return LatestVersion
            }

            return versionStr.replace(Regex("[^0-9.]"), "")
                    .trim('.')
                    .split('.')
                    .map { it.toInt() }
                    .let { SpecificVersion(it) }
        }

        operator fun invoke(versionStr: String): PlatformVersion = parse(versionStr)
    }
}

object LatestVersion : PlatformVersion()
class SpecificVersion(val versionNumbers: List<Int>) : PlatformVersion()