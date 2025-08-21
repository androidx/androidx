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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.copy
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ComposeFoundationFlags.isMouseWheel1DAxisLockingEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull

internal class MouseWheelScrollingLogic(
    private val scrollingLogic: ScrollingLogic,
    private val mouseWheelScrollConfig: ScrollConfig,
    private val onScrollStopped: suspend (velocity: Velocity) -> Unit,
    private var density: Density,
) {
    fun updateDensity(density: Density) {
        this.density = density
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (isMouseWheel1DAxisLockingEnabled) {
            if (pointerEvent.type != PointerEventType.Scroll) return
            if (pointerEvent.isConsumed) return
            /**
             * If this scrollable is already scrolling from a previous interaction, consume
             * immediately to give it priority.
             */
            if (pass == PointerEventPass.Initial && isScrolling) {
                onMouseWheel(pointerEvent, bounds)
                pointerEvent.consume()
            }

            /**
             * During the main pass. If this scrollable is not scrolling, decide if it should based
             * on the consumption. If the scrollable is scrolling we don't need to worry because it
             * consumed during the initial pass.
             */
            if (pass == PointerEventPass.Main && !isScrolling) {
                val consumed = onMouseWheel(pointerEvent, bounds)
                if (consumed) {
                    pointerEvent.consume()
                }
            }
        } else {
            if (pass == PointerEventPass.Main && pointerEvent.type == PointerEventType.Scroll) {
                if (!pointerEvent.isConsumed) {
                    val consumed = onMouseWheel(pointerEvent, bounds)
                    if (consumed) {
                        pointerEvent.consume()
                    }
                }
            }
        }
    }

    private inline val PointerEvent.isConsumed: Boolean
        get() = changes.fastAny { it.isConsumed }

    private fun PointerEvent.consume() = changes.fastForEach { it.consume() }

    private data class MouseWheelScrollDelta(
        val value: Offset,
        val timeMillis: Long,
        val shouldApplyImmediately: Boolean,
    ) {
        operator fun plus(other: MouseWheelScrollDelta) =
            MouseWheelScrollDelta(
                value = value + other.value,

                // Pick time from last one
                timeMillis = maxOf(timeMillis, other.timeMillis),

                // Ignore [other.shouldApplyImmediately] to avoid false-positive
                // [isPreciseWheelScroll]
                // detection during animation
                shouldApplyImmediately = shouldApplyImmediately,
            )
    }

    private val channel = Channel<MouseWheelScrollDelta>(capacity = Channel.UNLIMITED)
    private var isScrolling = false

    private var receivingMouseWheelEventsJob: Job? = null

    fun startReceivingMouseWheelEvents(coroutineScope: CoroutineScope) {
        if (receivingMouseWheelEventsJob == null) {
            receivingMouseWheelEventsJob =
                coroutineScope.launch {
                    try {
                        while (coroutineContext.isActive) {
                            val scrollDelta = channel.receive()
                            val threshold = with(density) { AnimationThreshold.toPx() }
                            val speed = with(density) { AnimationSpeed.toPx() }
                            scrollingLogic.dispatchMouseWheelScroll(scrollDelta, threshold, speed)
                        }
                    } finally {
                        receivingMouseWheelEventsJob = null
                    }
                }
        }
    }

    private suspend fun ScrollingLogic.userScroll(block: suspend NestedScrollScope.() -> Unit) {
        isScrolling = true
        // Run it in supervisorScope to ignore cancellations from scrolls with higher MutatePriority
        supervisorScope { scroll(MutatePriority.UserInput, block) }
        isScrolling = false
    }

    private fun onMouseWheel(pointerEvent: PointerEvent, bounds: IntSize): Boolean {
        val scrollDelta =
            with(mouseWheelScrollConfig) {
                with(density) { calculateMouseWheelScroll(pointerEvent, bounds) }
            }
        return if (scrollingLogic.canConsumeDelta(scrollDelta)) {
            channel
                .trySend(
                    MouseWheelScrollDelta(
                        value = scrollDelta,
                        timeMillis = pointerEvent.changes.first().uptimeMillis,
                        shouldApplyImmediately = !mouseWheelScrollConfig.isSmoothScrollingEnabled

                            // In case of high-resolution wheel, such as a freely rotating wheel
                            // with
                            // no notches or trackpads, delta should apply immediately, without any
                            // delays.
                            || mouseWheelScrollConfig.isPreciseWheelScroll(pointerEvent),
                    )
                )
                .isSuccess
        } else isScrolling
    }

    private fun Channel<MouseWheelScrollDelta>.sumOrNull(): MouseWheelScrollDelta? {
        var sum: MouseWheelScrollDelta? = null
        for (i in untilNull { tryReceive().getOrNull() }) {
            sum = if (sum == null) i else sum + i
        }
        return sum
    }

    /**
     * Replacement of regular [Channel.receive] that schedules an invalidation each frame. It avoids
     * entering an idle state while waiting for [ScrollProgressTimeout]. It's important for tests
     * that attempt to trigger another scroll after a mouse wheel event.
     */
    private suspend fun Channel<MouseWheelScrollDelta>.busyReceive() = coroutineScope {
        val job = launch {
            while (coroutineContext.isActive) {
                withFrameNanos {}
            }
        }
        try {
            receive()
        } finally {
            job.cancel()
        }
    }

    private fun <E> untilNull(builderAction: () -> E?) =
        sequence<E> {
            do {
                val element = builderAction()?.also { yield(it) }
            } while (element != null)
        }

    @OptIn(ExperimentalFoundationApi::class)
    private fun ScrollingLogic.canConsumeDelta(scrollDelta: Offset): Boolean {
        val delta =
            if (isMouseWheel1DAxisLockingEnabled) {
                /**
                 * Mouse wheel scroll deltas may come as 2 dimensional values. We use the angle to
                 * decide which axis in the delta is more important and should be triggered.
                 */
                scrollDelta.reverseIfNeeded().toSingleAxisDeltaFromAngle()
            } else {
                scrollDelta.reverseIfNeeded().toFloat() // Use only current axis
            }
        return if (delta == 0f) {
            false // It means that it's for another axis and cannot be consumed
        } else if (delta > 0f) {
            scrollableState.canScrollForward
        } else {
            scrollableState.canScrollBackward
        }
    }

    private val velocityTracker = MouseWheelVelocityTracker()

    private fun trackVelocity(scrollDelta: MouseWheelScrollDelta) {
        velocityTracker.addDelta(scrollDelta.timeMillis, scrollDelta.value)
    }

    @OptIn(ExperimentalFoundationApi::class)
    private suspend fun ScrollingLogic.dispatchMouseWheelScroll(
        scrollDelta: MouseWheelScrollDelta,
        threshold: Float, // px
        speed: Float, // px / ms
    ) {
        var targetScrollDelta = scrollDelta
        trackVelocity(scrollDelta)
        // Sum delta from all pending events to avoid multiple animation restarts.
        channel.sumOrNull()?.let {
            trackVelocity(it)
            targetScrollDelta += it
        }
        var targetValue = targetScrollDelta.value.reverseIfNeeded().toFloat()
        if (targetValue.isLowScrollingDelta()) {
            return
        }
        var animationState = AnimationState(0f)

        /*
         * TODO Handle real down/up events from touchpad to set isScrollInProgress correctly.
         *  Touchpads emit just multiple mouse wheel events, so detecting start and end of this
         *  "gesture" is not straight forward.
         *  Ideally it should be resolved by catching real touches from input device instead of
         *  waiting the next event with timeout before resetting progress flag.
         */
        suspend fun waitNextScrollDelta(timeoutMillis: Long): Boolean {
            if (timeoutMillis < 0) return false
            return withTimeoutOrNull(timeoutMillis) { channel.busyReceive() }
                ?.let {
                    // Keep this value unchanged during animation
                    // Currently, [isPreciseWheelScroll] might be unstable in case if
                    // a precise value is almost equal regular one.
                    val previousDeltaShouldApplyImmediately =
                        targetScrollDelta.shouldApplyImmediately
                    targetScrollDelta =
                        it.copy(shouldApplyImmediately = previousDeltaShouldApplyImmediately)
                    targetValue =
                        if (isMouseWheel1DAxisLockingEnabled) {
                            targetScrollDelta.value.reverseIfNeeded().toSingleAxisDeltaFromAngle()
                        } else {
                            targetScrollDelta.value.reverseIfNeeded().toFloat()
                        }
                    animationState = AnimationState(0f) // Reset previous animation leftover
                    trackVelocity(it)

                    !targetValue.isLowScrollingDelta()
                } ?: false
        }

        userScroll {
            var requiredAnimation = true
            while (requiredAnimation) {
                requiredAnimation = false
                val targetValueLeftover = targetValue - animationState.value
                if (
                    targetScrollDelta.shouldApplyImmediately || abs(targetValueLeftover) < threshold
                ) {
                    dispatchMouseWheelScroll(targetValueLeftover)
                    requiredAnimation = waitNextScrollDelta(ScrollProgressTimeout)
                } else {
                    // Animation will start only on the next frame,
                    // so apply threshold immediately to avoid delays.
                    val instantDelta = sign(targetValueLeftover) * threshold
                    dispatchMouseWheelScroll(instantDelta)
                    animationState =
                        animationState.copy(value = animationState.value + instantDelta)

                    val durationMillis =
                        (abs(targetValue - animationState.value) / speed)
                            .roundToInt()
                            .coerceAtMost(MaxAnimationDuration)
                    animateMouseWheelScroll(animationState, targetValue, durationMillis) { lastValue
                        ->
                        // Sum delta from all pending events to avoid multiple animation restarts.
                        val nextScrollDelta = channel.sumOrNull()
                        if (nextScrollDelta != null) {
                            trackVelocity(nextScrollDelta)
                            targetScrollDelta += nextScrollDelta
                            targetValue =
                                if (isMouseWheel1DAxisLockingEnabled) {
                                    targetScrollDelta.value
                                        .reverseIfNeeded()
                                        .toSingleAxisDeltaFromAngle()
                                } else {
                                    targetScrollDelta.value.reverseIfNeeded().toFloat()
                                }

                            requiredAnimation = !(targetValue - lastValue).isLowScrollingDelta()
                        }
                        nextScrollDelta != null
                    }
                    if (!requiredAnimation) {
                        // If it's completed, wait the next event with timeout before resetting
                        // progress flag
                        requiredAnimation =
                            waitNextScrollDelta(ScrollProgressTimeout - durationMillis)
                    }
                }
            }
        }

        var velocity = velocityTracker.calculateVelocity()
        if (velocity == Velocity.Zero) {
            // In case of single data point use animation speed and delta direction
            val velocityPxInMs = minOf(abs(targetValue) / MaxAnimationDuration, speed)
            velocity = (sign(targetValue).reverseIfNeeded() * velocityPxInMs * 1000).toVelocity()
        }
        onScrollStopped(velocity)
    }

    private suspend fun NestedScrollScope.animateMouseWheelScroll(
        animationState: AnimationState<Float, AnimationVector1D>,
        targetValue: Float,
        durationMillis: Int,
        shouldCancelAnimation: (lastValue: Float) -> Boolean,
    ) {
        var lastValue = animationState.value
        animationState.animateTo(
            targetValue,
            animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            sequentialAnimation = true,
        ) {
            val delta = value - lastValue
            if (!delta.isLowScrollingDelta()) {
                val consumedDelta = dispatchMouseWheelScroll(delta)
                if (!(delta - consumedDelta).isLowScrollingDelta()) {
                    cancelAnimation()
                    return@animateTo
                }
                lastValue += delta
            }
            if (shouldCancelAnimation(lastValue)) {
                cancelAnimation()
            }
        }
    }

    private fun NestedScrollScope.dispatchMouseWheelScroll(delta: Float) =
        with(scrollingLogic) {
            val offset = delta.reverseIfNeeded().toOffset()
            val consumed = scrollBy(offset, NestedScrollSource.UserInput)
            consumed.reverseIfNeeded().toFloat()
        }
}

private class MouseWheelVelocityTracker {
    private val xVelocityTracker = VelocityTracker1D(isDataDifferential = true)
    private val yVelocityTracker = VelocityTracker1D(isDataDifferential = true)

    fun addDelta(timeMillis: Long, delta: Offset) {
        xVelocityTracker.addDataPoint(timeMillis, delta.x)
        yVelocityTracker.addDataPoint(timeMillis, delta.y)
    }

    fun calculateVelocity(): Velocity {
        val velocityX = xVelocityTracker.calculateVelocity(Float.MAX_VALUE)
        val velocityY = yVelocityTracker.calculateVelocity(Float.MAX_VALUE)
        return Velocity(velocityX, velocityY)
    }
}

/*
 * Returns true, if the value is too low for visible change in scroll (consumed delta, animation-based change, etc),
 * false otherwise
 */
private fun Float.isLowScrollingDelta(): Boolean = isNaN() || abs(this) < 0.5f

private val AnimationThreshold = 6.dp // (AnimationSpeed * MaxAnimationDuration) / (1000ms / 60Hz)
private val AnimationSpeed = 1.dp // dp / ms
private const val MaxAnimationDuration = 100 // ms
private const val ScrollProgressTimeout = 50L // ms
