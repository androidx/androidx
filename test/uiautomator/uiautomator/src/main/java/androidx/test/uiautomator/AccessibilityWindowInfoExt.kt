/*
 * Copyright 2025 The Android Open Source Project
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
@file:JvmName("AccessibilityWindowInfoExt")

package androidx.test.uiautomator

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.uiautomator.internal.TimeoutClock
import androidx.test.uiautomator.internal.displayManager
import androidx.test.uiautomator.internal.nowMs
import androidx.test.uiautomator.internal.takeScreenshotBitmap
import androidx.test.uiautomator.internal.takeViewNodeTree
import androidx.test.uiautomator.internal.uiAutomation
import androidx.test.uiautomator.internal.waitForStableInternal

/**
 * Takes a screenshot of the screen that contains this window and cuts only the area covered by it.
 *
 * @return a bitmap containing the image of this node.
 */
public fun AccessibilityWindowInfo.takeScreenshot(): Bitmap =
    takeScreenshotBitmap(nodeBounds = Rect().apply { getBoundsInScreen(this) })

/**
 * Waits for the root node to become available in this window.
 *
 * @param timeoutMs a timeout for the root node to become available.
 * @param sleepIntervalMs a interval to wait before retrying checking if the node is available.
 * @param clearCache whether the accessibility nodes cache should be cleared when checking.
 * @return the root node for this window.
 */
@JvmOverloads
public fun AccessibilityWindowInfo.waitForRoot(
    timeoutMs: Long = 10000L,
    sleepIntervalMs: Long = 100L,
    clearCache: Boolean = true,
): AccessibilityNodeInfo {

    // Clear accessibility cache: some nodes are cached and may not get updated.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && clearCache) {
        uiAutomation.clearCache()
    }

    val clock = TimeoutClock(timeoutMs = timeoutMs, sleepIntervalMs = sleepIntervalMs)
    while (root == null) {
        if (clock.isTimeoutOrSleep()) {
            throw IllegalStateException("Cannot acquire root element in this window.")
        }
    }

    return root
}

/**
 * Waits for the root node of this window to become stable. A node is considered stable when it and
 * its descendants have not changed over an interval of time. Optionally also the node image can be
 * checked. Internally it works checking periodically that the internal properties of the node have
 * not changed.
 *
 * @param stableTimeoutMs a timeout for the wait operation, to ensure not waiting forever for
 *   stability.
 * @param stableIntervalMs the interval during which the node should not be changing, in order to be
 *   considered stable.
 * @param stablePollIntervalMs specifies how often the ui should be checked for changes.
 * @param requireStableScreenshot specifies if also the bitmap of the node should not change over
 *   the specified [stableIntervalMs]. Note that this won't work with elements that change
 *   constantly, like a video player.
 * @return a [StableResult] containing the latest acquired element hierarchy and screenshot, and a
 *   flag indicating if the node was stable before timeout. The flag [StableResult.isTimeout] is set
 *   to false if the node was stable before the timeout expired, true otherwise.
 */
@JvmOverloads
public fun AccessibilityWindowInfo.waitForStable(
    stableTimeoutMs: Long = 3000,
    stableIntervalMs: Long = 500,
    stablePollIntervalMs: Long = 50,
    requireStableScreenshot: Boolean = true,
): StableResult {

    // Here we wait for a non-null root node.
    // The time taken is accounted in the timeout for stable.
    val startMs = nowMs()
    val root = waitForRoot(timeoutMs = stableTimeoutMs, sleepIntervalMs = stablePollIntervalMs)
    val elapsedMs = nowMs() - startMs
    val remainingMs = stableTimeoutMs - elapsedMs

    val displayRect =
        Rect().apply {
            val displayId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) displayId
                else Display.DEFAULT_DISPLAY
            @Suppress("DEPRECATION") displayManager.getDisplay(displayId).getRectSize(this)
        }

    return waitForStableInternal(
        stableTimeoutMs = remainingMs,
        stablePollIntervalMs = stablePollIntervalMs,
        stableIntervalMs = stableIntervalMs,
        bitmapProvider = { if (requireStableScreenshot) takeScreenshot() else null },
        rootElementNodeProvider = { takeViewNodeTree(root = root, displayRect = displayRect) },
    )
}

/**
 * Performs a DFS on the accessibility tree starting from the root node of this window and returns
 * the first node matching the given [block]. The node is returned as an [UiObject2] that allows
 * interacting with it. If the requested node doesn't exist, a [ElementNotFoundException] is thrown.
 *
 * Example:
 * ```kotlin
 * onElement { textAsString == "Search" }.click()
 * ```
 *
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a [UiObject2] from a node that matches the given [block] condition.
 */
@JvmOverloads
public fun AccessibilityWindowInfo.onElement(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2 =
    waitForRoot(timeoutMs = timeoutMs, sleepIntervalMs = pollIntervalMs)
        .onElement(timeoutMs = timeoutMs, pollIntervalMs = pollIntervalMs, block = block)

/**
 * Performs a DFS on the accessibility tree starting from the root node of this window and returns
 * the first node matching the given [block]. The node is returned as an [UiObject2] that allows
 * interacting with it. If the requested node doesn't exist, null is returned. Internally it works
 * searching periodically every [pollIntervalMs].
 *
 * Example:
 * ```kotlin
 * onElement { textAsString == "Search" }.click()
 * ```
 *
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a [UiObject2] from a node that matches the given [block] condition or null.
 */
@JvmOverloads
public fun AccessibilityWindowInfo.onElementOrNull(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2? =
    waitForRoot(timeoutMs = timeoutMs, sleepIntervalMs = pollIntervalMs)
        .onElementOrNull(timeoutMs = timeoutMs, pollIntervalMs = pollIntervalMs, block = block)

/**
 * Performs a DFS on the accessibility tree starting from the root node of this window and returns
 * all the nodes matching the given [block]. This method stops waiting as soon as a single node with
 * the given condition is returned. The nodes returned are [UiObject2] that allow interacting with
 * them. Internally it works searching periodically every [pollIntervalMs].
 *
 * Example:
 * ```kotlin
 * node.onElements { className == Button::class.java.name }
 * ```
 *
 * If multiple nodes are expected but they appear at different times, it's recommended to call
 * [androidx.test.uiautomator.waitForStable] before, to ensure any operation is complete.
 *
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a list of [UiObject2] from nodes that matches the given [block] condition.
 */
@JvmOverloads
public fun AccessibilityWindowInfo.onElements(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): List<UiObject2> =
    waitForRoot(timeoutMs = timeoutMs, sleepIntervalMs = pollIntervalMs)
        .onElements(timeoutMs = timeoutMs, pollIntervalMs = pollIntervalMs, block = block)
