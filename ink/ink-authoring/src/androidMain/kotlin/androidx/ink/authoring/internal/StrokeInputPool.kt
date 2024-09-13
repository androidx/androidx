/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.graphics.Matrix
import android.view.MotionEvent
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Helps manage [StrokeInput] objects in an efficient way, including reusing and recycling instances
 * to avoid allocating an instance each time one is needed.
 *
 * This class includes functionality to populate [StrokeInput] instances from [MotionEvent] objects.
 * This technique of populating a separate piece of data from a [MotionEvent] is required to pass
 * the input data to another thread, because a [MotionEvent] can only be used on the UI thread:
 * - That is where [MotionEvent] objects are delivered, via an `onTouch` method.
 * - The [MotionEvent] is considered invalid after `onTouch` returns, because its instance will be
 *   recycled by the OS.
 * - It is invalid to hand a [MotionEvent] off to another thread without blocking `onTouch` from
 *   returning.
 * - `onTouch` runs on the UI thread, and it is strongly discouraged to block the UI thread.
 *
 * @param preAllocatedInstances The number of [StrokeInput] instances to be allocated and added to
 *   the recycling pool immediately upon creation. If this value is slightly larger than the number
 *   of instances that will ever be needed simultaneously, then the allocation risk (with potential
 *   garbage collection penalty) can be paid once up front instead of possibly later.
 */
internal class StrokeInputPool(preAllocatedInstances: Int = 15) {

    private val pool =
        ConcurrentLinkedQueue<StrokeInput>().apply {
            for (i in 0 until preAllocatedInstances) add(StrokeInput())
        }

    /** Only usable by the UI thread for [MotionEvent] calculations. */
    private val scratchPoint = FloatArray(2)

    /**
     * Get a [StrokeInput] instance, hopefully (but not necessarily) one that was already allocated
     * and recycled, and fill it with the given values. When the caller is done using it, the
     * [StrokeInput] should be passed to [recycle] to ensure it can be reused by a future call to
     * [obtain] so that the future call should not need to allocate a new instance.
     *
     * See [StrokeInput.update] for a description of each argument.
     */
    fun obtain(
        x: Float,
        y: Float,
        @IntRange(from = 0L) elapsedTimeMillis: Long,
        toolType: InputToolType = InputToolType.UNKNOWN,
        strokeUnitLengthCm: Float = StrokeInput.NO_STROKE_UNIT_LENGTH,
        pressure: Float = StrokeInput.NO_PRESSURE,
        tiltRadians: Float = StrokeInput.NO_TILT,
        orientationRadians: Float = StrokeInput.NO_ORIENTATION,
    ): StrokeInput {
        return (pool.poll() ?: StrokeInput()).apply {
            update(
                x = x,
                y = y,
                elapsedTimeMillis = elapsedTimeMillis,
                toolType = toolType,
                strokeUnitLengthCm = strokeUnitLengthCm,
                pressure = pressure,
                tiltRadians = tiltRadians,
                orientationRadians = orientationRadians,
            )
        }
    }

    /**
     * Allow the given [StrokeInput] to be reused by a future call to [obtain]. It is illegal to
     * access this instance of [StrokeInput] after this call until it is made available for reuse
     * through [obtain].
     */
    fun recycle(strokeInput: StrokeInput) {
        pool.offer(strokeInput)
    }

    /**
     * Get a [StrokeInput] instance, hopefully (but not necessarily) one that was already allocated
     * and recycled, which contains the most recent (non-historical) input data from the given
     * [MotionEvent] at the given [pointerIndex]. This is often useful for down and up events, where
     * historical data doesn't tend to exist (or wouldn't make much sense if it did). For move
     * events, where it is useful to have the full history, see [obtainAllHistoryForMotionEvent].
     *
     * This function must be called on the UI thread, but its results can be passed to another
     * thread.
     *
     * @param event The [MotionEvent] containing the desired data.
     * @param pointerIndex The index (not ID!) of the pointer within [event] to obtain data from.
     * @param motionEventToStrokeTransform A [Matrix] that transforms the `x` and `y` position
     *   coordinates of [event] into the client-defined stroke coordinate system.
     * @param strokeStartTimeMillis The time at which the stroke started in the
     *   [android.os.SystemClock.elapsedRealtime] time base.
     * @return A [StrokeInput] instance that is populated with the appropriate input data.
     */
    @UiThread
    fun obtainSingleValueForMotionEvent(
        event: MotionEvent,
        pointerIndex: Int,
        motionEventToStrokeTransform: Matrix,
        strokeStartTimeMillis: Long,
        strokeUnitLengthCm: Float = StrokeInput.NO_STROKE_UNIT_LENGTH,
    ): StrokeInput {
        return obtainHistoricalValueForMotionEvent(
            event,
            pointerIndex,
            // `historySize` is a special value for `historyIndex` indicating the non-historical
            // input
            // point in this event.
            event.historySize,
            motionEventToStrokeTransform,
            strokeStartTimeMillis,
            strokeUnitLengthCm,
        )
    }

