/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.testutils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.matchers.PixelPerfectMatcher
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class XrScreenshotTestRuleTest {

    private lateinit var originalArguments: Bundle
    private val createdOutputDirs = mutableListOf<File>()

    @Before
    fun setUp() {
        originalArguments = Bundle(InstrumentationRegistry.getArguments())
        ShadowBuild.setModel("Moohan")
        setXrSystemFeature(true)
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.registerInstance(
            InstrumentationRegistry.getInstrumentation(),
            originalArguments,
        )
        createdOutputDirs.forEach { it.deleteRecursively() }
    }

    private fun setXrSystemFeature(enabled: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.packageManager).setSystemFeature(FEATURE_XR_API_SPATIAL, enabled)
    }

    private fun createSolidBitmap(color: Int, width: Int = 4, height: Int = 4): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun runWithRule(rule: XrScreenshotTestRule, block: () -> Unit) {
        createdOutputDirs.add(rule.deviceOutputDirectory)
        val description =
            Description.createTestDescription(XrScreenshotTestRuleTest::class.java, "sampleTest")
        rule
            .apply(
                object : Statement() {
                    override fun evaluate() {
                        block()
                    }
                },
                description,
            )
            .evaluate()
    }

    @Test
    fun constructor_rejectsParentDirectoryTraversal() {
        assertFailsWith<IllegalArgumentException> { XrScreenshotTestRule("../xr/testutils") }
        assertFailsWith<IllegalArgumentException> { XrScreenshotTestRule("xr/../testutils") }
        assertFailsWith<IllegalArgumentException> { XrScreenshotTestRule("..") }
    }

    @Test
    fun constructor_trimsLeadingAndTrailingSlashes() {
        val rule = XrScreenshotTestRule("/xr/testutils/", updateGoldens = true)
        val bitmap = createSolidBitmap(Color.BLUE)
        try {
            runWithRule(rule) {
                rule.assertBitmapAgainstGolden(bitmap, "missing_golden", PixelPerfectMatcher())
            }
            val expectedStagedFile =
                File(rule.stagedGoldensDirectory, "xr/testutils/missing_golden_moohan.png")
            assertTrue(expectedStagedFile.exists())
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun assumeSupportedDevice_skipsWhenXrFeatureMissing() {
        setXrSystemFeature(false)
        val rule = XrScreenshotTestRule("xr/testutils")
        var bodyExecuted = false

        assertFailsWith<AssumptionViolatedException> {
            runWithRule(rule) { bodyExecuted = true }
        }
        assertFalse(bodyExecuted)
    }

    @Test
    fun assumeSupportedDevice_runsWhenXrFeaturePresent() {
        setXrSystemFeature(true)
        val rule = XrScreenshotTestRule("xr/testutils")
        var bodyExecuted = false

        runWithRule(rule) { bodyExecuted = true }
        assertTrue(bodyExecuted)
    }

    @Test
    fun assertBitmapAgainstGolden_missingGolden_stagesAndThrowsWhenUpdateDisabled() {
        val rule = XrScreenshotTestRule("xr/testutils", updateGoldens = false)
        val bitmap = createSolidBitmap(Color.GREEN)
        try {
            val error =
                assertFailsWith<AssertionError> {
                    runWithRule(rule) {
                        rule.assertBitmapAgainstGolden(
                            bitmap,
                            "missing_golden",
                            PixelPerfectMatcher(),
                        )
                    }
                }
            val stagedFile =
                File(rule.stagedGoldensDirectory, "xr/testutils/missing_golden_moohan.png")
            assertTrue(stagedFile.exists())
            val decoded = BitmapFactory.decodeFile(stagedFile.absolutePath)
            assertEquals(4, decoded.width)
            assertEquals(4, decoded.height)
            decoded.recycle()

            val message = error.message.orEmpty()
            assertTrue(message.contains(stagedFile.absolutePath))
            assertTrue(
                message.contains(
                    "adb pull ${stagedFile.absolutePath} ../../golden/xr/testutils/missing_golden_moohan.png"
                )
            )
            assertTrue(
                message.contains(
                    "-Pandroid.testInstrumentationRunnerArguments.$UPDATE_GOLDENS_ARG=true"
                )
            )
            assertTrue(
                message.contains(
                    "adb pull ${rule.stagedGoldensDirectory.absolutePath}/. ../../golden/"
                )
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun assertBitmapAgainstGolden_missingGolden_stagesAndSucceedsWhenUpdateEnabled() {
        val rule = XrScreenshotTestRule("xr/testutils", updateGoldens = true)
        val bitmap = createSolidBitmap(Color.GREEN)
        try {
            runWithRule(rule) {
                rule.assertBitmapAgainstGolden(bitmap, "missing_golden", PixelPerfectMatcher())
            }
            val stagedFile =
                File(rule.stagedGoldensDirectory, "xr/testutils/missing_golden_moohan.png")
            assertTrue(stagedFile.exists())
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun assertBitmapAgainstGolden_mismatchedGolden_stagesAndThrowsWhenUpdateDisabled() {
        val rule = XrScreenshotTestRule("xr/testutils", updateGoldens = false)
        // test_golden_moohan.png in test assets is solid Color.RED; Color.BLUE mismatches.
        val bitmap = createSolidBitmap(Color.BLUE)
        try {
            assertFailsWith<AssertionError> {
                runWithRule(rule) {
                    rule.assertBitmapAgainstGolden(bitmap, "test_golden", PixelPerfectMatcher())
                }
            }
            val stagedFile =
                File(rule.stagedGoldensDirectory, "xr/testutils/test_golden_moohan.png")
            assertTrue(stagedFile.exists())
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun assertBitmapAgainstGolden_mismatchedGolden_stagesAndSucceedsWhenUpdateEnabled() {
        val rule = XrScreenshotTestRule("xr/testutils", updateGoldens = true)
        val bitmap = createSolidBitmap(Color.BLUE)
        try {
            runWithRule(rule) {
                rule.assertBitmapAgainstGolden(bitmap, "test_golden", PixelPerfectMatcher())
            }
            val stagedFile =
                File(rule.stagedGoldensDirectory, "xr/testutils/test_golden_moohan.png")
            assertTrue(stagedFile.exists())
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun assertBitmapAgainstGolden_matchingGolden_deletesPreviouslyStagedFile() {
        val rule = XrScreenshotTestRule("xr/testutils", updateGoldens = false)
        val stagedFile = File(rule.stagedGoldensDirectory, "xr/testutils/test_golden_moohan.png")
        stagedFile.parentFile?.mkdirs()
        stagedFile.writeBytes(byteArrayOf(1, 2, 3))
        assertTrue(stagedFile.exists())

        val matchingBitmap = createSolidBitmap(Color.RED)
        try {
            runWithRule(rule) {
                rule.assertBitmapAgainstGolden(
                    matchingBitmap,
                    "test_golden",
                    PixelPerfectMatcher(),
                )
            }
            assertFalse(stagedFile.exists())
        } finally {
            matchingBitmap.recycle()
        }
    }

    @Test
    fun updateGoldensArgument_defaultsFromInstrumentationArguments() {
        val args = Bundle(originalArguments).apply { putString(UPDATE_GOLDENS_ARG, "true") }
        InstrumentationRegistry.registerInstance(InstrumentationRegistry.getInstrumentation(), args)

        val rule = XrScreenshotTestRule("xr/testutils")
        val bitmap = createSolidBitmap(Color.GREEN)
        try {
            runWithRule(rule) {
                rule.assertBitmapAgainstGolden(bitmap, "arg_golden", PixelPerfectMatcher())
            }
            val stagedFile = File(rule.stagedGoldensDirectory, "xr/testutils/arg_golden_moohan.png")
            assertTrue(stagedFile.exists())
        } finally {
            bitmap.recycle()
        }
    }
}
