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

@file:Suppress("PrimitiveInCollection", "BanThreadSleep")
@file:JvmName("SpatialSceneHelper")

package androidx.xr.testutils

import android.app.Activity
import android.app.UiAutomation
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.testutils.runtime.cpm.captureSceneWithPolling
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Captures a point-in-time snapshot of the spatial scene graph from `dumpsys spf_cpm`.
 *
 * Parses point-in-time scene graph snapshots from `dumpsys spf_cpm` into a clean object structure
 * based strictly on system-guaranteed CPM and SpaceFlinger keywords, properties, and task
 * boundaries.
 *
 * For the complete structural and keyword specification of `dumpsys spf_cpm`, see
 * `CPM_DUMPSYS_ARCHITECTURE.md` located in the `xr:testutils` root directory.
 *
 * @param uiAutomation The [UiAutomation] instance used to execute the shell command.
 */
@JvmOverloads
public fun captureSpatialScene(
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
): SpatialScene = captureSceneWithPolling(uiAutomation = uiAutomation)

/**
 * Repeatedly captures the spatial scene graph until [until] holds for the value returned by
 * [select].
 *
 * Note that each iteration calls [captureSpatialScene], which may itself poll internally for up to
 * 3 seconds while waiting for window transition snapshots to clear.
 *
 * @param description Human-readable description of the condition being awaited, used in the timeout
 *   error message.
 * @param timeout Maximum duration to poll before throwing an [AssertionError].
 * @param pollInterval Delay between successive scene captures.
 * @param uiAutomation The [UiAutomation] instance used to execute the shell command.
 * @param select Selector that extracts a value from each captured [SpatialScene].
 * @param until Predicate that determines whether the selected value satisfies the condition.
 * @return The first selected value for which [until] returns `true`.
 * @throws AssertionError if [until] does not hold within [timeout].
 */
@JvmOverloads
public fun <T> waitForSpatialScene(
    description: String,
    timeout: Duration = 5.seconds,
    pollInterval: Duration = 100.milliseconds,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    select: (SpatialScene) -> T,
    until: (T) -> Boolean,
): T {
    val start = TimeSource.Monotonic.markNow()
    while (true) {
        val scene = captureSpatialScene(uiAutomation = uiAutomation)
        val selected = select(scene)
        if (until(selected)) return selected
        if (start.elapsedNow() >= timeout) {
            throw AssertionError(
                "Timed out after $timeout waiting for $description, last value: $selected"
            )
        }
        SystemClock.sleep(pollInterval.inWholeMilliseconds)
    }
}

/**
 * Repeatedly captures the spatial scene graph until the target [SpatialTask] (for [activity] if
 * provided, or [SpatialScene.activeTask] otherwise) satisfies [SpatialTask.isSettled].
 *
 * @param activity Optional [Activity] whose spatial task should be awaited. When `null`, awaits the
 *   foreground [SpatialScene.activeTask].
 * @param expectedEmbeddedPanelCount Expected number of visible [SpatialTask.embeddedWindowLeashes]
 *   with valid surface bounds, or `null` to not constrain the embedded panel count.
 * @param expectedMainPanelVisible Expected visibility of [SpatialTask.taskWindowLeash], or `null`
 *   to not constrain the main panel visibility.
 * @param requireFullSpace Whether the task must have an active Full Space transform
 *   ([SpatialTask.isFullSpace]).
 * @param timeout Maximum duration to poll before throwing an [AssertionError].
 * @param pollInterval Delay between successive scene captures.
 * @param uiAutomation The [UiAutomation] instance used to execute the shell command.
 * @return The settled [SpatialTask].
 * @throws AssertionError if the task does not settle within [timeout].
 */
@JvmOverloads
public fun waitForSettledSpatialTask(
    activity: Activity? = null,
    expectedEmbeddedPanelCount: Int? = null,
    expectedMainPanelVisible: Boolean? = null,
    requireFullSpace: Boolean = true,
    timeout: Duration = 5.seconds,
    pollInterval: Duration = 100.milliseconds,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): SpatialTask {
    val targetDesc = activity?.let { "task for $it" } ?: "active spatial task"
    val panelDesc =
        if (expectedEmbeddedPanelCount != null) {
            " with $expectedEmbeddedPanelCount visible embedded window leashes"
        } else {
            ""
        }
    val settledTask =
        waitForSpatialScene(
            description = "settled $targetDesc$panelDesc",
            timeout = timeout,
            pollInterval = pollInterval,
            uiAutomation = uiAutomation,
            select = { scene ->
                if (activity != null) {
                    scene.findTaskByActivity(activity)
                } else {
                    scene.activeTask
                }
            },
            until = { task ->
                task != null &&
                    task.isSettled(
                        expectedEmbeddedPanelCount = expectedEmbeddedPanelCount,
                        expectedMainPanelVisible = expectedMainPanelVisible,
                        requireFullSpace = requireFullSpace,
                    )
            },
        )
    return checkNotNull(settledTask) {
        "waitForSpatialScene returned null for settled $targetDesc$panelDesc"
    }
}

