/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@SmallTest
@RunWith(AndroidJUnit4::class)
public class OutputsTest {
    private val outputs: MutableList<String> = mutableListOf()

    @Before
    public fun setUp() {
        outputs.addAll(
            // Don't add the / prefix.
            listOf(
                "foo/a.txt",
                "foo/b.txt",
                "foo/bar/a.txt",
                "foo/bar/baz/a.txt",
            )
        )
    }

    @Test
    public fun testRelativePaths_usesIntendedOutputDirectory() {
        assertRelativePaths(Outputs.outputDirectory, outputs)
    }

    @Test
    @SdkSuppress(minSdkVersion = 30, maxSdkVersion = 30)
    public fun testRelativePaths_usesDirectoryUsableByAppAndShell() {
        assertRelativePaths(Outputs.dirUsableByAppAndShell, outputs)
    }

    @Test
    public fun sanitizeFilename() {
        assertEquals(
            "testFilename[Thing[]]",
            Outputs.sanitizeFilename("testFilename[Thing( )]")
        )
    }

    @Test
    public fun testDateToFileName() {
        val date = Date(0)
        val expected = "1970-01-01-00-00-00"
        assertEquals(Outputs.dateToFileName(date), expected)
    }

    private fun assertRelativePaths(base: File, paths: List<String>) {
        val basePath = base.absolutePath
        val relativePaths = paths.map { Outputs.relativePathFor(File(base, it).absolutePath) }
        relativePaths.forEach { path ->
            assertFalse(path.startsWith("/"), "$path cannot start with a `/`.")
            assertFalse(
                path.startsWith(basePath),
                "Invalid relative path ($path), Base ($basePath)."
            )
        }

        for ((path, relativePath) in paths.zip(relativePaths)) {
            assertEquals(path, relativePath, "$path != $relativePath")
        }
    }
}
