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
import android.graphics.Color
import android.graphics.Rect
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
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
class ProjectedScreenshotTestRuleTest {

    private lateinit var originalArguments: Bundle
    private val createdOutputDirs = mutableListOf<File>()

    @Before
    fun setUp() {
        originalArguments = Bundle(InstrumentationRegistry.getArguments())
        ShadowBuild.setModel("Cuttlefish GMS x86_64")
        setXrSystemFeature(false)
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

    private fun createForegroundBitmap(
        foregroundColor: Int,
        width: Int = 16,
        height: Int = 16,
    ): Bitmap {
        val pixels = IntArray(width * height) { Color.BLACK }
        for (y in height / 4 until (height * 3) / 4) {
            for (x in width / 4 until (width * 3) / 4) {
                pixels[y * width + x] = foregroundColor
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun runWithRule(rule: ProjectedScreenshotTestRule, block: () -> Unit) {
        createdOutputDirs.add(rule.deviceOutputDirectory)
        val description =
            Description.createTestDescription(
                ProjectedScreenshotTestRuleTest::class.java,
                "sampleProjectedTest",
            )
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
    fun assumeSupportedDevice_skipsWhenProjectedDisconnectedAndNotRequired() {
        val rule =
            ProjectedScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                requireProjectedDevice = false,
                isProjectedDeviceAvailable = { false },
            )
        var bodyExecuted = false

        assertFailsWith<AssumptionViolatedException> {
            runWithRule(rule) { bodyExecuted = true }
        }
        assertFalse(bodyExecuted)
    }

    @Test
    fun assumeSupportedDevice_skipsOnSpatialHeadsetWhenProjectedDisconnected() {
        setXrSystemFeature(true)
        val rule =
            ProjectedScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                requireProjectedDevice = false,
                isProjectedDeviceAvailable = { false },
            )
        var bodyExecuted = false

        assertFailsWith<AssumptionViolatedException> {
            runWithRule(rule) { bodyExecuted = true }
        }
        assertFalse(bodyExecuted)
    }

    @Test
    fun assumeSupportedDevice_runsWhenProjectedConnectedWithoutSpatialFeature() {
        setXrSystemFeature(false)
        val rule =
            ProjectedScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                requireProjectedDevice = false,
                isProjectedDeviceAvailable = { true },
            )
        var bodyExecuted = false

        runWithRule(rule) { bodyExecuted = true }
        assertTrue(bodyExecuted)
    }

    @Test
    fun assumeSupportedDevice_whenRequired_pollsUntilProjectedConnects() {
        val timeSource = TestTimeSource()
        var checks = 0
        val rule =
            ProjectedScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                requireProjectedDevice = true,
                connectionTimeout = 2.seconds,
                connectionPollInterval = 250.milliseconds,
                isProjectedDeviceAvailable = {
                    checks++
                    checks >= 3
                },
                sleeper = { ms -> timeSource += ms.milliseconds },
                timeSource = timeSource,
            )
        var bodyExecuted = false

        runWithRule(rule) { bodyExecuted = true }
        assertTrue(bodyExecuted)
        assertEquals(3, checks)
    }

    @Test
    fun assumeSupportedDevice_whenRequired_throwsAssertionErrorOnTimeout() {
        val timeSource = TestTimeSource()
        val rule =
            ProjectedScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                requireProjectedDevice = true,
                connectionTimeout = 1.seconds,
                connectionPollInterval = 250.milliseconds,
                isProjectedDeviceAvailable = { false },
                sleeper = { ms -> timeSource += ms.milliseconds },
                timeSource = timeSource,
            )
        var bodyExecuted = false

        val error = assertFailsWith<AssertionError> { runWithRule(rule) { bodyExecuted = true } }
        assertFalse(bodyExecuted)
        assertTrue(error.message.orEmpty().contains(REQUIRE_PROJECTED_DEVICE_ARG))
    }

