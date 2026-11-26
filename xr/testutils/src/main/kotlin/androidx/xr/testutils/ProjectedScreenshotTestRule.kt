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

import android.app.UiAutomation
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.matchers.BitmapMatcher
import androidx.xr.testutils.runtime.cpm.executeSceneShellCommand
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import org.junit.Assume

/**
 * Instrumentation runner argument key that, when set to `"true"`, causes
 * [ProjectedScreenshotTestRule] to wait for a paired Projected device to connect and throw an
 * [AssertionError] instead of skipping via [Assume] if none becomes available.
 *
 * Example usage:
 * ```
 * ./gradlew :xr:projected:integration-tests:xr-integration-tests:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.requireProjectedDevice=true
 * ```
 */
public const val REQUIRE_PROJECTED_DEVICE_ARG: String = "requireProjectedDevice"

/**
 * Host-independent device suffix appended to golden filenames for Projected screenshot tests
 * (`<goldenIdentifier>_xr_projected.png`), decoupling golden names from the companion phone's
 * [Build.MODEL].
 *
 * Goldens recorded with this suffix target the canonical CI Projected emulator display profile
 * (`cf_x86_64_glasses`, `450x394` at `160 dpi`). Projected displays with a different native
 * resolution or density (such as physical glasses hardware or alternate emulator profiles) are not
 * currently distinguished by this suffix and will mismatch emulator-pair goldens.
 */
public const val PROJECTED_DEVICE_MODEL: String = "xr_projected"

internal const val PROJECTED_DEVICE_NAME_PREFIX: String = "ProjectionDevice"
internal const val PROJECTED_DISPLAY_NAME: String = "ProjectionDisplay"

private const val TAG = "ProjectedScreenshotTestRule"
private const val DUMPSYS_SURFACEFLINGER_DISPLAY_ID_CMD = "dumpsys SurfaceFlinger --display-id"
private val DEFAULT_PROJECTED_CONNECTION_TIMEOUT = 15.seconds
private val DEFAULT_PROJECTED_CONNECTION_POLL_INTERVAL = 250.milliseconds
private val SURFACE_FLINGER_PROJECTED_DISPLAY_REGEX =
    Regex("""^\s*Display\s+(\d+)\b.*displayName="$PROJECTED_DISPLAY_NAME"(?:\s|$)""")

/**
 * [XrScreenshotTestRule] subclass for AndroidX screenshot tests running on paired Companion Phone +
 * Glasses Projected setups.
 *
 * * **Projected device gating**: Verifies that a paired Projected virtual device
 *   (`ProjectionDevice*`) with an active `ProjectionDisplay` virtual display is present, keeping
 *   Projected tests cleanly isolated from standalone spatial XR headsets (`XrScreenshotTestRule`).
 *   Skips cleanly via [Assume] on unpaired phones (such as single-device FTL runs) and headsets,
 *   unless [requireProjectedDevice] (or `-e requireProjectedDevice true`) is enabled, in which case
 *   the rule waits up to 15 seconds for connection and fails with [AssertionError] if unavailable.
 * * **Host-independent golden naming (emulator display profile)**: Overrides [getDeviceModel] to
 *   return [PROJECTED_DEVICE_MODEL] (`"xr_projected"`) so golden filenames
 *   (`<goldenId>_xr_projected.png`) do not vary across host phone models (e.g., Cuttlefish vs.
 *   Pixel). Note that goldens remain tied to the canonical CI Projected emulator display profile
 *   (`cf_x86_64_glasses`, `450x394` at `160 dpi`) and are intended for that emulator pair rather
 *   than arbitrary Projected display sizes/densities.
 * * **No headset SystemUI AppOps mutation**: Disables `com.android.systemui` `TOAST_WINDOW` AppOps
 *   suppression so the host phone's SystemUI settings are untouched.
 * * **Shared XR staging & matching**: Inherits [updateGoldens] staging under
 *   [stagedGoldensDirectory], hardware-to-software bitmap conversion, crop-size tolerance, and
 *   [XrSsimMatcher] defaults from [XrScreenshotTestRule], and works directly with
 *   [Bitmap.assertAgainstGolden] and [captureProjectedDisplayToBitmap].
 *
 * @param moduleDirectory Relative path to the module containing the tests (e.g.,
 *   `"xr/projected/integration-tests/xr-integration-tests"`).
 * @param updateGoldens When `true`, stages captured bitmaps for missing or mismatched goldens and
 *   does not throw [AssertionError]. Defaults to reading the [UPDATE_GOLDENS_ARG] instrumentation
 *   argument.
 * @param defaultMatcher Default [BitmapMatcher] used when calling `assertAgainstGolden` without an
 *   explicit matcher or calling the two-argument [assertBitmapAgainstGolden] overload. Defaults to
 *   [XrSsimMatcher].
 * @param requireProjectedDevice When `true`, waits for a Projected device to connect and throws
 *   [AssertionError] if unavailable instead of skipping via [Assume]. Defaults to reading the
 *   [REQUIRE_PROJECTED_DEVICE_ARG] instrumentation argument.
 */
