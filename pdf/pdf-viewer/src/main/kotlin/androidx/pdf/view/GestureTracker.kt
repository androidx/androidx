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

package androidx.pdf.view

import android.content.Context
import android.graphics.PointF
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.ViewConfiguration
import android.view.ViewParent
import androidx.pdf.featureflag.PdfFeatureFlags
import androidx.pdf.view.GestureTracker.Gesture
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Processes [MotionEvent] and interprets them as [Gesture]s. Intended to be plugged in to
 * [android.view.View.onTouchEvent]. Provides callbacks via the [GestureHandler] listener, which
 * includes the signals from [SimpleOnGestureListener] and [OnScaleGestureListener] as well as start
 * and end signals for all detected [Gesture]s.
 */
internal class GestureTracker(context: Context) {
    /**
     * Used for requestDisallowInterceptTouchEvent() so that the fling and scroll gestures can be
     * managed.
     */
    private var parent: ViewParent? = null

    /** Minimal identifier for a [MotionEvent]. */
    internal class EventId(event: MotionEvent) {
        /** Returns the [MotionEvent.getEventTime] of the event. */
        val eventTimeMs: Long = event.eventTime

        /** Returns the [MotionEvent.getAction] code for the event. */
        val eventAction: Int = event.actionMasked

        fun matches(other: MotionEvent?): Boolean {
            return other != null &&
                eventTimeMs == other.eventTime &&
                eventAction == other.actionMasked
        }
    }

    /** A recognized user gesture. */
    internal enum class Gesture {
        /** First touch event, usually [MotionEvent.ACTION_DOWN] */
        TOUCH,

        /** First tap, to be confirmed as [SINGLE_TAP], or superseded by another gesture */
        FIRST_TAP,

        /**
         * [FIRST_TAP] after it's confirmed to be a single tap and not the start of a more complex
         * gesture.
         */
        SINGLE_TAP,

        /**
         * Two consecutive [MotionEvent.ACTION_DOWN] events within
         * [ViewConfiguration.getDoubleTapTimeout]
         */
        DOUBLE_TAP,

        /** Press and hold for [ViewConfiguration.getLongPressTimeout] or longer */
        LONG_PRESS,

        /** Touch and drag, not aligned on any one axis */
        DRAG,

        /** Touch and drag along the X axis */
        DRAG_X,

        /** Touch and drag along the Y axis */
        DRAG_Y,

        /** Touch, quickly drag, and release */
        FLING,

        /** Either pinch-to-zoom or a quick scale gesture, as detected by [ScaleGestureDetector] */
        ZOOM;

        /** True if this [Gesture] is a better guess than [other] in the case of ambiguity */
        fun supersedes(other: Gesture?): Boolean {
            if (other == this) {
                return false
            }
            if (other == null || other == TOUCH) {
                // Every Gesture is finer than nothing or a TOUCH.
                return true
            }
            if (other == FIRST_TAP) {
                // TAP is overridden by any other Gesture except TOUCH.
                return this != TOUCH
            }
            if (other == DOUBLE_TAP) {
                // A Double tap is overridden by any drag while on the second tap, or a zoom (quick
                // scale) gesture
                return this == DRAG || (this == DRAG_X) || (this == DRAG_Y) || (this == ZOOM)
            }
            return when (this) {
                FLING,
                ZOOM -> true
                else -> other == LONG_PRESS
            }
        }
    }

    private val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout()
    private val moveSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val listener = DetectorListener()
    private val zoomDetector = ScaleGestureDetector(context, listener)
    private val moveDetector =
        GestureDetector(context, listener).apply {
            // Detection of double tap on the main detector messes up with everything else, so
            // divert it on a secondary detector:
            setOnDoubleTapListener(null)
        }
    private val doubleTapDetector =
        GestureDetector(context, SimpleOnGestureListener()).apply {
            setOnDoubleTapListener(listener)
        }

    var delegate: GestureHandler? = null

