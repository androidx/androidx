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

package androidx.compose.foundation.gestures

import androidx.animation.AnimatedFloat
import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationEndReason
import androidx.animation.Spring
import androidx.animation.SpringSpec
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.animation.asDisposableClock
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.ScaleObserver
import androidx.ui.core.gesture.scaleGestureFilter

/**
 * Create [ZoomableState] with default [AnimationClockObservable].
 *
 * @param onZoomDelta callback to be invoked when pinch/smooth zooming occurs. The callback
 * receives the delta as the ratio of the new size compared to the old. Callers should update
 * their state and UI in this callback.
 */
@Composable
fun ZoomableState(onZoomDelta: (Float) -> Unit): ZoomableState {
    val clocks = AnimationClockAmbient.current.asDisposableClock()
    return remember(clocks) { ZoomableState(onZoomDelta, clocks) }
}

/**
 * State of the [zoomable] composable modifier. Provides smooth scaling capabilities.
 *
 * @param onZoomDelta callback to be invoked when pinch/smooth zooming occurs. The callback
 * receives the delta as the ratio of the new size compared to the old. Callers should update
 * their state and UI in this callback.
 * @param animationClock clock observable to run animation on. Consider querying
 * [AnimationClockAmbient] to get current composition value
 */
class ZoomableState(val onZoomDelta: (Float) -> Unit, animationClock: AnimationClockObservable) {

    /**
     * Smooth scale by a ratio of [value] over the current size.
     *
     * @param value ratio over the current size by which to scale
     * @pram [onEnd] callback invoked when the smooth scaling has ended
     */
    fun smoothScaleBy(
        value: Float,
        onEnd: ((endReason: AnimationEndReason, finishValue: Float) -> Unit)? = null
    ) {
        val to = animatedFloat.value * value
        animatedFloat.animateTo(
            to,
            onEnd = onEnd,
            anim = SpringSpec(stiffness = Spring.StiffnessLow)
        )
    }

    internal fun onScale(scaleFactor: Float) = onZoomDelta(scaleFactor)

    private val animatedFloat = DeltaAnimatedScale(1f, animationClock, ::onScale)
}

/**
 * Enable zooming of the modified UI element.
 *
 * [ZoomableState.onZoomDelta] will be invoked with the change in proportion of the UI element's
 * size at each change in either ratio of the gesture or smooth scaling. Callers should update
 * their state and UI in this callback.
 *
 * @sample androidx.compose.foundation.samples.ZoomableSample
 *
 * @param zoomableState [ZoomableState] object that holds the internal state of this zoomable,
 * and provides smooth scaling capabilities.
 * @param onZoomStopped callback to be invoked when zoom has stopped.
 */
@Composable
fun Modifier.zoomable(zoomableState: ZoomableState, onZoomStopped: (() -> Unit)? = null): Modifier {
    return scaleGestureFilter(
            scaleObserver = object : ScaleObserver {
                override fun onScale(scaleFactor: Float) = zoomableState.onScale(scaleFactor)

                override fun onStop() {
                    onZoomStopped?.invoke()
                }
            }
        )
}

/**
 * Enable zooming of the modified UI element.
 *
 * [onZoomDelta] will be invoked with the change in proportion of the UI element's
 * size at each change in either position of the gesture or smooth scaling. Callers should update
 * their state and UI in this callback.
 *
 * @sample androidx.compose.foundation.samples.ZoomableSample
 *
 * @param onZoomStopped callback to be invoked when zoom has stopped.
 * @param onZoomDelta callback to be invoked when pinch/smooth zooming occurs. The callback
 * receives the delta as the ratio of the new size compared to the old. Callers should update
 * their state and UI in this callback.
 */
@Composable
fun Modifier.zoomable(
    onZoomStopped: (() -> Unit)? = null,
    onZoomDelta: (Float) -> Unit
) = Modifier.zoomable(ZoomableState(onZoomDelta), onZoomStopped)

private class DeltaAnimatedScale(
    initial: Float,
    clock: AnimationClockObservable,
    private val onDelta: (Float) -> Unit
) : AnimatedFloat(clock) {

    override var value = initial
        set(value) {
            if (isRunning) {
                val delta = value / field
                onDelta(delta)
            }
            field = value
        }
}
