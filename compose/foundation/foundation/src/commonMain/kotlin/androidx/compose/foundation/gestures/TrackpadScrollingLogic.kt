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

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class TrackpadScrollingLogic(
    scrollingLogic: ScrollingLogic,
    onScrollStopped: suspend (velocity: Velocity) -> Unit,
    density: Density,
) : NonTouchScrollingLogic(scrollingLogic, onScrollStopped, density) {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        @OptIn(ExperimentalFoundationApi::class)
        if (
            !ComposeFoundationFlags.isTrackpadGestureHandlingEnabled ||
                (pointerEvent.type != PointerEventType.PanStart &&
                    pointerEvent.type != PointerEventType.PanMove &&
                    pointerEvent.type != PointerEventType.PanEnd)
        )
            return
        if (pointerEvent.isConsumed) return

        /**
         * If this scrollable is already scrolling from a previous interaction, consume immediately
         * to give it priority.
         */
        if (pass == PointerEventPass.Initial && isScrolling) {
            onPan(pointerEvent)
            pointerEvent.consume()
        }

        /**
         * During the main pass. If this scrollable is not scrolling, decide if it should based on
         * the consumption. If the scrollable is scrolling we don't need to worry because it
         * consumed during the initial pass.
         */
        if (pass == PointerEventPass.Main && !isScrolling) {
            val consumed = onPan(pointerEvent)
            if (consumed) {
                pointerEvent.consume()
            }
        }
    }

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
                            scrollingLogic.dispatchTrackpadScroll(channel.receive())
                        }
                    } finally {
                        receivingPanEventsJob = null
                    }
                }
        }
    }

    private fun onPan(pointerEvent: PointerEvent): Boolean {
        @OptIn(ExperimentalFoundationApi::class)
        if (!ComposeFoundationFlags.isTrackpadGestureHandlingEnabled) return false

        var sent = false

        pointerEvent.changes.firstOrNull()?.let {
            it.historical.fastForEach { historicalChange ->
                val delta = -historicalChange.panOffset
                if (scrollingLogic.canConsumeDelta(delta)) {
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
            val delta = -it.panOffset
            val isPanEnd = pointerEvent.type == PointerEventType.PanEnd
            if (scrollingLogic.canConsumeDelta(delta) || isPanEnd) {
                sent =
                    channel
                        .trySend(
                            TrackpadScrollDelta(
                                value = delta,
                                timeMillis = it.uptimeMillis,
                                isEnd = isPanEnd,
                            )
                        )
                        .isSuccess || sent
            }
        }

        return sent || isScrolling
    }

    private fun Channel<TrackpadScrollDelta>.sumOrNull(): TrackpadScrollDelta? {
        var sum: TrackpadScrollDelta? = null
        for (i in untilNull { tryReceive().getOrNull() }) {
            sum = if (sum == null) i else sum + i
        }
        return sum
    }

    private fun ScrollingLogic.canConsumeDelta(scrollDelta: Offset): Boolean =
        scrollDelta.reverseIfNeeded().toSingleAxisDeltaFromAngle() != 0f

    private fun trackVelocity(scrollDelta: TrackpadScrollDelta) {
        velocityTracker.addDelta(scrollDelta.timeMillis, scrollDelta.value)
    }

    private suspend fun ScrollingLogic.dispatchTrackpadScroll(scrollDelta: TrackpadScrollDelta) {
        var targetScrollDelta = scrollDelta
        trackVelocity(scrollDelta)
        // Sum delta from all pending events to drain the channel.
        channel.sumOrNull()?.let {
            trackVelocity(it)
            targetScrollDelta += it
        }

        userScroll {
            dispatchTrackpadScroll(
                targetScrollDelta.value.reverseIfNeeded().toSingleAxisDeltaFromAngle()
            )
            while (!targetScrollDelta.isEnd) {
                targetScrollDelta = channel.busyReceive()
                trackVelocity(targetScrollDelta)
                channel.sumOrNull()?.let {
                    trackVelocity(it)
                    targetScrollDelta += it
                }
                dispatchTrackpadScroll(
                    targetScrollDelta.value.reverseIfNeeded().toSingleAxisDeltaFromAngle()
                )
            }
        }

        onScrollStopped(velocityTracker.calculateVelocity())
    }

    private fun NestedScrollScope.dispatchTrackpadScroll(delta: Float) =
        with(scrollingLogic) {
            val offset = delta.reverseIfNeeded().toOffset()
            val consumed = scrollByWithOverscroll(offset, NestedScrollSource.UserInput)
            consumed.reverseIfNeeded().toFloat()
        }
}
