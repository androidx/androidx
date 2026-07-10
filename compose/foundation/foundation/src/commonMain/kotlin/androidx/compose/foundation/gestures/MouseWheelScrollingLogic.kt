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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.copy
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal fun MouseWheel1DScrollingLogic(
    scrollingLogic: ScrollingLogic,
    scrollConfig: ScrollConfig,
    onScrollStopped: suspend (velocity: Velocity) -> Unit,
    density: Density,
): NonTouchScrollingLogic {
    val adapter =
        OneDimensionalScrollValueAdapter(
            isVertical = { scrollingLogic.orientation == Orientation.Vertical }
        )
    return MouseWheelScrollingLogicImpl(
        scrollValueAdapter = adapter,
        scrollLogic = scrollingLogic,
        scrollConfig = scrollConfig,
        onScrollStopped = onScrollStopped,
        density = density,
        canConsumeDelta = { scrollingLogic.canConsumeDelta(adapter.decodeToFloat(it)) },
    )
}

internal fun MouseWheel2DScrollingLogic(
    scrollingLogic: ScrollingLogic2D,
    scrollConfig: ScrollConfig,
    onScrollStopped: suspend (velocity: Velocity) -> Unit,
    density: Density,
): NonTouchScrollingLogic {
    val adapter = TwoDimensionalScrollValueAdapter
    return MouseWheelScrollingLogicImpl(
        scrollLogic = scrollingLogic,
        scrollConfig = scrollConfig,
        onScrollStopped = onScrollStopped,
        density = density,
        scrollValueAdapter = adapter,
        canConsumeDelta = { scrollingLogic.scrollableState.canScroll(adapter.decodeToOffset(it)) },
    )
}

