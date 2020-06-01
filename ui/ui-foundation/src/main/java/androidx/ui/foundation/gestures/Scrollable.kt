/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation.gestures

import androidx.animation.AnimatedFloat
import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationClockObserver
import androidx.animation.AnimationEndReason
import androidx.animation.Spring
import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.animation.asDisposableClock
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.foundation.animation.fling
import androidx.ui.unit.PxPosition

/**
 * Create [ScrollableState] for [scrollable] with default [FlingConfig] and
 * [AnimationClockObservable]
 *
 * @param onScrollDeltaConsumptionRequested callback invoked when scrollable drag/fling/smooth
 * scrolling occurs. The callback receives the delta in pixels. Callers should update their state
 * in this lambda and return amount of delta consumed
 */
@Composable
fun ScrollableState(
    onScrollDeltaConsumptionRequested: (Float) -> Float
): ScrollableState {
    val clocks = AnimationClockAmbient.current.asDisposableClock()
    val flingConfig = FlingConfig()
    return remember(clocks, flingConfig) {
        ScrollableState(onScrollDeltaConsumptionRequested, flingConfig, clocks)
    }
}

/**
 * State of the [scrollable] composable. Contains necessary information about ongoing fling and
 * provides smooth scrolling capabilities.
 *
 * @param onScrollDeltaConsumptionRequested callback invoked when scrollable drag/fling/smooth
 * scrolling occurs. The callback receives the delta in pixels. Callers should update their state
 * in this lambda and return amount of delta consumed
 * @param flingConfig configuration that specifies fling logic when scrolling ends with velocity
 * @param animationClock clock observable to run animation on. Consider querying
 * [AnimationClockAmbient] to get current composition value
 */
class ScrollableState(
    val onScrollDeltaConsumptionRequested: (Float) -> Float,
    val flingConfig: FlingConfig,
    animationClock: AnimationClockObservable
) {
    /**
     * Smooth scroll by [value] amount of pixels
     *
     * @param value delta to scroll by
     * @param onEnd lambda to be called when smooth scrolling has ended
     */
    fun smoothScrollBy(
        value: Float,
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        val to = animatedFloat.value + value
        animatedFloat.animateTo(to, onEnd = onEnd)
    }

    private val isAnimationRunningState = mutableStateOf(false)

    private val clocksProxy = object : AnimationClockObservable {
        override fun subscribe(observer: AnimationClockObserver) {
            isAnimationRunningState.value = true
            animationClock.subscribe(observer)
        }

        override fun unsubscribe(observer: AnimationClockObserver) {
            isAnimationRunningState.value = false
            animationClock.unsubscribe(observer)
        }
    }

    /**
     * whether this [ScrollableState] is currently animating/flinging
     */
    val isAnimating
        get() = isAnimationRunningState.value

    /**
     * Stop any animation, smooth scrolling or fling ongoing for this scrollable
     *
     * Call this to stop receiving scrollable deltas in [onScrollDeltaConsumptionRequested]
     */
    fun stopAnimation() {
        animatedFloat.stop()
    }

    private val animatedFloat =
        DeltaAnimatedFloat(0f, clocksProxy, onScrollDeltaConsumptionRequested)

    /**
     * current position for scrollable
     */
    internal var value: Float
        get() = animatedFloat.value
        set(value) = animatedFloat.snapTo(value)

    internal fun fling(velocity: Float, onScrollEnd: (Float) -> Unit) {
        val config = flingConfig.copy(
            onAnimationEnd = { endReason, valueLeft, velocityLeft ->
                flingConfig.onAnimationEnd?.invoke(endReason, valueLeft, velocityLeft)
                onScrollEnd(velocityLeft)
            }
        )
        animatedFloat.fling(config = config, startVelocity = velocity)
    }
}

/**
 * Enable scrolling and flinging of the modified UI element.
 *
 * Although [ScrollableState] is required for this composable to work correctly, users of this
 * composable should own, update and reflect their own state. When constructing
 * [ScrollableState], you must provide a [ScrollableState.onScrollDeltaConsumptionRequested]
 * lambda, which will be invoked every time with the delta in pixels when scroll is happening (by
 * gesture input, by smooth scrolling or flinging). In this lambda callers should update their own
 * state and reflect it on UI. The amount of scrolling delta consumed must be returned from this
 * lambda.
 *
 * @sample androidx.ui.foundation.samples.ScrollableSample
 *
 * @param dragDirection axis to scroll alongside
 * @param scrollableState [ScrollableState] object that holds internal state of this Scrollable,
 * invokes [ScrollableState.onScrollDeltaConsumptionRequested] callback and provides smooth
 * scrolling capabilities
 * @param onScrollStarted callback to be invoked when scroll has started from the certain
 * position on the screen
 * @param onScrollStopped callback to be invoked when scroll stops with amount of velocity
 * unconsumed provided
 * @param enabled whether of not scrolling in enabled
 */
fun Modifier.scrollable(
    dragDirection: DragDirection,
    scrollableState: ScrollableState,
    onScrollStarted: (startedPosition: PxPosition) -> Unit = {},
    onScrollStopped: (velocity: Float) -> Unit = {},
    enabled: Boolean = true
): Modifier = composed {
    onDispose {
        scrollableState.stopAnimation()
    }
    dragGestureFilter(
        dragObserver = object : DragObserver {

            override fun onStart(downPosition: PxPosition) {
                if (enabled) {
                    scrollableState.stopAnimation()
                    onScrollStarted(downPosition)
                }
            }

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                if (!enabled) return PxPosition.Origin
                val projected = dragDirection.project(dragDistance)
                val consumed = scrollableState.onScrollDeltaConsumptionRequested(projected)
                scrollableState.value = scrollableState.value + consumed
                val fractionConsumed = if (projected == 0f) 0f else consumed / projected
                return PxPosition(
                    dragDirection.xProjection(dragDistance.x) * fractionConsumed,
                    dragDirection.yProjection(dragDistance.y) * fractionConsumed
                )
            }

            override fun onCancel() {
                scrollableState.stopAnimation()
                if (enabled) onScrollStopped(0f)
            }

            override fun onStop(velocity: PxPosition) {
                if (enabled) {
                    scrollableState.fling(dragDirection.project(velocity), onScrollStopped)
                }
            }
        },
        canDrag = { direction ->
            enabled && dragDirection.isDraggableInDirection(direction, -scrollableState.value)
        },
        startDragImmediately = scrollableState.isAnimating
    )
}

private class DeltaAnimatedFloat(
    initial: Float,
    clock: AnimationClockObservable,
    private val onDelta: (Float) -> Float
) : AnimatedFloat(clock, Spring.DefaultDisplacementThreshold) {

    override var value = initial
        set(value) {
            if (isRunning) {
                val delta = value - field
                onDelta(delta)
            }
            field = value
        }
}
