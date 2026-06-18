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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun Trackpad1DScrollingLogic(
    scrollingLogic: ScrollingLogic,
    onScrollStopped: suspend (velocity: Velocity) -> Unit,
    density: Density,
): NonTouchScrollingLogic {
    val adapter =
        OneDimensionalScrollValueAdapter(
            isVertical = { scrollingLogic.orientation == Orientation.Vertical }
        )
    return TrackpadScrollingLogicImpl(
        scrollValueAdapter = adapter,
        scrollLogic = scrollingLogic,
        onScrollStopped = onScrollStopped,
        density = density,
        canConsumeDelta = { scrollingLogic.canConsumeDelta(adapter.decodeToFloat(it)) },
    )
}

internal fun Trackpad2DScrollingLogic(
    scrollingLogic: ScrollingLogic2D,
    onScrollStopped: suspend (velocity: Velocity) -> Unit,
    density: Density,
): NonTouchScrollingLogic {
    val adapter = TwoDimensionalScrollValueAdapter
    return TrackpadScrollingLogicImpl(
        scrollValueAdapter = adapter,
        scrollLogic = scrollingLogic,
        onScrollStopped = onScrollStopped,
        density = density,
        canConsumeDelta = { scrollingLogic.scrollableState.canScroll(adapter.decodeToOffset(it)) },
    )
}

/**
 * Base class for 1-D [Trackpad1DScrollingLogic] and 2-D [Trackpad1DScrollingLogic] trackpad
 * scrolling logic.
 */
internal class TrackpadScrollingLogicImpl<T>(
    scrollValueAdapter: ScrollValueAdapter<T>,
    scrollLogic: ScrollLogic,
    onScrollStopped: suspend (velocity: Velocity) -> Unit,
    density: Density,
    private val canConsumeDelta: (delta: ScrollValue) -> Boolean,
) :
    NonTouchScrollingLogic(scrollLogic, onScrollStopped, density),
    ScrollValueAdapter<T> by scrollValueAdapter {
    private class TrackpadScrollDelta(val value: Offset, val timeMillis: Long, val isEnd: Boolean) {
        operator fun plus(other: TrackpadScrollDelta) =
            TrackpadScrollDelta(
                value = value + other.value,
                // Pick time from last one
                timeMillis = maxOf(timeMillis, other.timeMillis),
                // Combine together the flag to end the gesture
                isEnd = isEnd || other.isEnd,
            )
    }

    private val channel = Channel<TrackpadScrollDelta>(capacity = Channel.UNLIMITED)

    private var receivingPanEventsJob: Job? = null

    override fun startReceivingEvents(coroutineScope: CoroutineScope) {
        if (receivingPanEventsJob == null) {
            receivingPanEventsJob =
                coroutineScope.launch {
                    try {
                        while (coroutineContext.isActive) {
                            dispatchTrackpadScroll(channel.receive())
                        }
                    } finally {
                        receivingPanEventsJob = null
                    }
                }
        }
    }

    override fun isScrollingEvent(pointerEvent: PointerEvent) =
        pointerEvent.type == PointerEventType.PanStart ||
            pointerEvent.type == PointerEventType.PanMove ||
            pointerEvent.type == PointerEventType.PanEnd

    override fun onScrollingEvent(pointerEvent: PointerEvent, bounds: IntSize): Boolean {
        var sent = false

        fun result() = sent || isScrolling

        val change = pointerEvent.changes.firstOrNull() ?: return result()

        // On PanStart, if there is nothing to scroll yet, we don't want mark ourselves as
        // `isScrolling` just yet because there could be a descendant node that wants to handle
        // the events. Setting `isScrolling = true` will make this logic consume the event on
        // the next Initial pass, preventing the descendant node from receiving it.
        if (
            (pointerEvent.type == PointerEventType.PanStart) &&
                change.panOffset.isZero() &&
                change.historical.fastAll { it.panOffset.isZero() }
        ) {
            return result()
        }

        change.historical.fastForEach { historicalChange ->
            val delta = -historicalChange.panOffset
            if (canConsumeDelta(delta.toScrollValue())) {
                sent =
                    channel
                        .trySend(
                            TrackpadScrollDelta(
                                value = delta,
                                timeMillis = historicalChange.uptimeMillis,
                                isEnd = false,
                            )
                        )
                        .isSuccess || sent
            }
        }
        val delta = -change.panOffset
        val isPanEnd = pointerEvent.type == PointerEventType.PanEnd
        if (canConsumeDelta(delta.toScrollValue()) || isPanEnd) {
            sent =
                channel
                    .trySend(
                        TrackpadScrollDelta(
                            value = delta,
                            timeMillis = change.uptimeMillis,
                            isEnd = isPanEnd,
                        )
                    )
                    .isSuccess || sent
        }

        return result()
    }

    // Can't just compare to Offset.Zero because -Offset.Zero != Offset.Zero
    private fun Offset.isZero() = (x == 0f) && (y == 0f)

    private suspend fun dispatchTrackpadScroll(delta: TrackpadScrollDelta) {
        var targetScrollDelta = delta
        trackVelocity(delta)
        // Sum delta from all pending events to drain the channel.
        channel.sumOrNull()?.let {
            trackVelocity(it)
            targetScrollDelta += it
        }

        userScroll {
            dispatchTrackpadScroll(targetScrollDelta.value.toScrollValue())
            while (!targetScrollDelta.isEnd) {
                targetScrollDelta = channel.busyReceive()
                trackVelocity(targetScrollDelta)
                channel.sumOrNull()?.let {
                    trackVelocity(it)
                    targetScrollDelta += it
                }
                dispatchTrackpadScroll(targetScrollDelta.value.toScrollValue())
            }
        }

        onScrollStopped(velocityTracker.calculateVelocity())
    }

    private fun NestedScrollScope.dispatchTrackpadScroll(delta: ScrollValue): ScrollValue {
        val offset = delta.toOffset()
        val consumedOffset = scrollByWithOverscroll(offset, UserInput)
        return consumedOffset.toScrollValue()
    }

    private fun Channel<TrackpadScrollDelta>.sumOrNull(): TrackpadScrollDelta? {
        var sum: TrackpadScrollDelta? = null
        for (i in untilNull { tryReceive().getOrNull() }) {
            sum = if (sum == null) i else sum + i
        }
        return sum
    }

    private fun trackVelocity(scrollDelta: TrackpadScrollDelta) {
        velocityTracker.addDelta(scrollDelta.timeMillis, scrollDelta.value)
    }
}