private class MouseWheelScrollingLogicImpl<T>(
    scrollValueAdapter: ScrollValueAdapter<T>,
    scrollLogic: ScrollLogic,
    private val scrollConfig: ScrollConfig,
    onScrollStopped: suspend (velocity: Velocity) -> Unit,
    density: Density,
    private val canConsumeDelta: (delta: ScrollValue) -> Boolean,
) :
    NonTouchScrollingLogic(scrollLogic, onScrollStopped, density),
    ScrollValueAdapter<T> by scrollValueAdapter {

    override fun isScrollingEvent(pointerEvent: PointerEvent) =
        pointerEvent.type == PointerEventType.Scroll

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

    private var receivingMouseWheelEventsJob: Job? = null

    override fun startReceivingEvents(coroutineScope: CoroutineScope) {
        if (receivingMouseWheelEventsJob == null) {
            receivingMouseWheelEventsJob =
                coroutineScope.launch {
                    try {
                        while (coroutineContext.isActive) {
                            val scrollDelta = channel.receive()
                            val threshold = with(density) { AnimationThreshold.toPx() }
                            val speed = with(density) { AnimationSpeed.toPx() }
                            dispatchMouseWheelScroll(scrollDelta, threshold, speed)
                        }
                    } finally {
                        receivingMouseWheelEventsJob = null
                    }
                }
        }
    }

    override fun onScrollingEvent(pointerEvent: PointerEvent, bounds: IntSize): Boolean {
        val scrollDelta =
            with(scrollConfig) { with(density) { calculateMouseWheelScroll(pointerEvent, bounds) } }
        return if (canConsumeDelta(scrollDelta.toScrollValue())) {
            channel
                .trySend(
                    MouseWheelScrollDelta(
                        value = scrollDelta,
                        timeMillis = pointerEvent.changes.first().uptimeMillis,
                        shouldApplyImmediately = !scrollConfig.isSmoothScrollingEnabled
                            // In case of high-resolution wheel, such as a freely rotating wheel
                            // with no notches or trackpads, delta should apply immediately, without
                            // any delays.
                            || scrollConfig.isPreciseWheelScroll(pointerEvent),
                    )
                )
                .isSuccess
        } else isScrolling
    }

    @OptIn(ExperimentalFoundationApi::class)
    private suspend fun dispatchMouseWheelScroll(
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
        var targetValue = targetScrollDelta.value.toScrollValue()
        if (targetValue.isLowScrollingDelta()) {
            return
        }
        var animationState = newAnimationState()

        /*
         * TODO Handle real down/up events from touchpad to set isScrollInProgress correctly.
         *  Touchpads emit just multiple mouse wheel events, so detecting start and end of this
         *  "gesture" is not straight forward.
         *  Ideally it should be resolved by catching real touches from input device instead of
         *  waiting the next event with timeout before resetting progress flag.
         */
        suspend fun waitNextScrollDelta(timeoutMillis: Long): Boolean {
            if (timeoutMillis < 0) return false
            return withTimeoutOrNull(timeoutMillis.milliseconds) { channel.busyReceive() }
                ?.let {
                    // Keep this value unchanged during animation
                    // Currently, [isPreciseWheelScroll] might be unstable in case if
                    // a precise value is almost equal regular one.
                    val previousDeltaShouldApplyImmediately =
                        targetScrollDelta.shouldApplyImmediately
                    targetScrollDelta =
                        it.copy(shouldApplyImmediately = previousDeltaShouldApplyImmediately)
                    targetValue = targetScrollDelta.value.toScrollValue()
                    // Reset previous animation leftover
                    animationState = newAnimationState()
                    trackVelocity(it)

                    !targetValue.isLowScrollingDelta()
                } ?: false
        }

        userScroll {
            var requiredAnimation = true
            while (requiredAnimation) {
                requiredAnimation = false
                val animationValue = animationState.value.encode()
                val targetValueLeftover = targetValue - animationValue
                if (
                    targetScrollDelta.shouldApplyImmediately ||
                        (targetValueLeftover.size() < threshold)
                ) {
                    dispatchMouseWheelScroll(targetValueLeftover)
                    requiredAnimation = waitNextScrollDelta(ScrollProgressTimeout)
                } else {
                    // Animation will start only on the next frame,
                    // so apply threshold immediately to avoid delays.
                    val instantDelta = targetValueLeftover.normalize() * threshold
                    dispatchMouseWheelScroll(instantDelta)
                    val currentAnimationValue = animationValue + instantDelta
                    animationState = animationState.copy(value = currentAnimationValue.decode())

                    val durationMillis =
                        ((targetValue - currentAnimationValue).size() / speed)
                            .roundToInt()
                            .coerceAtMost(MaxAnimationDuration)
                    animateMouseWheelScroll(
                        animationState = animationState,
                        currentAnimationValue = currentAnimationValue,
                        targetValue = targetValue,
                        durationMillis = durationMillis,
                    ) { lastValue ->
                        // Sum delta from all pending events to avoid multiple animation restarts.
                        val nextScrollDelta = channel.sumOrNull()
                        if (nextScrollDelta != null) {
                            trackVelocity(nextScrollDelta)
                            targetScrollDelta += nextScrollDelta
                            targetValue = targetScrollDelta.value.toScrollValue()

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
            val velocityPxInMs = minOf(targetValue.size() / MaxAnimationDuration, speed)
            velocity = (targetValue.normalize() * velocityPxInMs * 1000f).toVelocity()
        }
        onScrollStopped(velocity)
    }

    suspend fun NestedScrollScope.animateMouseWheelScroll(
        animationState: AnimationState<T, *>,
        currentAnimationValue: ScrollValue,
        targetValue: ScrollValue,
        durationMillis: Int,
        shouldCancelAnimation: (lastValue: ScrollValue) -> Boolean,
    ) {
        var lastValue = currentAnimationValue
        animationState.animateTo(
            targetValue.decode(),
            animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            sequentialAnimation = true,
        ) {
            val delta = value.encode() - lastValue
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

    private fun NestedScrollScope.dispatchMouseWheelScroll(delta: ScrollValue): ScrollValue {
        val offset = delta.toOffset()
        val consumedOffset = scrollBy(offset, UserInput)
        return consumedOffset.toScrollValue()
    }

    private fun trackVelocity(scrollDelta: MouseWheelScrollDelta) {
        velocityTracker.addDelta(scrollDelta.timeMillis, scrollDelta.value)
    }

    private fun Channel<MouseWheelScrollDelta>.sumOrNull(): MouseWheelScrollDelta? {
        var sum: MouseWheelScrollDelta? = null
        for (i in untilNull { tryReceive().getOrNull() }) {
            sum = if (sum == null) i else sum + i
        }
        return sum
    }
}

private val AnimationThreshold = 6.dp // (AnimationSpeed * MaxAnimationDuration) / (1000ms / 60Hz)
private val AnimationSpeed = 1.dp // dp / ms
private const val MaxAnimationDuration = 100 // ms
private const val ScrollProgressTimeout = 50L // ms
