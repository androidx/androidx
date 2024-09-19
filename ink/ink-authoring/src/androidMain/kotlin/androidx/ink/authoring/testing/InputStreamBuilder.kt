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

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * Helper to build MotionEvents on demand to simulate a stream of input traveling over time from
 * ACTION_DOWN, through ACTION_MOVE, and ending in ACTION_UP.
 *
 * MotionEvents will be generated with a frequency of timeIncrement, but will embed historical
 * motion at 2x timeIncrement.
 *
 * Note that the timestamps of the generated events start with SystemClock.uptimeMillis and thus
 * aren't suitable for deterministic line rendering.
 *
 * MotionEvents that don't use SystemClock.uptimeMillis as their base won't be received if
 * dispatched on an Android View.
 *
 * If deterministic line rendering is needed for a test, best is to dispatch input with fixed
 * timestamps directly to the LegacyStrokeBuilder using its own input format.
 *
 * Consider if you can use [MultiTouchInputBuilder] instead with pointerCount=1, since as we
 * continue to generalize that utility there may not be much need to maintain this separately.
 */
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
public class InputStreamBuilder(
    private val streamToolType: Int = MotionEvent.TOOL_TYPE_STYLUS,
    private val buttons: Int = 0,
    private val pointerId: Int = 0,
    private val startX: Float = 0F,
    private val startY: Float = 0F,
    private val timeIncrement: Long = 10,
    private val xIncrement: Float = 50F,
    private val yIncrement: Float = 100F,
    private val cancel: Boolean = false,
) {
    private val streamDownTime: Long = SystemClock.uptimeMillis()
    private var moveCount = 0

    /**
     * Runs a [block] with the down event for this input stream and manages the MotionEvent clean
     * up.
     */
    public fun runWithDownEvent(block: (MotionEvent) -> Unit) {
        getDownEvent().use(block)
    }

    /**
     * Runs a [block] with the next move event for this input stream and manages the MotionEvent
     * clean up.
     */
    public fun runWithMoveEvent(block: (MotionEvent) -> Unit) {
        getNextMoveEvent().use(block)
    }

    /**
     * Runs a [block] with the up event for this input stream and manages the MotionEvent clean up.
     */
    public fun runWithUpEvent(block: (MotionEvent) -> Unit) {
        getUpEvent().use(block)
    }

    /**
     * Runs a [block] with the down, move, and up event for this input stream and manages the
     * MotionEvent clean up.
     */
    public fun runInputStreamWith(block: (MotionEvent) -> Unit) {
        runWithDownEvent(block)
        runWithMoveEvent(block)
        runWithUpEvent(block)
    }

    /**
     * The initial MotionEvent with ACTION_DOWN for a stream of input. Caller should call recycle()
     * to clean up resources of MotionEvent after use. Consider using method [runWithMoveEvent] to
     * avoid managing the clean up call to MotionEvent.recycle() explicitly.
     */
    public fun getDownEvent(): MotionEvent {
        return obtainWithDefaults(
            eventTime = streamDownTime,
            action = MotionEvent.ACTION_DOWN,
            pointerProperties =
                arrayOf(
                    PointerProperties().apply {
                        id = pointerId
                        toolType = streamToolType
                    }
                ),
            pointerCoords =
                arrayOf(
                    PointerCoords().apply {
                        x = startX
                        y = startY
                    }
                ),
            buttonState = buttons,
        )
    }

    /**
     * The next MotionEvent with ACTION_MOVE for a stream of input where the eventTime, pointer x
     * position, and pointer y position are all incremented from the previous MotionEvent. Caller
     * should call recycle() to clean up resources of MotionEvent after use. Consider using method
     * [runWithMoveEvent] to avoid managing the clean up call to MotionEvent.recycle() explicitly.
     */
    public fun getNextMoveEvent(): MotionEvent {
        moveCount++
        return obtainWithDefaults(
                eventTime = (streamDownTime + (moveCount - 0.5) * timeIncrement).toLong(),
                action = MotionEvent.ACTION_MOVE,
                pointerProperties =
                    arrayOf(
                        PointerProperties().apply {
                            id = pointerId
                            toolType = streamToolType
                        }
                    ),
                pointerCoords =
                    arrayOf(
                        PointerCoords().apply {
                            x = startX + xIncrement * (moveCount - 0.5f)
                            y = startY + yIncrement * (moveCount - 0.5f)
                        }
                    ),
                buttonState = buttons,
            )
            .apply {
                addBatch(
                    (streamDownTime + moveCount * timeIncrement).toLong(),
                    arrayOf(
                        PointerCoords().apply {
                            x = startX + xIncrement * moveCount
                            y = startY + yIncrement * moveCount
                        }
                    ),
                    0,
                )
            }
    }

    /**
     * The final MotionEvent with ACTION_UP for a stream of input where the eventTime, pointer x
     * position, and pointer y position are all incremented from the previous MotionEvent. Caller
     * should call recycle() to clean up resources of MotionEvent after use. Consider using method
     * [runWithUpEvent] to avoid managing the clean up call to MotionEvent.recycle() explicitly.
     */
    public fun getUpEvent(): MotionEvent {
        val action = if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP
        moveCount++
        return obtainWithDefaults(
            eventTime = streamDownTime + moveCount * timeIncrement,
            action = action,
            pointerProperties =
                arrayOf(
                    PointerProperties().apply {
                        id = pointerId
                        toolType = streamToolType
                    }
                ),
            pointerCoords =
                arrayOf(
                    PointerCoords().apply {
                        x = startX + xIncrement * moveCount
                        y = startY + yIncrement * moveCount
                    }
                ),
            buttonState = 0,
        )
    }

    public companion object {

        /**
         * Creates a stylus line of 3 motion events, designed to be called with [runInputStreamWith]
         * or the sequence [runWithDownEvent], [runWithMoveEvent], [runWithUpEvent].
         */
        public fun stylusLine(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            endWithCancel: Boolean = false,
        ): InputStreamBuilder =
            InputStreamBuilder(
                streamToolType = MotionEvent.TOOL_TYPE_STYLUS,
                pointerId = 1,
                startX = startX,
                startY = startY,
                xIncrement = (endX - startX) / 2,
                yIncrement = (endY - startY) / 2,
                cancel = endWithCancel,
            )

        /**
         * Creates a finger-drawn line of 3 motion events, designed to be called with
         * [runInputStreamWith] or the sequence [runWithDownEvent], [runWithMoveEvent], and
         * [runWithUpEvent].
         */
        public fun fingerLine(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            endWithCancel: Boolean = false,
        ): InputStreamBuilder =
            InputStreamBuilder(
                streamToolType = MotionEvent.TOOL_TYPE_FINGER,
                pointerId = 1,
                startX = startX,
                startY = startY,
                xIncrement = (endX - startX) / 2,
                yIncrement = (endY - startY) / 2,
                cancel = endWithCancel,
            )

        /**
         * Creates a mouse-drawn line of 3 motion events, designed to be called with
         * [runInputStreamWith] or the sequence [runWithDownEvent], [runWithMoveEvent], and
         * [runWithUpEvent].
         */
        public fun mouseLine(
            buttons: Int,
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            endWithCancel: Boolean = false,
        ): InputStreamBuilder =
            InputStreamBuilder(
                streamToolType = MotionEvent.TOOL_TYPE_MOUSE,
                buttons = buttons,
                pointerId = 1,
                startX = startX,
                startY = startY,
                xIncrement = (endX - startX) / 2,
                yIncrement = (endY - startY) / 2,
                cancel = endWithCancel,
            )

        public fun scrollWheel(scrollHorz: Float, scrollVert: Float, block: (MotionEvent) -> Unit) {
            val event: MotionEvent =
                obtainWithDefaults(
                    eventTime = SystemClock.uptimeMillis(),
                    action = MotionEvent.ACTION_SCROLL,
                    pointerProperties =
                        arrayOf(
                            PointerProperties().apply {
                                id = 0
                                toolType = MotionEvent.TOOL_TYPE_MOUSE
                            }
                        ),
                    pointerCoords =
                        arrayOf(
                            PointerCoords().apply {
                                setAxisValue(MotionEvent.AXIS_HSCROLL, scrollHorz)
                                setAxisValue(MotionEvent.AXIS_VSCROLL, scrollVert)
                            }
                        ),
                    source = InputDevice.SOURCE_MOUSE,
                )
            event.use(block)
        }

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
            pointerProperties: Array<MotionEvent.PointerProperties>,
            pointerCoords: Array<MotionEvent.PointerCoords>,
            metaState: Int = 0,
            buttonState: Int = 0,
            xPrecision: Float = 0.001F,
            yPrecision: Float = 0.001F,
            deviceId: Int = 0,
            edgeFlags: Int = 0,
            source: Int = 0,
            flags: Int = 0,
        ) =
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

        private fun MotionEvent.use(block: (MotionEvent) -> Unit) {
            try {
                block(this)
            } finally {
                recycle()
            }
        }
    }
}
