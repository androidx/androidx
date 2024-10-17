/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.spatial

import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import kotlin.math.min
import kotlinx.coroutines.DisposableHandle

internal class ThrottledCallbacks {

    inner class Entry(
        val id: Int,
        val throttleMillis: Long,
        val debounceMillis: Long,
        val node: DelegatableNode,
        val callback: (RectInfo) -> Unit,
    ) : DisposableHandle {

        var next: Entry? = null

        var topLeft: Long = 0
        var bottomRight: Long = 0
        var lastInvokeMillis: Long = -throttleMillis
        var lastUninvokedFireMillis: Long = -1

        override fun dispose() {
            map.multiRemove(id, this)
        }

        fun fire(
            topLeft: Long,
            bottomRight: Long,
            windowOffset: IntOffset,
            screenOffset: IntOffset,
            viewToWindowMatrix: Matrix?,
        ) {
            val rect =
                rectInfoFor(
                    node,
                    topLeft,
                    bottomRight,
                    windowOffset,
                    screenOffset,
                    viewToWindowMatrix
                )
            if (rect != null) {
                callback(rect)
            }
        }
    }

    val map = mutableIntObjectMapOf<Entry>()

    // We can use this to schedule a "triggerDebounced" call. If it is -1, then nothing
    // needs to be scheduled.
    var minDebounceDeadline: Long = -1
    var windowOffset: IntOffset = IntOffset.Zero
    var screenOffset: IntOffset = IntOffset.Zero
    var viewToWindowMatrix: Matrix? = null

    fun updateOffsets(screen: IntOffset, window: IntOffset, matrix: Matrix?): Boolean {
        var updated = false
        if (window != windowOffset) {
            windowOffset = window
            updated = true
        }
        if (screen != screenOffset) {
            screenOffset = screen
            updated = true
        }
        if (matrix != null) {
            viewToWindowMatrix = matrix
            updated = true
        }
        return updated
    }

    private fun roundDownToMultipleOf8(x: Long): Long {
        return (x shr 3) shl 3
    }

    fun register(
        id: Int,
        throttleMs: Long,
        debounceMs: Long,
        node: DelegatableNode,
        callback: (RectInfo) -> Unit,
    ): DisposableHandle {
        // If zero is set for debounce, we use throttle in its place. This guarantees that
        // consumers will get the value where the node "settled".
        val debounceToUse = if (debounceMs == 0L) throttleMs else debounceMs

        return map.multiPut(id, Entry(id, throttleMs, debounceToUse, node, callback))
    }

    // We call this when a layout node with `semanticsId = id` changes it's global bounds. For
    // throttled callbacks this may cause the callback to get invoked, for debounced nodes it
    // updates the deadlines
    fun fire(id: Int, topLeft: Long, bottomRight: Long, currentMillis: Long) {
        map.runFor(id) { entry ->
            val lastInvokeMillis = entry.lastInvokeMillis
            val throttleMillis = entry.throttleMillis
            val debounceMillis = entry.debounceMillis
            val pastThrottleDeadline = currentMillis - lastInvokeMillis >= throttleMillis
            val zeroDebounce = debounceMillis == 0L
            val zeroThrottle = throttleMillis == 0L

            entry.topLeft = topLeft
            entry.bottomRight = bottomRight

            // There are essentially 3 different cases that we need to handle here:

            // 1. throttle = 0, debounce = 0
            //      -> always invoke immediately
            // 2. throttle = 0, debounce > 0
            //      -> set deadline to <debounce> milliseconds from now
            // 3. throttle > 0, debounce > 0
            //      -> invoke if we haven't invoked for <throttle> milliseconds, otherwise, set the
            //         deadline to <debounce>

            // Note that the `throttle > 0, debounce = 0` case is not possible, since we use the
            // throttle value as a debounce value in that case.

            val canInvoke = (!zeroDebounce && !zeroThrottle) || zeroDebounce

            if (pastThrottleDeadline && canInvoke) {
                entry.lastUninvokedFireMillis = -1
                entry.lastInvokeMillis = currentMillis
                entry.fire(topLeft, bottomRight, windowOffset, screenOffset, viewToWindowMatrix)
            } else if (!zeroDebounce) {
                entry.lastUninvokedFireMillis = currentMillis
                val currentMinDeadline = minDebounceDeadline
                val thisDeadline = currentMillis + debounceMillis
                if (currentMinDeadline > 0 && thisDeadline < currentMinDeadline) {
                    minDebounceDeadline = currentMinDeadline
                }
            }
        }
    }

