/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull

internal class MouseWheelScrollNode(
    private val scrollingLogic: ScrollingLogic,
    private val onScrollStopped: suspend CoroutineScope.(velocity: Velocity) -> Unit,
    private var enabled: Boolean,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    private lateinit var mouseWheelScrollConfig: ScrollConfig

    override fun onAttach() {
        mouseWheelScrollConfig = platformScrollConfig()
        coroutineScope.launch {
            receiveMouseWheelEvents()
        }
    }

    private val pointerInputNode = delegate(SuspendingPointerInputModifierNode {
        if (enabled) {
            mouseWheelInput()
        }
    })

    fun update(
        enabled: Boolean,
    ) {
        var resetPointerInputHandling = false
        if (this.enabled != enabled) {
            this.enabled = enabled
            resetPointerInputHandling = true
        }
        if (resetPointerInputHandling) {
            pointerInputNode.resetPointerInputHandler()
        }
    }

    private suspend fun PointerInputScope.mouseWheelInput() = awaitPointerEventScope {
        while (coroutineScope.isActive) {
            val event = awaitScrollEvent()
            if (!event.isConsumed) {
                val consumed = onMouseWheel(event)
                if (consumed) {
                    event.consume()
                }
            }
        }
    }

    private suspend fun AwaitPointerEventScope.awaitScrollEvent(): PointerEvent {
        var event: PointerEvent
        do {
            event = awaitPointerEvent()
        } while (event.type != PointerEventType.Scroll)
        return event
    }

    private inline val PointerEvent.isConsumed: Boolean get() = changes.fastAny { it.isConsumed }
    private inline fun PointerEvent.consume() = changes.fastForEach { it.consume() }

    private data class MouseWheelScrollDelta(
        val value: Offset,
        val shouldApplyImmediately: Boolean
    ) {
        operator fun plus(other: MouseWheelScrollDelta) = MouseWheelScrollDelta(
            value = value + other.value,
            shouldApplyImmediately = shouldApplyImmediately || other.shouldApplyImmediately
        )
    }
    private val channel = Channel<MouseWheelScrollDelta>(capacity = Channel.UNLIMITED)

    private suspend fun receiveMouseWheelEvents() {
        while (coroutineContext.isActive) {
            val scrollDelta = channel.receive()
            val density = currentValueOf(LocalDensity)
            val threshold = with(density) { AnimationThreshold.toPx() }
            val speed = with(density) { AnimationSpeed.toPx() }
            scrollingLogic.dispatchMouseWheelScroll(scrollDelta, threshold, speed)
        }
    }

    private suspend fun ScrollableState.userScroll(
        block: suspend ScrollScope.() -> Unit
    ) = supervisorScope {
        // Run it in supervisorScope to ignore cancellations from scrolls with higher MutatePriority
        scroll(MutatePriority.UserInput, block)
    }

    private fun PointerInputScope.onMouseWheel(pointerEvent: PointerEvent): Boolean {
        val scrollDelta = with(mouseWheelScrollConfig) {
            calculateMouseWheelScroll(pointerEvent, size)
        }
        return if (scrollingLogic.canConsumeDelta(scrollDelta)) {
            channel.trySend(MouseWheelScrollDelta(
                value = scrollDelta,
                shouldApplyImmediately = !mouseWheelScrollConfig.isSmoothScrollingEnabled

                    // In case of high-resolution wheel, such as a freely rotating wheel with
                    // no notches or trackpads, delta should apply immediately, without any delays.
                    || mouseWheelScrollConfig.isPreciseWheelScroll(pointerEvent)
            )).isSuccess
        } else false
    }

    private fun Channel<MouseWheelScrollDelta>.sumOrNull() =
        untilNull { tryReceive().getOrNull() }
            .toList()
            .reduceOrNull { accumulator, it -> accumulator + it }

    /**
     * Replacement of regular [Channel.receive] that schedules an invalidation each frame.
     * It avoids entering an idle state while waiting for [ScrollProgressTimeout].
     * It's important for tests that attempt to trigger another scroll after a mouse wheel event.
     */
    private suspend fun Channel<MouseWheelScrollDelta>.busyReceive() = coroutineScope {
        val job = launch {
            while (coroutineContext.isActive) {
                withFrameNanos { }
            }
        }
        try {
            receive()
        } finally {
            job.cancel()
        }
    }

    private fun <E> untilNull(builderAction: () -> E?) = sequence<E> {
        do {
            val element = builderAction()?.also {
                yield(it)
            }
        } while (element != null)
    }

    private fun ScrollingLogic.canConsumeDelta(scrollDelta: Offset): Boolean {
        val delta = scrollDelta.reverseIfNeeded().toFloat() // Use only current axis
        return if (delta == 0f) {
            false // It means that it's for another axis and cannot be consumed
        } else if (delta > 0f) {
            scrollableState.canScrollForward
        } else {
            scrollableState.canScrollBackward
        }
    }

    private suspend fun ScrollingLogic.dispatchMouseWheelScroll(
        scrollDelta: MouseWheelScrollDelta,
        threshold: Float, // px
        speed: Float, // px / ms
    ) {
        var targetScrollDelta = scrollDelta
        // Sum delta from all pending events to avoid multiple animation restarts.
        channel.sumOrNull()?.let {
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
        suspend fun waitNextScrollDelta(timeoutMillis: Long, forceApplyImmediately: Boolean = false): Boolean {
            if (timeoutMillis < 0) return false
            return withTimeoutOrNull(timeoutMillis) {
                channel.busyReceive()
            }?.let {
                targetScrollDelta = if (forceApplyImmediately) it.copy(shouldApplyImmediately = true) else it
                targetValue = targetScrollDelta.value.reverseIfNeeded().toFloat()
                animationState = AnimationState(0f) // Reset previous animation leftover

                !targetValue.isLowScrollingDelta()
            } ?: false
        }

        scrollableState.userScroll {
            var requiredAnimation = true
            while (requiredAnimation) {
                requiredAnimation = false
                if (targetScrollDelta.shouldApplyImmediately || abs(targetValue) < threshold) {
                    dispatchMouseWheelScroll(targetValue)
                    requiredAnimation = waitNextScrollDelta(
                        timeoutMillis = ScrollProgressTimeout,

                        // Apply the next event without `ProgressTimeout` immediately too.
                        // Currently, `isPreciseWheelScroll` might be false-negative in case if
                        // precise value is almost equal regular one.
                        forceApplyImmediately = targetScrollDelta.shouldApplyImmediately
                    )
                } else {
                    // Animation will start only on the next frame,
                    // so apply threshold immediately to avoid delays.
                    val instantDelta = sign(targetValue) * threshold
                    dispatchMouseWheelScroll(instantDelta)
                    targetValue -= instantDelta

                    val durationMillis = (abs(targetValue - animationState.value) / speed)
                        .roundToInt()
                        .coerceAtMost(MaxAnimationDuration)
                    animateMouseWheelScroll(animationState, targetValue, durationMillis) { lastValue ->
                        // Sum delta from all pending events to avoid multiple animation restarts.
                        val nextScrollDelta = channel.sumOrNull()
                        if (nextScrollDelta != null) {
                            targetScrollDelta += nextScrollDelta
                            targetValue = targetScrollDelta.value.reverseIfNeeded().toFloat()

                            requiredAnimation = !(targetValue - lastValue).isLowScrollingDelta()
                        }
                        nextScrollDelta != null
                    }
                    if (!requiredAnimation) {
                        // If it's completed, wait the next event with timeout before resetting progress flag
                        requiredAnimation = waitNextScrollDelta(ScrollProgressTimeout - durationMillis)
                    }
                }
            }
        }
        val velocityPxInMs = minOf(abs(targetValue) / MaxAnimationDuration, speed)
        val velocity = Velocity.Zero.update(sign(targetValue).reverseIfNeeded() * velocityPxInMs * 1000)
        coroutineScope.onScrollStopped(velocity)
    }

    private suspend fun ScrollScope.animateMouseWheelScroll(
        animationState: AnimationState<Float, AnimationVector1D>,
        targetValue: Float,
        durationMillis: Int,
        shouldCancelAnimation: (lastValue: Float) -> Boolean
    ) {
        var lastValue = animationState.value
        animationState.animateTo(
            targetValue,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            sequentialAnimation = true
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

    private fun ScrollScope.dispatchMouseWheelScroll(delta: Float) = with(scrollingLogic) {
        val offset = delta.reverseIfNeeded().toOffset()
        val consumed = dispatchScroll(offset, NestedScrollSource.Wheel)
        consumed.reverseIfNeeded().toFloat()
    }
}

/*
 * Returns true, if the value is too low for visible change in scroll (consumed delta, animation-based change, etc),
 * false otherwise
 */
private inline fun Float.isLowScrollingDelta(): Boolean = abs(this) < 0.5f

private val AnimationThreshold = 6.dp // (AnimationSpeed * MaxAnimationDuration) / (1000ms / 60Hz)
private val AnimationSpeed = 1.dp // dp / ms
private const val MaxAnimationDuration = 100 // ms
private const val ScrollProgressTimeout = 50L // ms
