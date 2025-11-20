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
@file:JvmName("AccessibilityNodeInfoExt")

package androidx.test.uiautomator

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.internal.displayManager
import androidx.test.uiautomator.internal.findElements
import androidx.test.uiautomator.internal.notNull
import androidx.test.uiautomator.internal.takeScreenshotBitmap
import androidx.test.uiautomator.internal.takeViewNodeTree
import androidx.test.uiautomator.internal.uiDevice
import androidx.test.uiautomator.internal.waitForStableInternal

/**
 * Returns the bounds of this node in screen coordinates. This method is a shortcut for
 * [AccessibilityNodeInfo.getBoundsInScreen].
 *
 * @return a [Rect] with the bounds of this node within the screen limits.
 */
public fun AccessibilityNodeInfo.boundsInScreen(): Rect = Rect().apply { getBoundsInScreen(this) }

/**
 * Returns all the immediate children of this node. To return all the descendants (including
 * children of children , children of children of children and so on use[
 * [AccessibilityNodeInfo.descendants].
 *
 * @param block an optional predicate to filter the node children
 * @return the list of children of this node, with the given filter if specified.
 */
@JvmOverloads
public fun AccessibilityNodeInfo.children(
    block: (AccessibilityNodeInfo) -> Boolean = { true }
): List<AccessibilityNodeInfo> = (0 until childCount).mapNotNull { getChild(it) }.filter(block)

/**
 * Performs a DFS on the accessibility tree starting from this node and returns all the descendants
 * discovered recursively. Optionally a filter can be specified. Note that this differs from
 * [AccessibilityNodeInfo.children()] because this will return also children of children, children
 * of children of children and so on.
 *
 * @param filterBlock an optional predicate to filter the node children
 * @return the list of all the descendants of this node, with the given filter if specified.
 */
@JvmOverloads
public fun AccessibilityNodeInfo.descendants(
    filterBlock: (AccessibilityNodeInfo) -> Boolean = { true }
): List<AccessibilityNodeInfo> {
    val collector = mutableListOf<AccessibilityNodeInfo>()
    fun dfs(node: AccessibilityNodeInfo) {
        if (filterBlock(node)) collector.add(node)
        node.children().forEach { dfs(it) }
    }
    dfs(this)
    return collector
}

/**
 * Returns all the siblings of the this node.
 *
 * @param filterBlock an optional predicate to filter the node siblings
 * @return the list of the siblings of this node, with the given filter if specified.
 */
@JvmOverloads
public fun AccessibilityNodeInfo.siblings(
    filterBlock: (AccessibilityNodeInfo) -> Boolean = { true }
): List<AccessibilityNodeInfo> = parent.children().filter(filterBlock).minus(this)

/**
 * Takes a screenshot of the screen that contains this node and cuts only the area covered by it.
 *
 * @return a bitmap containing the image of this node.
 */
public fun AccessibilityNodeInfo.takeScreenshot(): Bitmap =
    takeScreenshotBitmap(nodeBounds = Rect().apply { getBoundsInScreen(this) })

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
public fun AccessibilityNodeInfo.waitForStable(
    stableTimeoutMs: Long = 3000,
    stableIntervalMs: Long = 500,
    stablePollIntervalMs: Long = 50,
    requireStableScreenshot: Boolean = true,
): StableResult {
    val displayRect =
        Rect().apply {
            val displayId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.displayId
                else Display.DEFAULT_DISPLAY
            @Suppress("DEPRECATION") displayManager.getDisplay(displayId).getRectSize(this)
        }

    return waitForStableInternal(
        stableTimeoutMs = stableTimeoutMs,
        stablePollIntervalMs = stablePollIntervalMs,
        stableIntervalMs = stableIntervalMs,
        bitmapProvider = { if (requireStableScreenshot) takeScreenshot() else null },
        rootElementNodeProvider = { takeViewNodeTree(root = this, displayRect = displayRect) },
    )
}

/**
 * Performs a DFS on the accessibility tree starting from this node and returns the first node
 * matching the given [block]. The node is returned as an [UiObject2] that allows interacting with
 * it. If the requested node doesn't exist, a [androidx.test.uiautomator.ElementNotFoundException]
 * is thrown. Internally it works searching periodically every [pollIntervalMs].
 *
 * Example:
 * ```kotlin
 * node.onElement { textAsString == "Search" }.click()
 * ```
 *
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a [UiObject2] from a node that matches the given [block] condition.
 */
@JvmOverloads
public fun AccessibilityNodeInfo.onElement(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2 =
    onElementOrNull(timeoutMs = timeoutMs, pollIntervalMs = pollIntervalMs, block = block)
        .notNull(ElementNotFoundException())

/**
 * Performs a DFS on the accessibility tree starting from this node and returns the first node
 * matching the given [block]. The node is returned as an [UiObject2] that allows interacting with
 * it. If the requested node doesn't exist, null is returned. Internally it works searching
 * periodically every [pollIntervalMs].
 *
 * Example:
 * ```kotlin
 * node.onElement { textAsString == "Search" }.click()
 * ```
 *
 * @param timeoutMs a timeout to find the element that satisfies the given condition.
 * @param pollIntervalMs an interval to wait before rechecking the accessibility tree for updates.
 * @param block a block that specifies a condition on the node to find.
 * @return a [UiObject2] from a node that matches the given [block] condition or null.
 */
@JvmOverloads
public fun AccessibilityNodeInfo.onElementOrNull(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): UiObject2? =
    findElements(
            shouldStop = { it.size == 1 },
            block = block,
            timeoutMs = timeoutMs,
            pollIntervalMs = pollIntervalMs,
            rootNodeBlock = { listOf(this) },
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
public fun AccessibilityNodeInfo.onElements(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    block: AccessibilityNodeInfo.() -> (Boolean),
): List<UiObject2> =
    findElements(
            timeoutMs = timeoutMs,
            pollIntervalMs = pollIntervalMs,
            shouldStop = { false },
            block = block,
            rootNodeBlock = { listOf(this) },
        )
        .mapNotNull { it.toUiObject2() }

internal fun AccessibilityNodeInfo.toUiObject2(): UiObject2? =
    UiObject2.create(uiDevice, BySelector(), this)

/**
 * Returns this node's element id resource name without the resource namespace, i.e. only the
 * portion after the "/" character. If the element id is not specified, then it returns `null`.
 */
public fun AccessibilityNodeInfo.simpleViewResourceName(): String? =
    viewIdResourceName?.substringAfter("/")

/**
 * Returns this node's text as a string. This should always be preferred to
 * [AccessibilityNodeInfo.text] that instead returns a [CharSequence], that might be either a
 * [String] or a [android.text.SpannableString]. If a text is not specified, then it returns `null`.
 *
 * Usage:
 * ```kotlin
 * onElement { textAsString() == "Some text" }.click()
 * ```
 */
public fun AccessibilityNodeInfo.textAsString(): String? = text?.toString()

/**
 * Returns whether this node class name is the same of the given class.
 *
 * Usage:
 * ```kotlin
 * onElement { isClass(Button::class.java) }.click()
 * ```
 */
public fun AccessibilityNodeInfo.isClass(cls: Class<*>): Boolean = cls.name == this.className
