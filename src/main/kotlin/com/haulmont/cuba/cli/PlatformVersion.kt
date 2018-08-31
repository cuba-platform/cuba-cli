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

import org.kodein.di.direct
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

/**
 * Represents CUBA Platform version.
 *
 * May be [com.haulmont.cuba.cli.SpecificVersion] or [com.haulmont.cuba.cli.LatestVersion].
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

        return versionNumbersWithoutTrailingZeros.size.compareTo(other.versionNumbersWithoutTrailingZeros.size)
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
        private val specificVersionRegex = "([0-9]+\\.)*([0-9]+)(\\.[0-9\\w-]+)?".toRegex()

        fun parse(versionStr: String): PlatformVersion {
            try {
                if (versionStr.isEmpty() || versionStr == "latest") {
                    return LatestVersion
                }

                if (!versionStr.matches(specificVersionRegex))
                    throw PlatformVersionParseException()

                return versionStr.replace(Regex("[^0-9.]"), "")
                        .trim('.')
                        .split('.')
                        .map { it.toInt() }
                        .let { SpecificVersion(it) }
            } catch (e: Exception) {
                throw PlatformVersionParseException(e)
            }
        }

        operator fun invoke(versionStr: String): PlatformVersion = parse(versionStr)

        fun findVersion(): PlatformVersion {
            val context = kodein.direct.instance<CliContext>()
            if (context.hasModel(ProjectModel.MODEL_NAME)) {
                val model = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)
                return model.platformVersion
            }

            return LatestVersion
        }
    }
}

object LatestVersion : PlatformVersion()
class SpecificVersion(val versionNumbers: List<Int>) : PlatformVersion() {
    internal val versionNumbersWithoutTrailingZeros
        get() = versionNumbers.dropLastWhile { it == 0 }
}

class PlatformVersionParseException : Exception {
    constructor() : super()

    constructor(cause: Exception) : super(cause)

}