    /**
     * Get multiple [StrokeInput] instances added to the given [outBatch], containing all the input
     * data (both the most recent/non-historical data and the historical data) from the given
     * [MotionEvent] at the given [pointerIndex]. This is often useful for move events, where
     * historical data is expected and fill in data at a more desirable granularity. For down and up
     * events, where historical data isn't as applicable, see [obtainSingleValueForMotionEvent].
     * Note that this will produce [MotionEvent.getHistorySize] + 1 [StrokeInput] values, where the
     * final value is the non-historical (primary) value on the given [MotionEvent].
     *
     * This function must be called on the UI thread, but its results can be passed to another
     * thread.
     *
     * @param event The [MotionEvent] containing the desired data.
     * @param pointerIndex The index (not ID!) of the pointer within [event] to obtain data from.
     * @param motionEventToStrokeTransform A [Matrix] that transforms the `x` and `y` position
     *   coordinates of [event] into the client-defined stroke coordinate system.
     * @param strokeStartTimeMillis The time at which the stroke started in the
     *   [android.os.SystemClock.elapsedRealtime] time base.
     * @param outBatch The [StrokeInputBatch.Builder] that will contain the produced result values.
     *   Any existing data in here will be lost.
     */
    @UiThread
    fun obtainAllHistoryForMotionEvent(
        event: MotionEvent,
        pointerIndex: Int,
        motionEventToStrokeTransform: Matrix,
        strokeStartTimeMillis: Long,
        strokeUnitLengthCm: Float = StrokeInput.NO_STROKE_UNIT_LENGTH,
        outBatch: MutableStrokeInputBatch,
    ) {
        // This does not trim the capacity of the list, so if it was pre-allocated to a big enough
        // size
        // then adding to it would not require any allocations for resizing.
        outBatch.clear()
        // Include `historySize` in this loop to represent the non-historical input point in this
        // event.
        for (historyIndex in 0..event.historySize) {
            val input =
                obtainHistoricalValueForMotionEvent(
                    event,
                    pointerIndex,
                    historyIndex,
                    motionEventToStrokeTransform,
                    strokeStartTimeMillis,
                    strokeUnitLengthCm,
                )
            try {
                outBatch.addOrIgnore(input)
            } finally {
                recycle(input)
            }
        }
    }

    /**
     * A convenience function to [recycle] all the inputs in a [Collection]. Often called after the
     * data from [obtainAllHistoryForMotionEvent] has been used, which may be on a thread that is
     * not the UI thread.
     */
    fun recycleAll(strokeInputs: Collection<StrokeInput>) {
        strokeInputs.forEach(::recycle)
    }

