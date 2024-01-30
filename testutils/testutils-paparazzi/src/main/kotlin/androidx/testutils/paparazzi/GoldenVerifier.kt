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
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.TestName
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * This [SnapshotHandler] implements AndroidX-specific logic for screenshot testing. Specifically,
 * it writes a report to [reportDirectory] at every comparison just before throwing an exception,
 * even on successful comparison. The report contains text and binary protos containing the status
 * of the comparison and relative paths to actual, expected (copy of golden), and difference
 * (magenta highlights) images as applicable. CI consumes these reports to allow updating golden
 * images from the web UI. Golden images can also be updated by the `:updateGolden` Gradle task,
 * which copies actual images to their expected location in the golden repo.
 *
 * It also knows that the AndroidX golden repo, located at [goldenRootDirectory], is partitioned
 * by Gradle project path or [modulePath] and loads images from it.
 *
 * As noted in documentation on [androidxPaparazzi], the verifier is the component to enforce a
 * one-to-one relationship between test functions and golden images. This isn't strictly necessary,
 * but CI currently expects reports to be generated as a side effect of a regular test invocation.
 * Enforcing a one-to-one relationship avoids a situation where a failing assertion earlier in the
 * test would stop a later assertion from executing and generating its report as a side effect,
 * requiring multiple rounds of updating golden images to get the test passing.
 *
 * Additionally, CI currently only supports one comparison per report (note the [ScreenshotResult]
 * proto is not repeated). If this limit is lifted in the future, we would need to devise a scheme
 * for naming additional golden images such as an index of the order they were invoked and wrap the
 * test rule with something like an `ErrorCollector` to ensure later assertions are always
 * reachable.
 *
 * @property modulePath Unique path for the module, derived from Gradle project path. The verifier
 * will search for golden images in this directory relative to [goldenRootDirectory].
 *
 * @property goldenRootDirectory Location on disk of the golden images repo. Golden images for this
 * module are found in the [modulePath] directory under this directory.
 *
 * @property reportDirectory Directory to write reports, including protos, actual and expected
 * images, and image diffs.
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

    /** The set of tests seen by this verifier, used to enforce single assertion per test. */
    private val attemptedTests = mutableSetOf<TestName>()

    /**
     * Asserts that the [actual] matches the expected golden for the test described in [snapshot].
     * As a side effect, this writes the report proto, actual, expected, and difference images to
     * [reportDirectory] as appropriate.
     */
    fun assertMatchesGolden(snapshot: Snapshot, actual: BufferedImage) {
        check(snapshot.testName !in attemptedTests) {
            "Snapshot already taken for test \"${snapshot.testName.methodName}\". Taking " +
                "multiple snapshots per test is not currently supported."
        }
        attemptedTests += snapshot.testName

        val expected = snapshot.toGoldenFile().takeIf { it.canRead() }?.let { ImageIO.read(it) }
        val analysis = analyze(expected, actual)

        fun updateMessage() = "To update golden images for this test module, run " +
            "${updateGoldenGradleCommand()}."

        writeReport(snapshot, analysis)

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
                "Expected golden image for test \"${snapshot.testName.methodName}\" does not " +
                    "exist. Run ${updateGoldenGradleCommand()} " +
                    "to create it and update all golden images for this test module."
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
     * Write the [analysis] for test described by [snapshot] to [reportDirectory] as both binary
     * and text proto, including actual, expected, and difference image files as appropriate.
     */
    fun writeReport(snapshot: Snapshot, analysis: AnalysisResult) {
        val actualFile = snapshot.toActualFile().also { ImageIO.write(analysis.actual, "PNG", it) }
        val goldenFile = snapshot.toGoldenFile()

        val resultProto = ScreenshotResult.newBuilder().apply {
            currentScreenshotFileName = actualFile.toRelativeString(reportDirectory)
            repoRootPath = goldenRepoName
            locationOfGoldenInRepo = goldenFile.toRelativeString(goldenRootDirectory)
        }

        fun diffFile(diff: BufferedImage) = snapshot.toDiffFile()
            .also { ImageIO.write(diff, "PNG", it) }
            .toRelativeString(reportDirectory)

        fun expectedFile() = goldenFile.copyTo(snapshot.toExpectedFile())
            .toRelativeString(reportDirectory)

        when (analysis) {
            is AnalysisResult.Passed -> resultProto.apply {
                result = Status.PASSED
                expectedImageFileName = expectedFile()
                analysis.imageDiff.highlights?.let { diffImageFileName = diffFile(it) }
                comparisonStatistics = analysis.imageDiff.taggedDescription()
            }
            is AnalysisResult.Failed -> resultProto.apply {
                result = Status.FAILED
                expectedImageFileName = expectedFile()
                diffImageFileName = diffFile(analysis.imageDiff.highlights)
                comparisonStatistics = analysis.imageDiff.taggedDescription()
            }
            is AnalysisResult.SizeMismatch -> resultProto.apply {
                result = Status.SIZE_MISMATCH
                expectedImageFileName = expectedFile()
            }
            is AnalysisResult.MissingGolden -> resultProto.apply {
                result = Status.MISSING_GOLDEN
            }
        }

        val result = resultProto.build()
        snapshot.toResultProtoFile().outputStream().use { result.writeTo(it) }

        // TODO(b/244200590): Remove text proto output, or replace with JSON
        snapshot.toResultTextProtoFile().writeText(result.toString())
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
                assertMatchesGolden(snapshot, image)
            }

            override fun close() = Unit
        }
    }

    override fun close() = Unit

    /** Adds [ImageDiffer.name] as a prefix to [ImageDiffer.DiffResult.description]. */
    private fun ImageDiffer.DiffResult.taggedDescription() =
        "[${imageDiffer.name}]: $description"

    // Filename templates based for a given snapshot
    private fun Snapshot.toGoldenFile() = goldenDirectory.resolve("${toFileName()}_paparazzi.png")
    private fun Snapshot.toExpectedFile() = reportDirectory.resolve("${toFileName()}_expected.png")
    private fun Snapshot.toActualFile() = reportDirectory.resolve("${toFileName()}_actual.png")
    private fun Snapshot.toDiffFile() = reportDirectory.resolve("${toFileName()}_diff.png")
    private fun Snapshot.toResultProtoFile() =
        reportDirectory.resolve("${toFileName()}_goldResult.pb")
    private fun Snapshot.toResultTextProtoFile() =
        reportDirectory.resolve("${toFileName()}_goldResult.textproto")

    companion object {
        /** Name of the AndroidX golden repo. */
        const val ANDROIDX_GOLDEN_REPO_NAME = "platform/frameworks/support-golden"

        /** Name of the updateGolden Gradle command for this module, via system properties. */
        fun updateGoldenGradleCommand() =
            "./gradlew ${
                (System.getProperty("$PACKAGE_NAME.updateGoldenTask") ?: ":updateGolden")
            } -Pandroidx.ignoreTestFailures=true"

        /** Render test function name as a fully qualified string. */
        fun TestName.toQualifiedName(): String {
            return if (packageName.isEmpty()) {
                "$className.$methodName"
            } else {
                "$packageName.$className.$methodName"
            }
        }

        /** Get a file name with special characters removed from a snapshot. */
        fun Snapshot.toFileName() = testName.toQualifiedName().replace(Regex("\\W+"), "_")
    }
}