public class ProjectedScreenshotTestRule
internal constructor(
    moduleDirectory: String,
    updateGoldens: Boolean,
    defaultMatcher: BitmapMatcher = XrSsimMatcher(),
    private val requireProjectedDevice: Boolean,
    private val connectionTimeout: Duration = DEFAULT_PROJECTED_CONNECTION_TIMEOUT,
    private val connectionPollInterval: Duration = DEFAULT_PROJECTED_CONNECTION_POLL_INTERVAL,
    private val isProjectedDeviceAvailable: () -> Boolean,
    private val sleeper: (Long) -> Unit = SystemClock::sleep,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    shellCommandExecutor: (String) -> String = { "" },
) :
    XrScreenshotTestRule(
        moduleDirectory = moduleDirectory,
        updateGoldens = updateGoldens,
        defaultMatcher = defaultMatcher,
        shellCommandExecutor = shellCommandExecutor,
    ) {

    @JvmOverloads
    public constructor(
        moduleDirectory: String,
        updateGoldens: Boolean = isUpdateGoldensArgumentEnabled(),
        defaultMatcher: BitmapMatcher = XrSsimMatcher(),
        requireProjectedDevice: Boolean = isRequireProjectedDeviceArgumentEnabled(),
    ) : this(
        moduleDirectory = moduleDirectory,
        updateGoldens = updateGoldens,
        defaultMatcher = defaultMatcher,
        requireProjectedDevice = requireProjectedDevice,
        isProjectedDeviceAvailable = { isProjectedDeviceConnected() },
    )

    override fun shouldSuppressSystemUiToasts(): Boolean = false

    /**
     * Returns [PROJECTED_DEVICE_MODEL] (`"xr_projected"`), decoupling golden filenames from the
     * companion phone's [Build.MODEL] for the canonical Projected emulator pair (`450x394`, `160
     * dpi`).
     */
    override fun getDeviceModel(): String = PROJECTED_DEVICE_MODEL

    override fun assumeSupportedDevice() {
        if (!requireProjectedDevice) {
            Assume.assumeTrue(
                "No paired Projected device connected; pass " +
                    "-e $REQUIRE_PROJECTED_DEVICE_ARG true to fail instead of skipping.",
                isProjectedDeviceAvailable(),
            )
            return
        }
        val start = timeSource.markNow()
        while (!isProjectedDeviceAvailable()) {
            if (start.elapsedNow() >= connectionTimeout) {
                throw AssertionError(
                    "Projected device did not connect within $connectionTimeout " +
                        "($REQUIRE_PROJECTED_DEVICE_ARG=true)"
                )
            }
            sleeper(connectionPollInterval.inWholeMilliseconds)
        }
    }
}

/**
 * Captures the paired Projected virtual display (`ProjectionDisplay`) from SurfaceFlinger via
 * `screencap -d <sf_display_id> -p`, waits for consecutive frames to stabilize, optionally crops to
 * [cropBounds] (or foreground content when [autoCrop] is `true`), and returns a software-backed
 * [Bitmap.Config.ARGB_8888] [Bitmap] for assertion with [ProjectedScreenshotTestRule].
 *
 * Unlike zero-arg [UiAutomation.takeScreenshot] (which captures the host phone's default display,
 * `displayId = 0`), targeting `ProjectionDisplay`'s SurfaceFlinger display ID captures the full
 * projected display composition, including dialogs, popups, and `SurfaceView` layers.
 *
 * The caller owns the returned [Bitmap] and is responsible for calling [Bitmap.recycle] when
 * finished.
 *
 * @param cropBounds Optional [Rect] in display pixel coordinates to crop from the captured frame.
 * @param padding Extra padding in pixels to preserve around cropped foreground bounds when
 *   [cropBounds] is `null` and [autoCrop] is `true`.
 * @param autoCrop When `true` and [cropBounds] is `null`, automatically crops out the background
 *   via [cropForeground]. Defaults to `false` so the full `ProjectionDisplay` viewport is asserted.
 * @param timeout Maximum duration to wait for `ProjectionDisplay` to appear and consecutive frames
 *   to stabilize. If frames do not stabilize before [timeout], the latest frame is returned and a
 *   warning is logged.
 * @param pollInterval Delay between successive display captures.
 * @param uiAutomation The [UiAutomation] instance used to execute SurfaceFlinger and `screencap`
 *   shell commands.
 * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
 * @throws IllegalArgumentException if [padding] is negative or [cropBounds] does not overlap the
 *   captured bitmap.
 * @throws AssertionError if `ProjectionDisplay` cannot be found or no frame could be captured
 *   within [timeout].
 */
