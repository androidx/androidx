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

package androidx.camera.viewfinder.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.UiThread
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Detector that interprets [MotionEvent]s and notify users when a zooming gesture has occurred.
 *
 * To use this class to do pinch-to-zoom on the viewfinder:
 * - In the [OnZoomGestureListener.onZoomEvent], when receiving [ZoomEvent.Move], get the
 *   [ZoomEvent.Move.incrementalScaleFactor] and use the value with `CameraControl.setZoomRatio` if
 *   the factor is in the range of `ZoomState.getMinZoomRatio` and `ZoomState.getMaxZoomRatio`. Then
 *   create an instance of the `ZoomGestureDetector` with the [OnZoomGestureListener].
 * - In the [View.onTouchEvent], call [onTouchEvent] and pass the [MotionEvent]s to the
 *   `ZoomGestureDetector`.
 *
 * @param context The application context.
 * @param spanSlop The distance in pixels touches can wander before a gesture to be interpreted as
 *   zooming.
 * @param minSpan The distance in pixels between touches that must be reached for a gesture to be
 *   interpreted as zooming.
 * @param listener The listener to receive the callback.
 * @constructor Creates a ZoomGestureDetector for detecting zooming gesture.
 * @sample androidx.camera.viewfinder.core.samples.onTouchEventSample
 */
