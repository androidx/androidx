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
import androidx.annotation.AnyThread
import androidx.annotation.CheckResult
import androidx.annotation.Size
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.ink.authoring.ExperimentalCustomShapeWorkflowApi
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressShape
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.ShapeWorkflow
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.authoring.latency.LatencyDataCallback
import androidx.ink.authoring.latency.LatencyDataPool
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.MutableBox
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import androidx.test.espresso.idling.CountingIdlingResource
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Accepts [MotionEvent] inputs for in-progress strokes, processes them into meshes, and draws them
 * to screen with as little latency as possible. This coordinates the majority of logic for the
 * public-facing [androidx.ink.authoring.InProgressStrokesView].
 *
 * A term used throughout this and related classes is a "cohort" of strokes. It refers to a group of
 * strokes that are in progress simultaneously, and due to how low latency rendering works, **must**
 * be handed off from in-progress to finished HWUI rendering all at the same time to avoid a flicker
 * during the handoff. Two strokes are considered simultaneous and in the same cohort if they are
 * present on screen during the same HWUI frame, even if the first stroke was finished earlier in
 * the same frame than the second stroke was started. This can require a stroke to stay in progress
 * longer than it may seem like it should, but this cohort boundary is required because handoff
 * synchronization depends on HWUI frames while user inputs may happen multiple times per HWUI frame
 * without a guaranteed order.
 */
@OptIn(ExperimentalLatencyDataApi::class, ExperimentalCustomShapeWorkflowApi::class)
internal class InProgressStrokesManager<
    ShapeSpecT : Any,
    InProgressShapeT : InProgressShape<ShapeSpecT, CompletedShapeT>,
    CompletedShapeT : Any,
