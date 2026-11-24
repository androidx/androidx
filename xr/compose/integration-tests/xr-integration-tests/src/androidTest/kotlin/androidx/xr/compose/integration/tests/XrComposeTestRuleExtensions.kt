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

package androidx.xr.compose.integration.tests

import android.graphics.Rect
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as createV2AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.xr.compose.platform.SpaceRequestResult
import androidx.xr.compose.platform.requestFullSpace as requestActivityFullSpace
import androidx.xr.compose.platform.requestHomeSpace as requestActivityHomeSpace
import androidx.xr.testutils.SnapshotXrSpace
import androidx.xr.testutils.XrSessionRule
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.performSpatialInteraction
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** Timeout applied when waiting for a space mode transition to complete. */
internal val SPACE_MODE_TIMEOUT = 5.seconds

/**
 * [AndroidComposeTestRule] whose activity and [androidx.xr.runtime.Session] come from an
 * [XrSessionRule], created via [createXrComposeRule].
 *
 * Use it only when the test body needs the session outside composition, for example to:
 * * call `Session.configure` before `setContent` (such as enabling `DeviceTrackingMode.SPATIAL` for
 *   `rotateToLookAtUser`), read `ArDevice` state, or run SceneCore APIs via
 *   [XrSessionRule.onMainThread];
 * * capture with [XrSessionRule.captureToBitmap], which first waits for the host activity's spatial
 *   task to settle.
 *
 * Otherwise use `createAndroidComposeRule<ComponentActivity>()`: composables can read the session
 * via `LocalSession.current`, [requestFullSpace] works on any [AndroidComposeTestRule], and
 * screenshots can use [androidx.xr.testutils.captureSceneToBitmap] or the Subspace
 * `captureToBitmap` helpers.
 */
internal typealias XrComposeTestRule =
    AndroidComposeTestRule<
        XrSessionRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>,
        ComponentActivity,
    >

/**
 * Creates an [XrComposeTestRule] that launches a [ComponentActivity] through an internal
 * [XrSessionRule], so Compose UI and the XR [androidx.xr.runtime.Session] share one activity
 * lifecycle.
 *
 * Register only the returned rule with `@get:Rule`, and reach the session via [sessionRule].
 */
internal fun createXrComposeRule(): XrComposeTestRule =
    createV2AndroidComposeTestRule(
        activityRule = createXrSessionRule(),
        activityProvider = { it.activity },
    )

/**
 * [XrSessionRule] backing this rule, for [XrSessionRule.session] and [XrSessionRule.onMainThread].
 */
internal val XrComposeTestRule.sessionRule:
    XrSessionRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
    get() = activityRule

/**
 * Requests Full Space Mode from the instrumentation thread, asserts [SpaceRequestResult.Success]
 * within [timeout], and calls [AndroidComposeTestRule.waitForIdle].
 *
 * @throws AssertionError if the request does not succeed within [timeout]
 */
internal fun AndroidComposeTestRule<*, out ComponentActivity>.requestFullSpace(
    timeout: Duration = SPACE_MODE_TIMEOUT
) {
    requestSpaceMode(modeName = "Full Space Mode", timeout = timeout) { requestActivityFullSpace() }
}

/**
 * Requests Home Space Mode from the instrumentation thread, asserts [SpaceRequestResult.Success]
 * within [timeout], and calls [AndroidComposeTestRule.waitForIdle].
 *
 * @throws AssertionError if the request does not succeed within [timeout]
 */
internal fun AndroidComposeTestRule<*, out ComponentActivity>.requestHomeSpace(
    timeout: Duration = SPACE_MODE_TIMEOUT
) {
    requestSpaceMode(modeName = "Home Space Mode", timeout = timeout) { requestActivityHomeSpace() }
}

private fun AndroidComposeTestRule<*, out ComponentActivity>.requestSpaceMode(
    modeName: String,
    timeout: Duration,
    request: suspend ComponentActivity.() -> SpaceRequestResult,
) {
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "$modeName request must be called from the instrumentation thread, not the main thread."
    }
    // @XrDeviceTest already filters out devices without spatial support, so any result other than
    // Success indicates a regression and fails the test instead of skipping it.
    val spaceResult =
        runBlocking(Dispatchers.Main.immediate) {
            withTimeoutOrNull(timeout) { activity.request() }
        } ?: throw AssertionError("Timed out after $timeout requesting $modeName")
    when (spaceResult) {
        SpaceRequestResult.Success -> Unit
        SpaceRequestResult.Unsupported ->
            throw AssertionError("$modeName is not supported on this device")
        is SpaceRequestResult.Error ->
            throw AssertionError("Failed to enter $modeName", spaceResult.cause)
        else -> throw AssertionError("Unexpected result requesting $modeName: $spaceResult")
    }
    waitForIdle()
}

/**
 * Aims a 3D spatial pointer at the window bounds of 2D [node] inside [panelNode], performs a
 * spatial click (`select()`), and calls [AndroidComposeTestRule.waitForIdle].
 *
 * Unlike 2D `SemanticsNodeInteraction.performClick()`, this routes through the XR compositor input
 * pipeline so window focus transfers to [panelNode]. Call `resetSpatialInteraction()` in `@After`.
 *
 * @throws IllegalStateException if called on the main thread
 */
internal fun AndroidComposeTestRule<*, out ComponentActivity>.performSpatialClickOn2dNode(
    node: SemanticsNodeInteraction,
    panelNode: SnapshotXrSpace,
) {
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "performSpatialClickOn2dNode must be called from the instrumentation thread, not the main thread."
    }
    val bounds = node.fetchSemanticsNode().boundsInWindow
    val rect =
        Rect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt(),
        )
    panelNode.performSpatialInteraction {
        pointAt(boundsInWindow = rect, targetNode = panelNode)
        select()
    }
    waitForIdle()
}
