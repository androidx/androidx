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

import android.graphics.Bitmap
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.ScreenshotTestRule
import androidx.test.screenshot.ScreenshotTestRuleConfig
import androidx.test.screenshot.matchers.BitmapMatcher
import androidx.test.screenshot.matchers.PixelPerfectMatcher
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assume
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Default Structural Similarity Index (MSSIM) threshold for 3D XR compositor screenshots on XR
 * devices. Tolerates minor subpixel/MSAA GPU variance while catching visual regressions in
 * materials, lighting, and 3D spatial layouts.
 */
public const val DEFAULT_XR_MSSIM_THRESHOLD: Double = 0.95

/**
 * Instrumentation runner argument key that, when set to `"true"`, causes [XrScreenshotTestRule] to
 * write captured screenshots to [XrScreenshotTestRule.stagedGoldensDirectory] and succeed instead
 * of throwing an [AssertionError] when a golden image is missing or mismatched.
 *
 * Example usage:
 * ```
 * ./gradlew <module>:connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.updateGoldens=true
 * ```
 */
public const val UPDATE_GOLDENS_ARG: String = "updateGoldens"

private const val REPO_ROOT_PATH_FOR_GOLDENS = "platform/frameworks/support-golden"
private const val GOLDENS_SUBDIRECTORY = "goldens"
private const val TAG = "XrScreenshotTestRule"
private const val SYSTEMUI_PACKAGE = "com.android.systemui"
private const val GET_SYSTEMUI_TOAST_APPOP_CMD = "cmd appops get $SYSTEMUI_PACKAGE TOAST_WINDOW"
private const val IGNORE_SYSTEMUI_TOAST_APPOP_CMD =
    "cmd appops set $SYSTEMUI_PACKAGE TOAST_WINDOW ignore"
private const val MIN_CROP_SIZE_TOLERANCE_PX = 6
private const val MAX_CROP_SIZE_TOLERANCE_RATIO = 0.05f
private val SIZES_DIFFERENT_REGEX =
    Regex("Sizes are different! Expected: \\[(\\d+), (\\d+)], Actual: \\[(\\d+), (\\d+)]")
private val TOAST_WINDOW_MODE_REGEX =
    Regex("(?:TOAST_WINDOW|Uid mode):\\s*(allow|ignore|deny|default)\\b")

/**
 * [ScreenshotTestRule] for AndroidX tests running on XR devices.
 *
 * Uses the same golden repository root and [moduleDirectory] layout as
 * `AndroidXScreenshotTestRule`, but runs on any device with the XR spatial feature instead of only
 * on API 35 emulators. Use it with the standard `androidx.test.screenshot.assertAgainstGolden`
 * extensions. While a test is executing on an XR device, `com.android.systemui` `TOAST_WINDOW`
 * overlays (such as the post-wake input-recovery toast) are temporarily suppressed via AppOps so
 * transient system toasts cannot pollute compositor captures or auto-cropped bounds.
 *
 * When a golden is missing or mismatched, the captured bitmap is staged under
 * [stagedGoldensDirectory] using its repository-relative path so that it can be pulled directly
 * into the local `golden/` checkout. To record or update all goldens in a run without failing, pass
 * the [UPDATE_GOLDENS_ARG] instrumentation runner argument:
 * ```
 * ./gradlew <module>:connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.updateGoldens=true
 * ```
 *
 * Modules that apply `xr/testutils/xr-device-tests.gradle` automatically configure connected XR
 * devices before `connectedAndroidTest` (disabling DON/DOFF wearing detection, keeping displays
 * awake, and enabling CPM fake head tracking locked to the identity head pose) and restore default
 * device properties upon completion. When [UPDATE_GOLDENS_ARG] is `"true"`, the script also clears
 * [stagedGoldensDirectory] on the device beforehand and pulls staged goldens into `../../golden/`
 * after the test run. Otherwise, from `frameworks/support`, pull the staged goldens into the golden
 * repository manually:
 * ```
 * adb pull /sdcard/Android/data/<test_pkg>/cache/androidx_screenshots/goldens/. ../../golden/
 * ```
 *
 * Staged goldens for tests that pass are deleted, but goldens staged by tests that were since
 * renamed or removed are not; clear the directory on the device if needed.
 *
 * @param moduleDirectory Relative path to the module containing the tests (e.g.,
 *   `"xr/scenecore/integration-tests/xr-integration-tests"`).
 * @param updateGoldens When `true`, stages captured bitmaps for missing or mismatched goldens and
 *   does not throw [AssertionError]. Defaults to reading the [UPDATE_GOLDENS_ARG] instrumentation
 *   argument.
 */
