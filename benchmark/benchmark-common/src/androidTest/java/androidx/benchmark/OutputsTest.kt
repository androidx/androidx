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
import java.io.File
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assert
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
public class OutputsTest {
    private val outputs: MutableList<String> = mutableListOf()

    @Before
    public fun setUp() {
        outputs.addAll(
            // Don't add the / prefix.
            listOf("foo/a.txt", "foo/b.txt", "foo/bar/a.txt", "foo/bar/baz/a.txt")
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

    /**
     * This test validates the strings searched for by BPGP aren't broken by filename sanitization.
     */
    @Test
    public fun sanitizeFilename_baselineProfileGradlePlugin() {
        val baselineSanitized = Outputs.sanitizeFilename("foo[a=b]-baseline-prof-001.txt")
        val startupSanitized = Outputs.sanitizeFilename("foo[a=b]-startup-prof-001.txt")

        assertEquals("foo_a_b_-baseline-prof-001.txt", baselineSanitized)
        assertEquals("foo_a_b_-startup-prof-001.txt", startupSanitized)
        baselineSanitized.apply {
            // NOTE: behavior MUST MATCH baseline profile gradle plugin check, see b/303034735
            assertTrue(contains("-baseline-prof-") && endsWith(".txt"))
        }
        startupSanitized.apply {
            // NOTE: behavior MUST MATCH baseline profile gradle plugin check, see b/303034735
            assertTrue(contains("-startup-prof-") && endsWith(".txt"))
        }
    }

    @Test
    public fun sanitizeFilename() {
        assertEquals(
            "testFilename_one_Thing_two_other_",
            Outputs.sanitizeFilename("testFilename[one=Thing( ),two:other]"),
        )
    }

    @Test
    public fun sanitizeFilename_tooLong() {
        assertEquals("a".repeat(199), Outputs.sanitizeFilename("a".repeat(199)))
        assertFailsWith<IllegalArgumentException> { Outputs.sanitizeFilename("a".repeat(200)) }
    }

    @Test
    public fun sanitizeFilename_withExtension() {
        assertEquals(
            "testFilename_one_Thing_two_other_.trace",
            Outputs.sanitizeFilename("testFilename[one=Thing( ),two:other].trace"),
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
                "Invalid relative path ($path), Base ($basePath).",
            )
        }

        for ((path, relativePath) in paths.zip(relativePaths)) {
            assertEquals(path, relativePath, "$path != $relativePath")
        }
    }

    @Test
    public fun dirUsableByAppAndShell_writeAppReadApp() {
        val dir = Outputs.dirUsableByAppAndShell
        val file = File.createTempFile("testFile", null, dir)
        try {
            file.writeText(file.name) // use name, as it's fairly unique
            Assert.assertEquals(file.name, file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    public fun dirUsableByAppAndShell_writeAppReadShell() {
        val dir = Outputs.dirUsableByAppAndShell
        val file = File.createTempFile("testFile", null, dir)
        file.setReadable(true, false)
        try {
            file.writeText(file.name) // use name, as it's fairly unique
            Assert.assertEquals(
                file.name,
                Shell.executeScriptCaptureStdout("cat ${file.absolutePath}"),
            )
        } finally {
            file.delete()
        }
    }

    @Test
    public fun dirUsableByAppAndShell_writeShellReadShell() {
        val dir = Outputs.dirUsableByAppAndShell

        // simple way to get a unique path, not shared across runs
        val file = File.createTempFile("shellwrite", null, dir)
        val path = file.absolutePath
        file.delete()

        Shell.executeScriptSilent("rm -f $path")
        try {
            Shell.executeScriptSilent("echo test > $path")
            assertEquals("test\n", Shell.executeScriptCaptureStdout("cat $path"))
            file.appendBytes("extra".toByteArray())
        } finally {
            Shell.executeScriptSilent("rm -f $path")
        }
    }

    @Test
    public fun dirUsableByAppAndShell_writeShellReadApp() {
        val dir = Outputs.dirUsableByAppAndShell

        // simple way to get a unique path, not shared across runs
        val file = File.createTempFile("shellwrite", null, dir)
        val path = file.absolutePath
        file.delete()

        Shell.executeScriptSilent("rm -f $path")
        try {
            Shell.executeScriptSilent("echo test > $path")
            assertEquals("test\n", File(path).readText())
            file.appendBytes("extra".toByteArray())
        } finally {
            Shell.executeScriptSilent("rm -f $path")
        }
    }

    /**
     * NOTE: this test checks that the instrumentation argument additionalTestOutputDir isn't set to
     * an invalid / unusable location.
     *
     * Running through Studio/Gradle, this isn't defined by the library, it's defined by AGP.
     *
     * If this test fails, we need to handle the directory differently.
     */
    @Test
    fun additionalTestOutputDir_appWrite() {
        val additionalTestOutputDir = Arguments.additionalTestOutputDir
        assumeTrue(additionalTestOutputDir != null)
        val file = File.createTempFile("testFile", null, File(additionalTestOutputDir!!))
        try {
            file.writeText("testString")
        } finally {
            file.delete()
        }
    }
}
