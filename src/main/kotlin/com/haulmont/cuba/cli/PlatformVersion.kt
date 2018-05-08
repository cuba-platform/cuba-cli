package com.haulmont.cuba.cli

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