    @Test
    fun requireProjectedDeviceArgument_defaultsFromInstrumentationArguments() {
        assertFalse(isRequireProjectedDeviceArgumentEnabled())

        val trueArgs =
            Bundle(originalArguments).apply { putString(REQUIRE_PROJECTED_DEVICE_ARG, "TRUE") }
        InstrumentationRegistry.registerInstance(
            InstrumentationRegistry.getInstrumentation(),
            trueArgs,
        )
        assertTrue(isRequireProjectedDeviceArgumentEnabled())

        val falseArgs =
            Bundle(originalArguments).apply { putString(REQUIRE_PROJECTED_DEVICE_ARG, "false") }
        InstrumentationRegistry.registerInstance(
            InstrumentationRegistry.getInstrumentation(),
            falseArgs,
        )
        assertFalse(isRequireProjectedDeviceArgumentEnabled())
    }

    @Test
    fun isProjectedDeviceConnected_returnsFalseBelowApi35() {
        assertFalse(isProjectedDeviceConnected())
    }

    @Test
    fun assertBitmapAgainstGolden_usesHostIndependentProjectedSuffix() {
        val bitmap = createSolidBitmap(Color.BLUE)
        try {
            for (hostModel in listOf("Cuttlefish GMS x86_64", "Pixel 9")) {
                ShadowBuild.setModel(hostModel)
                val rule =
                    ProjectedScreenshotTestRule(
                        moduleDirectory = "xr/testutils",
                        updateGoldens = true,
                        requireProjectedDevice = false,
                        isProjectedDeviceAvailable = { true },
                    )
                runWithRule(rule) {
                    bitmap.assertAgainstGolden(rule, "host_independent", PixelPerfectMatcher())
                }
                val expectedStagedFile =
                    File(
                        rule.stagedGoldensDirectory,
                        "xr/testutils/host_independent_$PROJECTED_DEVICE_MODEL.png",
                    )
                assertTrue(
                    expectedStagedFile.exists(),
                    "Expected staged golden at $expectedStagedFile for host model '$hostModel'",
                )
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun assertAgainstGolden_matchesProjectedGoldenAndDeletesStagedFile() {
        val rule =
            ProjectedScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                requireProjectedDevice = false,
                isProjectedDeviceAvailable = { true },
            )
        val stagedFile =
            File(
                rule.stagedGoldensDirectory,
                "xr/testutils/test_golden_$PROJECTED_DEVICE_MODEL.png",
            )
        stagedFile.parentFile?.mkdirs()
        stagedFile.writeBytes(byteArrayOf(1, 2, 3))
        assertTrue(stagedFile.exists())

        val matchingBitmap = createSolidBitmap(Color.RED)
        val mismatchedBitmap = createSolidBitmap(Color.BLUE)
        try {
            runWithRule(rule) {
                matchingBitmap.assertAgainstGolden(rule, "test_golden")
                assertFalse(stagedFile.exists())

                val error =
                    assertFailsWith<AssertionError> {
                        mismatchedBitmap.assertAgainstGolden(rule, "test_golden")
                    }
                assertTrue(stagedFile.exists())
                assertTrue(
                    error.message
                        .orEmpty()
                        .contains("xr/testutils/test_golden_$PROJECTED_DEVICE_MODEL.png")
                )
            }
        } finally {
            matchingBitmap.recycle()
            mismatchedBitmap.recycle()
        }
    }

    @Test
    fun apply_neverModifiesSystemUiToastWindowAppOp() {
        setXrSystemFeature(true)
        val executedCommands = mutableListOf<String>()
        val rule =
            ProjectedScreenshotTestRule(
                moduleDirectory = "xr/testutils",
                updateGoldens = false,
                requireProjectedDevice = false,
                isProjectedDeviceAvailable = { true },
                shellCommandExecutor = { cmd ->
                    executedCommands.add(cmd)
                    "TOAST_WINDOW: allow"
                },
            )

        assertFailsWith<IllegalStateException> {
            runWithRule(rule) { throw IllegalStateException("simulated test failure") }
        }
        assertTrue(executedCommands.isEmpty())
    }

    @Test
    fun parseProjectedSurfaceFlingerDisplayId_extractsUnsigned64BitProjectionDisplayId() {
        val dumpsysOutput =
            """
            Display 4619827353912518656 (HWC display 0): port=0 pnpId=GGL screenPartStatus=UNSUPPORTED displayName="CrosvmDisplay"
            Display 11529215047487655091 (Virtual display): displayName="ProjectionDisplay" uniqueId="virtual:com.google.android.glasses.core,10127,ProjectionDisplay,0"
            Display 11529215048416665975 (Virtual display): displayName="ProjectionDisplaySysui" uniqueId="virtual:com.google.android.glasses.core,10127,ProjectionDisplaySysui,1"
            """
                .trimIndent()

        assertEquals("11529215047487655091", parseProjectedSurfaceFlingerDisplayId(dumpsysOutput))
    }

    @Test
    fun parseProjectedSurfaceFlingerDisplayId_ignoresOtherProjectionDisplays() {
        val dumpsysOutput =
            """
            Display 4619827353912518656 (HWC display 0): port=0 displayName="CrosvmDisplay"
            Display 11529215048416665975 (Virtual display): displayName="ProjectionDisplaySysui"
            Display 11529215048416665976 (Virtual display): displayName="ProjectionDisplayLockscreen"
            Display 11529215048416665977 (Virtual display): displayName="ProjectionDisplayRestricted"
            Display 11529215048416665978 (Virtual display): displayName="ProjectionDisplayPhone"
            """
                .trimIndent()

        assertNull(parseProjectedSurfaceFlingerDisplayId(dumpsysOutput))
        assertNull(parseProjectedSurfaceFlingerDisplayId(""))
    }

    @Test
    fun parseProjectedSurfaceFlingerDisplayId_prefersLastMatchingDisplay() {
        val dumpsysOutput =
            """
            Display 11529215047000000001 (Virtual display): displayName="ProjectionDisplay"
            Display 11529215047000000002 (Virtual display): displayName="ProjectionDisplay"
            """
                .trimIndent()

        assertEquals("11529215047000000002", parseProjectedSurfaceFlingerDisplayId(dumpsysOutput))
    }

    @Test
    fun captureProjectedDisplayToBitmap_waitsForSettledFrameAndCachesDisplayId() {
        val timeSource = TestTimeSource()
        var dumpQueries = 0
        val requestedIds = mutableListOf<String>()
        val frames =
            ArrayDeque(
                listOf(
                    createForegroundBitmap(Color.RED, width = 16, height = 16),
                    createForegroundBitmap(Color.GREEN, width = 16, height = 16),
                    createForegroundBitmap(Color.GREEN, width = 16, height = 16),
                    createForegroundBitmap(Color.GREEN, width = 16, height = 16),
                )
            )

        val result =
            captureProjectedDisplayToBitmap(
                timeout = 5.seconds,
                pollInterval = 100.milliseconds,
                displayIdDumpProvider = {
                    dumpQueries++
                    "Display 11529215047487655091 (Virtual display): displayName=\"ProjectionDisplay\""
                },
                frameProvider = { id ->
                    requestedIds.add(id)
                    frames.removeFirst()
                },
                sleeper = { ms -> timeSource += ms.milliseconds },
                timeSource = timeSource,
            )
        try {
            assertEquals(1, dumpQueries)
            assertEquals(
                listOf(
                    "11529215047487655091",
                    "11529215047487655091",
                    "11529215047487655091",
                    "11529215047487655091",
                ),
                requestedIds,
            )
            assertEquals(16, result.width)
            assertEquals(16, result.height)
            assertEquals(Color.GREEN, result.getPixel(8, 8))
        } finally {
            result.recycle()
        }
    }

    @Test
    fun captureProjectedDisplayToBitmap_reQueriesDisplayIdAfterFailedCapture() {
        val timeSource = TestTimeSource()
        var dumpQueries = 0
        val requestedIds = mutableListOf<String>()

        val result =
            captureProjectedDisplayToBitmap(
                timeout = 5.seconds,
                pollInterval = 100.milliseconds,
                displayIdDumpProvider = {
                    dumpQueries++
                    val id = if (dumpQueries == 1) "1001" else "2002"
                    "Display $id (Virtual display): displayName=\"ProjectionDisplay\""
                },
                frameProvider = { id ->
                    requestedIds.add(id)
                    if (id == "1001") {
                        null
                    } else {
                        createForegroundBitmap(Color.CYAN, width = 16, height = 16)
                    }
                },
                sleeper = { ms -> timeSource += ms.milliseconds },
                timeSource = timeSource,
            )
        try {
            assertEquals(2, dumpQueries)
            assertEquals(listOf("1001", "2002", "2002", "2002"), requestedIds)
            assertEquals(Color.CYAN, result.getPixel(8, 8))
        } finally {
            result.recycle()
        }
    }

    @Test
    fun captureProjectedDisplayToBitmap_appliesCropBoundsAndAutoCrop() {
        val timeSource = TestTimeSource()
        val width = 40
        val height = 30
        fun makeFrame(): Bitmap {
            val pixels = IntArray(width * height) { Color.BLACK }
            for (y in 10 until 20) {
                for (x in 15 until 25) {
                    pixels[y * width + x] = Color.YELLOW
                }
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }
        val dump =
            "Display 11529215047487655091 (Virtual display): displayName=\"ProjectionDisplay\""

        val autoCropped =
            captureProjectedDisplayToBitmap(
                autoCrop = true,
                padding = 2,
                displayIdDumpProvider = { dump },
                frameProvider = { makeFrame() },
                sleeper = { ms -> timeSource += ms.milliseconds },
                timeSource = timeSource,
            )
        val explicitCropped =
            captureProjectedDisplayToBitmap(
                cropBounds = Rect(15, 10, 25, 20),
                displayIdDumpProvider = { dump },
                frameProvider = { makeFrame() },
                sleeper = { ms -> timeSource += ms.milliseconds },
                timeSource = timeSource,
            )
        try {
            assertEquals(14, autoCropped.width)
            assertEquals(14, autoCropped.height)
            assertEquals(Color.YELLOW, autoCropped.getPixel(7, 7))

            assertEquals(10, explicitCropped.width)
            assertEquals(10, explicitCropped.height)
            assertEquals(Color.YELLOW, explicitCropped.getPixel(5, 5))
        } finally {
            autoCropped.recycle()
            explicitCropped.recycle()
        }
    }

    @Test
    fun captureProjectedDisplayToBitmap_rejectsNegativePadding() {
        assertFailsWith<IllegalArgumentException> {
            captureProjectedDisplayToBitmap(
                padding = -1,
                displayIdDumpProvider = { "" },
                frameProvider = { null },
            )
        }
    }

    @Test
    fun captureProjectedDisplayToBitmap_throwsDiagnosticErrorWhenDisplayMissing() {
        val timeSource = TestTimeSource()
        val error =
            assertFailsWith<AssertionError> {
                captureProjectedDisplayToBitmap(
                    timeout = 300.milliseconds,
                    pollInterval = 100.milliseconds,
                    displayIdDumpProvider = {
                        "Display 4619827353912518656 (HWC display 0): displayName=\"CrosvmDisplay\""
                    },
                    frameProvider = { null },
                    sleeper = { ms -> timeSource += ms.milliseconds },
                    timeSource = timeSource,
                )
            }
        assertTrue(error.message.orEmpty().contains(PROJECTED_DISPLAY_NAME))
        assertTrue(error.message.orEmpty().contains("CrosvmDisplay"))
    }

    @Test
    fun captureProjectedDisplayToBitmap_throwsDiagnosticErrorWhenScreencapFails() {
        val timeSource = TestTimeSource()
        val error =
            assertFailsWith<AssertionError> {
                captureProjectedDisplayToBitmap(
                    timeout = 300.milliseconds,
                    pollInterval = 100.milliseconds,
                    displayIdDumpProvider = {
                        "Display 11529215047487655091 (Virtual display): displayName=\"ProjectionDisplay\""
                    },
                    frameProvider = { null },
                    sleeper = { ms -> timeSource += ms.milliseconds },
                    timeSource = timeSource,
                )
            }
        assertTrue(error.message.orEmpty().contains("screencap -d 11529215047487655091 -p"))
    }

    @Test
    fun captureProjectedDisplayToBitmap_attachesFailureCauseOnTimeout() {
        val timeSource = TestTimeSource()
        val expectedCause = SecurityException("shell command denied")
        var lastError: Throwable? = null
        val error =
            assertFailsWith<AssertionError> {
                captureProjectedDisplayToBitmap(
                    timeout = 300.milliseconds,
                    pollInterval = 100.milliseconds,
                    displayIdDumpProvider = {
                        "Display 11529215047487655091 (Virtual display): displayName=\"ProjectionDisplay\""
                    },
                    frameProvider = {
                        lastError = expectedCause
                        null
                    },
                    sleeper = { ms -> timeSource += ms.milliseconds },
                    timeSource = timeSource,
                    failureCause = { lastError },
                )
            }
        assertEquals(expectedCause, error.cause)
    }
}
