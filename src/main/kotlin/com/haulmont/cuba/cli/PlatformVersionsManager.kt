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

import com.google.gson.Gson
import com.haulmont.cuba.cli.commands.LaunchOptions
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread

class PlatformVersionsManager {
    private val messages by localMessages()

    private val logger = Logger.getLogger(PlatformVersionsManager::class.java.name)

    var versions: List<String> = messages["platformVersions"].split(",").map { it.trim() }
        private set

    fun load() {
        if (!LaunchOptions.skipVersionLoading) thread(isDaemon = true) {
            try {
                versions = loadInfoJson()
                        .let(::extractVersions)
                        .let(::filterVersions)
            } catch (e: Throwable) {
                logger.log(Level.SEVERE, "Error during platform versions retrieving", e)
            }
        }
    }

    private fun extractVersions(infoJson: String) = Gson()
            .fromJson(infoJson, StudioConfig::class.java)
            .platform_versions

    private fun loadInfoJson(): String = URL(LaunchOptions.versionsConfigUrl)
            .openConnection()
            .apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            .getInputStream()
            .use {
                it.bufferedReader().readText()
            }

    private fun filterVersions(fullList: List<String>): List<String> {
        val v68 = SpecificVersion(listOf(6, 8))
        val v69 = SpecificVersion(listOf(6, 9))
        val v6_10 = SpecificVersion(listOf(6, 10))

        val sortedVersions = fullList.sortedBy { PlatformVersion(it) }

        val lastTwo68 = sortedVersions.filter { PlatformVersion(it).let { v -> v >= v68 && v < v69 } }.takeLast(1)
        val lastTwo69 = sortedVersions.filter { PlatformVersion(it).let { v -> v >= v69 && v < v6_10 } }.takeLast(1)

        return lastTwo68 + lastTwo69
    }

    data class StudioConfig(val platform_versions: List<String>)

    companion object {
        private const val TIMEOUT_MS = 60_000
    }
}