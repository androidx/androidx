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
import androidx.test.screenshot.matchers.MSSIMMatcher
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
                    "adb pull ${stagedFile.absolutePath} " +
                        "../../golden/xr/testutils/missing_golden_moohan.png"
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

    @Test
    fun assertBitmapAgainstGolden_usesDeviceModelSuffixInGoldenFilename() {
        val bitmap = createSolidBitmap(Color.RED)
        try {
            ShadowBuild.setModel("Android XR Puck")
            val puckRule = XrScreenshotTestRule("xr/testutils", updateGoldens = true)
            runWithRule(puckRule) {
                puckRule.assertBitmapAgainstGolden(
                    bitmap,
                    "device_golden",
                    PixelPerfectMatcher(),
                )
            }
            assertTrue(
                File(
                        puckRule.stagedGoldensDirectory,
                        "xr/testutils/device_golden_android_xr_puck.png",
                    )
                    .exists()
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun spatialNode_crop_cropsToContentBounds() {
        val width = 600
        val height = 400
        val pixels = IntArray(width * height) { Color.BLACK }
        // Draw a 200x100 blue panel centered at (300, 200)
        for (y in 150 until 250) {
            for (x in 200 until 400) {
                pixels[y * width + x] = Color.BLUE
            }
        }
        val source = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        val node = SpatialNode(name = "space-transform-spatial-1", id = 10)
        try {
            val result = node.crop(source)
            try {
                assertEquals(200, result.width)
                assertEquals(100, result.height)
                assertEquals(Color.BLUE, result.getPixel(result.width / 2, result.height / 2))
            } finally {
                result.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun spatialNode_crop_withPadding_preservesMargin() {
        val width = 300
        val height = 200
        val pixels = IntArray(width * height) { Color.BLACK }
        for (y in 60 until 140) {
            for (x in 100 until 200) {
                pixels[y * width + x] = Color.GREEN
            }
        }
        val source = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        val node = SpatialNode(name = "space-transform-spatial-1", id = 10)
        try {
            val result = node.crop(source, padding = 10)
            try {
                assertEquals(120, result.width)
                assertEquals(100, result.height)
                assertEquals(Color.BLACK, result.getPixel(2, 2))
                assertEquals(Color.GREEN, result.getPixel(result.width / 2, result.height / 2))
            } finally {
                result.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun spatialNode_crop_multiPanelScene_cropsToSelectedChildNode() {
        val width = 400
        val height = 200
        val pixels = IntArray(width * height) { Color.BLACK }
        for (y in 50 until 150) {
            for (x in 50 until 150) {
                pixels[y * width + x] = Color.BLUE
            }
            for (x in 250 until 350) {
                pixels[y * width + x] = Color.RED
            }
        }
        val source = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        val taskRoot = SpatialNode(name = "space-root-task-1", id = 1)
        val leftNode =
            SpatialNode(
                name = "LeftPanel",
                id = 2,
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(-0.2f, 0f, -1.5f),
                        "ImpNodeWorldRotation" to floatArrayOf(1f, 0f, 0f, 0f),
                        "ImpNodeWorldScale" to floatArrayOf(1f, 1f, 1f),
                        "root2DSurfaceBoundsInMeters" to floatArrayOf(0.2f, 0.2f),
                    ),
            )
        val rightNode =
            SpatialNode(
                name = "RightPanel",
                id = 3,
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(0.2f, 0f, -1.5f),
                        "ImpNodeWorldRotation" to floatArrayOf(1f, 0f, 0f, 0f),
                        "ImpNodeWorldScale" to floatArrayOf(1f, 1f, 1f),
                        "root2DSurfaceBoundsInMeters" to floatArrayOf(0.2f, 0.2f),
                    ),
            )
        taskRoot.addChild(leftNode)
        taskRoot.addChild(rightNode)
        SpatialScene(listOf(taskRoot))

        try {
            val leftCropped = leftNode.crop(source)
            val rightCropped = rightNode.crop(source)
            try {
                assertEquals(100, leftCropped.width)
                assertEquals(100, leftCropped.height)
                assertEquals(Color.BLUE, leftCropped.getPixel(50, 50))

                assertEquals(100, rightCropped.width)
                assertEquals(100, rightCropped.height)
                assertEquals(Color.RED, rightCropped.getPixel(50, 50))
            } finally {
                leftCropped.recycle()
                rightCropped.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun cropForeground_cropsToNonBlackForeground() {
        val width = 400
        val height = 300
        val pixels = IntArray(width * height) { Color.BLACK }
        for (y in 100 until 180) {
            for (x in 150 until 250) {
                pixels[y * width + x] = Color.GREEN
            }
        }
        val source = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        try {
            val result = cropForeground(source)
            try {
                assertEquals(100, result.width)
                assertEquals(80, result.height)
                assertEquals(Color.GREEN, result.getPixel(50, 40))
            } finally {
                result.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun cropForeground_withPadding_preservesMargin() {
        val width = 400
        val height = 300
        val pixels = IntArray(width * height) { Color.BLACK }
        for (y in 100 until 180) {
            for (x in 150 until 250) {
                pixels[y * width + x] = Color.YELLOW
            }
        }
        val source = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        try {
            val result = cropForeground(source, padding = 10)
            try {
                assertEquals(120, result.width)
                assertEquals(100, result.height)
                assertEquals(Color.BLACK, result.getPixel(2, 2))
                assertEquals(Color.YELLOW, result.getPixel(60, 50))
            } finally {
                result.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun cropForeground_customBackgroundColor_cropsNonBackgroundContent() {
        val width = 300
        val height = 200
        val pixels = IntArray(width * height) { Color.BLUE }
        for (y in 50 until 120) {
            for (x in 80 until 180) {
                pixels[y * width + x] = Color.RED
            }
        }
        val source = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        try {
            val result = cropForeground(source, backgroundColor = Color.BLUE)
            try {
                assertEquals(100, result.width)
                assertEquals(70, result.height)
                assertEquals(Color.RED, result.getPixel(50, 35))
            } finally {
                result.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun cropForeground_allBackground_returnsSourceBitmap() {
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) { Color.BLACK }
        val source = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        try {
            val result = cropForeground(source)
            try {
                assertEquals(100, result.width)
                assertEquals(100, result.height)
            } finally {
                if (result !== source) result.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun spatialScene_crop_cropsForegroundOfEntireScene() {
        val width = 500
        val height = 400
        val pixels = IntArray(width * height) { Color.BLACK }
        for (y in 100 until 250) {
            for (x in 150 until 350) {
                pixels[y * width + x] = Color.MAGENTA
            }
        }
        val source = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        // GLTF node without CPM surface bounds (only transform/scale)
        val gltfNode =
            SpatialNode(
                name = "GltfModelEntity",
                id = 10,
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(0f, 0f, -1.0f),
                        "ImpNodeWorldRotation" to floatArrayOf(1f, 0f, 0f, 0f),
                        "ImpNodeWorldScale" to floatArrayOf(1f, 1f, 1f),
                    ),
            )
        val taskRoot = SpatialNode(name = "space-root-task-1", id = 1)
        taskRoot.addChild(gltfNode)
        val scene = SpatialScene(listOf(taskRoot))

        try {
            val sceneCropped = scene.crop(source)
            val nodeCropped = gltfNode.crop(source)
            try {
                assertEquals(200, sceneCropped.width)
                assertEquals(150, sceneCropped.height)
                assertEquals(Color.MAGENTA, sceneCropped.getPixel(100, 75))

                // Node without CPM surface bounds falls back gracefully to full foreground crop
                assertEquals(200, nodeCropped.width)
                assertEquals(150, nodeCropped.height)
                assertEquals(Color.MAGENTA, nodeCropped.getPixel(100, 75))
            } finally {
                sceneCropped.recycle()
                nodeCropped.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun assertBitmapAgainstGolden_minorCropSizeDifference_scalesAndPassesWithMSSIMMatcher() {
        val rule = XrScreenshotTestRule("xr/testutils", updateGoldens = false)
        // test_golden_moohan.png is 4x4 Color.RED; a 6x5 Color.RED bitmap is within the 6px
        // auto-crop size tolerance and should pass when using MSSIMMatcher.
        val slightlyLargerRedBitmap = createSolidBitmap(Color.RED, width = 6, height = 5)
        try {
            runWithRule(rule) {
                rule.assertBitmapAgainstGolden(
                    slightlyLargerRedBitmap,
                    "test_golden",
                    MSSIMMatcher(threshold = 0.95),
                )
            }
        } finally {
            slightlyLargerRedBitmap.recycle()
        }
    }

    @Test
    fun apply_suppressesSystemUiToastWindowAndRestoresPreviousAllowMode() {
        val executedCommands = mutableListOf<String>()
        val rule =
            XrScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                shellCommandExecutor = { cmd ->
                    executedCommands.add(cmd)
                    if (cmd == "cmd appops get com.android.systemui TOAST_WINDOW") {
                        "TOAST_WINDOW: allow; time=+1m0s0ms ago"
                    } else {
                        ""
                    }
                },
            )

        assertFailsWith<IllegalStateException> {
            runWithRule(rule) {
                assertEquals(
                    listOf(
                        "cmd appops get com.android.systemui TOAST_WINDOW",
                        "cmd appops set com.android.systemui TOAST_WINDOW ignore",
                    ),
                    executedCommands,
                )
                throw IllegalStateException("simulated test failure")
            }
        }

        assertEquals(
            listOf(
                "cmd appops get com.android.systemui TOAST_WINDOW",
                "cmd appops set com.android.systemui TOAST_WINDOW ignore",
                "cmd appops set com.android.systemui TOAST_WINDOW allow",
            ),
            executedCommands,
        )
    }

    @Test
    fun apply_preservesPreExistingIgnoreToastWindowModeOnTeardown() {
        val executedCommands = mutableListOf<String>()
        val rule =
            XrScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                shellCommandExecutor = { cmd ->
                    executedCommands.add(cmd)
                    if (cmd == "cmd appops get com.android.systemui TOAST_WINDOW") {
                        "TOAST_WINDOW: ignore"
                    } else {
                        ""
                    }
                },
            )

        runWithRule(rule) {}

        assertEquals(
            listOf(
                "cmd appops get com.android.systemui TOAST_WINDOW",
                "cmd appops set com.android.systemui TOAST_WINDOW ignore",
            ),
            executedCommands,
        )
    }

    @Test
    fun apply_doesNotModifyToastWindowAppOpWhenDeviceUnsupported() {
        setXrSystemFeature(false)
        val executedCommands = mutableListOf<String>()
        val rule =
            XrScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                shellCommandExecutor = { cmd ->
                    executedCommands.add(cmd)
                    "No operations."
                },
            )

        assertFailsWith<AssumptionViolatedException> { runWithRule(rule) {} }
        assertTrue(executedCommands.isEmpty())
    }
}