@JvmOverloads
public fun captureProjectedDisplayToBitmap(
    cropBounds: Rect? = null,
    padding: Int = 0,
    autoCrop: Boolean = false,
    timeout: Duration = 5.seconds,
    pollInterval: Duration = 100.milliseconds,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): Bitmap {
    require(padding >= 0) { "padding must be non-negative, was $padding" }
    if (Looper.myLooper() != Looper.getMainLooper()) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
    var lastCaptureError: Throwable? = null
    return captureProjectedDisplayToBitmap(
        cropBounds = cropBounds,
        padding = padding,
        autoCrop = autoCrop,
        timeout = timeout,
        pollInterval = pollInterval,
        displayIdDumpProvider = {
            executeSceneShellCommand(DUMPSYS_SURFACEFLINGER_DISPLAY_ID_CMD, uiAutomation)
        },
        frameProvider = { sfDisplayId ->
            captureSurfaceFlingerDisplayBitmap(
                    sfDisplayId = sfDisplayId,
                    uiAutomation = uiAutomation,
                    onError = { lastCaptureError = it },
                )
                ?.also { lastCaptureError = null }
        },
        failureCause = { lastCaptureError },
    )
}

internal fun captureProjectedDisplayToBitmap(
    cropBounds: Rect? = null,
    padding: Int = 0,
    autoCrop: Boolean = false,
    timeout: Duration = 5.seconds,
    pollInterval: Duration = 100.milliseconds,
    displayIdDumpProvider: () -> String,
    frameProvider: (String) -> Bitmap?,
    sleeper: (Long) -> Unit = SystemClock::sleep,
    timeSource: TimeSource = TimeSource.Monotonic,
    failureCause: () -> Throwable? = { null },
): Bitmap {
    require(padding >= 0) { "padding must be non-negative, was $padding" }
    var cachedDisplayId: String? = null
    var lastResolvedDisplayId: String? = null
    var lastDisplayDump = ""
    val settledBitmap =
        waitForSettledSoftwareScreenshot(
            screenshotProvider = {
                val displayId =
                    cachedDisplayId
                        ?: run {
                            val dump = displayIdDumpProvider()
                            lastDisplayDump = dump
                            parseProjectedSurfaceFlingerDisplayId(dump).also {
                                cachedDisplayId = it
                                lastResolvedDisplayId = it
                            }
                        }
                if (displayId == null) {
                    null
                } else {
                    val bitmap = frameProvider(displayId)
                    if (bitmap == null) {
                        // Re-query SurfaceFlinger on the next poll in case the virtual display was
                        // recreated with a new display ID.
                        cachedDisplayId = null
                    }
                    bitmap
                }
            },
            timeout = timeout,
            pollInterval = pollInterval,
            timeSource = timeSource,
            sleepMillis = sleeper,
            failureMessage = {
                val displayId = lastResolvedDisplayId
                if (displayId == null) {
                    "Timed out after $timeout waiting for SurfaceFlinger display " +
                        "\"$PROJECTED_DISPLAY_NAME\" in '$DUMPSYS_SURFACEFLINGER_DISPLAY_ID_CMD' " +
                        "output:\n$lastDisplayDump"
                } else {
                    "Timed out after $timeout waiting for 'screencap -d $displayId -p' on " +
                        "\"$PROJECTED_DISPLAY_NAME\" to return a non-null Bitmap"
                }
            },
            failureCause = failureCause,
        )
    return applyScreenshotCrop(settledBitmap, cropBounds, padding, autoCrop)
}

/**
 * Extracts the SurfaceFlinger display ID string for `"ProjectionDisplay"` from the output of
 * `dumpsys SurfaceFlinger --display-id`.
 *
 * Returns the ID as a [String] because SurfaceFlinger virtual display IDs are unsigned 64-bit
 * integers that can exceed [Long.MAX_VALUE]. When multiple matching entries exist, returns the last
 * one.
 */
