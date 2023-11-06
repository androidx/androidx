/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.testutils.paparazzi

import androidx.test.screenshot.proto.ScreenshotResultProto.ScreenshotResult
import androidx.test.screenshot.proto.ScreenshotResultProto.ScreenshotResult.Status
import app.cash.paparazzi.Snapshot
import java.awt.image.BufferedImage
import java.io.File
import java.util.Date
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName

class GoldenVerifierTest {
    @get:Rule
    val testName = TestName()

    @get:Rule
    val goldenDirectory = TemporaryFolder()

    @get:Rule
    val reportDirectory = TemporaryFolder()

    private val modulePath = "testutils/testutils-paparazzi"

    @Test
    fun `snapshot handler success`() {
        createGolden("circle")
        goldenVerifier().newFrameHandler(snapshot(), 1, 0).handle(loadTestImage("circle"))
    }

    @Test
    fun `snapshot handler failure`() {
        createGolden("star")
        assertFails {
            goldenVerifier().newFrameHandler(snapshot(), 1, 0).handle(loadTestImage("circle"))
        }
    }

    @Test
    fun `removes special characters in file names`() {
        createGolden("circle")
        // Test that createGolden/goldenFile match naming in verifier
        goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle"))

        assertEquals(
            goldenFile().name,
            "androidx_testutils_paparazzi_GoldenVerifierTest_" +
                "removes_special_characters_in_file_names_paparazzi.png"
        )
    }

    @Test
    fun `writes report on success`() {
        createGolden("circle")
        goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle"))

        val proto = reportProto()
        assertEquals(Status.PASSED, proto.result)
        assertEquals(reportFile("expected.png").name, proto.expectedImageFileName)
        assertEquals("", proto.diffImageFileName)
        assertEquals("[PixelPerfect]: 0 of 65536 pixels different", proto.comparisonStatistics)
        assertContains(reportFile("goldResult.textproto").readText(), "PASSED")
    }

    @Test
    fun `writes actual image on success`() {
        createGolden("circle")
        goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle"))
        assertEquals(loadTestImage("circle"), reportFile("actual.png").readImage())
    }

    @Test
    fun `writes expected image on success`() {
        createGolden("circle")
        goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle"))
        assertEquals(loadTestImage("circle"), reportFile("expected.png").readImage())
    }

    @Test
    fun `analysis of success`() {
        val analysis = goldenVerifier().analyze(loadTestImage("circle"), loadTestImage("circle"))
        assertIs<GoldenVerifier.AnalysisResult.Passed>(analysis)
        assertEquals(loadTestImage("circle"), analysis.actual)
        assertEquals(loadTestImage("circle"), analysis.expected)
    }

    @Test
    fun `asserts on failure`() {
        createGolden("star")
        val message = "Actual image differs from golden image: 17837 of 65536 pixels different. " +
            "To update golden images for this test module, run ./gradlew :updateGolden " +
            "-Pandroidx.ignoreTestFailures=true."

        assertFailsWithMessage(message) {
            goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle"))
        }
    }

    @Test
    fun `writes result proto on failure`() {
        createGolden("star")
        assertFails { goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle")) }

        val proto = reportProto()
        assertEquals(Status.FAILED, proto.result)
        assertEquals(reportFile("expected.png").name, proto.expectedImageFileName)
        assertEquals(reportFile("diff.png").name, proto.diffImageFileName)
        assertEquals("[PixelPerfect]: 17837 of 65536 pixels different", proto.comparisonStatistics)
        assertContains(reportFile("goldResult.textproto").readText(), "FAILED")
    }

    @Test
    fun `writes actual image on failure`() {
        createGolden("star")
        assertFails { goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle")) }
        assertEquals(loadTestImage("circle"), reportFile("actual.png").readImage())
    }

    @Test
    fun `writes expected image on failure`() {
        createGolden("star")
        assertFails { goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle")) }
        assertEquals(loadTestImage("star"), reportFile("expected.png").readImage())
    }

    @Test
    fun `writes diff image on failure`() {
        createGolden("star")
        assertFails { goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle")) }
        assertEquals(loadTestImage("PixelPerfect_diff"), reportFile("diff.png").readImage())
    }

    @Test
    fun `analysis of failure`() {
        val analysis = goldenVerifier().analyze(loadTestImage("circle"), loadTestImage("star"))
        assertIs<GoldenVerifier.AnalysisResult.Failed>(analysis)
        assertEquals(loadTestImage("star"), analysis.actual)
        assertEquals(loadTestImage("circle"), analysis.expected)
        assertEquals(loadTestImage("PixelPerfect_diff"), analysis.imageDiff.highlights)
    }

    @Test
    fun `asserts on size mismatch`() {
        createGolden("horizontal_rectangle")
        val message = "Actual image has different dimensions than golden image. Actual: 72x128. " +
            "Golden: 128x72. To update golden images for this test module, run ./gradlew " +
            ":updateGolden -Pandroidx.ignoreTestFailures=true."

        assertFailsWithMessage(message) {
            goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("vertical_rectangle"))
        }
    }

    @Test
    fun `writes result proto for size mismatch`() {
        createGolden("horizontal_rectangle")
        assertFails {
            goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("vertical_rectangle"))
        }

        val proto = reportProto()
        assertEquals(Status.SIZE_MISMATCH, proto.result)
        assertEquals(reportFile("expected.png").name, proto.expectedImageFileName)
        assertEquals("", proto.diffImageFileName)
        assertEquals("", proto.comparisonStatistics)
        assertContains(reportFile("goldResult.textproto").readText(), "SIZE_MISMATCH")
    }

    @Test
    fun `writes actual image for size mismatch`() {
        createGolden("horizontal_rectangle")
        assertFails {
            goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("vertical_rectangle"))
        }

        assertEquals(loadTestImage("vertical_rectangle"), reportFile("actual.png").readImage())
    }

    @Test
    fun `writes expected image for size mismatch`() {
        createGolden("horizontal_rectangle")
        assertFails {
            goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("vertical_rectangle"))
        }

        assertEquals(loadTestImage("horizontal_rectangle"), reportFile("expected.png").readImage())
    }

