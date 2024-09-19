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

package androidx.ink.authoring.testing

import android.view.MotionEvent
import android.view.MotionEvent.AXIS_TILT
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * Builds MotionEvents on demand to simulate 2 to 5 streams of input traveling over time from
 * ACTION_DOWN, through ACTION_MOVE, and ending in ACTION_UP, where pointers 2 and higher will be
 * added incrementally in ascending order with ACTION_POINTER_DOWN, all pointers will have
 * ACTION_MOVE run 4x times, and then will be removed incrementally in descending order with
 * ACTION_POINTER_UP. [pointerId], [startX], [startY], [xIncrement], and [yIncrement] are arrays of
 * values for each pointer in the gesture, such that startX = [10, 50, 75] would mean that the first
 * pointer starts at x=10, the second pointer starts at x=50, and the third pointer starts at x=75.
 * The default values are the appropriate size for the [pointerCount], but if you provide your own
 * be sure that their sizes are all equal to the [pointerCount].
 *
 * Note that the timestamps of the generated events start with downtime = 1000L and not
 * SystemClock.uptimeMillis.
 *
 * MotionEvents that aren't close enough to system time aren't guaranteed to be received if
 * dispatched on an Android View. For this use case, make sure to set [downtime] to
 * [android.os.SystemClock.uptimeMillis].
 *
 * Change [historyIncrements] to have more than one input point per [MotionEvent]. It represents the
 * number of steps from the previous event to the next one, so there will be `historyIncrements - 1`
 * historical events preceding the primary [MotionEvent] data.
 */
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
public class MultiTouchInputBuilder(
    private val pointerCount: Int = 2,
    private val pointerId: IntArray = IntArray(pointerCount) { 9000 + it },
    private val toolTypes: IntArray = IntArray(pointerCount) { MotionEvent.TOOL_TYPE_FINGER },
    private val startX: FloatArray = FloatArray(pointerCount) { 100F * it },
    private val startY: FloatArray = FloatArray(pointerCount),
    /** Set an entry to null if that pointer should not have pressure. */
    private val startPressure: Array<Float?> =
        Array(pointerCount) {
            if (toolTypes[it] == MotionEvent.TOOL_TYPE_STYLUS) 0.05F * (it + 1) else null
        },
    /** Set an entry to null if that pointer should not have orientation. */
    private val startOrientation: Array<Float?> =
        Array(pointerCount) {
            if (toolTypes[it] == MotionEvent.TOOL_TYPE_STYLUS) 0.01F * (it + 1) else null
        },
    /**
     * Set an entry to null if that pointer should not have tilt.
     *
     * Note: Tilt does not seem to be supported currently in Robolectric, but it can be used for
     * emulator/device tests.
     */
    private val startTilt: Array<Float?> =
        Array(pointerCount) {
            if (toolTypes[it] == MotionEvent.TOOL_TYPE_STYLUS) 0.07F * (it + 1) else null
        },
    private val historyIncrements: Int = 1,
    private val timeIncrementMillis: Long = 10L * historyIncrements,
    private val xIncrement: FloatArray = FloatArray(pointerCount) { 100F },
    private val yIncrement: FloatArray = FloatArray(pointerCount) { 100F },
    private val pressureIncrement: FloatArray = FloatArray(pointerCount) { 0.1F },
    private val orientationIncrement: FloatArray = FloatArray(pointerCount) { 0.2F },
    private val tiltIncrement: FloatArray = FloatArray(pointerCount) { 0.1F },
    private val downFlags: IntArray = IntArray(pointerCount),
    private val upFlags: IntArray = IntArray(pointerCount),
    private val downtime: Long = 1000L,
) {
    private var moveCount: Int = 0

    init {
        require(timeIncrementMillis % historyIncrements == 0L) {
            "Time increment ($timeIncrementMillis) must be a multiple of the history increments ($historyIncrements)."
        }
        require(
            pointerId.size == pointerCount &&
                toolTypes.size == pointerCount &&
                startX.size == pointerCount &&
                startY.size == pointerCount &&
                startPressure.size == pointerCount &&
                startOrientation.size == pointerCount &&
                startTilt.size == pointerCount &&
                xIncrement.size == pointerCount &&
                yIncrement.size == pointerCount &&
                pressureIncrement.size == pointerCount &&
                orientationIncrement.size == pointerCount &&
                tiltIncrement.size == pointerCount &&
                downFlags.size == pointerCount &&
                upFlags.size == pointerCount
        ) {
            "Input arrays must be the same size as the pointerCount."
        }
    }

    /**
     * Perform [block] with a stream of [MotionEvent] such that the stream begins with one pointer
     * and ACTION_DOWN, where pointers 2 and higher will be added incrementally in ascending order
     * with ACTION_POINTER_DOWN, all pointers will have ACTION_MOVE run 4x times, and then will be
     * removed incrementally in descending order with ACTION_POINTER_UP.
     */
    public fun runGestureWith(block: (MotionEvent) -> Unit) {
        val arrayOfPointerProperties = ArrayList<PointerProperties>()
        val arrayOfPointerCoords = ArrayList<PointerCoords>()

        // Add each pointer incrementally.
        for (p in 0 until pointerCount) {
            arrayOfPointerProperties.add(
                PointerProperties().apply {
                    id = pointerId[p]
                    toolType = toolTypes[p]
                }
            )
            arrayOfPointerCoords.add(
                PointerCoords().apply {
                    x = startX[p]
                    y = startY[p]
                    startPressure[p]?.let { pressure = it }
                    startOrientation[p]?.let { orientation = it }
                    startTilt[p]?.let { setAxisValue(AXIS_TILT, it) }
                }
            )
            val ev =
                obtainWithDefaults(
                    downTime = downtime,
                    eventTime = downtime,
                    action =
                        if (p == 0) MotionEvent.ACTION_DOWN
                        else
                            (MotionEvent.ACTION_POINTER_DOWN or
                                (p shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)),
                    pointerCount = p + 1,
                    arrayOfPointerProperties.toTypedArray(),
                    arrayOfPointerCoords.toTypedArray(),
                    metaState = 0,
                    buttonState = 0,
                    xPrecision = 0.001F,
                    yPrecision = 0.001F,
                    deviceId = 0,
                    edgeFlags = 0,
                    source = 0,
                    flags = downFlags[p],
                )
            ev.use(block)
        }
        moveCount++

        // Perform 4x move actions with all pointers touching.
        repeat(4) {
            // Treat the original event obtain as the first history increment.
            for (p in 0 until pointerCount) {
                val previousPointerCoords = arrayOfPointerCoords[p]
                arrayOfPointerCoords[p] =
                    PointerCoords().apply {
                        x = previousPointerCoords.x + xIncrement[p] / historyIncrements
                        y = previousPointerCoords.y + yIncrement[p] / historyIncrements
                        if (startPressure[p] != null) {
                            pressure =
                                previousPointerCoords.pressure +
                                    pressureIncrement[p] / historyIncrements
                        }
                        if (startOrientation[p] != null) {
                            orientation =
                                previousPointerCoords.orientation +
                                    orientationIncrement[p] / historyIncrements
                        }
                        if (startTilt[p] != null) {
                            setAxisValue(
                                AXIS_TILT,
                                previousPointerCoords.getAxisValue(AXIS_TILT) +
                                    tiltIncrement[p] / historyIncrements,
                            )
                        }
                    }
            }
            val ev =
                obtainWithDefaults(
                    downTime = downtime,
                    eventTime =
                        downtime +
                            (moveCount - 1) * timeIncrementMillis +
                            timeIncrementMillis / historyIncrements,
                    action = MotionEvent.ACTION_MOVE,
                    pointerCount,
                    arrayOfPointerProperties.toTypedArray(),
                    arrayOfPointerCoords.toTypedArray(),
                    metaState = 0,
                    buttonState = 0,
                    xPrecision = 0.001F,
                    yPrecision = 0.001F,
                    deviceId = 0,
                    edgeFlags = 0,
                    source = 0,
                    flags = 0,
                )
            // Start with the second history increment and go all the way through the primary event
            // value.
            for (h in 2..historyIncrements) {
                for (p in 0 until pointerCount) {
                    val previousPointerCoords = arrayOfPointerCoords[p]
                    arrayOfPointerCoords[p] =
                        PointerCoords().apply {
                            x = previousPointerCoords.x + xIncrement[p] / historyIncrements
                            y = previousPointerCoords.y + yIncrement[p] / historyIncrements
                            if (startPressure[p] != null) {
                                pressure =
                                    previousPointerCoords.pressure +
                                        pressureIncrement[p] / historyIncrements
                            }
                            if (startOrientation[p] != null) {
                                orientation =
                                    previousPointerCoords.orientation +
                                        orientationIncrement[p] / historyIncrements
                            }
                            if (startTilt[p] != null) {
                                setAxisValue(
                                    AXIS_TILT,
                                    previousPointerCoords.getAxisValue(AXIS_TILT) +
                                        tiltIncrement[p] / historyIncrements,
                                )
                            }
                        }
                }
                ev.addBatch(
                    downtime +
                        (moveCount - 1) * timeIncrementMillis +
                        (timeIncrementMillis / historyIncrements) * h,
                    arrayOfPointerCoords.toTypedArray(),
                    /* metaState = */ 0,
                )
            }
            ev.use(block)
            moveCount++
        }

        // Remove each pointer incrementally. Up events do not contain history, so ignore
        // `historyIncrements`.
        for (p in (pointerCount - 1) downTo 0) {
            val ev =
                obtainWithDefaults(
                    downTime = downtime,
                    eventTime = downtime + moveCount * timeIncrementMillis,
                    action =
                        if (p == 0) MotionEvent.ACTION_UP
                        else
                            (MotionEvent.ACTION_POINTER_UP or
                                (p shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)),
                    pointerCount = p + 1,
                    arrayOfPointerProperties.toTypedArray(),
                    arrayOfPointerCoords.toTypedArray(),
                    metaState = 0,
                    buttonState = 0,
                    xPrecision = 0.001F,
                    yPrecision = 0.001F,
                    deviceId = 0,
                    edgeFlags = 0,
                    source = 0,
                    flags = upFlags[p],
                )
            ev.use(block)
            arrayOfPointerProperties.removeAt(p)
            arrayOfPointerCoords.removeAt(p)
        }
        moveCount++
    }

    private fun MotionEvent.use(block: (MotionEvent) -> Unit) {
        try {
            block(this)
        } finally {
            recycle()
        }
    }

    public companion object {
        /**
         * Creates a stream of MotionEvents for a two-finger gesture such that the canvas shows
         * strokes at [zoomFactor] times their original size centered about ([centerX], [centerY]).
         *
         * Example: zoomFactor = 2 will make all strokes appear twice their original size. Strokes
         * will be larger on the screen and less of the canvas will be shown.
         */
        public fun pinchOutWithFactor(
            centerX: Float,
            centerY: Float,
            zoomFactor: Float = 2F,
        ): MultiTouchInputBuilder =
            MultiTouchInputBuilder(
                pointerCount = 2,
                startX = floatArrayOf(centerX - 100F, centerX + 100F),
                startY = floatArrayOf(centerY, centerY),
                timeIncrementMillis = 10,
                xIncrement =
                    floatArrayOf(
                        if (zoomFactor <= 1f) 0F else -(100 / 4F) * (zoomFactor - 1),
                        if (zoomFactor <= 1f) 0F else (100 / 4F) * (zoomFactor - 1),
                    ),
                yIncrement = floatArrayOf(0F, 0F),
            )

        /**
         * Creates a stream of MotionEvents for a two-finger gesture such that the canvas shows
         * strokes at [zoomFactor] times their original size centered about ([centerX], [centerY]).
         *
         * Example: zoomFactor = 0.5 will make all strokes appear half their original size. Strokes
         * will be smaller on the screen and more of the canvas will be shown.
         */
        public fun pinchInWithFactor(
            centerX: Float,
            centerY: Float,
            zoomFactor: Float = 0.5F,
        ): MultiTouchInputBuilder =
            MultiTouchInputBuilder(
                pointerCount = 2,
                startX = floatArrayOf(centerX - 100F, centerX + 100F),
                startY = floatArrayOf(centerY, centerY),
                timeIncrementMillis = 10,
                xIncrement =
                    floatArrayOf(
                        if (zoomFactor <= 0f || zoomFactor >= 1F) 0F
                        else 100 * (1 - zoomFactor) / 4F,
                        if (zoomFactor <= 0f || zoomFactor >= 1F) 0F
                        else -100 * (1 - zoomFactor) / 4F,
                    ),
                yIncrement = floatArrayOf(0F, 0F),
            )

        /**
         * Creates a stream of MotionEvents for a two-finger gesture such that the canvas rotates 90
         * degrees clockwise centered about ([centerX], [centerY]).
         */
        public fun rotate90DegreesClockwise(
            centerX: Float,
            centerY: Float
        ): MultiTouchInputBuilder =
            MultiTouchInputBuilder(
                pointerCount = 2,
                startX = floatArrayOf(centerX - 50F, centerX + 50),
                startY = floatArrayOf(centerY - 50F, centerY + 50F),
                timeIncrementMillis = 10,
                xIncrement = floatArrayOf(25F, -25F),
                yIncrement = floatArrayOf(0F, 0F),
            )

        /**
         * Helper function wrapping a MotionEvent.obtain() function such that defaults are set and
         * parameter names can be listed at the time of call to this function, improving readability
         * of the long list of often unimportant parameters.
         */
        private fun obtainWithDefaults(
            downTime: Long = 1000L,
            eventTime: Long,
            action: Int,
            pointerCount: Int = 1,
            pointerProperties: Array<PointerProperties>,
            pointerCoords: Array<PointerCoords>,
            metaState: Int = 0,
            buttonState: Int = 0,
            xPrecision: Float = 0.001F,
            yPrecision: Float = 0.001F,
            deviceId: Int = 0,
            edgeFlags: Int = 0,
            source: Int = 0,
            flags: Int = 0,
        ): MotionEvent =
            MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                pointerCount,
                pointerProperties,
                pointerCoords,
                metaState,
                buttonState,
                xPrecision,
                yPrecision,
                deviceId,
                edgeFlags,
                source,
                flags,
            )
    }
}