internal fun parseProjectedSurfaceFlingerDisplayId(dumpsysOutput: String): String? =
    dumpsysOutput
        .lineSequence()
        .mapNotNull { line ->
            SURFACE_FLINGER_PROJECTED_DISPLAY_REGEX.find(line)?.groupValues?.get(1)
        }
        .lastOrNull()

private fun captureSurfaceFlingerDisplayBitmap(
    sfDisplayId: String,
    uiAutomation: UiAutomation,
    onError: (Exception) -> Unit = {},
): Bitmap? {
    val command = "screencap -d $sfDisplayId -p"
    return try {
        val (stdoutBytes, stderrText) = executeBinaryShellCommand(command, uiAutomation)
        if (stdoutBytes.isEmpty()) {
            Log.w(TAG, "'$command' returned empty output (stderr=${stderrText.trim()})")
            return null
        }
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        BitmapFactory.decodeByteArray(stdoutBytes, 0, stdoutBytes.size, options).also { decoded ->
            if (decoded == null) {
                Log.w(
                    TAG,
                    "Failed to decode PNG from '$command' (${stdoutBytes.size} bytes, " +
                        "stderr=${stderrText.trim()})",
                )
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Error executing '$command'", e)
        onError(e)
        null
    }
}

private fun executeBinaryShellCommand(
    command: String,
    uiAutomation: UiAutomation,
): Pair<ByteArray, String> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val (stdout, stdin, stderr) = Api34ShellImpl.executeShellCommandRwe(uiAutomation, command)
        return stdout.use {
            stdin.use {
                stderr.use {
                    // Close stdin immediately so the child process does not block waiting for
                    // input.
                    stdin.close()
                    val stdoutStream = ParcelFileDescriptor.AutoCloseInputStream(stdout)
                    val stderrStream = ParcelFileDescriptor.AutoCloseInputStream(stderr)
                    var stderrText = ""
                    var stderrError: Throwable? = null
                    val stderrThread =
                        Thread(
                                {
                                    try {
                                        stderrText = stderrStream.bufferedReader().readText()
                                    } catch (t: Throwable) {
                                        stderrError = t
                                    }
                                },
                                "ProjectedScreenshotStderrDrain",
                            )
                            .apply { start() }
                    val stdoutBytes =
                        try {
                            stdoutStream.readBytes()
                        } catch (t: Throwable) {
                            runCatching { stderrStream.close() }
                            throw t
                        } finally {
                            try {
                                stderrThread.join()
                            } catch (_: InterruptedException) {
                                Thread.currentThread().interrupt()
                            }
                        }
                    stderrError?.let { error ->
                        if (stdoutBytes.isEmpty()) {
                            throw error
                        }
                        Log.w(TAG, "Error draining stderr for '$command'", error)
                    }
                    stdoutBytes to stderrText
                }
            }
        }
    }
    val stdoutBytes =
        uiAutomation.executeShellCommand(command).use { pfd ->
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        }
    return stdoutBytes to ""
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private object Api34ShellImpl {
    fun executeShellCommandRwe(
        uiAutomation: UiAutomation,
        command: String,
    ): Array<ParcelFileDescriptor> = uiAutomation.executeShellCommandRwe(command)
}

internal fun isProjectedDeviceConnected(
    context: Context = InstrumentationRegistry.getInstrumentation().targetContext
): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        return false
    }
    return ProjectedDisplayApi35Impl.isProjectedDisplayAvailable(context)
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private object ProjectedDisplayApi35Impl {
    fun isProjectedDisplayAvailable(context: Context): Boolean {
        val hostContext =
            if (context.deviceId == Context.DEVICE_ID_DEFAULT) {
                context
            } else {
                context.createDeviceContext(Context.DEVICE_ID_DEFAULT)
            }
        val virtualDeviceManager =
            hostContext.getSystemService(VirtualDeviceManager::class.java) ?: return false
        val displayManager =
            hostContext.getSystemService(DisplayManager::class.java) ?: return false
        val displays = displayManager.displays
        return virtualDeviceManager.virtualDevices.any { device ->
            device.name?.startsWith(PROJECTED_DEVICE_NAME_PREFIX) == true &&
                displays.any { display ->
                    display.name == PROJECTED_DISPLAY_NAME &&
                        device.displayIds.contains(display.displayId)
                }
        }
    }
}

internal fun isRequireProjectedDeviceArgumentEnabled(): Boolean = runCatching {
    InstrumentationRegistry.getArguments()
        .getString(REQUIRE_PROJECTED_DEVICE_ARG)
        .equals("true", ignoreCase = true)
}
    .getOrDefault(false)