    /**
     * Whether we are currently tracking a gesture in progress, i.e. between the initial ACTION_DOWN
     * and the end of the gesture.
     */
    private var tracking = false

    private val touchDown = PointF()
    private var lastEvent: EventId? = null
    private var detectedGesture: Gesture? = null

    /**
     * Feed an event into this tracker. To be plugged in a [android.view.View.onTouchEvent]
     *
     * @param event The event.
     * @param viewParent [ViewParent] of the [PdfView]
     * @param contentAtEdge Represents if the content in the viewport is currently at edge or not.
     * @return true if the event was recorded, false if it was discarded as a duplicate
     */
    fun feed(
        event: MotionEvent,
        viewParent: ViewParent? = null,
        contentAtEdge: Boolean = false,
    ): Boolean {
        parent = if (contentAtEdge) viewParent else null
        if (lastEvent?.matches(event) == true) {
            // We have already processed this event in this way (handling or non-handling).
            return false
        }

        if (!tracking) {
            initTracking(event.x, event.y)
            delegate?.onGestureStart()
        }

        moveDetector.onTouchEvent(event)
        if (!shouldSkipZoomDetector(event)) {
            zoomDetector.onTouchEvent(event)
        }
        doubleTapDetector.onTouchEvent(event)

        if (event.actionMasked == MotionEvent.ACTION_UP) {
            if (detectedGesture == Gesture.DOUBLE_TAP && delegate != null) {
                // Delayed from detection which happens too early.
                delegate?.onDoubleTap(event)
            }
            if (detectedGesture != Gesture.FIRST_TAP) {
                // All gestures but FIRST_TAP are final, should end gesture here.
                endGesture()
            }
        }

        if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
            endGesture()
        }

