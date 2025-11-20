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
@file:JvmName("UiObject2Ext")

package androidx.test.uiautomator

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.internal.TimeoutClock
import androidx.test.uiautomator.internal.notNull

/**
 * Performs a DFS on the accessibility tree starting from the node associated to this [UiObject2]
 * and returns the first node matching the given [block]. The node is returned as an [UiObject2]
 * that allows interacting with it. If the requested node doesn't exist, a
 * [ElementNotFoundException] is thrown. Internally it works searching periodically every
 * [pollIntervalMs].
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
public fun UiObject2.onElement(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2 =
    accessibilityNodeInfo.onElement(
        timeoutMs = timeoutMs,
        pollIntervalMs = pollIntervalMs,
        block = block,
    )

/**
 * Performs a DFS on the accessibility tree starting from the node associated to this [UiObject2]
 * and returns the first node matching the given [block]. The node is returned as an [UiObject2]
 * that allows interacting with it. If the requested node doesn't exist, null is returned.
 * Internally it works searching periodically every [pollIntervalMs].
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
public fun UiObject2.onElementOrNull(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2? =
    accessibilityNodeInfo.onElementOrNull(
        timeoutMs = timeoutMs,
        pollIntervalMs = pollIntervalMs,
        block = block,
    )

/**
 * Performs a DFS on the accessibility tree starting from the node associated to this [UiObject2]
 * and returns all the nodes matching the given [block]. This method stops waiting as soon as a
 * single node with the given condition is returned. The nodes returned are [UiObject2] that allow
 * interacting with them. Internally it works searching periodically every [pollIntervalMs].
 *
 * Example:
 * ```kotlin
 * node.onElements { isClass(Button::class.java) }
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
public fun UiObject2.onElements(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): List<UiObject2> =
    accessibilityNodeInfo.onElements(
        timeoutMs = timeoutMs,
        pollIntervalMs = pollIntervalMs,
        block = block,
    )

/**
 * Keeps scrolling until the given [block] condition is satisfied or until the given [timeoutMs].
 * Throws a [ElementNotFoundException] if the condition is not satisfied at the end of the timeout.
 *
 * Example:
 * ```kotlin
 * onElement { isScrollable }.scrollToElement(Direction.DOWN) { id == "button" }.click()
 * ```
 *
 * @param direction the scroll [Direction].
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a [UiObject2] that matches the given [block] condition.
 */
@JvmOverloads
public fun UiObject2.scrollToElement(
    direction: Direction,
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2 =
    scrollToElementOrNull(
            direction = direction,
            timeoutMs = timeoutMs,
            pollIntervalMs = pollIntervalMs,
            block = block,
        )
        .notNull(ElementNotFoundException())

/**
 * Keeps scrolling until the given [block] condition is satisfied or until the given [timeoutMs].
 * Returns null if the condition is not satisfied at the end of the timeout.
 *
 * Example:
 * ```kotlin
 * onElement { isScrollable }.scrollToElement(Direction.DOWN) { id == "button" }.click()
 * ```
 *
 * @param direction the scroll [Direction].
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a [UiObject2] that matches the given [block] condition.
 */
@JvmOverloads
public fun UiObject2.scrollToElementOrNull(
    direction: Direction,
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2 {
    val clock = TimeoutClock(timeoutMs = timeoutMs, sleepIntervalMs = pollIntervalMs)
    return scrollUntil(direction) {
            try {
                return@scrollUntil onElement(timeoutMs = 0, pollIntervalMs = 0, block)
            } catch (e: ElementNotFoundException) {
                if (clock.isTimeoutOrSleep()) throw e
                return@scrollUntil null
            }
        }
        .notNull(ElementNotFoundException())
}

/**
 * Waits for the node to become stable. A node is considered stable when it and its descendants have
 * not changed over an interval of time. Optionally also the node image can be checked. Internally
 * it works checking periodically that the internal properties of the node have not changed.
 *
 * @param stableTimeoutMs a timeout for the wait operation, to ensure not waiting forever for
 *   stability.
 * @param stableIntervalMs the interval during which the node should not be changing, in order to be
 *   considered stable.
 * @param stablePollIntervalMs specifies how often the ui should be checked for changes.
 * @param requireStableScreenshot specifies if also the bitmap of the node should not change over
 *   the specified [stableIntervalMs]. Note that this won't work with elements that change
 *   constantly, like a video player.
 * @return a [androidx.test.uiautomator.StableResult] containing the latest acquired element
 *   hierarchy and screenshot, and a flag indicating if the node was stable before timeout. The flag
 *   [StableResult.isTimeout] is set to false if the node was stable before the timeout expired,
 *   true otherwise.
 */
@JvmOverloads
public fun UiObject2.waitForStable(
    stableTimeoutMs: Long = 3000,
    stableIntervalMs: Long = 500,
    stablePollIntervalMs: Long = 50,
    requireStableScreenshot: Boolean = true,
): StableResult =
    accessibilityNodeInfo.waitForStable(
        stableTimeoutMs = stableTimeoutMs,
        stablePollIntervalMs = stablePollIntervalMs,
        stableIntervalMs = stableIntervalMs,
        requireStableScreenshot = requireStableScreenshot,
    )

/**
 * Takes a screenshot of the screen that contains the accessibility node associated to this
 * [UiObject2] and cuts only the area covered by it.
 *
 * @return a bitmap containing the image of the node associated to this [UiObject2].
 */
public fun UiObject2.takeScreenshot(): Bitmap = accessibilityNodeInfo.takeScreenshot()