    /**
     * See [obtainAllHistoryForMotionEvent]. Gets a [StrokeInput] populated with the input data from
     * the given [MotionEvent] at the given [pointerIndex] for a given [historyIndex]. The
     * [historyIndex] is between `0` and `event.historySize`, with the special value
     * `event.historySize` representing the most recent (non-historical) input data in this event.
     *
     * This function must be called on the UI thread, but its results can be passed to another
     * thread.
     */
    @UiThread
    private fun obtainHistoricalValueForMotionEvent(
        event: MotionEvent,
        pointerIndex: Int,
        historyIndex: Int,
        motionEventToStrokeTransform: Matrix,
        strokeStartTimeMillis: Long,
        strokeUnitLengthCm: Float,
    ): StrokeInput {
        scratchPoint[0] =
            event.getMaybeHistoricalAxisValue(MotionEvent.AXIS_X, pointerIndex, historyIndex)
        scratchPoint[1] =
            event.getMaybeHistoricalAxisValue(MotionEvent.AXIS_Y, pointerIndex, historyIndex)
        // Modify `scratchPoint` in place.
        motionEventToStrokeTransform.mapPoints(scratchPoint)
        return obtain(
            x = scratchPoint[0],
            y = scratchPoint[1],
            elapsedTimeMillis =
                (event.getMaybeHistoricalEventTimeMillis(historyIndex) - strokeStartTimeMillis),
            toolType = getToolTypeFromMotionEvent(event, pointerIndex),
            strokeUnitLengthCm = strokeUnitLengthCm,
            pressure =
                if (event.getToolType(pointerIndex) == MotionEvent.TOOL_TYPE_STYLUS) {
                    event
                        .getMaybeHistoricalAxisValue(
                            MotionEvent.AXIS_PRESSURE,
                            pointerIndex,
                            historyIndex
                        )
                        .coerceIn(0f, 1f)
                } else {
                    StrokeInput.NO_PRESSURE
                },
            tiltRadians =
                if (event.getToolType(pointerIndex) == MotionEvent.TOOL_TYPE_STYLUS) {
                    event
                        .getMaybeHistoricalAxisValue(
                            MotionEvent.AXIS_TILT,
                            pointerIndex,
                            historyIndex
                        )
                        .coerceIn(0f, Math.PI.toFloat() / 2F)
                } else {
                    StrokeInput.NO_TILT
                },
            orientationRadians =
                convertOrientationToStrokeInputRadians(
                    event.getToolType(pointerIndex),
                    event.getMaybeHistoricalAxisValue(
                        MotionEvent.AXIS_ORIENTATION,
                        pointerIndex,
                        historyIndex,
                    ),
                ),
        )
    }

    /** Map the [MotionEvent] tool type into a [StrokeInput] tool type. */
    private fun getToolTypeFromMotionEvent(
        motionEvent: MotionEvent,
        pointerIndex: Int,
    ): InputToolType {
        return when (motionEvent.getToolType(pointerIndex)) {
            MotionEvent.TOOL_TYPE_MOUSE -> InputToolType.MOUSE
            MotionEvent.TOOL_TYPE_STYLUS,
            MotionEvent.TOOL_TYPE_ERASER -> InputToolType.STYLUS
            else -> InputToolType.TOUCH
        }
    }

    /**
     * Gets the axis value of a historical event - one that was previously unreported, was batched
     * into this [MotionEvent], but isn't the primary (most recent) event in this [MotionEvent].
     * Normally [MotionEvent.getHistorySize] would be an invalid argument for [historyIndex], as the
     * history index is zero-based, but for convenience we treat it as a special value to get the
     * time of the primary (most recent) event in this [MotionEvent].
     *
     * See [MotionEvent.getHistoricalAxisValue].
     */
    private fun MotionEvent.getMaybeHistoricalAxisValue(
        axis: Int,
        pointerIndex: Int,
        historyIndex: Int,
    ): Float {
        return if (historyIndex == historySize) {
            getAxisValue(axis, pointerIndex)
        } else {
            getHistoricalAxisValue(axis, pointerIndex, historyIndex)
        }
    }

    /**
     * Gets the time in milliseconds of a historical event - one that was previously unreported, was
     * batched into this [MotionEvent], but isn't the primary (most recent) event in this
     * [MotionEvent]. Normally [MotionEvent.getHistorySize] would be an invalid argument for
     * [historyIndex], as the history index is zero-based, but for convenience we treat it as a
     * special value to get the time of the primary (most recent) event in this [MotionEvent].
     *
     * See [MotionEvent.getHistoricalEventTime].
     */
    private fun MotionEvent.getMaybeHistoricalEventTimeMillis(historyIndex: Int): Long {
        return if (historyIndex == historySize) {
            eventTime
        } else {
            getHistoricalEventTime(historyIndex)
        }
    }

    /**
     * Convert an orientation angle from how [MotionEvent] reports it to how [StrokeInput] expects
     * it.
     */
    private fun convertOrientationToStrokeInputRadians(toolType: Int, orientation: Float): Float {
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
            // Convert MotionEvent orientation angles into StrokeInput orientation angles.
            // MotionEvent orientation values lie in [-PI, PI] with zero where the tip of the stylus
            // is
            // pointing "up" (think the tool bar), positive values are the tip pointing "right" and
            // the
            // negative values are the tip pointing "left".
            // StrokeInput orientationRadians values lie in [0, 2PI] with zero being where the tip
            // points
            // to the "left" and increases as you rotate clockwise (towards "up", and so on).
            return (orientation + 2.5f * Math.PI.toFloat()).mod(2 * Math.PI.toFloat())
        }
        return StrokeInput.NO_ORIENTATION
    }
}
