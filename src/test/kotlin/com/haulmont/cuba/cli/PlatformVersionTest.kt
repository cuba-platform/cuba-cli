package com.haulmont.cuba.cli

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class PlatformVersionTest : Spek({
    describe("PlatformVersion") {

        it("should parse empty line and `latest` strings as LatestVersion") {
            assertEquals(PlatformVersion(""), LatestVersion)
            assertEquals(PlatformVersion("latest"), LatestVersion)
        }

        it("should parse correctly") {
            val version = PlatformVersion("1.2.3")

            assertTrue(version is SpecificVersion)
            version as SpecificVersion

            assertEquals(version.versionNumbers, listOf(1, 2, 3))
        }

        it("should ignore all characters except numeric and dots") {
            val snapshot = PlatformVersion("1.2.3-SNAPSHOT")
            snapshot as SpecificVersion
            assertEquals(snapshot.versionNumbers, listOf(1, 2, 3))

            val release = PlatformVersion("4.5.6.RELEASE")
            release as SpecificVersion
            assertEquals(release.versionNumbers, listOf(4, 5, 6))
        }
    }

    given("LatestVersion") {
        val latest = LatestVersion

        on("comparision with itself") {
            it("should be equal") {
                assertTrue(latest.compareTo(latest) == 0)
            }
        }

        on("comparision with another") {
            val others = listOf("1.2", "3.4", "5.5.5", "0.1", "1000", "1.0-SNAPSHOT").map { PlatformVersion(it) }
            it("always greater") {
                others.forEach {
                    assertTrue(latest > it)
                    assertTrue(it < latest)
                }
            }
        }
    }

    given("one specific version") {
        val one = SpecificVersion(listOf(6, 8, 5))

        given("greater specific version") {
            val another = SpecificVersion(listOf(6, 9, 0))

            on("comparision, first should be less") {
                assertTrue(one < another)
                assertTrue(another > one)
            }
        }

        given("greater specific version with same numbers on same positions") {
            val another = SpecificVersion(listOf(6, 8, 5, 2))

            on("comparision, first should be less") {
                assertTrue(one < another)
                assertTrue(another > one)
            }
        }
    }
})