public class XrScreenshotTestRule
internal constructor(
    moduleDirectory: String,
    private val updateGoldens: Boolean,
    private val shellCommandExecutor: (String) -> String,
) :
    ScreenshotTestRule(
        ScreenshotTestRuleConfig(
            REPO_ROOT_PATH_FOR_GOLDENS,
            moduleDirectory.trim('/').also {
                require(!it.split('/').contains("..")) {
                    "moduleDirectory must not contain '..': $moduleDirectory"
                }
            },
        )
    ) {

    @JvmOverloads
    public constructor(
        moduleDirectory: String,
        updateGoldens: Boolean = isUpdateGoldensArgumentEnabled(),
    ) : this(
        moduleDirectory = moduleDirectory,
        updateGoldens = updateGoldens,
        shellCommandExecutor = { executeSceneShellCommand(it) },
    )

    /**
     * Directory on the device where candidate goldens are written using their exact
     * repository-relative path and `<goldenIdentifier>_<deviceId>.png` filename, so they can be
     * pulled directly into `platform/frameworks/support-golden` via `adb pull`.
     */
    public val stagedGoldensDirectory: File
        get() = File(deviceOutputDirectory, GOLDENS_SUBDIRECTORY)

    override fun apply(base: Statement, description: Description?): Statement {
        val toastSuppressedStatement =
            object : Statement() {
                override fun evaluate() {
                    val previousMode = querySystemUiToastWindowMode()
                    shellCommandExecutor(IGNORE_SYSTEMUI_TOAST_APPOP_CMD)
                    try {
                        base.evaluate()
                    } finally {
                        if (previousMode != null && previousMode != "ignore") {
                            shellCommandExecutor(
                                "cmd appops set $SYSTEMUI_PACKAGE TOAST_WINDOW $previousMode"
                            )
                        }
                    }
                }
            }
        return super.apply(toastSuppressedStatement, description)
    }

    private fun querySystemUiToastWindowMode(): String? {
        val output = shellCommandExecutor(GET_SYSTEMUI_TOAST_APPOP_CMD)
        if (output.isBlank()) return null
        if (output.contains("No operations", ignoreCase = true)) return "allow"
        return TOAST_WINDOW_MODE_REGEX.find(output)?.groupValues?.get(1)
    }

    /** Skips the test on devices without the XR spatial feature, matching [XrDeviceTest]. */
    override fun assumeSupportedDevice() {
        Assume.assumeTrue("Requires an XR device", isXrDevice())
    }

    /**
     * Asserts [actual] against the golden identified by [goldenIdentifier], staging the captured
     * bitmap under [stagedGoldensDirectory] when the golden is missing or mismatched.
     *
     * Hardware bitmaps are copied to [Bitmap.Config.ARGB_8888] before comparison.
     */
    override fun assertBitmapAgainstGolden(
        actual: Bitmap,
        goldenIdentifier: String,
        matcher: BitmapMatcher,
    ) {
        val softwareBitmap = toSoftwareBitmap(actual)
        try {
            try {
                assertWithCropSizeTolerance(softwareBitmap, goldenIdentifier, matcher)
            } catch (e: AssertionError) {
                val stagedFile = writeStagedGolden(softwareBitmap, goldenIdentifier)
                if (updateGoldens) {
                    Log.i(
                        TAG,
                        "Staged golden '${resolveGoldenPathInRepo(goldenIdentifier)}' at " +
                            "${stagedFile.absolutePath} (${e.message}). Pull all staged goldens " +
                            "from frameworks/support with:\n  ${buildAdbPullAllCommand()}",
                    )
                    return
                }
                throw AssertionError(
                    "${e.message}\n" +
                        "Captured candidate golden on device at: ${stagedFile.absolutePath}\n" +
                        "To accept it, run from frameworks/support:\n" +
                        "  ${buildAdbPullSingleCommand(stagedFile, goldenIdentifier)}\n" +
                        "Or re-run with -Pandroid.testInstrumentationRunnerArguments." +
                        "$UPDATE_GOLDENS_ARG=true and pull all staged goldens with:\n" +
                        "  ${buildAdbPullAllCommand()}",
                    e,
                )
            }
            // The golden matches, so drop any candidate staged by an earlier failing run.
            stagedGoldenFile(goldenIdentifier).delete()
        } finally {
            if (softwareBitmap !== actual && !softwareBitmap.isRecycled) {
                softwareBitmap.recycle()
            }
        }
    }

    private fun assertWithCropSizeTolerance(
        softwareBitmap: Bitmap,
        goldenIdentifier: String,
        matcher: BitmapMatcher,
    ) {
        try {
            super.assertBitmapAgainstGolden(softwareBitmap, goldenIdentifier, matcher)
        } catch (e: AssertionError) {
            if (updateGoldens || matcher is PixelPerfectMatcher) {
                throw e
            }
            val match = SIZES_DIFFERENT_REGEX.find(e.message.orEmpty()) ?: throw e
            val expectedWidth = match.groupValues[1].toIntOrNull() ?: throw e
            val expectedHeight = match.groupValues[2].toIntOrNull() ?: throw e
            val maxWidthDelta =
                max(
                    MIN_CROP_SIZE_TOLERANCE_PX,
                    (expectedWidth * MAX_CROP_SIZE_TOLERANCE_RATIO).toInt(),
                )
            val maxHeightDelta =
                max(
                    MIN_CROP_SIZE_TOLERANCE_PX,
                    (expectedHeight * MAX_CROP_SIZE_TOLERANCE_RATIO).toInt(),
                )
            if (
                abs(softwareBitmap.width - expectedWidth) > maxWidthDelta ||
                    abs(softwareBitmap.height - expectedHeight) > maxHeightDelta
            ) {
                throw e
            }
            val scaled = scaleBilinear(softwareBitmap, expectedWidth, expectedHeight)
            try {
                super.assertBitmapAgainstGolden(scaled, goldenIdentifier, matcher)
            } finally {
                if (scaled !== softwareBitmap && !scaled.isRecycled) {
                    scaled.recycle()
                }
            }
        }
    }

    private fun stagedGoldenFile(goldenIdentifier: String): File =
        File(stagedGoldensDirectory, resolveGoldenPathInRepo(goldenIdentifier))

    private fun writeStagedGolden(actual: Bitmap, goldenIdentifier: String): File {
        val stagedFile = stagedGoldenFile(goldenIdentifier)
        val parentDir = stagedFile.parentFile
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw IOException("Could not create staged golden folder $parentDir.")
        }
        try {
            FileOutputStream(stagedFile).use {
                actual.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for png */, it)
            }
        } catch (e: Exception) {
            throw IOException(
                "Could not write staged golden to storage (path: ${stagedFile.absolutePath}).",
                e,
            )
        }
        return stagedFile
    }

    private fun buildAdbPullSingleCommand(stagedFile: File, goldenIdentifier: String): String =
        "adb pull ${stagedFile.absolutePath} ../../golden/${resolveGoldenPathInRepo(goldenIdentifier)}"

    private fun buildAdbPullAllCommand(): String =
        "adb pull ${stagedGoldensDirectory.absolutePath}/. ../../golden/"
}

private fun isUpdateGoldensArgumentEnabled(): Boolean = runCatching {
    InstrumentationRegistry.getArguments().getString(UPDATE_GOLDENS_ARG)?.toBoolean() == true
}
    .getOrDefault(false)
