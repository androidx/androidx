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
@file:JvmName("UiDeviceExt")

package androidx.test.uiautomator

import android.app.Instrumentation
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.uiautomator.internal.TimeoutClock
import androidx.test.uiautomator.internal.displayManager
import androidx.test.uiautomator.internal.findElements
import androidx.test.uiautomator.internal.keyCharacterMap
import androidx.test.uiautomator.internal.notNull
import androidx.test.uiautomator.internal.takeViewNodeTree
import androidx.test.uiautomator.internal.uiDevice
import androidx.test.uiautomator.internal.waitForStableInternal

/** Returns all the windows on all the displays. */
public fun UiDevice.windows(): List<AccessibilityWindowInfo> = windowRoots.map { it.window }

/** Returns the active window. */
public fun UiDevice.activeWindow(): AccessibilityWindowInfo = waitForRootInActiveWindow().window

/**
 * Waits for the root node to become available in this window.
 *
 * @param timeoutMs a timeout for the root node to become available.
 * @param sleepIntervalMs a interval to wait before retrying checking if the node is available.
 * @param clearCache whether the accessibility nodes cache should be cleared when checking.
 * @return the root node for this window.
 * @throws [IllegalStateException] if the root node in the active window cannot be acquired before
 *   the give timeout [timeoutMs].
 */
@JvmOverloads
public fun UiDevice.waitForRootInActiveWindow(
    timeoutMs: Long = 10000L,
    sleepIntervalMs: Long = 100L,
    clearCache: Boolean = true,
): AccessibilityNodeInfo {

    val clock = TimeoutClock(timeoutMs = timeoutMs, sleepIntervalMs = sleepIntervalMs)
    var node: AccessibilityNodeInfo? = null
    var window: AccessibilityWindowInfo? = null

    while (true) {

        // Check if both root node and window for it exist
        if (node != null && window != null) {
            return node
        }

        // Clear accessibility cache: some nodes are cached and may not get updated.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && clearCache) {
            uiAutomation.clearCache()
        }

        // If this was a timeout, throw exception
        if (clock.isTimeoutOrSleep()) {
            throw IllegalStateException("Cannot acquire root element in active window.")
        }

        // Otherwise try again to retrieve node and window
        node = uiAutomation.rootInActiveWindow
        window = node?.window
    }
}

/**
 * Waits for the root node of the active window to become stable.
 *
 * A node is considered stable when it and its descendants have not changed over an interval of
 * time. Optionally also the node image can be checked. Internally it works checking periodically
 * that the internal properties of the node have not changed.
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
public fun UiDevice.waitForStableInActiveWindow(
    stableTimeoutMs: Long = 3000,
    stableIntervalMs: Long = 500,
    stablePollIntervalMs: Long = 50,
    requireStableScreenshot: Boolean = true,
): StableResult =
    waitForStableInternal(
        stableTimeoutMs = stableTimeoutMs,
        stablePollIntervalMs = stablePollIntervalMs,
        stableIntervalMs = stableIntervalMs,
        bitmapProvider = {
            if (requireStableScreenshot) waitForRootInActiveWindow().takeScreenshot() else null
        },
        rootElementNodeProvider = {
            val node = waitForRootInActiveWindow()
            val displayRect =
                Rect().apply {
                    val displayId =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) node.window.displayId
                        else Display.DEFAULT_DISPLAY
                    @Suppress("DEPRECATION") displayManager.getDisplay(displayId).getRectSize(this)
                }
            takeViewNodeTree(root = node, displayRect = displayRect)
        },
    )

/**
 * Types the given [text] string simulating key press through [Instrumentation.sendKeySync]. This is
 * similar to tapping the keys on a virtual keyboard and will trigger the same listeners in the
 * target app, as opposed to [AccessibilityNodeInfo.setText] that programmaticaly sets the given
 * text in the target node.
 *
 * @param text the text to type.
 */
public fun UiDevice.type(text: String) {
    text
        .flatMap { keyCharacterMap.getEvents(charArrayOf(it)).toList() }
        .forEach { instrumentation.sendKeySync(it) }
}

/**
 * Similar to [type] but presses the delete key for the given [count] times.
 *
 * @param count how many times the press delete key should be pressed.
 */
public fun UiDevice.pressDelete(count: Int) {
    (0 until count).map { KeyEvent.KEYCODE_DEL }.toIntArray().let { uiDevice.pressKeyCodes(it, 0) }
}

/**
 * Waits for an application to become visible. Note that internally it checks if an accessibility
 * node with the given [appPackageName] exists in the accessibility tree.
 *
 * @param appPackageName the package name of the app to wait for. By default is the target app
 *   package name.
 * @param timeoutMs a timeout for the app to become visible.
 * @return whether the app became visible in the given timeout.
 */
@JvmOverloads
public fun UiDevice.waitForAppToBeVisible(
    appPackageName: String,
    timeoutMs: Long = 10000L,
): Boolean = onElementOrNull(timeoutMs = timeoutMs) { packageName == appPackageName } != null

/**
 * Performs a DFS on the accessibility tree starting from the root node in the active window and
 * returns the first node matching the given [block]. The node is returned as an [UiObject2] that
 * allows interacting with it. If the requested node doesn't exist, a [ElementNotFoundException] is
 * thrown. Internally it works searching periodically every [pollIntervalMs].
 *
 * Example:
 * ```kotlin
 * uiDevice.onElement { textAsString == "Search" }.click()
 * ```
 *
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a [UiObject2] from a node that matches the given [block] condition.
 */
@JvmOverloads
public fun UiDevice.onElement(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2 =
    onElementOrNull(timeoutMs = timeoutMs, pollIntervalMs = pollIntervalMs, block = block)
        .notNull(ElementNotFoundException())

/**
 * Performs a DFS on the accessibility tree starting from the root node in the active window and
 * returns the first node matching the given [block]. The node is returned as an [UiObject2] that
 * allows interacting with it. If the requested node doesn't exist, null is returned. Internally it
 * works searching periodically every [pollIntervalMs].
 *
 * Example:
 * ```kotlin
 * uiDevice.onElement { textAsString == "Search" }.click()
 * ```
 *
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a [UiObject2] from a node that matches the given [block] condition or null.
 */
@JvmOverloads
public fun UiDevice.onElementOrNull(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2? =
    findElements(
            shouldStop = { it.size == 1 },
            block = block,
            timeoutMs = timeoutMs,
            pollIntervalMs = pollIntervalMs,
            rootNodeBlock = { windowRoots },
        )
        .firstOrNull()
        ?.toUiObject2()

/**
 * Performs a DFS on the accessibility tree starting from this node and returns all the nodes
 * matching the given [block]. This method stops waiting as soon as a single node with the given
 * condition is returned. The nodes returned are [UiObject2] that allow interacting with them.
 * Internally it works searching periodically every [pollIntervalMs].
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
public fun UiDevice.onElements(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): List<UiObject2> =
    findElements(
            shouldStop = { false },
            block = block,
            timeoutMs = timeoutMs,
            pollIntervalMs = pollIntervalMs,
            rootNodeBlock = { windowRoots },
        )
        .mapNotNull { it.toUiObject2() }
