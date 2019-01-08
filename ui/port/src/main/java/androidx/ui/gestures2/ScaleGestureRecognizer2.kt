/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures2

import androidx.ui.engine.geometry.Offset
import kotlin.math.absoluteValue
import kotlin.math.hypot

enum class ScaleDirection {
    EXPAND_X, CONTRACT_X, EXPAND_Y, CONTRACT_Y
}

interface ScaleGestureRecognizerCallback {
    fun onScaleStart() {}
    fun onScaleStop() {}
    fun onCanScale(direction: ScaleDirection) = false
    fun onAveragePointMove(offset: Offset) = Offset.zero
    fun onScaleRatio(ratio: Float) = 0.0f
}

class ScaleGestureRecognizer2(
    private val slop: Int,
    private val consumePass: PointerEventPass,
    private val callback: ScaleGestureRecognizerCallback
) : GestureRecognizer2 {

    var accumulatedAverageSpanDiff = 0.0f
    var passedSlop = false
    var points = HashMap<Int, Offset>()
    var averagePoint = Offset.zero
    var averageSpan = 0.0f

    override fun handleEvent(event: PointerEvent2, pass: PointerEventPass): PointerEvent2 {
        var pointerEvent = event

        if (pass == PointerEventPass.INITIAL_DOWN &&
            event.changedToDown(true) ||
            event.positionChanged(true)) {
            event.current.position?.let {
                points[event.id] = it
            }
        }

        if (pass == consumePass && points.size > 1) {
            if (
                (points.size == 2 && event.changedToUp(true)) ||
                event.current.cancelled
            ) {
                if (passedSlop && !event.current.cancelled) {
                    callback.onScaleStop()
                }
                passedSlop = false
            } else if (event.changedToDown(true)) {
                averagePoint = averageOffset(points)
                averageSpan = averageSpan(points, averagePoint)
            } else if (event.changedToUp(true)) {
                averagePoint = averageOffset(points, event.id)
                averageSpan = averageSpan(points, averagePoint, event.id)
            } else if (event.current.down && event.positionChanged()) {

                val newFocalPoint = averageOffset(points)
                val newAverageSpan = averageSpan(points, newFocalPoint)

                val focalPointDiff = averagePoint - newFocalPoint
                averagePoint = newFocalPoint

                val averageSpanDiff = averageSpan - newAverageSpan
                val averageSpanScale = newAverageSpan / averageSpan
                accumulatedAverageSpanDiff += averageSpanDiff
                averageSpan = newAverageSpan

                if (accumulatedAverageSpanDiff.absoluteValue > slop) {
                    passedSlop = true
                    callback.onScaleStart()
                }

                if (passedSlop) {
                    callback.onScaleRatio(averageSpanScale)
                    callback.onAveragePointMove(focalPointDiff)
                    val change = pointerEvent.positionChange(true)
                    pointerEvent = pointerEvent.consumePositionChange(change.dx, change.dy)
                }
            }
        }

        if (pass == PointerEventPass.POST_DOWN && pointerEvent.changedToUp(true)) {
            points.remove(pointerEvent.id)
        }

        return pointerEvent
    }

    private fun averageOffset(offsets: Map<Int, Offset>, toIgnore: Int? = null): Offset {
        var totalX = 0.0f
        var totalY = 0.0f
        offsets.forEach {
            if (it.key != toIgnore) {
                totalX += it.value.dx
                totalY += it.value.dy
            }
        }
        val size = if (toIgnore != null) offsets.size - 1 else offsets.size
        val averageX = totalX / size
        val averageY = totalY / size
        return Offset(averageX, averageY)
    }

    private fun averageSpan(
        offsets: Map<Int, Offset>,
        focalPoint: Offset,
        toIgnore: Int? = null
    ): Float {
        // Determine average deviation from focal point
        var devSumX = 0.0f
        var devSumY = 0.0f
        offsets.forEach {
            if (it.key != toIgnore) {
                // Convert the resulting diameter into a radius.
                devSumX += Math.abs(it.value.dx - focalPoint.dx)
                devSumY += Math.abs(it.value.dy - focalPoint.dy)
            }
        }
        val size = if (toIgnore != null) offsets.size - 1 else offsets.size
        val devX = devSumX / size
        val devY = devSumY / size

        // Span is the average distance between touch points through the focal point;
        // i.e. the diameter of the circle with a radius of the average deviation from
        // the focal point.
        val spanX = devX * 2
        val spanY = devY * 2
        return hypot(spanX, spanY)
    }
}