    fun fireAll(currentMillis: Long) {
        val windowOffset = windowOffset
        val screenOffset = screenOffset
        val viewToWindowMatrix = viewToWindowMatrix
        map.multiForEach { entry ->
            val lastInvokeMillis = entry.lastInvokeMillis
            val throttleOkay = currentMillis - lastInvokeMillis > entry.throttleMillis
            val debounceOkay = entry.debounceMillis == 0L
            entry.lastUninvokedFireMillis = currentMillis
            if (throttleOkay && debounceOkay) {
                entry.lastInvokeMillis = currentMillis
                entry.fire(
                    entry.topLeft,
                    entry.bottomRight,
                    windowOffset,
                    screenOffset,
                    viewToWindowMatrix
                )
            }
            if (!debounceOkay) {
                val currentMinDeadline = minDebounceDeadline
                val thisDeadline = currentMillis + entry.debounceMillis
                if (currentMinDeadline > 0 && thisDeadline < currentMinDeadline) {
                    minDebounceDeadline = currentMinDeadline
                }
            }
        }
    }

    // We call this to invoke any debounced callbacks that have passed their deadline. This could
    // be done every frame, or on some other interval. This means the precision of the debouncing
    // is less, but it would reduce the overhead of all of this scheduling.
    fun triggerDebounced(currentMillis: Long) {
        if (minDebounceDeadline > currentMillis) return
        val windowOffset = windowOffset
        val screenOffset = screenOffset
        val viewToWindowMatrix = viewToWindowMatrix
        var minDeadline = Long.MAX_VALUE
        map.multiForEach {
            if (it.debounceMillis > 0 && it.lastUninvokedFireMillis > 0) {
                if (currentMillis - it.lastUninvokedFireMillis > it.debounceMillis) {
                    it.lastInvokeMillis = currentMillis
                    it.lastUninvokedFireMillis = -1
                    val topLeft = it.topLeft
                    val bottomRight = it.bottomRight
                    it.fire(topLeft, bottomRight, windowOffset, screenOffset, viewToWindowMatrix)
                } else {
                    minDeadline = min(minDeadline, it.lastUninvokedFireMillis + it.debounceMillis)
                }
            }
        }
        minDebounceDeadline = if (minDeadline == Long.MAX_VALUE) -1 else minDeadline
    }

    private inline fun MutableIntObjectMap<Entry>.multiForEach(block: (Entry) -> Unit) {
        forEachValue { it ->
            var entry: Entry? = it
            while (entry != null) {
                block(entry)
                entry = entry.next
            }
        }
    }

    private inline fun MutableIntObjectMap<Entry>.runFor(id: Int, block: (Entry) -> Unit) {
        var entry: Entry? = get(id)
        while (entry != null) {
            block(entry)
            entry = entry.next
        }
    }

    private fun MutableIntObjectMap<Entry>.multiPut(key: Int, value: Entry): Entry {
        var entry: Entry = getOrPut(key) { value }
        if (entry !== value) {
            while (entry.next != null) {
                entry = entry.next!!
            }
            entry.next = value
        }
        return value
    }

    private fun MutableIntObjectMap<Entry>.multiRemove(key: Int, value: Entry): Boolean {
        return when (val result = remove(key)) {
            null -> false
            value -> {
                val next = value.next
                value.next = null
                if (next != null) {
                    put(key, next)
                }
                true
            }
            else -> {
                put(key, result)
                var entry = result
                while (entry != null) {
                    val next = entry.next ?: return false
                    if (next === value) {
                        entry.next = value.next
                        value.next = null
                        break
                    }
                    entry = entry.next
                }
                true
            }
        }
    }
}

internal fun rectInfoFor(
    node: DelegatableNode,
    topLeft: Long,
    bottomRight: Long,
    windowOffset: IntOffset,
    screenOffset: IntOffset,
    viewToWindowMatrix: Matrix?,
): RectInfo? {
    val coordinator = node.requireCoordinator(Nodes.Layout)
    val layoutNode = node.requireLayoutNode()
    if (!layoutNode.isPlaced) return null
    // this is the outer-rect of the layout node. we may need to transform this
    // rectangle to be accurate up to the modifier node requesting the callback. Most
    // of the time this will be the outer-most rectangle, so no transformation will be
    // needed, and we should optimize for that fact, but we need to make sure that it
    // is accurate.
    val needsTransform = layoutNode.outerCoordinator !== coordinator
    return if (needsTransform) {
        val transformed = layoutNode.outerCoordinator.coordinates.localBoundingBoxOf(coordinator)
        RectInfo(
            transformed.topLeft.round().packedValue,
            transformed.bottomRight.round().packedValue,
            windowOffset,
            screenOffset,
            viewToWindowMatrix,
        )
    } else
        RectInfo(
            topLeft,
            bottomRight,
            windowOffset,
            screenOffset,
            viewToWindowMatrix,
        )
}