        lastEvent = EventId(event)
        return true
    }

    private fun endGesture() {
        parent?.requestDisallowInterceptTouchEvent(false)
        tracking = false
        if (delegate != null) {
            delegate?.onGestureEnd(detectedGesture)
        }
    }

    /** Returns whether the currently detected gesture matches any of [gestures]. */
    fun matches(vararg gestures: Gesture): Boolean {
        for (g in gestures) {
            if (detectedGesture == g) {
                return true
            }
        }
        return false
    }

    private fun getDistance(event: MotionEvent, axis: Int): Float {
        when (axis) {
            MotionEvent.AXIS_X -> return abs(event.x - touchDown.x)
            MotionEvent.AXIS_Y -> return abs(event.y - touchDown.y)
            NO_AXIS -> {
                val x = event.x - touchDown.x
                val y = event.y - touchDown.y
                return sqrt(x * x + y * y)
            }
            else -> throw IllegalArgumentException("Wrong axis value $axis")
        }
    }

    private fun detected(gesture: Gesture) {
        if (gesture.supersedes(detectedGesture)) {
            detectedGesture = gesture
        }
    }

    private fun initTracking(x: Float, y: Float) {
        tracking = true
        touchDown.set(x, y)
        detectedGesture = Gesture.TOUCH
    }

    /**
     * Returns whether to skip passing [event] to the [zoomDetector].
     *
     * [ScaleGestureDetector] sometimes misinterprets scroll gestures performed in quick succession
     * for a quick scale (double-tap-and-drag to zoom) gesture. This is because [GestureDetector]'s
     * double tap detection logic compares the position of the first [MotionEvent.ACTION_DOWN] event
     * to the second [MotionEvent.ACTION_DOWN] event, but ignores where the first gesture's
     * [MotionEvent.ACTION_UP] event took place. In a drag/fling gesture, the up event happens far
     * from the down event, but if a second drag/fling has its down event near the previous
     * gesture's down event (and occurs within [doubleTapTimeout] of the previous up event), a quick
     * scale will be detected.
     */
    private fun shouldSkipZoomDetector(event: MotionEvent): Boolean {
        if (lastEvent == null || lastEvent?.eventAction != MotionEvent.ACTION_UP) {
            return false
        }
        if (!SCROLL_GESTURES.contains(detectedGesture)) {
            return false
        }
        val lastEventTime = lastEvent?.eventTimeMs ?: Int.MAX_VALUE.toLong()
        val deltaTime = event.eventTime - lastEventTime

        return deltaTime < doubleTapTimeout
    }

    /** A recipient for all gesture handling. */
    open class GestureHandler : SimpleOnGestureListener(), OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {}

        /** Called at the start of any gesture, before any other callback */
        open fun onGestureStart() {}

        /**
         * Called at the end of any gesture, after any other callback
         *
         * @param gesture The detected gesture that just ended
         */
        open fun onGestureEnd(gesture: Gesture?) {}
    }

    /** The listener used for detecting various gestures. */
    private inner class DetectorListener : SimpleOnGestureListener(), OnScaleGestureListener {
        override fun onShowPress(e: MotionEvent) {
            if (delegate != null) {
                delegate?.onShowPress(e)
            }
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            detected(Gesture.FIRST_TAP)
            if (delegate != null) {
                delegate?.onSingleTapUp(e)
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            detected(Gesture.SINGLE_TAP)
            if (delegate != null) {
                delegate?.onSingleTapConfirmed(e)
            }
            // This comes from a delayed call from the doubleTapDetector, not an event
            endGesture()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Double-taps are only valid if the first gesture was just a FIRST_TAP, nothing else.
            if (detectedGesture == Gesture.FIRST_TAP) {
                detected(Gesture.DOUBLE_TAP)
                // The delegate is called on the corresponding UP event, because we can be not
                // handling the event yet
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            val dx = getDistance(e2, MotionEvent.AXIS_X)
            val dy = getDistance(e2, MotionEvent.AXIS_Y)

            // Release the gesture if the detected gesture is a horizontal scroll
            if (detectedGesture == Gesture.DRAG_X) {
                parent?.requestDisallowInterceptTouchEvent(false)
            }

            if (dx > moveSlop && dx > DRAG_X_MULTIPLIER * dy) {
                detected(Gesture.DRAG_X)
            } else if (dy > moveSlop && dy > DRAG_Y_MULTIPLIER * dx) {
                detected(Gesture.DRAG_Y)
            } else if (getDistance(e2, NO_AXIS) > moveSlop) {
                detected(Gesture.DRAG)
            }
            if (delegate != null) {
                delegate?.onScroll(e1, e2, distanceX, distanceY)
            }

            return false
        }

        override fun onLongPress(e: MotionEvent) {
            detected(Gesture.LONG_PRESS)
            if (!PdfFeatureFlags.isMultiTouchScrollEnabled && delegate != null) {
                delegate?.onLongPress(e)
            }
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            detected(Gesture.FLING)
            if (delegate != null) {
                return delegate?.onFling(e1, e2, velocityX, velocityY) != false
            }
            return false
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (delegate != null) {
                return delegate?.onScale(detector) != false
            }
            // Return true is required to keep the gesture detector happy (and the events flowing).
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            detected(Gesture.ZOOM)
            if (delegate != null) {
                return delegate?.onScaleBegin(detector) != false
            }
            // Return true is required to keep the gesture detector happy (and the events flowing).
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (delegate != null) {
                delegate?.onScaleEnd(detector)
            }
        }
    }
}

/** The set of [Gesture]s considered to constitute scrolling */
private val SCROLL_GESTURES: Set<Gesture> =
    setOf(Gesture.DRAG, Gesture.DRAG_X, Gesture.DRAG_Y, Gesture.FLING)

/**
 * The factor by which the swipe needs to be bigger horizontally than vertically to be considered
 * DRAG_X.
 */
private const val DRAG_X_MULTIPLIER = 1f

/**
 * The factor by which the swipe needs to be bigger vertically than horizontally to be considered
 * DRAG_Y.
 */
private const val DRAG_Y_MULTIPLIER = 3f

private const val NO_AXIS: Int = -1
