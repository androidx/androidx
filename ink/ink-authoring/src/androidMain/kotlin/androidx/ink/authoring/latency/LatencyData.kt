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

package androidx.ink.authoring.latency

import android.util.Log
import android.view.MotionEvent
import androidx.annotation.RestrictTo
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId

/**
 * Timestamps for signpost moments in the processing of a single input event. This structure is for
 * measuring and reporting the latency in [InProgressStrokesView] and its various helper classes.
 * Timestamps are in the [System.nanoTime] timebase, which is nanoseconds since system boot, except
 * for deep sleep time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@ExperimentalLatencyDataApi
public class LatencyData {

    /**
     * The type of input event being tracked. See [strokeAction] for a potentially more relevant
     * alternative.
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var eventAction: EventAction = EventAction.UNKNOWN

    /**
     * The type of stroke action being tracked. This often aligns with [eventAction], but since
     * clients can use any [MotionEvent] for any stroke action, this may be different.
     * [strokeAction] is often much more relevant to track as a dimension affecting performance
     * compared to [eventAction].
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var strokeAction: StrokeAction = StrokeAction.UNKNOWN

    /**
     * The ID of the stroke being tracked. For a given [strokeId], there should be a stream of
     * [LatencyData] events with a [strokeAction] pattern of a [StrokeAction.START], zero or more
     * [StrokeAction.ADD] and [StrokeAction.PREDICTED_ADD], and either a [StrokeAction.FINISH] or a
     * [StrokeAction.CANCEL].
     *
     * Note that this ID has no meaning outside of the current app session, so it is not meant to be
     * logged directly. It can be used as part of client-side aggregation logic to associate
     * [LatencyData] events with one another.
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var strokeId: InProgressStrokeId = UNKNOWN_STROKE_ID

    /**
     * The number of input events, including both the "primary" (most recent) event and any
     * "historical" events batched with it, in the `MotionEvent` whose processing is described by
     * this [LatencyData]. A value of 1 means that there were no historical inputs.
     *
     * Note that this value is 1 more than `MotionEvent.getHistorySize()`.
     *
     * MOVE and PREDICTED_MOVE [EventAction]s are batched separately by Android, so the [batchSize]
     * for real and predicted inputs received at the same time are independent.
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public var batchSize: Int = Int.MIN_VALUE

    /**
     * The index of this input event in the batch: 0 <= [batchIndex] < [batchSize]. Index 0
     * signifies the earliest event; `batchSize-1` signifies the most recent event.
     *
     * It is not guaranteed that every index in the batch will be present in a [LatencyData]. START
     * and FINISH [StrokeAction]s may originate from batched MOVE [EventAction]s, in which case only
     * the primary input is used. In addition, input sanitization may drop input points.
     *
     * MOVE and PREDICTED_MOVE [EventAction]s are batched separately by Android, so the [batchIndex]
     * for real and predicted inputs received at the same time are independent.
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public var batchIndex: Int = Int.MIN_VALUE

    /**
     * Nanosecond timestamp of when the low-level input driver recorded the user input. For
     * predicted inputs, this is a future timestamp, so it may be later than other signpost times.
     * For Android U+ (API 34+), this value is just [MotionEvent.getEventTimeNanos]. On earlier
     * Android versions, only millisecond precision is available, and therefore any calculations
     * based on this value should be considered to have only millisecond accuracy.
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public var osDetectsEvent: Long = Long.MIN_VALUE

    public val isOsDetectsEventSet: Boolean
        get() = osDetectsEvent != Long.MIN_VALUE

    /**
     * Nanosecond timestamp of the start of the call to [InProgressStrokesView.startStroke],
     * [InProgressStrokesView.addToStroke], [InProgressStrokesView.finishStroke], or
     * [InProgressStrokesView.cancelStroke].
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var strokesViewGetsAction: Long = Long.MIN_VALUE

    public val isStrokesViewGetsActionSet: Boolean
        get() = strokesViewGetsAction != Long.MIN_VALUE

    /**
     * Nanosecond timestamp of when [InProgressStrokesView] finishes all geometry generation and
     * renderer draw calls.
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var strokesViewFinishesDrawCalls: Long = Long.MIN_VALUE

    /**
     * Estimated nanosecond timestamp of when the newly-drawn pixels will become visible to the
     * user. The nature of this estimate depends on the graphics backend, but this field is meant to
     * be comparable across graphics backends.
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var estimatedPixelPresentationTime: Long = Long.MIN_VALUE

    public val canvasFrontBufferStrokesRenderHelperData: CanvasFrontBufferStrokesRenderHelperData =
        CanvasFrontBufferStrokesRenderHelperData()

    /** Fields specific to [CanvasInProgressStrokesRenderHelperV29]. */
    public class CanvasFrontBufferStrokesRenderHelperData {
        /**
         * Nanosecond timestamp of when the render helper finishes draw calls to the front-buffered
         * layer. The updated appearance will be visible once the front-buffered layer is submitted
         * to the Hardware Composer, all layers are composited together, and the display scan
         * finishes.
         */
        @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public var finishesDrawCalls: Long = Long.MIN_VALUE

        public val isFinishesDrawCallsSet: Boolean
            get() = finishesDrawCalls != Long.MIN_VALUE

        public override fun toString(): String {
            return "CanvasFrontBufferStrokesRenderHelperData(finishesDrawCalls=$finishesDrawCalls)"
        }
    }

    public val hwuiInProgressStrokesRenderHelperData: HwuiInProgressStrokesRenderHelperData =
        HwuiInProgressStrokesRenderHelperData()

    /** Fields specific to [CanvasInProgressStrokesRenderHelperV21]. */
    public class HwuiInProgressStrokesRenderHelperData {
        /**
         * Nanosecond timestamp of when the render helper finishes draw calls to the
         * [android.view.View]. The updated appearance will be visible in the next animation frame.
         */
        @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public var finishesDrawCalls: Long = Long.MIN_VALUE

        public override fun toString(): String {
            return "HwuiInProgressStrokesRenderHelperData(finishesDrawCalls=$finishesDrawCalls)"
        }
    }

    init {
        reset()
    }

    /** Resets all fields to their default values. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun reset() {
        strokeAction = StrokeAction.UNKNOWN
        eventAction = EventAction.UNKNOWN
        strokeId = UNKNOWN_STROKE_ID
        batchSize = Int.MIN_VALUE
        batchIndex = Int.MIN_VALUE
        osDetectsEvent = Long.MIN_VALUE
        strokesViewGetsAction = Long.MIN_VALUE
        strokesViewFinishesDrawCalls = Long.MIN_VALUE
        estimatedPixelPresentationTime = Long.MIN_VALUE
        canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = Long.MIN_VALUE
        hwuiInProgressStrokesRenderHelperData.finishesDrawCalls = Long.MIN_VALUE
    }

    public override fun toString(): String {
        return "LatencyData(" +
            "strokeAction=$strokeAction, " +
            "eventAction=$eventAction, " +
            "strokeId=$strokeId, " +
            "batchSize=$batchSize, " +
            "batchIndex=$batchIndex, " +
            "osDetectsEvent=$osDetectsEvent, " +
            "strokesViewGetsAction=$strokesViewGetsAction, " +
            "strokesViewFinishesDrawCalls=$strokesViewFinishesDrawCalls, " +
            "estimatedPixelPresentationTime=$estimatedPixelPresentationTime, " +
            "canvasFrontBufferStrokesRenderHelperData=$canvasFrontBufferStrokesRenderHelperData, " +
            "hwuiInProgressStrokesRenderHelperData=$hwuiInProgressStrokesRenderHelperData" +
            ")"
    }

    public class StrokeAction private constructor() {
        public override fun toString(): String =
            when (this) {
                UNKNOWN -> "LatencyData.StrokeAction.UNKNOWN"
                START -> "LatencyData.StrokeAction.START"
                ADD -> "LatencyData.StrokeAction.ADD"
                PREDICTED_ADD -> "LatencyData.StrokeAction.PREDICTED_ADD"
                FINISH -> "LatencyData.StrokeAction.FINISH"
                CANCEL -> "LatencyData.StrokeAction.CANCEL"
                else -> throw IllegalStateException("Unrecognized StrokeAction: $this")
            }

        public companion object {
            public val UNKNOWN: StrokeAction = StrokeAction()
            public val START: StrokeAction = StrokeAction()
            public val ADD: StrokeAction = StrokeAction()
            public val PREDICTED_ADD: StrokeAction = StrokeAction()
            public val FINISH: StrokeAction = StrokeAction()
            public val CANCEL: StrokeAction = StrokeAction()
        }
    }

    public class EventAction private constructor() {
        public override fun toString(): String =
            when (this) {
                UNKNOWN -> "LatencyData.EventAction.UNKNOWN"
                DOWN -> "LatencyData.EventAction.DOWN"
                MOVE -> "LatencyData.EventAction.MOVE"
                PREDICTED_MOVE -> "LatencyData.EventAction.PREDICTED_MOVE"
                UP -> "LatencyData.EventAction.UP"
                CANCEL -> "LatencyData.EventAction.CANCEL"
                // The else case is impossible because each instance is a singleton.
                else -> throw IllegalStateException("Unknown EventAction $this")
            }

        public companion object {
            // Identity of these singleton constants comes from their addresses alone.
            public val UNKNOWN: EventAction = EventAction()
            public val DOWN: EventAction = EventAction()
            public val MOVE: EventAction = EventAction()
            public val PREDICTED_MOVE: EventAction = EventAction()
            public val UP: EventAction = EventAction()
            public val CANCEL: EventAction = EventAction()

            public fun fromMotionEvent(
                event: MotionEvent,
                predicted: Boolean = false
            ): EventAction =
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> DOWN
                    MotionEvent.ACTION_MOVE -> if (predicted) PREDICTED_MOVE else MOVE
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP -> UP
                    MotionEvent.ACTION_CANCEL -> CANCEL
                    else ->
                        EventAction.UNKNOWN.also {
                            Log.e(
                                "LatencyData.EventAction",
                                "Unknown MotionEvent.actionMasked ${event.actionMasked}",
                            )
                        }
                }
        }
    }

    public companion object {
        public val UNKNOWN_STROKE_ID: InProgressStrokeId = InProgressStrokeId.create()
    }
}
