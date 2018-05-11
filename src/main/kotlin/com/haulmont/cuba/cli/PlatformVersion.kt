package com.haulmont.cuba.cli

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

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
        operator fun invoke(versionStr: String): PlatformVersion {
            if (versionStr.isEmpty() || versionStr == "latest") {
                return LatestVersion
            }

            versionStr.replace(Regex("[^0-9.]"), "")
                    .trim('.')
                    .split('.')
                    .map { it.toInt() }
                    .let { return SpecificVersion(it) }
        }
    }
}

object LatestVersion : PlatformVersion()
class SpecificVersion(val versionNumbers: List<Int>) : PlatformVersion()