>(
    private val inProgressStrokesRenderHelper:
        InProgressStrokesRenderHelper<ShapeSpecT, InProgressShapeT, CompletedShapeT>,
    private val shapeWorkflow: ShapeWorkflow<ShapeSpecT, InProgressShapeT, CompletedShapeT>,
    /** A lambda to run a [Runnable] on the next animation frame. */
    private val postOnAnimation: (Runnable) -> Unit,
    /** A lambda to run a [Runnable] on the next run loop of the UI thread. */
    private val postToUiThread: (Runnable) -> Unit,
    /** The callback for reporting latency data to the client. */
    private val latencyDataCallback: LatencyDataCallback = LatencyDataCallback {},
    /**
     * Monotonically non-decreasing timestamps, in the same time base used by
     * [MotionEvent.getEventTimeNanos]. Used to calculate animation progress, latency durations,
     * etc. For shape generation and rendering purposes, prefer to obtain a timestamp from this once
     * and pass it around rather than obtaining slightly different value for this in multiple
     * places. For accurate latency durations, new values should be obtained in real time.
     * Injectable for testing only.
     */
    private val getSystemElapsedTimeNanos: () -> Long = System::nanoTime,
    /** For getting instances of in-progress shapes. Injectable for testing only. */
    inProgressStrokePool: InProgressStrokePool<ShapeSpecT, InProgressShapeT> =
        InProgressStrokePoolImpl(shapeWorkflow),
) : InProgressStrokesRenderHelper.Callback<CompletedShapeT> {

    /**
     * Hook for controlling the timing of callbacks in tests that need to happen at the same time as
     * [flush] is in progress on the UI thread. If set, this will have `countDown` called on it when
     * [flush] is in progress on the UI thread.
     */
    @VisibleForTesting internal var countDownWhenFlushInProgressTestLatch: CountDownLatch? = null

    /**
     * Hook for controlling the timing of callbacks in tests that need to happen at the same time as
     * [flush] is in progress on the UI thread. If set, this will be awaited during
     * [pauseStrokeCohortHandoffs].
     */
    @VisibleForTesting internal var awaitAfterStartOfHandoffTestLatch: CountDownLatch? = null

    /**
     * The transform matrix to convert input (MotionEvent) coordinates into coordinates of this view
     * for rendering. Defaults to the identity matrix, for the case where LowLatencyView exactly
     * overlays the view from which MotionEvents are being forwarded. This should only be set from
     * the UI thread.
     */
    var motionEventToViewTransform = Matrix()
        get() = Matrix(field)
        set(value) {
            field.set(value)
            queueActionToRenderThread(MotionEventToViewTransformAction(Matrix(value)))
        }

    /**
     * Allows a test to easily wait until all in-progress strokes are completed and handed off.
     * There is no reason to set this in non-test code. The recommended approach is to include this
     * small object within production code, but actually registering it and making use of it would
     * be exclusive to test code.
     *
     * https://developer.android.com/training/testing/espresso/idling-resource#integrate-recommended-approach
     */
    var inProgressStrokeCounter: CountingIdlingResource? = null

    internal fun interface Listener<CompletedShapeT : Any> {
        /**
         * Called when there are no longer any in-progress strokes. All strokes that were in
         * progress simultaneously will be delivered in the same callback. This callback will
         * execute on the UI thread. The implementer must ensure that by the time this callback
         * function returns, these strokes are saved in a location where they will be picked up in a
         * view's next call to [onDraw], and that view's [android.view.View.invalidate] method is
         * called. Within the same UI thread run loop (HWUI frame), the provided
         * [androidx.ink.strokes.Stroke] instances will no longer be rendered by this class. Failure
         * to adhere to these guidelines will result in brief rendering errors when the stroke is
         * finished - either a gap where the stroke is not drawn during a frame, or a double draw
         * where the stroke is drawn twice and translucent strokes appear more opaque than they
         * should.
         *
         * @param strokes The finished strokes, with iteration order in stroke z-order from back to
         *   front.
         */
        @UiThread fun onAllStrokesFinished(strokes: List<FinishedStroke<CompletedShapeT>>)
    }

    /**
     * Pool of [LatencyData]s to be used and then recycled after handoff. These objects exist only
     * for tracking and reporting the latency of the input processing pipeline.
     */
    private val latencyDataPool = LatencyDataPool()

    /** The state that is accessed just by the UI thread. */
    private val uiThreadState =
        object {

            /**
             * All strokes in the current cohort, with iteration order in stroke z-order, from back
             * to front.
             */
            val currentCohort = mutableMapOf<InProgressStrokeId, UiStrokeState>()

            /**
             * Minimum delay from when the user finishes a stroke (via [finishStroke]) until
             * rendering is handed off to the client's dry layer. This only applies when
             * [InProgressStrokesRenderHelper.supportsDebounce] is true, which currently is only for
             * [CanvasInProgressStrokesRenderHelperV29].
             *
             * Consider moving debouncing logic into [CanvasInProgressStrokesRenderHelperV29] and
             * not making it configurable by developers, e.g. by making use of
             * [pauseStrokeCohortHandoffs].
             */
            var cohortHandoffDebounceDurationMs = 0L

            var cohortHandoffAsap = false

            /**
             * The timestamp, in the same time base as [getSystemElapsedTimeNanos], when a stroke
             * most recently had its final input via either [finishStroke] or [cancelStroke]. This
             * is used to determine whether the current stroke cohort is eligible for handoff, or
             * not yet due to debouncing.
             */
            var lastStrokeInputCompletedSystemElapsedTimeMillis = Long.MIN_VALUE

            /** To notify when strokes have been completed. Owned by the UI thread. */
            val listeners = mutableSetOf<Listener<CompletedShapeT>>()
        }
        @UiThread
        get() {
            assertOnUiThread()
            return field
        }

    /**
     * Runnable for calling [handOffLatencyDataToClient] on the UI thread, cached to avoid per-draw
     * allocation of a method reference.
     */
    val handOffLatencyDataToClientRunnable = Runnable { handOffLatencyDataToClient() }

    /** The state that is accessed just by the render thread. */
    private val renderThreadState =
        object {
            /**
             * Runs [queueAnimationFrameAction] at most once per frame, even if this is passed to
             * [postOnAnimation] more than once during that frame.
             */
            val queueAnimationFrameActionOnce = AtMostOnceAfterSetUp(::queueAnimationFrameAction)

            /**
             * Strokes that are being drawn by this class, with map iteration order in stroke
             * z-order from back to front.
             */
            val toDrawStrokes =
                mutableMapOf<
                    InProgressStrokeId,
                    RenderThreadStrokeState<ShapeSpecT, InProgressShapeT, CompletedShapeT>,
                >()

            /** Whether a stroke was finished or canceled this draw. */
            var maybeCompletedCohortThisDraw = false

            /**
             * Contains instances of in-progress shapes that are not currently in use (does not
             * belong to [toDrawStrokes]) and are ready to be used in a new stroke. This is to reuse
             * memory that has already been allocated to improve performance after the first few
             * strokes, and to minimize memory fragmentation that can affect the health of the app's
             * process over time
             */
            val inProgressStrokePool = inProgressStrokePool

            /**
             * [LatencyData]s for the [Action]s that were processed in the latest call to [onDraw].
             */
            val latencyDatas: ArrayDeque<LatencyData> =
                ArrayDeque<LatencyData>(initialCapacity = 30)

            /**
             * The render thread's copy of LowLatencyView.motionEventToViewTransform. This is a copy
             * for thread safety.
             */
            val motionEventToViewTransform = Matrix()

            /**
             * Allocated once and reused on each draw to hold the result of a matrix multiplication.
             */
            val strokeToViewTransform = Matrix()

            /**
             * Pre-allocated list to contain actions that have been handled but need further
             * processing. Used locally only in [onDraw].
             */
            val handledActions = arrayListOf<Action>()

            /**
             * Allocated once and reused multiple times per draw to hold updated areas of strokes.
             */
            val updatedRegion = BoxAccumulator()

            /** Allocated once and reused multiple times per draw. */
            val scratchEnvelope = BoxAccumulator()

            /** Allocated once and reused multiple times per draw. */
            val scratchRect = MutableBox()
        }
        @WorkerThread
        get() {
            assertOnRenderThread()
            return field
        }

    /** The state that is accessed by more than one thread. Be careful here! */
    private val threadSharedState =
        object {
            val cohortHandoffPaused = AtomicBoolean(false)

            /**
             * Runs [onEndOfStrokeCohortCheck] at most once per frame, even if this is passed to
             * [postOnAnimation] more than once during that frame.
             */
            val checkEndOfStrokeCohortOnce = AtMostOnceAfterSetUp(::onEndOfStrokeCohortCheck)

            /**
             * Finished strokes that have just been generated. Produced on the render thread and
             * consumed on the UI thread.
             */
            val finishedStrokes = ConcurrentLinkedQueue<FinishedStroke<CompletedShapeT>>()

            /**
             * Used to hand off input events and other render thread actions across threads. This is
             * added to from the UI thread when inputs are received or other actions are queued, and
             * consumed from the render thread on the next draw.
             */
            val actions = ConcurrentLinkedQueue<Action>()

            /**
             * Reuse input objects so they don't need to be constantly allocated for each input.
             * This is added to from the render thread after it finishes processing an [AddAction],
             * and consumed from the UI thread when [addToStroke] wants to reuse and fill in an
             * [AddAction].
             */
            val addActionPool = AddActionPool()
            val strokeInputPool = StrokeInputPool()

            /**
             * Used to hand off finished [LatencyData]s from the render thread back to the UI thread
             * for reporting to the client. This is added to from the render thread in
             * [onFrontBufferedRenderComplete] and consumed from the UI thread in
             * [handOffLatencyData].
             */
            val finishedLatencyDatas = ConcurrentLinkedQueue<LatencyData>()

            /**
             * Whether a handoff is currently in progress. If this is true, requested draws should
             * be deferred until the handoff is complete, and in-progress draws will stop processing
             * actions before StartCohortAction. [flush] will set this to true and start drawing the
             * new cohort immediately even if the graphical behavior for that is not ideal.
             */
            val newCohortStartAwaitingHandoff = AtomicBoolean(false)

            /**
             * Some implementations of [InProgressStrokesRenderHelper] allow starting on the next
             * stroke cohort before they are ready to do another subsequent handoff. In that case,
             * they call [pauseStrokeCohortHandoffs] on handoff and [resumeStrokeCohortHandoffs]
             * when they are ready to start the next handoff.
             */
            val allowHandoffs = HandoffPause()

            val currentlyHandlingActions = AtomicBoolean(false)
        }

    /** Add a listener for when strokes have been completed. Must be called on the UI thread. */
    @UiThread
    fun addListener(listener: Listener<CompletedShapeT>) {
        uiThreadState.listeners.add(listener)
    }

    /** Remove a listener for when strokes have been completed. Must be called on the UI thread. */
    @UiThread
    fun removeListener(listener: Listener<CompletedShapeT>) {
        uiThreadState.listeners.remove(listener)
    }

    /**
     * Start building a stroke with the [event] data for [pointerId].
     *
     * @param event The first [MotionEvent] as part of a Stroke's input data, typically an
     *   ACTION_DOWN.
     * @param pointerId The index of the relevant pointer in the [event].
     * @param motionEventToWorldTransform The matrix that transforms [event] coordinates into the
     *   client app's "world" coordinates, which typically is defined by how a client app's document
     *   is panned/zoomed/rotated.
     * @param strokeToWorldTransform An optional matrix that transforms this stroke into the client
     *   app's "world" coordinates, which allows the coordinates of the stroke to be defined in
     *   something other than world coordinates. Defaults to the identity matrix, in which case the
     *   stroke coordinate space is the same as world coordinate space. This matrix must be
     *   invertible.
     * @param shapeSpec Specification for the shape being started.
     * @param strokeUnitLengthCm The physical distance that the pointer must travel in order to
     *   produce an input motion of one stroke unit for this particular stroke, in centimeters.
     * @return The Stroke ID of the stroke being built, later used to identify which stroke is being
     *   added to, finished, or canceled.
     * @throws IllegalArgumentException if [strokeToWorldTransform] is not invertible.
     */
    @UiThread
    fun startStroke(
        event: MotionEvent,
        pointerId: Int,
        motionEventToWorldTransform: Matrix,
        strokeToWorldTransform: Matrix,
        shapeSpec: ShapeSpecT,
        strokeUnitLengthCm: Float,
    ): InProgressStrokeId {
        val receivedActionTimeNanos = getSystemElapsedTimeNanos()
        val pointerIndex = event.findPointerIndex(pointerId)
        require(pointerIndex >= 0) { "Pointer id $pointerId is not present in event." }
        // Set up this stroke's matrix to be used to transform MotionEvent -> stroke coordinates.
        val motionEventToStrokeTransform =
            Matrix().also {
                // Compute (world -> stroke) = (stroke -> world)^-1
                require(strokeToWorldTransform.invert(it)) {
                    "strokeToWorldTransform must be invertible, but was $strokeToWorldTransform"
                }
                // Compute (MotionEvent -> stroke) = (world -> stroke) x (MotionEvent -> world)
                it.preConcat(motionEventToWorldTransform)
            }
        val strokeId = InProgressStrokeId.create()
        return startStrokeInternal(
            // This ignores any historical inputs included in this MotionEvent. ACTION_DOWN doesn't
            // have any, and if a user is passing ACTION_MOVE to startStroke, this assumes the
            // stroke
            // starts at (eventTime, x, y), ignoring any historical inputs between that and the
            // previous MotionEvent.
            input =
                threadSharedState.strokeInputPool.obtainSingleValueForMotionEvent(
                    event,
                    pointerIndex,
                    motionEventToStrokeTransform,
                    event.eventTime,
                    strokeUnitLengthCm,
                ),
            shapeSpec = shapeSpec,
            startTimeMillis = event.eventTime,
            inputsFromMotionEvents = true,
            strokeId = strokeId,
            inputToStrokeTransform = motionEventToStrokeTransform,
            latencyData =
                latencyDataPool.obtainLatencyDataForSingleEvent(
                    event,
                    LatencyData.StrokeAction.START,
                    strokeId,
                    receivedActionTimeNanos,
                ),
        )
    }

    /**
     * Start building a stroke with the provided [input].
     *
     * @param input The first input in a stroke.
     * @param shapeSpec Specification for the shape being started.
     * @param strokeToViewTransform The [Matrix] that converts stroke coordinates as provided in
     *   [input] into the coordinate space of this view for rendering.
     * @return The Stroke ID of the stroke being built, later used to identify which stroke is being
     *   added to, finished, or canceled.
     */
    @UiThread
    fun startStroke(
        input: StrokeInput,
        shapeSpec: ShapeSpecT,
        strokeToViewTransform: Matrix,
    ): InProgressStrokeId {
        // The start time here isn't really relevant unless this override of startStroke is combined
        // with the MotionEvent override of addToStroke or finishStroke.
        return startStrokeInternal(
            input = input,
            shapeSpec = shapeSpec,
            startTimeMillis = getSystemElapsedTimeNanos() / 1_000_000L,
            inputsFromMotionEvents = false,
            // Although a MotionEvent isn't used to start the stroke, the inputToStrokeTransform is
            // still needed to transform the stroke coordinates into view coordinates for rendering,
            // as
            // motionEventToStrokeTransform is used for all rendering.
            inputToStrokeTransform =
                Matrix().apply {
                    // Compute (view -> stroke) = (stroke -> view)^-1
                    set(strokeToViewTransform)
                    invert(this)
                    // Compute (MotionEvent -> stroke) = (view -> stroke) x (MotionEvent -> view)
                    preConcat(motionEventToViewTransform)
                },
        )
    }

    @UiThread
    private fun startStrokeInternal(
        input: StrokeInput,
        shapeSpec: ShapeSpecT,
        startTimeMillis: Long,
        inputsFromMotionEvents: Boolean,
        strokeId: InProgressStrokeId = InProgressStrokeId.create(),
        inputToStrokeTransform: Matrix = Matrix(),
        // TODO: b/364655356 - Add support for collecting LatencyData in the
        // StrokeInput[Batch]-based
        // API.
        latencyData: LatencyData? = null,
    ): InProgressStrokeId {
        inProgressStrokeCounter?.increment()
        uiThreadState.currentCohort[strokeId] =
            UiStrokeState.Started(
                motionEventToStrokeTransform = inputToStrokeTransform,
                startEventTimeMillis = startTimeMillis,
                inputsFromMotionEvents = inputsFromMotionEvents,
                strokeUnitLengthCm = input.strokeUnitLengthCm,
            )
        val startAction =
            StartAction(
                input,
                strokeId,
                inputToStrokeTransform,
                shapeSpec,
                latencyData,
                startTimeMillis,
            )
        queueActionToRenderThread(startAction)
        return startAction.strokeId
    }

    /** Gets the [UiStrokeState.Started] state for a stroke, or throws an error. */
    @UiThread
    private fun assertStrokeInStartedState(strokeId: InProgressStrokeId): UiStrokeState.Started =
        when (val state = uiThreadState.currentCohort[strokeId]) {
            is UiStrokeState.Started -> state
            is UiStrokeState.Canceled -> error("Stroke with ID $strokeId was already canceled.")
            is UiStrokeState.InputCompleted,
            is UiStrokeState.Finished<*> -> error("Stroke with ID $strokeId is already finished.")
            null -> error("Stroke with ID $strokeId was not found.")
        }

    /**
     * Add [event] data for [pointerId] to already started stroke with [strokeId]. The stroke must
     * have been started with the overload of [startStroke] that accepts a [MotionEvent].
     *
     * @param event the next [MotionEvent] as part of a Stroke's input data, typically an
     *   ACTION_MOVE.
     * @param pointerId the index of the relevant pointer in the [event].
     * @param strokeId the Stroke that is to be built upon with [event].
     * @param prediction optional predicted MotionEvent containing predicted inputs between event
     *   and the time of the next frame, as generated by MotionEventPredictor::predict.
     */
    @UiThread
    fun addToStroke(
        event: MotionEvent,
        pointerId: Int,
        strokeId: InProgressStrokeId,
        prediction: MotionEvent?,
    ) {
        val receivedActionTimeNanos = getSystemElapsedTimeNanos()
        val strokeState = assertStrokeInStartedState(strokeId)
        check(strokeState.inputsFromMotionEvents) {
            "Stroke ID $strokeId was started with a StrokeInput but added to with a MotionEvent"
        }
        val pointerIndex = event.findPointerIndex(pointerId)
        require(pointerIndex >= 0) { "Pointer id $pointerId is not present in event." }
        val addAction =
            threadSharedState.addActionPool.obtain().apply {
                threadSharedState.strokeInputPool.obtainAllHistoryForMotionEvent(
                    event = event,
                    pointerIndex = pointerIndex,
                    motionEventToStrokeTransform = strokeState.motionEventToStrokeTransform,
                    strokeStartTimeMillis = strokeState.startEventTimeMillis,
                    strokeUnitLengthCm = strokeState.strokeUnitLengthCm,
                    outBatch = realInputs,
                )
                // TODO b/306361370 - Generate LatencyData only for those inputs that pass
                // validation.
                if (!realInputs.isEmpty()) {
                    latencyDataPool.obtainLatencyDataForPrimaryAndHistoricalEvents(
                        event,
                        LatencyData.StrokeAction.ADD,
                        strokeId,
                        receivedActionTimeNanos,
                        predicted = false,
                        realInputLatencyDatas,
                    )
                }
                check(predictedInputs.isEmpty())
                check(predictedInputLatencyDatas.isEmpty())
                if (prediction != null) {
                    // The real and predicted MotionEvents don't necessarily align pointers by their
                    // index,
                    // but rather their ID. And there isn't always necessarily a prediction for
                    // every pointer.
                    // So look up the pointer by ID, but don't include predicted inputs if no
                    // prediction is
                    // available.
                    val predictionPointerIndex = prediction.findPointerIndex(pointerId)
                    if (predictionPointerIndex >= 0) {
                        threadSharedState.strokeInputPool.obtainAllHistoryForMotionEvent(
                            event = prediction,
                            pointerIndex = predictionPointerIndex,
                            motionEventToStrokeTransform = strokeState.motionEventToStrokeTransform,
                            strokeStartTimeMillis = strokeState.startEventTimeMillis,
                            strokeUnitLengthCm = strokeState.strokeUnitLengthCm,
                            outBatch = predictedInputs,
                        )
                        // TODO b/306361370 - Generate LatencyData only for those inputs that pass
                        // validation.
                        if (!predictedInputs.isEmpty()) {
                            latencyDataPool.obtainLatencyDataForPrimaryAndHistoricalEvents(
                                prediction,
                                LatencyData.StrokeAction.PREDICTED_ADD,
                                strokeId,
                                receivedActionTimeNanos,
                                predicted = true,
                                predictedInputLatencyDatas,
                            )
                        }
                    }
                }
                this.strokeId = strokeId
            }
        queueAddActionIfNonEmpty(addAction)
    }

    /**
     * Add [inputs] to already started stroke with [strokeId]. The stroke must have been started
     * with the overload of [startStroke] that accepts a [StrokeInput].
     *
     * @param inputs the next set of real inputs to extend the stroke.
     * @param strokeId the Stroke that is to be built upon with [inputs].
     * @param prediction optional predicted inputs.
     */
    @UiThread
    fun addToStroke(
        inputs: StrokeInputBatch,
        strokeId: InProgressStrokeId,
        prediction: StrokeInputBatch,
    ) {
        val strokeState = assertStrokeInStartedState(strokeId)
        check(!strokeState.inputsFromMotionEvents) {
            "Stroke ID $strokeId was started with a MotionEvent but added to with a StrokeInputBatch"
        }
        val addAction =
            threadSharedState.addActionPool.obtain().apply {
                runCatching { realInputs.add(inputs) }
                runCatching { predictedInputs.add(prediction) }
                this.strokeId = strokeId
            }
        queueAddActionIfNonEmpty(addAction)
    }

    @UiThread
    private fun queueAddActionIfNonEmpty(addAction: AddAction) {
        // If both real and predicted input batches have no valid inputs, return early.
        if (addAction.realInputs.isEmpty() && addAction.predictedInputs.isEmpty()) {
            threadSharedState.addActionPool.recycle(addAction)
            return
        }
        queueActionToRenderThread(addAction)
    }

    /**
     * Complete the building of a stroke. The stroke must have been started with the overload of
     * [startStroke] that accepts a [MotionEvent].
     *
     * @param event the last [MotionEvent] as part of a stroke, typically an ACTION_UP.
     * @param pointerId the id of the relevant pointer.
     * @param strokeId the stroke that is to be finished with the latest event.
     */
    @UiThread
    fun finishStroke(event: MotionEvent, pointerId: Int, strokeId: InProgressStrokeId) {
        val receivedActionTimeNanos = getSystemElapsedTimeNanos()
        val strokeState = uiThreadState.currentCohort[strokeId]
        if (strokeState !is UiStrokeState.Started) return
        check(strokeState.inputsFromMotionEvents) {
            "Stroke ID $strokeId was started with a StrokeInput but finished with a MotionEvent"
        }
        val pointerIndex = event.findPointerIndex(pointerId)
        require(pointerIndex >= 0) { "Pointer id $pointerId is not present in event." }
        finishStrokeInternal(
            // This ignores any historical inputs included in this MotionEvent. Typically, this
            // is called in response to an ACTION_UP, which doesn't have any. But potentially the
            // logic could be made to take those into account if present.
            threadSharedState.strokeInputPool.obtainSingleValueForMotionEvent(
                event,
                pointerIndex,
                strokeState.motionEventToStrokeTransform,
                strokeState.startEventTimeMillis,
                strokeState.strokeUnitLengthCm,
            ),
            strokeId,
            endTimeMs = event.eventTime,
            latencyData =
                latencyDataPool.obtainLatencyDataForSingleEvent(
                    event,
                    LatencyData.StrokeAction.FINISH,
                    strokeId,
                    receivedActionTimeNanos,
                ),
        )
    }

    /**
     * Complete the building of a stroke. The stroke must have been started with the overload of
     * [startStroke] that accepts a [StrokeInput].
     *
     * @param input the last [StrokeInput] in a stroke.
     * @param strokeId the stroke that is to be finished with that input.
     */
    @UiThread
    fun finishStroke(input: StrokeInput, strokeId: InProgressStrokeId) {
        val strokeState = uiThreadState.currentCohort[strokeId]
        if (strokeState !is UiStrokeState.Started) return
        check(!strokeState.inputsFromMotionEvents) {
            "Stroke ID $strokeId was started with a MotionEvent but finished with a StrokeInput"
        }
        finishStrokeInternal(input, strokeId, getSystemElapsedTimeNanos() / 1_000_000L)
    }

    /** Complete the input for a stroke that is currently [UiStrokeState.Started]. */
    @UiThread
    private fun finishStrokeInternal(
        input: StrokeInput?,
        strokeId: InProgressStrokeId,
        endTimeMs: Long,
        forceCompletion: Boolean = false,
        latencyData: LatencyData? = null,
    ) {
        assertStrokeInStartedState(strokeId)
        uiThreadState.lastStrokeInputCompletedSystemElapsedTimeMillis = endTimeMs
        uiThreadState.currentCohort[strokeId] = UiStrokeState.InputCompleted
        queueActionToRenderThread(FinishAction(input, strokeId, forceCompletion, latencyData))
    }

    /**
     * Cancel the building of a stroke. This has no effect on a stroke that has already been
     * finished with [finishStroke].
     *
     * @param strokeId the stroke to cancel.
     */
    @UiThread
    fun cancelStroke(strokeId: InProgressStrokeId, event: MotionEvent?) {
        val receivedActionTimeNanos = getSystemElapsedTimeNanos()
        if (uiThreadState.currentCohort[strokeId] !is UiStrokeState.Started) return
        uiThreadState.currentCohort[strokeId] = UiStrokeState.Canceled
        uiThreadState.lastStrokeInputCompletedSystemElapsedTimeMillis =
            receivedActionTimeNanos / 1_000_000
        val cancelAction =
            CancelAction(
                strokeId,
                latencyDataPool.obtainLatencyDataForSingleEvent(
                    event,
                    LatencyData.StrokeAction.CANCEL,
                    strokeId,
                    receivedActionTimeNanos,
                ),
            )
        queueActionToRenderThread(cancelAction)
    }

    /** Cancel all in-progress strokes. */
    @UiThread
    fun cancelUnfinishedStrokes() {
        // Defensive copy needed to avoid a ConcurrentModificationException.
        val unfinishedStrokes =
            uiThreadState.currentCohort.filterValues { it is UiStrokeState.Started }
        for (strokeId in unfinishedStrokes.keys) {
            cancelStroke(strokeId, event = null)
        }
    }

    @UiThread
    fun hasUnfinishedStrokes(): Boolean =
        uiThreadState.currentCohort.values.any { it is UiStrokeState.Started }

    /**
     * Begin the process of a possible handoff. If a handoff is actually possible right now, then
     * [Finished] will be returned containing the strokes to hand off, and state will be updated to
     * ensure that those strokes are not held anywhere else. If a handoff is not possible right now,
     * then a different type of [ClaimStrokesToHandOffResult] will be returned to indicate the
     * reason. The reason why a handoff cannot happen right now determines the next steps, mostly
     * whether a task should be scheduled to check again in a short period of time, or whether more
     * external input is needed to change the state.
     */
    @CheckResult
    @UiThread
    private fun claimStrokesToHandOff(): ClaimStrokesToHandOffResult {
        // First, make sure that any finished (input complete and fully generated) strokes that the
        // render thread is done with are marked as finished in the current cohort.
        while (threadSharedState.finishedStrokes.isNotEmpty()) {
            val finishedStroke =
                checkNotNull(threadSharedState.finishedStrokes.poll()) {
                    "finishedStrokes should only be polled on the UI thread, so it should not be empty here."
                }
            if (
                uiThreadState.currentCohort[finishedStroke.strokeId] is UiStrokeState.InputCompleted
            ) {
                uiThreadState.currentCohort[finishedStroke.strokeId] =
                    UiStrokeState.Finished(finishedStroke)
            }
        }

        // Check that all strokes currently being rendered are either canceled or finished (input
        // complete and fully generated) and ready to be handed off.
        var someStrokesAreFinished = false
        for (strokeState in uiThreadState.currentCohort.values) {
            when (strokeState) {
                is UiStrokeState.Started,
                is UiStrokeState.InputCompleted -> return StillInProgress
                is UiStrokeState.Canceled -> continue
                is UiStrokeState.Finished<*> -> someStrokesAreFinished = true
            }
        }
        if (!someStrokesAreFinished) {
            return NoneInProgressOrFinished
        }
        if (threadSharedState.allowHandoffs.isPaused()) {
            return NoneInProgressButHandoffsPaused
        }
        if (
            inProgressStrokesRenderHelper.supportsDebounce &&
                !uiThreadState.cohortHandoffAsap &&
                getSystemElapsedTimeNanos() / 1_000_000 <
                    uiThreadState.lastStrokeInputCompletedSystemElapsedTimeMillis +
                        uiThreadState.cohortHandoffDebounceDurationMs
        ) {
            return NoneInProgressButDebouncing
        }

        return Finished(
                buildList {
                    for (strokeState in uiThreadState.currentCohort.values) {
                        if (strokeState is UiStrokeState.Finished<*>) {
                            @Suppress("UNCHECKED_CAST")
                            add(
                                (strokeState as UiStrokeState.Finished<CompletedShapeT>)
                                    .finishedStroke
                            )
                        }
                    }
                }
            )
            .also { uiThreadState.currentCohort.clear() }
    }

    @UiThread
    private fun onEndOfStrokeCohortCheck() {
        when (val result = claimStrokesToHandOff()) {
            is Finished<*> ->
                @Suppress("UNCHECKED_CAST")
                handOffFinishedStrokes((result as Finished<CompletedShapeT>).finishedCohort)
            is NoneInProgressButDebouncing -> potentialEndOfStrokeCohort()
            else -> {}
        }
    }

    @UiThread
    fun setHandoffDebounceDurationMs(debounceDurationMs: Long) {
        if (!inProgressStrokesRenderHelper.supportsDebounce) {
            return
        }
        uiThreadState.cohortHandoffDebounceDurationMs = debounceDurationMs
        potentialEndOfStrokeCohort()
    }

    /**
     * Request that the value passed to [setHandoffDebounceDurationMs] be temporarily ignored to
     * hand off rendering to the client's dry layer via
     * [androidx.ink.authoring.InProgressStrokesFinishedListener.onStrokesFinished]. Afterwards,
     * handoff debouncing will resume as normal.
     *
     * This API is experimental for now, as one approach to address start-of-stroke latency for fast
     * subsequent strokes.
     */
    @UiThread
    fun requestImmediateHandoff() {
        uiThreadState.cohortHandoffAsap = true
        potentialEndOfStrokeCohort()
    }

    internal fun canSynchronouslyWaitForFlush(): Boolean =
        inProgressStrokesRenderHelper.canSynchronouslyWaitForFlush

    /**
     * Make a best effort to finish or cancel all in-progress strokes, and if appropriate, execute
     * [Listener.onAllStrokesFinished] synchronously. This must be called on the UI thread, and
     * blocks it, so this should only be used in synchronous shutdown scenarios.
     *
     * @param timeout the maximum time to wait for the flush to complete.
     * @param timeoutUnit the unit of time for the timeout.
     * @param cancelAllInProgress if true, cancel all in-progress strokes. Otherwise, finish them.
     * @return Whether the flush completed. Flushing is best effort, and finishing in-progress
     *   shapes synchronously is not supported for all Android versions, so this is not guaranteed
     *   to return `true`. Note that all strokes will be canceled or finished regardless of the
     *   return value.
     */
    @UiThread
    fun flush(timeout: Long, timeoutUnit: TimeUnit, cancelAllInProgress: Boolean): Boolean {
        // cancelStroke/finishStroke will modify uiThreadState.currentCohort, so make a copy to
        // avoid
        // a ConcurrentModificationException.
        val unfinishedStrokes =
            uiThreadState.currentCohort.filterValues { it is UiStrokeState.Started }
        for (id in unfinishedStrokes.keys) {
            if (cancelAllInProgress) {
                cancelStroke(id, event = null)
            } else {
                finishStrokeInternal(
                    input = null,
                    strokeId = id,
                    forceCompletion = true,
                    endTimeMs = getSystemElapsedTimeNanos() / 1_000_000,
                )
            }
        }

        // Test-only hook to allow waiting for flush to be in progress on the UI thread when set.
        countDownWhenFlushInProgressTestLatch?.countDown()

        val startTimeNanos = getSystemElapsedTimeNanos()
        if (
            inProgressStrokesRenderHelper.canSynchronouslyWaitForFlush &&
                (threadSharedState.actions.isNotEmpty() ||
                    threadSharedState.currentlyHandlingActions.get())
        ) {
            // Barrel through starting to process the next cohort, even if it would have otherwise
            // waited
            // for HWUI handoff to complete, and queue a flush action to the render thread.
            threadSharedState.newCohortStartAwaitingHandoff.set(false)
            val flushAction = FlushAction()
            queueActionToRenderThread(flushAction)
            // Wait for all previous actions to be processed.
            flushAction.flushCompleted.await(timeout, timeoutUnit)
        }

        // If waiting won't help or a handoff is definitely not needed, skip the wait.
        if (
            inProgressStrokesRenderHelper.canSynchronouslyWaitForFlush &&
                !uiThreadState.currentCohort.values.all { it is UiStrokeState.Canceled }
        ) {
            // If the InProgressStrokesRenderHelper has ordered us to wait for it to prepare for new
            // handoffs (e.g. while clearing a newly inactive buffer in the background), wait the
            // rest
            // of the timeout (if any remains) for that to complete.
            val timeElapsedNanos = getSystemElapsedTimeNanos() - startTimeNanos
            if (
                !threadSharedState.allowHandoffs.awaitResume(
                    timeoutUnit.toNanos(timeout) - timeElapsedNanos,
                    TimeUnit.NANOSECONDS,
                )
            ) {
                // Handoffs are needed but still not possible.
                return false
            }
        }
        // At this point, if we've successfully awaited the unpause, no one else can start another
        // handoff with us blocking the UI thread. If we timed out, we probably won't be able to
        // finish
        // the flush synchronously, but try in case a handoff is not needed or things complete just
        // in time.
        uiThreadState.cohortHandoffAsap = true
        // It's unlikely that the result would be anything other than Finished, but it's possible
        // with
        // a short enough timeout.
        return when (val result = claimStrokesToHandOff()) {
            is Finished<*> -> {
                @Suppress("UNCHECKED_CAST")
                handOffFinishedStrokes((result as Finished<CompletedShapeT>).finishedCohort)
                true
            }
            // None left in progress, so the flush completed successfully, but nothing to hand off.
            is NoneInProgressOrFinished -> true
            // Some strokes were still left in progress.
            else -> false
        }
    }

    @UiThread
    fun sync(timeout: Long, timeoutUnit: TimeUnit) {
        if (!inProgressStrokesRenderHelper.canSynchronouslyWaitForFlush) {
            return
        }
        val syncAction = SyncAction()
        queueActionToRenderThread(syncAction)
        syncAction.syncCompleted.await(timeout, timeoutUnit)
    }

    @UiThread
    override fun pauseStrokeCohortHandoffs() {
        assertOnUiThread()
        threadSharedState.allowHandoffs.pause()
    }

    @AnyThread
    override fun resumeStrokeCohortHandoffs() {
        if (threadSharedState.allowHandoffs.resume()) {
            potentialEndOfStrokeCohort()
        }
    }

    @UiThread
    override fun onStrokeCohortHandoffToHwui(cohort: List<FinishedStroke<CompletedShapeT>>) {
        for (listener in uiThreadState.listeners) {
            listener.onAllStrokesFinished(cohort)
        }
        inProgressStrokeCounter?.let { counter -> repeat(cohort.size) { counter.decrement() } }
    }

    @UiThread
    override fun onStrokeCohortHandoffToHwuiComplete() {
        threadSharedState.newCohortStartAwaitingHandoff.set(false)
        inProgressStrokesRenderHelper.requestDraw()
    }

    /**
     * Queue the [action] to the render thread, then request a frontbuffer redraw. Frontbuffer
     * redraws consume all queued actions.
     */
    @UiThread
    private fun queueActionToRenderThread(action: Action) {
        threadSharedState.actions.offer(action)
        if (!threadSharedState.newCohortStartAwaitingHandoff.get()) {
            inProgressStrokesRenderHelper.requestDraw()
        }
    }

    @WorkerThread
    private fun handleAction(action: Action, systemElapsedTimeNanos: Long) {
        assertOnRenderThread()
        when (action) {
            is StartAction<*> -> {
                @Suppress("UNCHECKED_CAST")
                handleStartStroke(action as StartAction<ShapeSpecT>, systemElapsedTimeNanos)
            }
            is AddAction -> handleAddToStroke(action)
            is FinishAction -> handleFinishStroke(action, systemElapsedTimeNanos)
            is CancelAction -> handleCancelStroke(action)
            is MotionEventToViewTransformAction -> handleMotionEventToViewTransform(action)
            is StartCohortAction -> handleStartCohort()
            is FlushAction -> handleFlush(action)
            // Nothing to do before drawing for [AnimationFrameAction]. Similar to [AddAction],
            // rather
            // than updating the shape immediately, we wait to update the shape until we have
            // handled all
            // the actions in threadSharedState.actions. This is being done to reduce that amount of
            // updateShape calls in case there are input points arriving around the same time as
            // animation
            // frame actions.
            // Nothing to do before drawing for [SyncAction].
            else -> {}
        }
    }

    @WorkerThread
    private fun handleActionAfterDraw(action: Action) {
        assertOnRenderThread()
        when (action) {
            is AnimationFrameAction -> handleAnimationFrameAfterDraw()
            is CancelAction -> handleCancelStrokeAfterDraw(action)
            is SyncAction -> handleSyncAfterDraw(action)
            // Nothing to do after drawing for the other actions.
            else -> {}
        }
    }

    private fun RenderThreadStrokeState<ShapeSpecT, InProgressShapeT, CompletedShapeT>.updateShape(
        systemElapsedTimeNanos: Long,
        forceCompletion: Boolean = false,
    ) {
        val systemElapsedTimeMillis = systemElapsedTimeNanos / 1_000_000L
        inProgressShape.update(shapeDurationMillis = systemElapsedTimeMillis - startEventTimeMillis)
        if (forceCompletion) {
            inProgressShape.forceCompletion()
        }
    }

    /** Handle an action that was initiated by [startStroke]. */
    @WorkerThread
    private fun handleStartStroke(action: StartAction<ShapeSpecT>, systemElapsedTimeNanos: Long) {
        assertOnRenderThread()
        val strokeToMotionEventTransform =
            Matrix().apply { action.motionEventToStrokeTransform.invert(this) }
        val shapeSpec = action.shapeSpec
        val inProgressShape = renderThreadState.inProgressStrokePool.obtain(shapeSpec)
        val strokeState =
            RenderThreadStrokeState(
                inProgressShape = inProgressShape,
                strokeToMotionEventTransform = strokeToMotionEventTransform,
                startEventTimeMillis = action.startEventTimeMillis,
            )
        inProgressShape.start(shapeSpec, action.startEventTimeMillis)
        inProgressShape.enqueueInputs(
            MutableStrokeInputBatch().apply { runCatching { add(action.strokeInput) } },
            ImmutableStrokeInputBatch.EMPTY,
        )
        // Use the current time rather than action.startEventTimeMillis, because some time may
        // have elapsed as part of input processing and the current time will be more accurate for
        // shape generation and animation effects.
        strokeState.updateShape(systemElapsedTimeNanos)
        threadSharedState.strokeInputPool.recycle(action.strokeInput)
        renderThreadState.toDrawStrokes[action.strokeId] = strokeState
        action.latencyData?.let { renderThreadState.latencyDatas.add(it) }
    }

    /** Handle an action that was initiated by [addToStroke]. */
    @WorkerThread
    private fun handleAddToStroke(action: AddAction) {
        assertOnRenderThread()
        val strokeState = renderThreadState.toDrawStrokes[action.strokeId]
        checkNotNull(strokeState) { "Stroke state with ID ${action.strokeId} was not found." }
        check(strokeState.status is RenderThreadStrokeState.Started) {
            "Stroke with ID ${action.strokeId} was already finished."
        }
        check(!strokeState.inProgressShape.isCanceled()) {
            "Stroke with ID ${action.strokeId} was canceled."
        }
        strokeState.inProgressShape.enqueueInputs(action.realInputs, action.predictedInputs)

        renderThreadState.latencyDatas.addAll(action.realInputLatencyDatas)
        renderThreadState.latencyDatas.addAll(action.predictedInputLatencyDatas)
        threadSharedState.addActionPool.recycle(action)
    }

    @WorkerThread
    private fun maybeDryStroke(
        strokeId: InProgressStrokeId,
        strokeState: RenderThreadStrokeState<ShapeSpecT, InProgressShapeT, CompletedShapeT>,
    ) {
        val completedShape = strokeState.inProgressShape.getCompletedShape()
        if (completedShape == null) {
            strokeState.status = RenderThreadStrokeState.Drying
        } else {
            strokeState.status = RenderThreadStrokeState.AwaitingHandoff
            threadSharedState.finishedStrokes.add(
                FinishedStroke(
                    strokeId = strokeId,
                    stroke = completedShape,
                    strokeToViewTransform = Matrix(renderThreadState.strokeToViewTransform),
                )
            )
            renderThreadState.maybeCompletedCohortThisDraw = true
        }
    }

    /** Handle an action that was initiated by [finishStroke]. */
    @WorkerThread
    private fun handleFinishStroke(action: FinishAction, systemElapsedTimeNanos: Long) {
        assertOnRenderThread()
        val strokeState = renderThreadState.toDrawStrokes[action.strokeId]
        checkNotNull(strokeState) { "Stroke state with ID ${action.strokeId} was not found." }
        check(strokeState.status is RenderThreadStrokeState.Started) {
            "Stroke with ID ${action.strokeId} was already finished."
        }
        check(!strokeState.inProgressShape.isCanceled()) {
            "Stroke with ID ${action.strokeId} was canceled."
        }
        val inProgressShape = strokeState.inProgressShape
        check(!inProgressShape.isCanceled()) { "Stroke with ID ${action.strokeId} was canceled." }
        fillStrokeToViewTransform(strokeState)
        // Save the stroke to be handed off.
        if (action.strokeInput != null) {
            inProgressShape.enqueueInputs(
                MutableStrokeInputBatch().apply { runCatching { add(action.strokeInput) } },
                ImmutableStrokeInputBatch.EMPTY,
            )
        }
        // InProgressShape contract specifies that `finishInput` is called between the last
        // `enqueueInputs` and before the subsequent `update`.
        inProgressShape.finishInput()
        // We update the finished stroke immediately after enqueueing because we know we are not
        // going
        // to be receiving any other inputs.
        strokeState.updateShape(systemElapsedTimeNanos, forceCompletion = action.forceCompletion)
        maybeDryStroke(action.strokeId, strokeState)
        if (action.strokeInput != null) {
            threadSharedState.strokeInputPool.recycle(action.strokeInput)
        }
        action.latencyData?.let { renderThreadState.latencyDatas.add(it) }
        // Clean up state and notify the UI thread of the potential end of this cohort after
        // drawing.
    }

    /**
     * Queues an [AnimationFrameAction] to the render thread. This is the implementation for
     * `queueAnimationFrameActionOnce` in [renderThreadState]; use that instead of calling this
     * directly.
     */
    @UiThread
    private fun queueAnimationFrameAction() {
        queueActionToRenderThread(AnimationFrameAction)
    }

    @WorkerThread
    private fun handleAnimationFrameAfterDraw() {
        for ((strokeId, strokeState) in renderThreadState.toDrawStrokes) {
            if (strokeState.status is RenderThreadStrokeState.Drying) {
                maybeDryStroke(strokeId, strokeState)
            }
        }
    }

    /** Handle an action that was initiated by [cancelStroke]. */
    @WorkerThread
    private fun handleCancelStroke(action: CancelAction) {
        assertOnRenderThread()
        val strokeState =
            checkNotNull(renderThreadState.toDrawStrokes[action.strokeId]) {
                "Stroke state with ID ${action.strokeId} was not found."
            }
        check(strokeState.status is RenderThreadStrokeState.Started) {
            "Stroke with ID ${action.strokeId} was already finished."
        }
        strokeState.inProgressShape.cancel()
        // Don't save the stroke to be handed off as in handleFinishStroke.
        renderThreadState.latencyDatas.add(action.latencyData)
    }

    @WorkerThread
    private fun handleCancelStrokeAfterDraw(action: CancelAction) {
        // Remove its state since we won't be adding to it anymore and it no longer should be drawn.
        val removedStrokeState = renderThreadState.toDrawStrokes.remove(action.strokeId)
        if (removedStrokeState != null) {
            renderThreadState.inProgressStrokePool.recycle(removedStrokeState.inProgressShape)
        }
        inProgressStrokeCounter?.decrement()
        renderThreadState.maybeCompletedCohortThisDraw = true
    }

    @AnyThread
    private fun potentialEndOfStrokeCohort() {
        // This may be the end of the current cohort of strokes, but wait until all inputs have been
        // processed in a HWUI frame (in onAnimation) to ensure that any strokes that are present in
        // the
        // same frame are considered part of the same cohort.
        postOnAnimation(threadSharedState.checkEndOfStrokeCohortOnce.setUp())
    }

    /** Handle an action that was initiated by setting [motionEventToViewTransform]. */
    @WorkerThread
    private fun handleMotionEventToViewTransform(action: MotionEventToViewTransformAction) {
        assertOnRenderThread()
        renderThreadState.motionEventToViewTransform.set(action.motionEventToViewTransform)
    }

    @WorkerThread
    private fun handleStartCohort() {
        assertOnRenderThread()
        for (strokeState in renderThreadState.toDrawStrokes.values) {
            renderThreadState.inProgressStrokePool.recycle(strokeState.inProgressShape)
        }
        renderThreadState.toDrawStrokes.clear()
        inProgressStrokesRenderHelper.startCohort()
    }

    @WorkerThread
    private fun handleFlush(action: FlushAction) {
        action.flushCompleted.countDown()
    }

    @WorkerThread
    private fun handleSyncAfterDraw(action: SyncAction) {
        action.syncCompleted.countDown()
    }

    /** Called by the [InProgressStrokesRenderHelper] when it can be drawn to. */
    @WorkerThread
    override fun onDraw() {
        assertOnRenderThread()
        check(renderThreadState.handledActions.isEmpty())
        // When handoffInProgress is true, we are in the middle of handing off the last stroke
        // cohort
        // and there's no point in continuing with draws until the call to
        // onStrokeCohortHandoffToHwuiComplete.
        if (threadSharedState.newCohortStartAwaitingHandoff.get()) return
        threadSharedState.currentlyHandlingActions.set(true)
        // Consider using the next frame time from Choreographer instead of the current time to
        // better
        // align with dry layer animation timing. There may be some heuristics to use here that
        // depend
        // on the RenderHelper implementation, e.g. whether it uses front buffer rendering or not.
        // For
        // example, it may calculate how much of the current frame's timeline has passed, to know
        // whether updates to the front buffer will be more likely to appear (be composited with)
        // the
        // current frame or the next frame.
        val systemElapsedTimeNanos = getSystemElapsedTimeNanos()
        // Process all available events in case any were added when the front buffer was not
        // available
        // (before onAttachedToWindow).
        while (threadSharedState.actions.isNotEmpty()) {
            // Double-check because the handoff can start from the UI thread while we're in the
            // middle
            // of a draw. Handoff shouldn't happen while any strokes are in progress, but other
            // actions
            // could have already been queued. In that case, we really don't want to jump the gun on
            // preparing to start the next cohort.
            if (
                threadSharedState.actions.peek() is StartCohortAction &&
                    threadSharedState.newCohortStartAwaitingHandoff.get()
            ) {
                break
            }
            val action = threadSharedState.actions.poll()
            checkNotNull(action) { "Actions should only be removed by onDraw." }
            handleAction(action, systemElapsedTimeNanos)
            renderThreadState.handledActions.add(action)
        }
        for (strokeState in renderThreadState.toDrawStrokes.values) {
            // TODO(b/486162363) - This calls update every time. So it misses opportunities to check
            // if
            // an update is really necessary and not call update, but it also avoids doing duplicate
            // work
            // if update itself does that, which it arguably should.
            strokeState.updateShape(systemElapsedTimeNanos)
        }
        if (inProgressStrokesRenderHelper.contentsPreservedBetweenDraws) {
            for (strokeStateToScissor in renderThreadState.toDrawStrokes.values) {
                strokeStateToScissor.fillUpdatedStrokeRegion()
                val updatedRegionBox = renderThreadState.updatedRegion.box
                if (updatedRegionBox != null) {
                    renderThreadState.scratchRect.populateFrom(updatedRegionBox)
                    // Change updatedRegion from stroke coordinates to view coordinates.
                    fillStrokeToViewTransform(strokeStateToScissor)
                    renderThreadState.scratchRect.transform(renderThreadState.strokeToViewTransform)
                    // This call loops over all live strokes and draws each one, so overall (with
                    // the outer
                    // loop over all update regions) we do N^2 draws. This is necessary to handle
                    // when two
                    // live strokes intersect. Without the inner loop (i.e., if scissor+draw
                    // happened for each
                    // stroke in isolation), a live stroke A drawing over another live stroke B
                    // would clear a
                    // rectangle where B was previously drawn and only draw A in that space - but
                    // that part of
                    // B needs to be filled in again.
                    drawAllStrokesInModifiedRegion(renderThreadState.scratchRect)
                }
            }
        } else {
            // When the contents of the previous draw call are not preserved for the next one, there
            // is no
            // need to do the N^2 operation of drawing every stroke into the modified region of
            // every
            // stroke. Instead, just draw every stroke, without any clipping to modified regions.
            renderThreadState.scratchRect.setXBounds(
                Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY,
            )
            renderThreadState.scratchRect.setYBounds(
                Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY,
            )
            drawAllStrokesInModifiedRegion(renderThreadState.scratchRect)
        }
    }

    private fun drawAllStrokesInModifiedRegion(modifiedRegion: MutableBox) {
        inProgressStrokesRenderHelper.prepareToDrawInModifiedRegion(modifiedRegion)
        // Iteration over MutableMap is guaranteed to be in insertion order, which results in proper
        // z-order for drawing.
        for (strokeStateToDraw in renderThreadState.toDrawStrokes.values) {
            // renderThreadState.strokeStates still contains any canceled strokes so that the space
            // they occupied (as reported by InProgressShape.getUpdatedRegion after calling
            // [InProgressShape.cancel]) is cleared and redrawn with other non-canceled strokes.
            // This
            // conditional `continue` prevents canceled strokes from being drawn here. The canceled
            // strokes will be removed from renderThreadState.strokeStates after drawing is
            // finished.
            if (strokeStateToDraw.inProgressShape.isCanceled()) continue
            drawStrokeState(strokeStateToDraw)
        }
        inProgressStrokesRenderHelper.afterDrawInModifiedRegion()
    }

    @WorkerThread
    override fun onDrawComplete() {
        for (action in renderThreadState.handledActions) {
            handleActionAfterDraw(action)
        }
        renderThreadState.handledActions.clear()
        threadSharedState.currentlyHandlingActions.set(false)
        if (renderThreadState.maybeCompletedCohortThisDraw) {
            if (renderThreadState.toDrawStrokes.values.all { it.isAwaitingHandoff() }) {
                potentialEndOfStrokeCohort()
            }
            renderThreadState.maybeCompletedCohortThisDraw = false
        }
        if (renderThreadState.toDrawStrokes.values.any { it.needsAnimationFrame() }) {
            postOnAnimation(renderThreadState.queueAnimationFrameActionOnce.setUp())
        }
    }

    @WorkerThread
    override fun reportEstimatedPixelPresentationTime(timeNanos: Long) {
        for (latencyData in renderThreadState.latencyDatas) {
            latencyData.estimatedPixelPresentationTime = timeNanos
        }
    }

    @WorkerThread
    override fun setCustomLatencyDataField(setter: (LatencyData, Long) -> Unit) {
        val time = getSystemElapsedTimeNanos()
        for (latencyData in renderThreadState.latencyDatas) {
            setter(latencyData, time)
        }
    }

    @WorkerThread
    override fun handOffAllLatencyData() {
        threadSharedState.finishedLatencyDatas.addAll(renderThreadState.latencyDatas)
        renderThreadState.latencyDatas.clear()
        postToUiThread(handOffLatencyDataToClientRunnable)
    }

    @UiThread
    private fun handOffLatencyDataToClient() {
        assertOnUiThread()
        while (!threadSharedState.finishedLatencyDatas.isEmpty()) {
            threadSharedState.finishedLatencyDatas.poll()?.let {
                try {
                    latencyDataCallback.onLatencyData(it)
                } finally {
                    // The callback synchronously processes the LatencyData; after it returns, we
                    // can recycle.
                    latencyDataPool.recycle(it)
                }
            }
        }
    }

    /**
     * Fill `updatedRegion` of a [RenderThreadStrokeState] with the region that has been updated and
     * must be redrawn, in stroke coordinates. Return `true` if and only if there is actually a
     * region to be updated.
     */
    @WorkerThread
    private fun RenderThreadStrokeState<ShapeSpecT, InProgressShapeT, CompletedShapeT>
        .fillUpdatedStrokeRegion() {
        renderThreadState.updatedRegion.populateFrom(inProgressShape.getUpdatedRegion())
        inProgressShape.resetUpdatedRegion()
    }

    /** Draw a live stroke. */
    @WorkerThread
    private fun drawStrokeState(
        strokeState: RenderThreadStrokeState<ShapeSpecT, InProgressShapeT, CompletedShapeT>
    ) {
        fillStrokeToViewTransform(strokeState)
        inProgressStrokesRenderHelper.drawInModifiedRegion(
            strokeState.inProgressShape,
            renderThreadState.strokeToViewTransform,
        )
    }

    /** Calculate and update strokeToViewTransform by combining other transform matrices. */
    @WorkerThread
    private fun fillStrokeToViewTransform(
        strokeState: RenderThreadStrokeState<ShapeSpecT, InProgressShapeT, CompletedShapeT>
    ) {
        renderThreadState.strokeToViewTransform.set(strokeState.strokeToMotionEventTransform)
        renderThreadState.strokeToViewTransform.postConcat(
            renderThreadState.motionEventToViewTransform
        )
    }

    /** Throws an error if not currently executing on the render thread. */
    @WorkerThread
    private fun assertOnRenderThread() = inProgressStrokesRenderHelper.assertOnRenderThread()

    /**
     * Hands off a cohort of finished strokes to HWUI.
     *
     * @param finishedStrokes The finished strokes, with map iteration order in stroke z-order from
     *   back to front.
     */
    @UiThread
    private fun handOffFinishedStrokes(finishedStrokes: List<FinishedStroke<CompletedShapeT>>) {
        // Test-only hook to allow blocking the render thread immediately after stroke cohort
        // handoffs
        // are paused.
        awaitAfterStartOfHandoffTestLatch?.apply {
            inProgressStrokesRenderHelper.executeOnRenderThread {
                check(await(10, TimeUnit.SECONDS)) {
                    "Timed out waiting for awaitAfterStartOfHandoffTestLatch"
                }
            }
        }

        uiThreadState.cohortHandoffAsap = false
        uiThreadState.lastStrokeInputCompletedSystemElapsedTimeMillis = Long.MIN_VALUE

        threadSharedState.newCohortStartAwaitingHandoff.set(true)
        // Queue an action to happen before any inputs on the new cohort.
        queueActionToRenderThread(StartCohortAction)
        inProgressStrokesRenderHelper.requestStrokeCohortHandoffToHwui(finishedStrokes)
    }

    private class HandoffPause {
        private val gate = AtomicReference<CountDownLatch?>(null)

        /** Returns whether handoffs are currently paused. */
        @AnyThread fun isPaused() = gate.get() != null

        /** Pause handoffs, doing nothing if already paused. */
        @UiThread
        fun pause() {
            // Only pause on the UI thread so we can't get paused again after awaiting unpause
            // during
            // flush. Since we might pause on initialization and resume might be preempted, make
            // pausing
            // idempotent.
            assertOnUiThread()
            // Would be slightly more efficient to use getAndUpdate:
            // gate.getAndUpdate { it ?: CountDownLatch(1) }
            // Because that only allocates if it needs to. Can't until our minSdk is 24 (currently
            // 23).
            gate.compareAndSet(null, CountDownLatch(1))
        }

        /** Resumes handoffs if paused. Returns whether handoffs were resumed. */
        @AnyThread
        fun resume(): Boolean {
            val latch = gate.getAndSet(null)
            if (latch == null) {
                return false
            }
            latch.countDown()
            return true
        }

        /**
         * Waits for the next resume if paused. Returns whether handoffs were resumed within the
         * given timeout or weren't paused in the first place.
         */
        @UiThread
        fun awaitResume(timeout: Long, unit: TimeUnit): Boolean {
            // Only await unpause on the UI thread during flush because we can't be paused again
            // while
            // that completes synchronously.
            assertOnUiThread()
            return gate.get()?.await(timeout, unit) ?: true
        }
    }

    /** An input event that can go in the (future) event queue to hand off across threads. */
    private sealed interface Action

    /** Represents the data passed to [startStroke]. */
    private data class StartAction<ShapeSpecT>(
        val strokeInput: StrokeInput,
        val strokeId: InProgressStrokeId,
        val motionEventToStrokeTransform: Matrix,
        val shapeSpec: ShapeSpecT,
        val latencyData: LatencyData?,
        val startEventTimeMillis: Long,
    ) : Action

    /**
     * Represents the data passed to [addToStroke]. This is meant to be overwritten for recycling
     * purposes, so it is not immutable like the less frequent start/finish actions.
     */
    private data class AddAction(
        val realInputs: MutableStrokeInputBatch = MutableStrokeInputBatch(),
        val predictedInputs: MutableStrokeInputBatch = MutableStrokeInputBatch(),
        var strokeId: InProgressStrokeId = InProgressStrokeId.create(),
        val realInputLatencyDatas: ArrayDeque<LatencyData> = ArrayDeque(initialCapacity = 15),
        val predictedInputLatencyDatas: ArrayDeque<LatencyData> = ArrayDeque(initialCapacity = 15),
    ) : Action {
        fun reset() {
            realInputs.clear()
            predictedInputs.clear()
            realInputLatencyDatas.clear()
            predictedInputLatencyDatas.clear()
        }
    }

    private class AddActionPool {
        private val pool = ConcurrentLinkedQueue<AddAction>()

        fun obtain(): AddAction {
            return pool.poll() ?: AddAction()
        }

        fun recycle(action: AddAction) {
            action.reset()
            pool.offer(action)
        }
    }

    /** Represents the data passed to [finishStroke]. */
    private data class FinishAction(
        val strokeInput: StrokeInput?,
        val strokeId: InProgressStrokeId,
        /**
         * Used by [flush] to accelerate time-based behaviors of shapes. See
         * [androidx.ink.authoring.InProgressShape.update] for more details.
         */
        val forceCompletion: Boolean,
        val latencyData: LatencyData?,
    ) : Action

    /**
     * Indicates that it's time to update the shape and/or appearance of drying or animated strokes.
     */
    private object AnimationFrameAction : Action

    /** Represents the data passed to [cancelStroke]. */
    private data class CancelAction(
        val strokeId: InProgressStrokeId,
        val latencyData: LatencyData,
    ) : Action

    /** Represents an update to [motionEventToViewTransform]. */
    private data class MotionEventToViewTransformAction(val motionEventToViewTransform: Matrix) :
        Action

    /**
     * Represents a request to clear the data of a stroke cohort being handed off by
     * [onEndOfStrokeCohortCheck].
     */
    private object StartCohortAction : Action

    /**
     * Represents a request to synchronize across threads, so that the UI thread can block on this
     * operation in the action queue being reached and handled by the render thread.
     */
    private class FlushAction : Action {
        val flushCompleted = CountDownLatch(1)
    }

    /**
     * Represents a request to synchronize across threads, so that the UI thread can block on this
     * operation in the action queue being reached and handled by the render thread.
     */
    private class SyncAction : Action {
        val syncCompleted = CountDownLatch(1)
    }

    /** The result type of [claimStrokesToHandOff]. */
    private sealed interface ClaimStrokesToHandOffResult

    /**
     * A result of [claimStrokesToHandOff] that indicates that no strokes are currently in progress,
     * and none are finished, so inking is in an idle state.
     */
    private object NoneInProgressOrFinished : ClaimStrokesToHandOffResult

    /**
     * A result of [claimStrokesToHandOff] that indicates that strokes are still in progress,
     * meaning some began with [startStroke] but haven't yet had [finishStroke] or [cancelStroke]
     * called on them.
     */
    private object StillInProgress : ClaimStrokesToHandOffResult

    /**
     * A result of [claimStrokesToHandOff] that indicates that no strokes are currently in progress,
     * but debouncing is currently preventing handoff.
     */
    private object NoneInProgressButDebouncing : ClaimStrokesToHandOffResult

    /**
     * A result of [claimStrokesToHandOff] that indicates that no strokes are currently in progress,
     * but [pauseStrokeCohortHandoffs] is currently preventing handoff.
     */
    private object NoneInProgressButHandoffsPaused : ClaimStrokesToHandOffResult

    /**
     * A result of [claimStrokesToHandOff] that indicates that no strokes are currently in progress,
     * and nothing else is preventing handoff of the provided strokes.
     *
     * @param finishedStrokes The finished strokes (which cannot be empty), with map iteration order
     *   in stroke z-order, from back to front.
     */
    private data class Finished<CompletedShapeT : Any>(
        @Size(min = 1) val finishedCohort: List<FinishedStroke<CompletedShapeT>>
    ) : ClaimStrokesToHandOffResult {
        init {
            require(finishedCohort.isNotEmpty())
        }
    }

    /** Holds the state for a given stroke, as needed by the render thread. */
    private class RenderThreadStrokeState<
        ShapeSpecT : Any,
        InProgressShapeT : InProgressShape<ShapeSpecT, CompletedShapeT>,
        CompletedShapeT : Any,
    >(
        val inProgressShape: InProgressShapeT,
        val strokeToMotionEventTransform: Matrix,
        val startEventTimeMillis: Long,
        var status: Status = RenderThreadStrokeState.Started,
    ) {

        fun needsAnimationFrame() = status is Drying || inProgressShape.changesWithTime()

        fun isAwaitingHandoff() = status is AwaitingHandoff

        /** Stage of progress of a stroke on the render thread. */
        sealed interface Status

        /** Stroke has been started but not finished. */
        object Started : Status

        /** Stroke input has been finished but the final shape is not yet available. */
        object Drying : Status

        /** Stroke is done and still being drawn before it's handed off. */
        object AwaitingHandoff : Status
    }

    /**
     * Holds the state for a given stroke in the current cohort, as needed by the UI thread. New
     * strokes start out in the [UiStrokeState.Started] state, then move to
     * [UiStrokeState.InputCompleted] once [finishStroke] is called, or to [UiStrokeState.Canceled]
     * if [cancelStroke] is called first. Strokes in the [UiStrokeState.InputCompleted] state move
     * to [UiStrokeState.Finished] once the stroke has been fully generated by the render thread.
     * Once all strokes in the cohort are either [UiStrokeState.Canceled] or
     * [UiStrokeState.Finished], the cohort can be handed off.
     */
    private sealed interface UiStrokeState {

        /** UI thread state for a stroke that has been started, but not yet finished or canceled. */
        class Started(
            val motionEventToStrokeTransform: Matrix,
            val startEventTimeMillis: Long,
            val inputsFromMotionEvents: Boolean,
            val strokeUnitLengthCm: Float,
        ) : UiStrokeState

        /** UI thread state for a stroke that has been canceled. */
        object Canceled : UiStrokeState

        /**
         * UI thread state for a stroke whose inputs are finished, but that has not yet been fully
         * generated by the render thread.
         */
        object InputCompleted : UiStrokeState

        /**
         * UI thread state for a stroke that has been finished and fully generated, and is ready to
         * be handed off.
         */
        class Finished<CompletedShapeT : Any>(val finishedStroke: FinishedStroke<CompletedShapeT>) :
            UiStrokeState
    }
}