class ZoomGestureDetector
@SuppressLint("ExecutorRegistration")
@JvmOverloads
constructor(
    private val context: Context,
    @Px private val spanSlop: Int = ViewConfiguration.get(context).scaledTouchSlop * 2,
    @Px private val minSpan: Int = DEFAULT_MIN_SPAN,
    private val listener: OnZoomGestureListener,
) {
    /**
     * The zoom event that contains extended info about event state.
     *
     * @param eventTime The event time in milliseconds of the current event being processed, in
     *   [SystemClock.uptimeMillis] time base.
     * @param focusX The X coordinate of the current gesture's focal point in pixels.
     * @param focusY The Y coordinate of the current gesture's focal point in pixels.
     */
    abstract class ZoomEvent
    private constructor(
        @IntRange(from = 0) val eventTime: Long,
        @Px @IntRange(from = 0) val focusX: Int,
        @Px @IntRange(from = 0) val focusY: Int,
    ) {
        /**
         * The beginning of a zoom gesture. Reported by new pointers going down.
         *
         * @param eventTime The event time in milliseconds of the current event being processed, in
         *   [SystemClock.uptimeMillis] time base.
         * @param focusX The X coordinate of the current gesture's focal point in pixels.
         * @param focusY The Y coordinate of the current gesture's focal point in pixels.
         */
        class Begin(
            @IntRange(from = 0) eventTime: Long,
            @Px @IntRange(from = 0) focusX: Int,
            @Px @IntRange(from = 0) focusY: Int,
        ) : ZoomEvent(eventTime, focusX, focusY)

        /**
         * The moving events of a gesture in progress. Reported by pointer motion.
         *
         * @param eventTime The event time in milliseconds of the current event being processed, in
         *   [SystemClock.uptimeMillis] time base.
         * @param focusX The X coordinate of the current gesture's focal point in pixels.
         * @param focusY The Y coordinate of the current gesture's focal point in pixels.
         * @param incrementalScaleFactor The scaling factor from the previous zoom event to the
         *   current event. The value will be less than `1.0` when zooming out (larger FOV) and will
         *   be larger than `1.0` when zooming in (narrower FOV).
         */
        class Move(
            @IntRange(from = 0) eventTime: Long,
            @Px @IntRange(from = 0) focusX: Int,
            @Px @IntRange(from = 0) focusY: Int,
            @FloatRange(from = 0.0, fromInclusive = false) val incrementalScaleFactor: Float,
        ) : ZoomEvent(eventTime, focusX, focusY)

        /**
         * The end of a zoom gesture. Reported by existing pointers going up.
         *
         * @param eventTime The event time in milliseconds of the current event being processed, in
         *   [SystemClock.uptimeMillis] time base.
         * @param focusX The X coordinate of the current gesture's focal point in pixels.
         * @param focusY The Y coordinate of the current gesture's focal point in pixels.
         * @param incrementalScaleFactor The scaling factor from the previous zoom event to the
         *   current event. The value will be less than `1.0` when zooming out (larger FOV) and will
         *   be larger than `1.0` when zooming in (narrower FOV).
         */
        class End(
            @IntRange(from = 0) eventTime: Long,
            @Px @IntRange(from = 0) focusX: Int,
            @Px @IntRange(from = 0) focusY: Int,
            @FloatRange(from = 0.0, fromInclusive = false) val incrementalScaleFactor: Float,
        ) : ZoomEvent(eventTime, focusX, focusY)
    }

    /**
     * The listener for receiving notifications when gestures occur.
     *
     * An application will receive events in the following order:
     * - One [ZoomEvent.Begin]
     * - Zero or more [ZoomEvent.Move]
     * - One [ZoomEvent.End]
     */
    fun interface OnZoomGestureListener {
        /**
         * Responds to the events of a zooming gesture.
         *
         * Return `true` to indicate the event is handled by the listener.
         * - For [ZoomEvent.Begin] events, the detector will ignore the rest of the gesture if it's
         *   not handled. For example, if a gesture is beginning with a focal point outside of a
         *   region where it makes sense, [ZoomEvent.Begin] event may return `false` to ignore the
         *   rest of the gesture.
         * - For [ZoomEvent.Move] events, the detector will continue to accumulate movement if it's
         *   not handled. This can be useful if an application, for example, only wants to update
         *   scaling factors if the change is greater than `0.01`.
         * - For [ZoomEvent.End] events, the return value is ignored and the zoom gesture will end
         *   regardless of what is returned.
         *
         * Once receiving [ZoomEvent.End] event, [ZoomEvent.End.focusX] and [ZoomEvent.End.focusY]
         * will return focal point of the pointers remaining on the screen.
         *
         * @param zoomEvent The zoom event that contains extended info about event state.
         * @return Whether or not the detector should consider this event as handled.
         */
        @UiThread fun onZoomEvent(zoomEvent: ZoomEvent): Boolean
    }

    /**
     * The X coordinate of the current gesture's focal point in pixels. If a gesture is in progress,
     * the focal point is between each of the pointers forming the gesture.
     *
     * If [isInProgress] would return `false`, the result of this function is undefined.
     */
    @Px private var focusX = 0

    /**
     * The Y coordinate of the current gesture's focal point in pixels. If a gesture is in progress,
     * the focal point is between each of the pointers forming the gesture.
     *
     * If [isInProgress] would return `false`, the result of this function is undefined.
     */
    @Px private var focusY = 0

    /**
     * Whether the quick zoom gesture, in which the user performs a double tap followed by a swipe,
     * should perform zooming.
     *
     * If not set, this is enabled by default.
     */
    var isQuickZoomEnabled: Boolean = true

    /**
     * Whether the stylus zoom gesture, in which the user uses a stylus and presses the button,
     * should perform zooming.
     *
     * If not set, this is enabled by default.
     */
    var isStylusZoomEnabled = true

    /**
     * The average distance in pixels between each of the pointers forming the gesture in progress
     * through the focal point.
     */
    private var currentSpan = 0f

    /**
     * The previous average distance in pixels between each of the pointers forming the gesture in
     * progress through the focal point.
     */
    private var previousSpan = 0f

    /**
     * The average X distance in pixels between each of the pointers forming the gesture in progress
     * through the focal point.
     */
    private var currentSpanX = 0f

    /**
     * The average Y distance in pixels between each of the pointers forming the gesture in progress
     * through the focal point.
     */
    private var currentSpanY = 0f

    /**
     * The previous average X distance in pixels between each of the pointers forming the gesture in
     * progress through the focal point.
     */
    private var previousSpanX = 0f

    /**
     * The previous average Y distance in pixels between each of the pointers forming the gesture in
     * progress through the focal point.
     */
    private var previousSpanY = 0f

    /** The event time in milliseconds of the current event being processed. */
    private var eventTime: Long = 0

    /** Whether a zoom gesture is in progress. */
    private var isInProgress = false

    private var initialSpan = 0f
    private var prevTime: Long = 0
    private var anchoredZoomStartX = 0f
    private var anchoredZoomStartY = 0f
    private var anchoredZoomMode = ANCHORED_ZOOM_MODE_NONE
    private var gestureDetector: GestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    // Double tap: start watching for a swipe
                    anchoredZoomStartX = e.x
                    anchoredZoomStartY = e.y
                    anchoredZoomMode = ANCHORED_ZOOM_MODE_DOUBLE_TAP
                    return true
                }
            },
        )
    private var eventBeforeOrAboveStartingGestureEvent = false

    /**
     * Accepts [MotionEvent]s and dispatches events to a [OnZoomGestureListener] when appropriate.
     *
     * Applications should pass a complete and consistent event stream to this method.
     *
     * A complete and consistent event stream involves all [MotionEvent]s from the initial
     * [MotionEvent.ACTION_DOWN] to the final [MotionEvent.ACTION_UP] or
     * [MotionEvent.ACTION_CANCEL].
     *
     * @param event The event to process.
     * @return `true` if the event was processed and the detector wants to receive the rest of the
     *   [MotionEvent]s in this event stream. Return it in the [View.onTouchEvent] for a normal use
     *   case.
     */
    @UiThread
    fun onTouchEvent(event: MotionEvent): Boolean {
        eventTime = event.eventTime

        val action = event.actionMasked

        // Forward the event to check for double tap gesture
        if (isQuickZoomEnabled) {
            gestureDetector.onTouchEvent(event)
        }

        val count = event.pointerCount
        val isStylusButtonDown = (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0

        val anchoredZoomCancelled =
            anchoredZoomMode == ANCHORED_ZOOM_MODE_STYLUS && !isStylusButtonDown
        val streamComplete =
            action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL ||
                anchoredZoomCancelled

        if (action == MotionEvent.ACTION_DOWN || streamComplete) {
            // Reset any scale in progress with the listener.
            // If it's an ACTION_DOWN we're beginning a new event stream.
            // This means the app probably didn't give us all the events.
            if (isInProgress) {
                listener.onZoomEvent(
                    ZoomEvent.End(eventTime, focusX, focusY, getIncrementalScaleFactor())
                )
                isInProgress = false
                initialSpan = 0f
                anchoredZoomMode = ANCHORED_ZOOM_MODE_NONE
            } else if (inAnchoredZoomMode() && streamComplete) {
                isInProgress = false
                initialSpan = 0f
                anchoredZoomMode = ANCHORED_ZOOM_MODE_NONE
            }
            if (streamComplete) {
                return true
            }
        }

        if (
            !isInProgress &&
                isStylusZoomEnabled &&
                !inAnchoredZoomMode() &&
                !streamComplete &&
                isStylusButtonDown
        ) {
            // Start of a button zoom gesture
            anchoredZoomStartX = event.x
            anchoredZoomStartY = event.y
            anchoredZoomMode = ANCHORED_ZOOM_MODE_STYLUS
            initialSpan = 0f
        }

        val configChanged =
            action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_POINTER_UP ||
                action == MotionEvent.ACTION_POINTER_DOWN ||
                anchoredZoomCancelled

        val pointerUp = action == MotionEvent.ACTION_POINTER_UP
        val skipIndex = if (pointerUp) event.actionIndex else -1

        // Determine focal point
        var sumX = 0f
        var sumY = 0f
        val div = if (pointerUp) count - 1 else count
        val focusXFloat: Float
        val focusYFloat: Float
        if (inAnchoredZoomMode()) {
            // In anchored scale mode, the focal pt is always where the double tap
            // or button down gesture started
            focusXFloat = anchoredZoomStartX
            focusYFloat = anchoredZoomStartY
            eventBeforeOrAboveStartingGestureEvent =
                if (event.y < focusYFloat) {
                    true
                } else {
                    false
                }
        } else {
            for (i in 0 until count) {
                if (skipIndex == i) continue
                sumX += event.getX(i)
                sumY += event.getY(i)
            }
            focusXFloat = sumX / div
            focusYFloat = sumY / div
        }

        // Determine average deviation from focal point
        var devSumX = 0f
        var devSumY = 0f
        for (i in 0 until count) {
            if (skipIndex == i) continue

            // Convert the resulting diameter into a radius.
            devSumX += abs(event.getX(i) - focusXFloat)
            devSumY += abs(event.getY(i) - focusYFloat)
        }
        val devX = devSumX / div
        val devY = devSumY / div

        // Span is the average distance between touch points through the focal point;
        // i.e. the diameter of the circle with a radius of the average deviation from
        // the focal point.
        val spanX = devX * 2
        val spanY = devY * 2
        val span: Float =
            if (inAnchoredZoomMode()) {
                spanY
            } else {
                hypot(spanX, spanY)
            }

        // Dispatch begin/end events as needed.
        // If the configuration changes, notify the app to reset its current state by beginning
        // a fresh zoom event stream.
        val wasInProgress = isInProgress
        focusX = focusXFloat.roundToInt()
        focusY = focusYFloat.roundToInt()
        if (!inAnchoredZoomMode() && isInProgress && (span < minSpan || configChanged)) {
            listener.onZoomEvent(
                ZoomEvent.End(eventTime, focusX, focusY, getIncrementalScaleFactor())
            )
            isInProgress = false
            initialSpan = span
        }
        if (configChanged) {
            currentSpanX = spanX
            previousSpanX = currentSpanX
            currentSpanY = spanY
            previousSpanY = currentSpanY
            currentSpan = span
            previousSpan = currentSpan
            initialSpan = previousSpan
        }
        val minSpan = if (inAnchoredZoomMode()) spanSlop else minSpan
        if (
            !isInProgress &&
                span >= minSpan &&
                (wasInProgress || abs(span - initialSpan) > spanSlop)
        ) {
            currentSpanX = spanX
            previousSpanX = currentSpanX
            currentSpanY = spanY
            previousSpanY = currentSpanY
            currentSpan = span
            previousSpan = currentSpan
            prevTime = eventTime
            isInProgress = listener.onZoomEvent(ZoomEvent.Begin(eventTime, focusX, focusY))
        }

        // Handle motion; focal point and span/scale factor are changing.
        if (action == MotionEvent.ACTION_MOVE) {
            currentSpanX = spanX
            currentSpanY = spanY
            currentSpan = span

            var updatePrev = true

            if (isInProgress) {
                updatePrev =
                    listener.onZoomEvent(
                        ZoomEvent.Move(eventTime, focusX, focusY, getIncrementalScaleFactor())
                    )
            }

            if (updatePrev) {
                previousSpanX = currentSpanX
                previousSpanY = currentSpanY
                previousSpan = currentSpan
                prevTime = eventTime
            }
        }
        return true
    }

    private fun inAnchoredZoomMode(): Boolean {
        return anchoredZoomMode != ANCHORED_ZOOM_MODE_NONE
    }

    private fun getIncrementalScaleFactor(): Float {
        if (inAnchoredZoomMode()) {
            // Drag is moving up; the further away from the gesture start, the smaller the span
            // should be, the closer, the larger the span, and therefore the larger the scale
            val scaleUp =
                eventBeforeOrAboveStartingGestureEvent && currentSpan < previousSpan ||
                    !eventBeforeOrAboveStartingGestureEvent && currentSpan > previousSpan
            val spanDiff = (abs(1 - currentSpan / previousSpan) * SCALE_FACTOR)
            return if (previousSpan <= spanSlop) 1.0f
            else if (scaleUp) 1.0f + spanDiff else 1.0f - spanDiff
        }
        return if (previousSpan > 0) currentSpan / previousSpan else 1.0f
    }

    val timeDelta: Long
        /**
         * Returns the time difference in milliseconds between the previous accepted zooming event
         * and the current zooming event.
         *
         * @return Time difference since the last zooming event in milliseconds.
         */
        get() = eventTime - prevTime

    companion object {
        // The default minimum span that the detector interprets a zooming event with. It's set to 0
        // to give the most responsiveness.
        // TODO(b/314702145): define a different span if appropriate.
        private const val DEFAULT_MIN_SPAN = 0
        private const val SCALE_FACTOR = .5f
        private const val ANCHORED_ZOOM_MODE_NONE = 0
        private const val ANCHORED_ZOOM_MODE_DOUBLE_TAP = 1
        private const val ANCHORED_ZOOM_MODE_STYLUS = 2
    }
}
