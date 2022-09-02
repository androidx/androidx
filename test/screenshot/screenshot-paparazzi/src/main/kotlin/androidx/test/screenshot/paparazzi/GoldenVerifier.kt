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

package androidx.test.screenshot.paparazzi

import androidx.test.screenshot.proto.ScreenshotResultProto.ScreenshotResult
import androidx.test.screenshot.proto.ScreenshotResultProto.ScreenshotResult.Status
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.TestName
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * This [SnapshotHandler] implements image diffing for AndroidX CI. It both throws exceptions for
 * failing tests and writes reports out for the image diffing tool in CI to consume.
 *
 * All golden images are identified by the qualified name of the test function. This limits tests
 * to one snapshot per test, but avoids introducing a secondary identifier. It's also currently
 * required for CI.
 *
 * It always fails the test if the expected golden image does not exist yet, but provides an
 * failure message including the path to the actual screenshot and the expected golden path.
 *
 * @property modulePath Unique path for the module, derived from gradle path. The verifier will
 * search for golden images in this directory relative to [goldenRootDirectory].
 * Example: `test/screenshot/paparazzi`.
 *
 * @property goldenRootDirectory Location on disk of the golden images repo. Golden images for this
 * module are found in the [modulePath] directory under this directory.
 *
 * @property reportDirectory Directory to write reports for CI to read, including protos,
 * actual and expected images, and image diffs.
 *
 * @property imageDiffer An [ImageDiffer] for comparing images.
 *
 * @property goldenRepoName Name of the repo containing golden images. Used for CI
 */
