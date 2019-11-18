package com.haulmont.cuba.cli.cubaplugin.model

interface PlatformVersionsManager {
    val supportedVersionsRange: VersionRange

    val versions: List<String>

    fun load()
}