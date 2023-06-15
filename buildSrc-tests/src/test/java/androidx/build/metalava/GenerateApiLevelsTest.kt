/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build.metalava

import androidx.build.Version
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerateApiLevelsTest {
    @Test
    fun testVersions() {
        val v100beta01 = File("/api/1.0.0-beta01.txt")
        val v110beta01 = File("/api/1.1.0-beta01.txt")
        val current = File("/api/current.txt")
        val currentVersion = Version("1.2.0-alpha03")

        val actualFiles = getFilesForApiLevels(
            listOf(v100beta01, v110beta01, current),
            currentVersion
        )
        val expectedFiles = listOf(v100beta01, v110beta01)
        assertEquals(actualFiles, expectedFiles)

        val actualVersions = getVersionsForApiLevels(expectedFiles)
        val expectedVersions = listOf(Version("1.0.0"), Version("1.1.0"))
        assertEquals(actualVersions, expectedVersions)
    }

    @Test
    fun testResourceApiFilesNotUsed() {
        val v100beta01 = File("/api/1.0.0-beta01.txt")
        val inputFiles = setOf(
            v100beta01,
            File("/api/res-1.0.0-beta01.txt"),
            File("/api/res-1.1.0-beta01.txt"),
            File("/api/res-current.txt"),
        )
        val currentVersion = Version("1.2.0-alpha03")

        val actualFiles = getFilesForApiLevels(inputFiles, currentVersion)
        val expectedFiles = listOf(v100beta01)
        assertEquals(actualFiles, expectedFiles)

        val actualVersions = getVersionsForApiLevels(expectedFiles)
        val expectedVersions = listOf(Version("1.0.0"))
        assertEquals(actualVersions, expectedVersions)
    }

    @Test
    fun testOnlyCurrentVersion() {
        val actualFiles = getFilesForApiLevels(
            setOf(File("/api/current.txt")),
            Version("1.0.0-alpha05")
        )
        assertEquals(actualFiles, emptyList<File>())
    }

    @Test
    fun testFiltering() {
        val v100beta03 = File("/api/1.0.0-beta03.txt")
        val v110beta02 = File("/api/1.1.0-beta02.txt")
        val v120beta01 = File("/api/1.2.0-beta01.txt")
        val inputFiles = setOf(
            File("/api/1.0.0-beta01.txt"),
            File("/api/1.0.0-beta02.txt"),
            v100beta03,
            File("/api/1.1.0-beta01.txt"),
            v110beta02,
            v120beta01,
            File("/api/1.3.0-beta01.txt"),
            File("/api/1.3.0-beta02.txt"),
            File("/api/current.txt")
        )
        val currentVersion = Version("1.3.0-beta02")

        val actualFiles = getFilesForApiLevels(inputFiles, currentVersion)
        val expectedFiles = listOf(v100beta03, v110beta02, v120beta01)
        assertEquals(actualFiles, expectedFiles)

        val actualVersions = getVersionsForApiLevels(expectedFiles)
        val expectedVersions = listOf(
            Version("1.0.0"),
            Version("1.1.0"),
            Version("1.2.0")
        )
        assertEquals(actualVersions, expectedVersions)
    }
}