internal class GoldenVerifier(
    val modulePath: String,
    val goldenRootDirectory: File,
    val reportDirectory: File,
    val imageDiffer: ImageDiffer = ImageDiffer.PixelPerfect,
    val goldenRepoName: String = ANDROIDX_GOLDEN_REPO_NAME
) : SnapshotHandler {
    /** Directory containing golden images for this module. */
    val goldenDirectory = goldenRootDirectory.resolve(modulePath)

    /**
     * Asserts that the [actual] matches the expected golden for the qualified test function name,
     * [testId]. As a side effect, this writes the report proto, actual, expected, and difference
     * images to [reportDirectory] as appropriate.
     */
    fun assertSimilarToGolden(testId: String, actual: BufferedImage) {
        val expected = testId.toGoldenFile().takeIf { it.canRead() }?.let { ImageIO.read(it) }
        val analysis = analyze(expected, actual)

        fun updateMessage() = "To update the golden image, copy " +
            "${testId.toActualFile().canonicalPath} to ${testId.toGoldenFile().canonicalPath} " +
            "and commit the updated golden image."

        writeReport(testId, analysis)

        when (analysis) {
            is AnalysisResult.Passed -> { /** Test passed, don't need to throw anything */ }
            is AnalysisResult.Failed -> throw AssertionError(
                "Actual image differs from golden image: ${analysis.imageDiff.description}. " +
                    updateMessage()
            )
            is AnalysisResult.SizeMismatch -> throw AssertionError(
                "Actual image has different dimensions than golden image. " +
                    "Actual: ${analysis.actual.width}x${analysis.actual.height}. " +
                    "Golden: ${analysis.expected.width}x${analysis.expected.height}. " +
                    updateMessage()
            )
            is AnalysisResult.MissingGolden -> throw AssertionError(
                "Expected golden image for $testId does not exist. To create it, copy " +
                    "${testId.toActualFile().canonicalPath} to " +
                    "${testId.toGoldenFile().canonicalPath} and commit the new golden image."
            )
        }
    }

    /** Compare [expected] golden image to [actual] image and return an [AnalysisResult] */
    fun analyze(expected: BufferedImage?, actual: BufferedImage): AnalysisResult {
        if (expected == null) {
            return AnalysisResult.MissingGolden(actual)
        }

        if (actual.width != expected.width || actual.height != expected.height) {
            return AnalysisResult.SizeMismatch(actual, expected)
        }

        return when (val diff = imageDiffer.diff(actual, expected)) {
            is ImageDiffer.DiffResult.Similar -> AnalysisResult.Passed(actual, expected, diff)
            is ImageDiffer.DiffResult.Different -> AnalysisResult.Failed(actual, expected, diff)
        }
    }

    /**
     * Write the [analysis] for test [testId] to [reportDirectory] as both binary and text proto,
     * including actual, expected, and difference image files as appropriate.
     */
    fun writeReport(testId: String, analysis: AnalysisResult) {
        val actualFile = testId.toActualFile().also { ImageIO.write(analysis.actual, "PNG", it) }
        val goldenFile = testId.toGoldenFile()

        val resultProto = ScreenshotResult.newBuilder().apply {
            currentScreenshotFileName = actualFile.name
            repoRootPath = goldenRepoName
            locationOfGoldenInRepo = goldenFile.relativeTo(goldenRootDirectory).path
        }

        fun diffFile(diff: BufferedImage) =
            testId.toDiffFile().also { ImageIO.write(diff, "PNG", it) }
        fun expectedFile() = goldenFile.copyTo(testId.toExpectedFile())

        when (analysis) {
            is AnalysisResult.Passed -> resultProto.apply {
                result = Status.PASSED
                expectedImageFileName = expectedFile().name
                analysis.imageDiff.highlights?.let { diffImageFileName = diffFile(it).name }
                comparisonStatistics = analysis.imageDiff.taggedDescription()
            }
            is AnalysisResult.Failed -> resultProto.apply {
                result = Status.FAILED
                expectedImageFileName = expectedFile().name
                diffImageFileName = diffFile(analysis.imageDiff.highlights).name
                comparisonStatistics = analysis.imageDiff.taggedDescription()
            }
            is AnalysisResult.SizeMismatch -> resultProto.apply {
                result = Status.SIZE_MISMATCH
                expectedImageFileName = expectedFile().name
            }
            is AnalysisResult.MissingGolden -> resultProto.apply {
                result = Status.MISSING_GOLDEN
            }
        }

        val result = resultProto.build()
        testId.toResultProtoFile().outputStream().use { result.writeTo(it) }

        // TODO(b/244200590): Remove text proto output, or replace with JSON
        testId.toResultTextProtoFile().writeText(result.toString())
    }

    /**
     * Analysis result ADT returned from [analyze], including actual and expected images and
     * an [ImageDiffer.DiffResult].
     */
    sealed interface AnalysisResult {
        val actual: BufferedImage

        data class Passed(
            override val actual: BufferedImage,
            val expected: BufferedImage,
            val imageDiff: ImageDiffer.DiffResult.Similar
        ) : AnalysisResult

        data class Failed(
            override val actual: BufferedImage,
            val expected: BufferedImage,
            val imageDiff: ImageDiffer.DiffResult.Different
        ) : AnalysisResult

        data class SizeMismatch(
            override val actual: BufferedImage,
            val expected: BufferedImage
        ) : AnalysisResult

        data class MissingGolden(
            override val actual: BufferedImage
        ) : AnalysisResult
    }

    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int
    ): SnapshotHandler.FrameHandler {
        require(frameCount == 1) { "Videos are not yet supported" }

        return object : SnapshotHandler.FrameHandler {
            override fun handle(image: BufferedImage) {
                assertSimilarToGolden(snapshot.testName.toTestId(), image)
            }

            override fun close() {}
        }
    }

    override fun close() {}

    /** Adds [ImageDiffer.name] as a prefix to [ImageDiffer.DiffResult.description]. */
    private fun ImageDiffer.DiffResult.taggedDescription() =
        "[${imageDiffer.name}]: $description"

    // Filename templates based for a given test ID
    private fun String.toGoldenFile() = goldenDirectory.resolve("${this}_paparazzi.png")
    private fun String.toExpectedFile() = reportDirectory.resolve("${this}_expected.png")
    private fun String.toActualFile() = reportDirectory.resolve("${this}_actual.png")
    private fun String.toDiffFile() = reportDirectory.resolve("${this}_diff.png")
    private fun String.toResultProtoFile() = reportDirectory.resolve("${this}_goldResult.pb")
    private fun String.toResultTextProtoFile() =
        reportDirectory.resolve("${this}_goldResult.textproto")

    companion object {
        /** Name of the AndroidX golden repo. */
        const val ANDROIDX_GOLDEN_REPO_NAME = "platform/frameworks/support-golden"

        /** Render test function name as a fully qualified string. */
        fun TestName.toTestId(): String {
            return if (packageName.isEmpty()) {
                "${className}_$methodName"
            } else {
                "$packageName.${className}_$methodName"
            }
        }
    }
}