/**
 * Captures the 3D XR compositor output via [UiAutomation.takeScreenshot], waits for consecutive
 * compositor frames to stabilize, converts hardware-backed bitmaps to [Bitmap.Config.ARGB_8888],
 * optionally crops to [cropBounds] (or automatically crops out the empty background when [autoCrop]
 * is true and [cropBounds] is null), and returns the resulting [Bitmap] for assertion with
 * `androidx.test.screenshot.assertAgainstGolden`.
 *
 * The caller owns the returned [Bitmap] and is responsible for calling [Bitmap.recycle] when
 * finished (or letting higher-level test scopes manage its lifecycle).
 *
 * Standard Android 2D `View.drawToBitmap()` or `SemanticsNodeInteraction.captureToImage()` only
 * captures flat 2D window surfaces and does not include 3D `GltfModelEntity`, `SurfaceEntity`,
 * `SpatialEnvironment` skyboxes, or 3D spatial relationships rendered by SpaceFlinger/Impress. Use
 * this helper after [waitForSpatialScene] for the scene graph to settle.
 *
 * @param cropBounds Optional [Rect] in screen pixel coordinates to crop the compositor screenshot.
 *   If null and [autoCrop] is true, the screenshot is automatically cropped to the rendered
 *   foreground content via [cropForeground].
 * @param padding Extra padding in pixels to preserve around the cropped foreground bounds when
 *   [cropBounds] is null and [autoCrop] is true.
 * @param autoCrop When true and [cropBounds] is null, automatically crops out the background. Set
 *   to false to retrieve the full uncropped compositor frame.
 * @param timeout Maximum duration to wait for consecutive frames to stabilize. If frames have not
 *   stabilized by then, the latest frame is returned and a warning is logged.
 * @param pollInterval Delay between successive compositor captures.
 * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
 * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
 * @throws IllegalArgumentException if [cropBounds] does not overlap the screenshot.
 * @throws AssertionError if no screenshot could be captured within [timeout].
 */
@JvmOverloads
public fun captureSceneToBitmap(
    cropBounds: Rect? = null,
    padding: Int = 0,
    autoCrop: Boolean = true,
    timeout: Duration = 5.seconds,
    pollInterval: Duration = 100.milliseconds,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): Bitmap {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
    captureSceneWithPolling(
        uiAutomation = uiAutomation,
        maxWaitMs = max(MIN_SCREENSHOT_SCENE_SETTLE_WAIT_MS, timeout.inWholeMilliseconds),
    )
    val settledBitmap = waitForSettledSoftwareScreenshot(uiAutomation, timeout, pollInterval)
    if (cropBounds != null) {
        val cropped =
            try {
                crop(settledBitmap, cropBounds)
            } catch (t: Throwable) {
                settledBitmap.recycle()
                throw t
            }
        // createBitmap returns the source itself when the crop covers the whole bitmap.
        if (cropped !== settledBitmap) {
            settledBitmap.recycle()
        }
        return cropped
    }
    if (autoCrop) {
        val cropped =
            try {
                cropForeground(settledBitmap, padding = padding)
            } catch (t: Throwable) {
                settledBitmap.recycle()
                throw t
            }
        if (cropped !== settledBitmap) {
            settledBitmap.recycle()
        }
        return cropped
    }
    return settledBitmap
}

/**
 * Captures the 3D XR compositor output and automatically crops the screenshot to the rendered
 * content of [node], accounting for its position relative to the user's head pose.
 *
 * Delegates to [SpatialNode.captureToBitmap].
 *
 * @param node Target [SpatialNode] (such as [SpatialTask.spatialTransform],
 *   [SpatialTask.activitySpaceRoot], or an individual window leash/panel node) whose rendered
 *   content should be cropped.
 * @param padding Extra padding in pixels to preserve around the cropped content bounds.
 * @param timeout Maximum duration to wait for consecutive frames to stabilize. If frames have not
 *   stabilized by then, the latest frame is returned and a warning is logged.
 * @param pollInterval Delay between successive compositor captures.
 * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
 * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
 * @throws AssertionError if no screenshot could be captured within [timeout].
 */
@JvmOverloads
public fun captureSceneToBitmap(
    node: SpatialNode,
    padding: Int = 0,
    timeout: Duration = 5.seconds,
    pollInterval: Duration = 100.milliseconds,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): Bitmap =
    node.captureToBitmap(
        padding = padding,
        timeout = timeout,
        pollInterval = pollInterval,
        uiAutomation = uiAutomation,
    )

/**
 * Captures the spatial scene and 3D XR compositor output, resolves the target [SpatialNode] via
 * [nodeSelector], and automatically crops the screenshot to the rendered content of the selected
 * node.
 *
 * @param nodeSelector Selector invoked on the settled [SpatialScene] to identify the target
 *   [SpatialNode] to crop.
 * @param padding Extra padding in pixels to preserve around the cropped content bounds.
 * @param timeout Maximum duration to wait for consecutive frames to stabilize.
 * @param pollInterval Delay between successive compositor captures.
 * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
 * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
 * @throws IllegalArgumentException if [nodeSelector] returns `null` for the captured scene.
 * @throws AssertionError if no screenshot could be captured within [timeout].
 */
@JvmOverloads
public fun captureSceneToBitmap(
    nodeSelector: (SpatialScene) -> SpatialNode?,
    padding: Int = 0,
    timeout: Duration = 5.seconds,
    pollInterval: Duration = 100.milliseconds,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): Bitmap {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
    val scene =
        captureSceneWithPolling(
            uiAutomation = uiAutomation,
            maxWaitMs = max(MIN_SCREENSHOT_SCENE_SETTLE_WAIT_MS, timeout.inWholeMilliseconds),
        )
    val targetNode =
        requireNotNull(nodeSelector(scene)) {
            "nodeSelector returned null for captured SpatialScene (tasks=${scene.tasks})"
        }
    return targetNode.captureToBitmapInternal(
        padding = padding,
        timeout = timeout,
        pollInterval = pollInterval,
        uiAutomation = uiAutomation,
        pollScene = false,
    )
}
