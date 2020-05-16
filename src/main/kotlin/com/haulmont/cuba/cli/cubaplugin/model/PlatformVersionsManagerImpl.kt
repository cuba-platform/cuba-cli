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

import com.google.gson.Gson
import com.haulmont.cli.core.commands.LaunchOptions
import com.haulmont.cli.core.localMessages
import com.haulmont.cli.core.thisClassLogger
import java.net.URL
import java.util.logging.Level
import kotlin.concurrent.thread

class PlatformVersionsManagerImpl : PlatformVersionsManager {
    private val messages by localMessages()

    private val logger by thisClassLogger()

    override val supportedVersionsRange = SpecificVersion(6, 8, 0)..SpecificVersion(7, 3, 0)

    override var versions: List<String> = messages["platformVersions"].split(",").map { it.trim() }
        private set

    val loadThread: Thread by lazy {
        thread(isDaemon = true) {
            if (!LaunchOptions.skipVersionLoading) {
                try {
                    versions = loadInfoJson()
                            .let(::extractVersions)
                            .let(::filterVersions)
                            .distinct()
                } catch (e: Throwable) {
                    logger.log(Level.SEVERE, "Error during platform versions retrieving", e)
                }
            }
        }
    }

    override fun load() {
        loadThread // force field initialization
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
        val borders = listOf<PlatformVersion>(
                SpecificVersion(6, 8),
                SpecificVersion(6, 9),
                SpecificVersion(6, 10),
                SpecificVersion(6, 11),
                SpecificVersion(7, 0),
                SpecificVersion(7, 1),
                SpecificVersion(7, 2),
                SpecificVersion(7, 3)
        )

        val supportedVersionRanges = (0 until borders.size - 1).map {
            borders[it]..borders[it + 1]
        }.toList()

        val sortedVersions = fullList.sortedBy { PlatformVersion(it) }

        return supportedVersionRanges.flatMap { range ->
            sortedVersions.filter {
                PlatformVersion(it) in range
            }.takeLast(1)
        }.reversed()
    }

    data class StudioConfig(val platform_versions: List<String> = emptyList())

    companion object {
        private const val TIMEOUT_MS = 60_000
    }
}