    @Test
    fun `analysis of size mismatch`() {
        val analysis = goldenVerifier()
            .analyze(loadTestImage("horizontal_rectangle"), loadTestImage("vertical_rectangle"))
        assertIs<GoldenVerifier.AnalysisResult.SizeMismatch>(analysis)
        assertEquals(loadTestImage("vertical_rectangle"), analysis.actual)
        assertEquals(loadTestImage("horizontal_rectangle"), analysis.expected)
    }

    @Test
    fun `asserts on missing golden`() {
        val message = "Expected golden image for test \"asserts on missing golden\" does not " +
            "exist. Run ./gradlew :updateGolden -Pandroidx.ignoreTestFailures=true to create it " +
            "and update all golden images for this test module."

        assertFailsWithMessage(message) {
            goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle"))
        }
    }

    @Test
    fun `writes result proto for missing golden`() {
        assertFails { goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle")) }

        val proto = reportProto()
        assertEquals(Status.MISSING_GOLDEN, proto.result)
        assertEquals("", proto.expectedImageFileName)
        assertEquals("", proto.diffImageFileName)
        assertEquals("", proto.comparisonStatistics)
        assertContains(reportFile("goldResult.textproto").readText(), "MISSING_GOLDEN")
    }

    @Test
    fun `writes actual image for missing golden`() {
        assertFails { goldenVerifier().assertMatchesGolden(snapshot(), loadTestImage("circle")) }
        assertEquals(loadTestImage("circle"), reportFile("actual.png").readImage())
    }

    @Test
    fun `analysis of missing golden`() {
        val analysis = goldenVerifier().analyze(null, loadTestImage("circle"))
        assertIs<GoldenVerifier.AnalysisResult.MissingGolden>(analysis)
        assertEquals(loadTestImage("circle"), analysis.actual)
    }

    @Test
    fun `ensures single snapshot per method`() {
        val verifier = goldenVerifier()
        createGolden("circle")

        verifier.assertMatchesGolden(snapshot(), loadTestImage("circle"))
        assertFails { verifier.assertMatchesGolden(snapshot(), loadTestImage("circle")) }
    }

    private fun goldenVerifier() = GoldenVerifier(
        modulePath = modulePath,
        goldenRootDirectory = goldenDirectory.root,
        reportDirectory = reportDirectory.root
    )

    /** Assert [block] throws an [AssertionError] with supplied [message]. */
    private inline fun assertFailsWithMessage(message: String, block: () -> Unit) {
        assertEquals(message, assertFailsWith<AssertionError> { block() }.message)
    }

    /** Compare two images using [ImageDiffer.PixelPerfect]. */
    private fun assertEquals(expected: BufferedImage, actual: BufferedImage) {
        assertIs<ImageDiffer.DiffResult.Similar>(
            ImageDiffer.PixelPerfect.diff(expected, actual),
            message = "Expected images to be identical, but they were not."
        )
    }

    private fun snapshot() = Snapshot(
        name = null,
        testName = app.cash.paparazzi.TestName(
            packageName = this::class.java.packageName,
            className = this::class.simpleName!!,
            methodName = testName.methodName
        ),
        timestamp = Date()
    )

    /** Create a golden image for this test from the supplied test image [name]. */
    private fun createGolden(name: String) = javaClass.getResourceAsStream("$name.png")!!
            .copyTo(goldenFile().apply { parentFile!!.mkdirs() }.outputStream())

    /** Relative path to golden image for this test. */
    private fun goldenPath() = "$modulePath/${testName()}_paparazzi.png"

    /** Resolve the file path for a golden image for this test under [goldenDirectory]. */
    private fun goldenFile() = goldenDirectory.root.resolve(goldenPath()).canonicalFile

    /** Read the binary result proto under for this test and check common fields. */
    private fun reportProto() =
        ScreenshotResult.parseFrom(reportFile("goldResult.pb").inputStream()).also { proto ->
            assertEquals(reportFile("actual.png").name, proto.currentScreenshotFileName)
            assertEquals(GoldenVerifier.ANDROIDX_GOLDEN_REPO_NAME, proto.repoRootPath)
            assertEquals(goldenPath(), proto.locationOfGoldenInRepo)
        }

    /** Resolve the file path for a report file with provided [suffix] under [reportDirectory]. */
    private fun reportFile(suffix: String) =
        reportDirectory.root.resolve("${testName()}_$suffix").canonicalFile

    /** Convenience function to read an image from a file. */
    private fun File.readImage() = ImageIO.read(this)

    /** Fully qualified test ID with special characters replaced for this test. */
    private fun testName() = "${this::class.qualifiedName!!}_${testName.methodName}"
        .replace(Regex("\\W+"), "_")

    /** Load a test image from resources. */
    private fun loadTestImage(name: String) =
        ImageIO.read(javaClass.getResourceAsStream("$name.png")!!)
}
