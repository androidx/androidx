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

package androidx.build.checkapi

import androidx.build.Version
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CheckApiTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    @Test
    fun getRequiredCompatibilityApiFileFromDirTest() {
        val apiDir = createTempDir("api", CORE_API_FILES)

        // If we haven't committed the current version's API surface to a file yet, use the most
        // recent version that came before it.
        assertEquals(
            "1.1.0-beta02.txt",
            getRequiredCompatibilityApiFileFromDir(
                apiDir,
                Version("1.1.0-beta03"),
                ApiType.CLASSAPI
            )?.name
        )

        // If we already committed the current version's API surface to a file, then we should
        // consider that the finalized API surface against which compatibility is checked.
        assertEquals(
            "1.1.0-beta02.txt",
            getRequiredCompatibilityApiFileFromDir(
                apiDir,
                Version("1.1.0-beta02"),
                ApiType.CLASSAPI
            )?.name
        )

        assertEquals(
            "1.3.0-beta01.txt",
            getRequiredCompatibilityApiFileFromDir(
                apiDir,
                Version("1.4.0-alpha01"),
                ApiType.CLASSAPI
            )?.name
        )
    }

    @Suppress("SameParameterValue")
    private fun createTempDir(dirName: String, fileNames: List<String>): File =
        tmpFolder.newFolder(dirName).also { apiDir ->
            fileNames.forEach { fileName ->
                File(apiDir, fileName).createNewFile()
            }
        }
}

/**
 * List of API files representing `androidx.core:core:1.3.0-beta01`.
 */
private val CORE_API_FILES = listOf(
    "1.1.0-beta01.txt",
    "1.1.0-beta02.txt",
    "1.1.0-rc01.txt",
    "1.2.0-beta01.txt",
    "1.2.0-beta02.txt",
    "1.3.0-beta01.txt",
    "api_lint.ignore",
    "current.txt",
    "public_plus_experimental_1.0.0.txt",
    "public_plus_experimental_1.1.0-beta01.txt",
    "public_plus_experimental_1.1.0-rc01.txt",
    "public_plus_experimental_1.2.0-beta01.txt",
    "public_plus_experimental_1.2.0-beta02.txt",
    "public_plus_experimental_1.3.0-beta01.txt",
    "res-1.1.0-beta01.txt",
    "res-1.1.0-beta02.txt",
    "res-1.1.0-rc01.txt",
    "res-1.2.0-beta01.txt",
    "res-1.2.0-beta02.txt",
    "res-1.3.0-beta01.txt",
    "res-current.txt",
    "restricted_1.0.0.txt",
    "restricted_1.1.0-beta01.txt",
    "restricted_1.1.0-beta02.txt",
    "restricted_1.1.0-rc01.txt",
    "restricted_1.2.0-beta01.txt",
    "restricted_1.2.0-beta02.txt",
    "restricted_1.3.0-beta01.txt",
    "restricted_current.txt"
)
