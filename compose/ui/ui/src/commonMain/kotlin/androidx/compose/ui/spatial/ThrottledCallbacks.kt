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
import androidx.compose.ui.node.DelegatableNode.RegistrationHandle
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import kotlin.math.min

internal class ThrottledCallbacks {

    /**
     * Entry for a throttled callback for [RelativeLayoutBounds] associated to the given [node].
     *
     * Supports a linked-list structure for multiple callbacks on the same [node] through [next].
     */
    inner class Entry(
        val id: Int,
        val throttleMillis: Long,
        val debounceMillis: Long,
        val node: DelegatableNode,
        val callback: (RelativeLayoutBounds) -> Unit,
    ) : RegistrationHandle {

        var next: Entry? = null

        var topLeft: Long = 0
        var bottomRight: Long = 0
        var lastInvokeMillis: Long = Long.MIN_VALUE
        var lastUninvokedFireMillis: Long = -1

        override fun unregister() {
            val result = rectChangedMap.multiRemove(id, this)
            if (!result) removeFromGlobalEntries(this)
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
                    node = node,
                    topLeft = topLeft,
                    bottomRight = bottomRight,
                    windowOffset = windowOffset,
                    screenOffset = screenOffset,
                    windowSize = windowSize,
                    viewToWindowMatrix = viewToWindowMatrix,
                )
            if (rect == null) {
                return
            }

            callback(rect)
        }
    }

    /** Set of callbacks for onRectChanged. */
    val rectChangedMap = mutableIntObjectMapOf<Entry>()

    /**
     * Set of callbacks for onGlobalLayoutListener given as a Linked List using [Entry].
     *
     * These are expected to be fired after any Rect or Window/Screen change.
     */
    var globalChangeEntries: Entry? = null

    // We can use this to schedule a "triggerDebounced" call. If it is -1, then nothing
    // needs to be scheduled.
    var minDebounceDeadline: Long = -1
    var windowOffset: IntOffset = IntOffset.Zero
    var screenOffset: IntOffset = IntOffset.Zero
    var windowSize: Long = 0
    var viewToWindowMatrix: Matrix? = null

    fun updateOffsets(
        screen: IntOffset,
        window: IntOffset,
        matrix: Matrix?,
        windowWidth: Int,
        windowHeight: Int,
    ): Boolean {
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
        val size = packXY(windowWidth, windowHeight)
        if (size != windowSize) {
            windowSize = size
            updated = true
        }
        return updated
    }

    private fun roundDownToMultipleOf8(x: Long): Long {
        return (x shr 3) shl 3
    }

    fun registerOnRectChanged(
        id: Int,
        throttleMillis: Long,
        debounceMillis: Long,
        node: DelegatableNode,
        callback: (RelativeLayoutBounds) -> Unit,
    ): RegistrationHandle {
        // If zero is set for debounce, we use throttle in its place. This guarantees that
        // consumers will get the value where the node "settled".
        val debounceToUse = if (debounceMillis == 0L) throttleMillis else debounceMillis

        return rectChangedMap.multiPut(
            key = id,
            value =
                Entry(
                    id = id,
                    throttleMillis = throttleMillis,
                    debounceMillis = debounceToUse,
                    node = node,
                    callback = callback,
                ),
        )
    }

    fun registerOnGlobalChange(
        id: Int,
        throttleMillis: Long,
        debounceMillis: Long,
        node: DelegatableNode,
        callback: (RelativeLayoutBounds) -> Unit,
    ): RegistrationHandle {
        // If zero is set for debounce, we use throttle in its place. This guarantees that
        // consumers will get the value where the node "settled".
        val debounceToUse = if (debounceMillis == 0L) throttleMillis else debounceMillis

        val entry =
            Entry(
                id = id,
                throttleMillis = throttleMillis,
                debounceMillis = debounceToUse,
                node = node,
                callback = callback,
            )
        addToGlobalEntries(entry)
        return entry
    }

    // We call this when a layout node with `semanticsId = id` changes it's global bounds. For
    // throttled callbacks this may cause the callback to get invoked, for debounced nodes it
    // updates the deadlines
    fun fireOnUpdatedRect(id: Int, topLeft: Long, bottomRight: Long, currentMillis: Long) {
        rectChangedMap.runFor(id) { entry ->
            fireWithUpdatedRect(entry, topLeft, bottomRight, currentMillis)
        }
    }

    inline fun forEachNewCallbackNeverInvoked(callback: (Entry) -> Unit) {
        rectChangedMap.forEachValue { entry ->
            var next: Entry? = entry
            while (next != null) {
                if (entry.lastInvokeMillis == Long.MIN_VALUE) {
                    callback(entry)
                }
                next = next.next
            }
        }
    }

    /** Fires all [rectChangedMap] entries with latest window/screen info. */
    fun fireOnRectChangedEntries(currentMillis: Long) {
        val windowOffset = windowOffset
        val screenOffset = screenOffset
        val viewToWindowMatrix = viewToWindowMatrix
        rectChangedMap.multiForEach { entry ->
            fire(
                entry = entry,
                windowOffset = windowOffset,
                screenOffset = screenOffset,
                viewToWindowMatrix = viewToWindowMatrix,
                currentMillis = currentMillis,
            )
        }
    }

    /** Fires all [globalChangeEntries] entries with latest window/screen info. */
    fun fireGlobalChangeEntries(currentMillis: Long) {
        val windowOffset = windowOffset
        val screenOffset = screenOffset
        val viewToWindowMatrix = viewToWindowMatrix
        globalChangeEntries?.linkedForEach { entry ->
            val node = entry.node.requireLayoutNode()
            val offsetFromRoot = node.offsetFromRoot
            val lastSize = node.lastSize

            // For global change callbacks, we'll still need to update the Entry bounds
            entry.topLeft = offsetFromRoot.packedValue
            entry.bottomRight =
                packXY(offsetFromRoot.x + lastSize.width, offsetFromRoot.y + lastSize.height)

            fire(
                entry = entry,
                windowOffset = windowOffset,
                screenOffset = screenOffset,
                viewToWindowMatrix = viewToWindowMatrix,
                currentMillis = currentMillis,
            )
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
        rectChangedMap.multiForEach { entry ->
            minDeadline =
                debounceEntry(
                    entry = entry,
                    windowOffset = windowOffset,
                    screenOffset = screenOffset,
                    viewToWindowMatrix = viewToWindowMatrix,
                    currentMillis = currentMillis,
                    minDeadline = minDeadline,
                )
        }
        globalChangeEntries?.linkedForEach { entry ->
            minDeadline =
                debounceEntry(
                    entry = entry,
                    windowOffset = windowOffset,
                    screenOffset = screenOffset,
                    viewToWindowMatrix = viewToWindowMatrix,
                    currentMillis = currentMillis,
                    minDeadline = minDeadline,
                )
        }
        minDebounceDeadline = if (minDeadline == Long.MAX_VALUE) -1 else minDeadline
    }

    internal fun fireWithUpdatedRect(
        entry: Entry,
        topLeft: Long,
        bottomRight: Long,
        currentMillis: Long,
    ) {
        val lastInvokeMillis = entry.lastInvokeMillis
        val throttleMillis = entry.throttleMillis
        val debounceMillis = entry.debounceMillis
        // We need to check separately for lastInvokeMillis being Long.MIN_VALUE because when it is,
        // we will end up with Long overflow.
        val pastThrottleDeadline =
            currentMillis - lastInvokeMillis >= throttleMillis || lastInvokeMillis == Long.MIN_VALUE
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

    private fun fire(
        entry: Entry,
        windowOffset: IntOffset,
        screenOffset: IntOffset,
        viewToWindowMatrix: Matrix?,
        currentMillis: Long,
    ) {
        val lastInvokeMillis = entry.lastInvokeMillis
        // We need to check separately for lastInvokeMillis being Long.MIN_VALUE because when it is,
        // we will end up with Long overflow.
        val throttleOkay =
            currentMillis - lastInvokeMillis > entry.throttleMillis ||
                lastInvokeMillis == Long.MIN_VALUE
        val debounceOkay = entry.debounceMillis == 0L
        entry.lastUninvokedFireMillis = currentMillis
        if (throttleOkay && debounceOkay) {
            entry.lastInvokeMillis = currentMillis
            entry.fire(
                entry.topLeft,
                entry.bottomRight,
                windowOffset,
                screenOffset,
                viewToWindowMatrix,
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

    /** @return updated minDeadline */
    private fun debounceEntry(
        entry: Entry,
        windowOffset: IntOffset,
        screenOffset: IntOffset,
        viewToWindowMatrix: Matrix?,
        currentMillis: Long,
        minDeadline: Long,
    ): Long {
        var newMinDeadline = minDeadline
        if (entry.debounceMillis > 0 && entry.lastUninvokedFireMillis > 0) {
            if (currentMillis - entry.lastUninvokedFireMillis > entry.debounceMillis) {
                entry.lastInvokeMillis = currentMillis
                entry.lastUninvokedFireMillis = -1
                val topLeft = entry.topLeft
                val bottomRight = entry.bottomRight
                entry.fire(topLeft, bottomRight, windowOffset, screenOffset, viewToWindowMatrix)
            } else {
                newMinDeadline =
                    min(newMinDeadline, entry.lastUninvokedFireMillis + entry.debounceMillis)
            }
        }
        return newMinDeadline
    }

    private fun addToGlobalEntries(entry: Entry) {
        // For global entries, we can append the new entry to the start
        val oldInitialEntry = globalChangeEntries
        entry.next = oldInitialEntry
        globalChangeEntries = entry
    }

    /**
     * Removes [entry] from the LinkedList in [globalChangeEntries].
     *
     * @return Whether the given [entry] was found & removed from [globalChangeEntries].
     */
    private fun removeFromGlobalEntries(entry: Entry): Boolean {
        val initialGlobalEntry = globalChangeEntries
        if (initialGlobalEntry === entry) {
            globalChangeEntries = initialGlobalEntry.next
            entry.next = null
            return true
        }
        var last = initialGlobalEntry
        var node = last?.next
        while (node != null) {
            if (node === entry) {
                last?.next = node.next
                entry.next = null
                return true
            }
            last = node
            node = node.next
        }
        return false
    }

    /** Calls [block] for every [Entry] reachable from the given node through [Entry.next]. */
    private inline fun Entry.linkedForEach(block: (Entry) -> Unit) {
        var node: Entry? = this
        while (node != null) {
            block(node)
            node = node.next
        }
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
    windowSize: Long,
    viewToWindowMatrix: Matrix?,
): RelativeLayoutBounds? {
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
        val topLeftOffset = IntOffset(topLeft).toOffset()
        val size = coordinator.coordinates.size
        val transformedPos =
            layoutNode.outerCoordinator.coordinates
                .localPositionOf(coordinator, topLeftOffset)
                .round()
        RelativeLayoutBounds(
            transformedPos.packedValue,
            IntOffset(transformedPos.x + size.width, transformedPos.y + size.height).packedValue,
            windowOffset,
            screenOffset,
            windowSize,
            viewToWindowMatrix,
            node,
        )
    } else
        RelativeLayoutBounds(
            topLeft,
            bottomRight,
            windowOffset,
            screenOffset,
            windowSize,
            viewToWindowMatrix,
            node,
        )
}
