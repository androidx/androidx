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

import android.graphics.Matrix as AndroidMatrix
import android.view.MotionEvent
import androidx.annotation.CheckResult
import androidx.annotation.Size
import androidx.annotation.UiThread
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
    /**
     * Allows tests to replace [CountDownLatch.await] with something that yields rather than blocks.
     */
    private val blockingAwait: (CountDownLatch, Long, TimeUnit) -> Boolean =
        { latch, timeout, timeoutUnit ->
            latch.await(timeout, timeoutUnit)
        },
) : InProgressStrokesRenderHelper.Callback<CompletedShapeT> {

    /**
     * The transform matrix to convert input (MotionEvent) coordinates into coordinates of this view
     * for rendering. Defaults to the identity matrix, for the case where LowLatencyView exactly
     * overlays the view from which MotionEvents are being forwarded. This should only be set from
     * the UI thread.
     */
    var motionEventToViewTransform: AndroidMatrix = AndroidMatrix()
        get() = AndroidMatrix(field)
        set(value) {
            field.set(value)
            queueInputToRenderThread(MotionEventToViewTransformAction(AndroidMatrix(value)))
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

    internal interface Listener<CompletedShapeT : Any> {
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
         * @param strokes The finished strokes, with map iteration order in stroke z-order from back
         *   to front.
         */
        @UiThread
        fun onAllStrokesFinished(strokes: Map<InProgressStrokeId, FinishedStroke<CompletedShapeT>>)
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
             * Runs [onEndOfStrokeCohortCheck] at most once per frame, even if this is passed to
             * [postOnAnimation] more than once during that frame.
             */
            val checkEndOfStrokeCohortOnce = AtMostOnceAfterSetUp(::onEndOfStrokeCohortCheck)

            /**
             * Minimum delay from when the user finishes a stroke (via [finishStroke]) until
             * rendering is handed off to the client's dry layer. This only applies when
             * [InProgressStrokesRenderHelper.supportsDebounce] is true, which currently is only for
             * [CanvasInProgressStrokesRenderHelperV29].
             *
             * Consider moving debouncing logic into [CanvasInProgressStrokesRenderHelperV29] and
             * not making it configurable by developers, e.g. by making use of
             * [setPauseStrokeCohortHandoffs].
             */
            var cohortHandoffDebounceDurationMs = 0L

            var cohortHandoffAsap = false

            var cohortHandoffPaused = false

            /**
             * The timestamp, in the same time base as [getSystemElapsedTimeNanos], when a stroke
             * most recently had its final input via either [finishStroke] or [cancelStroke]. This
             * is used to determine whether the current stroke cohort is eligible for handoff, or
             * not yet due to debouncing.
             */
            var lastStrokeInputCompletedSystemElapsedTimeMillis = Long.MIN_VALUE

            val queueAnimationFrameActionOnce = AtMostOnceAfterSetUp(::queueAnimationFrameAction)

            /** To notify when strokes have been completed. Owned by the UI thread. */
            val listeners = mutableSetOf<Listener<CompletedShapeT>>()
        }
        @UiThread
        get() {
            return field
        }

    /** The state that is accessed just by the render thread. */
    private val renderThreadState =
        object {

            /**
             * Strokes that are being drawn by this class, with map iteration order in stroke
             * z-order from back to front. This includes the contents of [generatedStrokes].
             */
            val toDrawStrokes =
                mutableMapOf<
                    InProgressStrokeId,
                    RenderThreadStrokeState<ShapeSpecT, InProgressShapeT, CompletedShapeT>,
                >()

            /**
             * Strokes in [toDrawStrokes] whose inputs are finished, but which still need further
             * calls to [updateShape] (e.g. due to time-since behaviors) before they will be fully
             * dry.
             */
            val dryingStrokes = mutableSetOf<InProgressStrokeId>()

            /**
             * Strokes that have been fully generated, but not yet passed to the UI thread for
             * client handoff.
             */
            val generatedStrokes =
                mutableMapOf<InProgressStrokeId, FinishedStroke<CompletedShapeT>>()

            /**
             * Strokes that have been canceled, thus should not be drawn or passed to the UI thread
             * for client handoff.
             */
            val canceledStrokes = mutableSetOf<InProgressStrokeId>()

            /**
             * Contains instances of in-progress shapes that are not currently in use (does not
             * belong to [toDrawStrokes]) and are ready to be used in a new stroke. This is to reuse
             * memory that has already been allocated to improve performance after the first few
             * strokes, and to minimize memory fragmentation that can affect the health of the app's
             * process over time
             */
            val inProgressStrokePool = inProgressStrokePool

            /**
             * [LatencyData]s for the [InputAction]s that were processed in the latest call to
             * [onDraw].
             */
            val latencyDatas: ArrayDeque<LatencyData> =
                ArrayDeque<LatencyData>(initialCapacity = 30)

            /**
             * The render thread's copy of LowLatencyView.motionEventToViewTransform. This is a copy
             * for thread safety.
             */
            val motionEventToViewTransform = AndroidMatrix()

            /**
             * Allocated once and reused on each draw to hold the result of a matrix multiplication.
             */
            val strokeToViewTransform = AndroidMatrix()

            /**
             * Pre-allocated list to contain actions that have been handled but need further
             * processing. Used locally only in [onDraw].
             */
            val handledActions = arrayListOf<InputAction>()

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
            /**
             * Finished strokes that have just been generated. Produced on the render thread and
             * consumed on the UI thread.
             */
            val finishedStrokes =
                ConcurrentLinkedQueue<
                    Map.Entry<InProgressStrokeId, FinishedStroke<CompletedShapeT>>
                >()

            /**
             * Used to hand off input events across threads. This is added to from the UI thread
             * when new inputs are given via the public functions, and consumed from the render
             * thread when the contents of that event need to be rendered.
             */
            val inputActions = ConcurrentLinkedQueue<InputAction>()

            /**
             * Reuse input objects so they don't need to be constantly allocated for each input.
             * This is added to from the render thread after it finishes processing an [AddAction],
             * and consumed from the UI thread when [addToStroke] wants to reuse and fill in an
             * [AddAction].
             */
            val addActionPool = ConcurrentLinkedQueue<AddAction>()
            val strokeInputPool = StrokeInputPool()

            /**
             * Used to hand off finished [LatencyData]s from the render thread back to the UI thread
             * for reporting to the client. This is added to from the render thread in
             * [onFrontBufferedRenderComplete] and consumed from the UI thread in
             * [handOffLatencyData].
             */
            val finishedLatencyDatas = ConcurrentLinkedQueue<LatencyData>()

            val pauseInputs = AtomicBoolean(false)

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
        motionEventToWorldTransform: AndroidMatrix,
        strokeToWorldTransform: AndroidMatrix,
        shapeSpec: ShapeSpecT,
        strokeUnitLengthCm: Float,
    ): InProgressStrokeId {
        val receivedActionTimeNanos = getSystemElapsedTimeNanos()
        val pointerIndex = event.findPointerIndex(pointerId)
        require(pointerIndex >= 0) { "Pointer id $pointerId is not present in event." }
        // Set up this stroke's matrix to be used to transform MotionEvent -> stroke coordinates.
        val motionEventToStrokeTransform =
            AndroidMatrix().also {
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
     * @param startTimeMillis Start time of the stroke, used to determine the relative timing of
     *   later additions of the stroke.
     * @param strokeToViewTransform The [AndroidMatrix] that converts stroke coordinates as provided
     *   in [input] into the coordinate space of this view for rendering.
     * @return The Stroke ID of the stroke being built, later used to identify which stroke is being
     *   added to, finished, or canceled.
     */
    @UiThread
    fun startStroke(
        input: StrokeInput,
        shapeSpec: ShapeSpecT,
        strokeToViewTransform: AndroidMatrix,
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
                AndroidMatrix().apply {
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
        inputToStrokeTransform: AndroidMatrix = AndroidMatrix(),
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
        queueInputToRenderThread(startAction)
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
            (threadSharedState.addActionPool.poll() ?: AddAction()).apply {
                check(realInputs.isEmpty())
                check(realInputLatencyDatas.isEmpty())
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
            (threadSharedState.addActionPool.poll() ?: AddAction()).apply {
                check(realInputs.isEmpty())
                check(realInputLatencyDatas.isEmpty())
                check(predictedInputs.isEmpty())
                check(predictedInputLatencyDatas.isEmpty())
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
            threadSharedState.addActionPool.offer(addAction)
            return
        }
        queueInputToRenderThread(addAction)
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
        queueInputToRenderThread(FinishAction(input, strokeId, forceCompletion, latencyData))
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
        queueInputToRenderThread(cancelAction)
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
        // render thread is done with are added to strokesAwaitingEndOfCohort.
        while (threadSharedState.finishedStrokes.isNotEmpty()) {
            // finishedStrokes was just confirmed to not be empty, so polling it should never return
            // null.
            // This wouldn't necessarily be true in all multithreaded scenarios, but for
            // finishedStrokes,
            // items are only ever removed from it by the UI thread, and the render thread only ever
            // adds
            // items to it, so there is not another thread that could have come in and removed items
            // between isEmpty and poll.
            val (strokeId, finishedStroke) = checkNotNull(threadSharedState.finishedStrokes.poll())
            val strokeState = uiThreadState.currentCohort[strokeId]
            if (strokeState is UiStrokeState.InputCompleted) {
                uiThreadState.currentCohort[strokeId] = UiStrokeState.Finished(finishedStroke)
            }
        }

        // Check that all strokes currently being rendered are either canceled or finished (input
        // complete and fully generated) and ready to be handed off.
        val handingOff = mutableMapOf<InProgressStrokeId, FinishedStroke<CompletedShapeT>>()
        for ((strokeId, strokeState) in uiThreadState.currentCohort) {
            when (strokeState) {
                is UiStrokeState.Started,
                is UiStrokeState.InputCompleted -> return StillInProgress
                is UiStrokeState.Canceled -> continue
                is UiStrokeState.Finished<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val finishedStrokeState = strokeState as UiStrokeState.Finished<CompletedShapeT>
                    handingOff[strokeId] = finishedStrokeState.finishedStroke
                }
            }
        }
        if (handingOff.isEmpty()) {
            return NoneInProgressOrFinished
        }
        if (uiThreadState.cohortHandoffPaused) {
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
        uiThreadState.currentCohort.clear()
        return Finished(handingOff)
    }

    @UiThread
    private fun onEndOfStrokeCohortCheck() {
        val claimStrokesToHandOffResult = claimStrokesToHandOff()
        if (claimStrokesToHandOffResult !is Finished<*>) {
            if (claimStrokesToHandOffResult is NoneInProgressButDebouncing) {
                potentialEndOfStrokeCohort()
            }
            return
        }

        @Suppress("UNCHECKED_CAST")
        val finished = claimStrokesToHandOffResult as Finished<CompletedShapeT>
        handOffFinishedStrokes(finished.finishedStrokes)
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
     * [InProgressStrokesFinishedListener.onStrokesFinished]. Afterwards, handoff debouncing will
     * resume as normal.
     *
     * This API is experimental for now, as one approach to address start-of-stroke latency for fast
     * subsequent strokes.
     */
    @UiThread
    fun requestImmediateHandoff() {
        uiThreadState.cohortHandoffAsap = true
        potentialEndOfStrokeCohort()
    }

    /**
     * Make a best effort to finish or cancel all in-progress strokes, and if appropriate, execute
     * [Listener.onAllStrokesFinished] synchronously. This must be called on the UI thread, and
     * blocks it, so this should only be used in synchronous shutdown scenarios.
     *
     * @return `true` if and only if the flush completed successfully. Note that not all
     *   configurations support flushing, and flushing is best effort, so this is not guaranteed to
     *   return `true`.
     */
    @UiThread
    fun flush(timeout: Long, timeoutUnit: TimeUnit, cancelAllInProgress: Boolean): Boolean {
        if (!inProgressStrokesRenderHelper.supportsFlush) {
            return false
        }
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
        if (
            threadSharedState.inputActions.isNotEmpty() ||
                threadSharedState.currentlyHandlingActions.get()
        ) {
            threadSharedState.pauseInputs.set(false)
            val flushAction = FlushAction()
            queueInputToRenderThread(flushAction)
            blockingAwait(flushAction.flushCompleted, timeout, timeoutUnit)
        }
        uiThreadState.cohortHandoffAsap = true
        uiThreadState.cohortHandoffPaused = false
        // It's unlikely that the result would be anything other than Finished, but it's possible
        // with
        // a short enough timeout.
        return when (val claimStrokesToHandOffResult = claimStrokesToHandOff()) {
            is Finished<*> -> {
                @Suppress("UNCHECKED_CAST")
                val finished = claimStrokesToHandOffResult as Finished<CompletedShapeT>
                handOffFinishedStrokes(finished.finishedStrokes)
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
        if (!inProgressStrokesRenderHelper.supportsFlush) {
            return
        }
        val syncAction = SyncAction()
        queueInputToRenderThread(syncAction)
        blockingAwait(syncAction.syncCompleted, timeout, timeoutUnit)
    }

    @UiThread
    override fun setPauseStrokeCohortHandoffs(paused: Boolean) {
        val oldPaused = uiThreadState.cohortHandoffPaused
        uiThreadState.cohortHandoffPaused = paused
        if (oldPaused && !paused) {
            potentialEndOfStrokeCohort()
        }
    }

    @UiThread
    override fun onStrokeCohortHandoffToHwui(
        strokeCohort: Map<InProgressStrokeId, FinishedStroke<CompletedShapeT>>
    ) {
        for (listener in uiThreadState.listeners) {
            listener.onAllStrokesFinished(strokeCohort)
        }
        inProgressStrokeCounter?.let { counter ->
            repeat(strokeCohort.size) { counter.decrement() }
        }
    }

    @UiThread
    override fun onStrokeCohortHandoffToHwuiComplete() {
        threadSharedState.pauseInputs.set(false)
        inProgressStrokesRenderHelper.requestDraw()
    }

    /**
     * Queue the [inputAction] to the render thread, then request a frontbuffer redraw. Frontbuffer
     * redraws consume all queued input actions.
     */
    @UiThread
    private fun queueInputToRenderThread(input: InputAction) {
        threadSharedState.inputActions.offer(input)
        if (!threadSharedState.pauseInputs.get()) {
            inProgressStrokesRenderHelper.requestDraw()
        }
    }

    @WorkerThread
    private fun handleAction(action: InputAction, systemElapsedTimeNanos: Long) {
        assertOnRenderThread()
        when (action) {
            is StartAction<*> -> {
                @Suppress("UNCHECKED_CAST") val startAction = action as StartAction<ShapeSpecT>
                handleStartStroke(startAction, systemElapsedTimeNanos)
            }
            is AddAction -> handleAddToStroke(action)
            is FinishAction -> handleFinishStroke(action, systemElapsedTimeNanos)
            is CancelAction -> handleCancelStroke(action)
            is MotionEventToViewTransformAction -> handleMotionEventToViewTransformAction(action)
            is ClearAction -> handleClear()
            is FlushAction -> handleFlushAction(action)
            // Nothing to do before drawing for [AnimationFrameAction]. Similar to [AddAction],
            // rather
            // than updating the shape immediately, we wait to update the shape until we have
            // handled all
            // the actions in threadSharedState.inputActions. This is being done to reduce that
            // amount of
            // updateShape calls in case there are input points arriving around the same time as
            // animation
            // frame actions.
            // Nothing to do before drawing for [SyncAction].
            else -> {}
        }
    }

    @WorkerThread
    private fun handleActionAfterDraw(action: InputAction) {
        assertOnRenderThread()
        when (action) {
            is FinishAction -> handleFinishStrokeAfterDraw()
            is AnimationFrameAction -> handleAnimationFrameAfterDraw()
            is CancelAction -> handleCancelStrokeAfterDraw(action)
            is SyncAction -> handleSyncActionAfterDraw(action)
            // Nothing to do after drawing for the other actions.
            else -> {}
        }
    }

    private fun RenderThreadStrokeState<ShapeSpecT, InProgressShapeT, CompletedShapeT>
        .enqueueInputs(realInputs: StrokeInputBatch, predictedInputs: StrokeInputBatch) {
        inProgressShape.enqueueInputs(realInputs = realInputs, predictedInputs = predictedInputs)
        hasUnprocessedInputs = true
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
            AndroidMatrix().apply { action.motionEventToStrokeTransform.invert(this) }
        val strokeState = run {
            val shapeSpec = action.shapeSpec
            val shape = renderThreadState.inProgressStrokePool.obtain(shapeSpec)
            RenderThreadStrokeState(
                    inProgressShape = shape,
                    strokeToMotionEventTransform = strokeToMotionEventTransform,
                    startEventTimeMillis = action.startEventTimeMillis,
                )
                .apply {
                    inProgressShape.start(shapeSpec, action.startEventTimeMillis)
                    enqueueInputs(
                        MutableStrokeInputBatch().apply { runCatching { add(action.strokeInput) } },
                        ImmutableStrokeInputBatch.EMPTY,
                    )
                    // Use the current time rather than action.startEventTimeMillis, because some
                    // time may
                    // have elapsed as part of input processing and the current time will be more
                    // accurate for
                    // shape generation and animation effects.
                    updateShape(systemElapsedTimeNanos)
                }
        }
        threadSharedState.strokeInputPool.recycle(action.strokeInput)
        renderThreadState.toDrawStrokes[action.strokeId] = strokeState
        if (strokeState.inProgressShape.changesWithTime()) {
            postToUiThread(::scheduleAnimationFrameAction)
        }
        action.latencyData?.let { renderThreadState.latencyDatas.add(it) }
    }

    /** Handle an action that was initiated by [addToStroke]. */
    @WorkerThread
    private fun handleAddToStroke(action: AddAction) {
        assertOnRenderThread()
        val strokeState = renderThreadState.toDrawStrokes[action.strokeId]
        checkNotNull(strokeState) { "Stroke state with ID ${action.strokeId} was not found." }
        check(!renderThreadState.generatedStrokes.contains(action.strokeId)) {
            "Stroke with ID ${action.strokeId} was already finished."
        }
        check(!renderThreadState.canceledStrokes.contains(action.strokeId)) {
            "Stroke with ID ${action.strokeId} was canceled."
        }
        strokeState.enqueueInputs(action.realInputs, action.predictedInputs)
        // Rather than calling updateShape immediately, we enqueue the inputs and wait to update the
        // shape until we have handled all the inputs in threadSharedState.inputActions. This is
        // being done to reduce that amount of update calls.
        strokeState.hasUnprocessedInputs = true

        // Recycle the AddAction.
        action.realInputs.clear()
        action.predictedInputs.clear()
        while (!action.realInputLatencyDatas.isEmpty()) {
            renderThreadState.latencyDatas.add(action.realInputLatencyDatas.removeFirst())
        }
        while (!action.predictedInputLatencyDatas.isEmpty()) {
            renderThreadState.latencyDatas.add(action.predictedInputLatencyDatas.removeFirst())
        }
        threadSharedState.addActionPool.offer(action)
    }

    /** Handle an action that was initiated by [finishStroke]. */
    @WorkerThread
    private fun handleFinishStroke(action: FinishAction, systemElapsedTimeNanos: Long) {
        assertOnRenderThread()
        val strokeState = renderThreadState.toDrawStrokes[action.strokeId]
        checkNotNull(strokeState) { "Stroke state with ID ${action.strokeId} was not found." }
        check(!renderThreadState.generatedStrokes.contains(action.strokeId)) {
            "Stroke with ID ${action.strokeId} was already finished."
        }
        check(!renderThreadState.canceledStrokes.contains(action.strokeId)) {
            "Stroke with ID ${action.strokeId} was canceled."
        }
        fillStrokeToViewTransform(strokeState)
        val copiedStrokeToViewTransform =
            AndroidMatrix().apply { set(renderThreadState.strokeToViewTransform) }
        // Save the stroke to be handed off.
        if (action.strokeInput != null) {
            strokeState.enqueueInputs(
                MutableStrokeInputBatch().apply { runCatching { add(action.strokeInput) } },
                ImmutableStrokeInputBatch.EMPTY,
            )
        }
        // InProgressShape contract specifies that `finishInput` is called between the last
        // `enqueueInputs` and before the subsequent `update`.
        strokeState.inProgressShape.finishInput()
        // We update the finished stroke immediately after enqueueing because we know we are not
        // going
        // to be receiving any other inputs.
        strokeState.updateShape(systemElapsedTimeNanos, forceCompletion = action.forceCompletion)
        val completedShape = strokeState.inProgressShape.getCompletedShape()
        if (completedShape == null) {
            renderThreadState.dryingStrokes.add(action.strokeId)
            postToUiThread(::scheduleAnimationFrameAction)
        } else {
            renderThreadState.generatedStrokes[action.strokeId] =
                FinishedStroke(stroke = completedShape, copiedStrokeToViewTransform)
            if (strokeState.inProgressShape.changesWithTime()) {
                postToUiThread(::scheduleAnimationFrameAction)
            }
        }
        if (action.strokeInput != null) {
            threadSharedState.strokeInputPool.recycle(action.strokeInput)
        }
        action.latencyData?.let { renderThreadState.latencyDatas.add(it) }
        // Clean up state and notify the UI thread of the potential end of this cohort after
        // drawing.
    }

    @WorkerThread
    private fun handleFinishStrokeAfterDraw() {
        moveGeneratedStrokesToFinishedStrokes()
    }

    /**
     * Arranges to queue an [AnimationFrameAction] on the next animation frame. If this is called
     * multiple times between animation frames, only one [AnimationFrameAction] will be queued.
     */
    @UiThread
    private fun scheduleAnimationFrameAction() {
        postOnAnimation(uiThreadState.queueAnimationFrameActionOnce.setUp())
    }

    /**
     * Queues an [AnimationFrameAction] to the render thread. This is the implementation for
     * [uiThreadState.queueAnimationFrameActionOnce]; use that instead of calling this directly.
     */
    @UiThread
    private fun queueAnimationFrameAction() {
        queueInputToRenderThread(AnimationFrameAction)
    }

    @WorkerThread
    private fun handleAnimationFrameAfterDraw() {
        for ((strokeId, strokeState) in renderThreadState.toDrawStrokes) {
            if (renderThreadState.dryingStrokes.contains(strokeId)) {
                val completedShape = strokeState.inProgressShape.getCompletedShape()
                if (completedShape != null) {
                    // The stroke is now fully dry - remove it from [dryingStrokes] and mark it
                    // finished.
                    renderThreadState.dryingStrokes.remove(strokeId)
                    fillStrokeToViewTransform(strokeState)
                    val copiedStrokeToViewTransform =
                        AndroidMatrix().apply { set(renderThreadState.strokeToViewTransform) }
                    renderThreadState.generatedStrokes[strokeId] =
                        FinishedStroke(
                            stroke = completedShape,
                            strokeToViewTransform = copiedStrokeToViewTransform,
                        )
                }
            }
        }
        if (renderThreadState.toDrawStrokes.values.any { it.inProgressShape.changesWithTime() }) {
            postToUiThread(::scheduleAnimationFrameAction)
        }
        moveGeneratedStrokesToFinishedStrokes()
    }

    /**
     * Moves ownership of the generated strokes from the render thread to the UI thread, and
     * notifies the UI thread of the potential end of this stroke cohort, but keeps the in-progress
     * version of those strokes in [toDrawStrokes] so they can continue to be drawn as wet strokes
     * until the UI thread actually ends this stroke cohort.
     */
    @WorkerThread
    private fun moveGeneratedStrokesToFinishedStrokes() {
        threadSharedState.finishedStrokes.addAll(renderThreadState.generatedStrokes.asIterable())
        renderThreadState.generatedStrokes.clear()
        postToUiThread(::potentialEndOfStrokeCohort)
    }

    /** Handle an action that was initiated by [cancelStroke]. */
    @WorkerThread
    private fun handleCancelStroke(action: CancelAction) {
        assertOnRenderThread()
        val strokeState =
            checkNotNull(renderThreadState.toDrawStrokes[action.strokeId]) {
                "Stroke state with ID ${action.strokeId} was not found."
            }
        strokeState.inProgressShape.cancel()
        // Mark the stroke as canceled just for the draw step so it can be cleared, and then forget
        // about it entirely in handleCancelStrokeAfterDraw.
        renderThreadState.canceledStrokes.add(action.strokeId)
        // If it was already finished but not yet handed off, can still cancel it.
        renderThreadState.generatedStrokes.remove(action.strokeId)
        // Don't save the stroke to be handed off as in handleFinishStroke.
        renderThreadState.latencyDatas.add(action.latencyData)
        // Clean up state and possibly send callbacks after drawing.
    }

    @WorkerThread
    private fun handleCancelStrokeAfterDraw(action: CancelAction) {
        // Remove its state since we won't be adding to it anymore and it no longer should be drawn.
        val removedStrokeState = renderThreadState.toDrawStrokes.remove(action.strokeId)
        if (removedStrokeState != null) {
            renderThreadState.inProgressStrokePool.recycle(removedStrokeState.inProgressShape)
        }

        inProgressStrokeCounter?.decrement()

        postToUiThread(::potentialEndOfStrokeCohort)
    }

    @UiThread
    private fun potentialEndOfStrokeCohort() {
        // This may be the end of the current cohort of strokes, but wait until all inputs have been
        // processed in a HWUI frame (in onAnimation) to ensure that any strokes that are present in
        // the
        // same frame are considered part of the same cohort.
        postOnAnimation(uiThreadState.checkEndOfStrokeCohortOnce.setUp())
    }

    /** Handle an action that was initiated by setting [motionEventToViewTransform]. */
    @WorkerThread
    private fun handleMotionEventToViewTransformAction(action: MotionEventToViewTransformAction) {
        assertOnRenderThread()
        renderThreadState.motionEventToViewTransform.set(action.motionEventToViewTransform)
    }

    @WorkerThread
    private fun handleClear() {
        assertOnRenderThread()
        for (strokeState in renderThreadState.toDrawStrokes.values) {
            renderThreadState.inProgressStrokePool.recycle(strokeState.inProgressShape)
        }

        // Clear state.
        renderThreadState.toDrawStrokes.clear()
        renderThreadState.generatedStrokes.clear()
        renderThreadState.canceledStrokes.clear()
        if (inProgressStrokesRenderHelper.contentsPreservedBetweenDraws) {
            inProgressStrokesRenderHelper.clear()
        }
    }

    @WorkerThread
    private fun handleFlushAction(action: FlushAction) {
        action.flushCompleted.countDown()
    }

    @WorkerThread
    private fun handleSyncActionAfterDraw(action: SyncAction) {
        action.syncCompleted.countDown()
    }

    /** Called by the [InProgressStrokesRenderHelper] when it can be drawn to. */
    @WorkerThread
    override fun onDraw() {
        assertOnRenderThread()
        check(renderThreadState.handledActions.isEmpty())
        // Skip drawing until input is unpaused.
        if (threadSharedState.pauseInputs.get()) return
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
        while (threadSharedState.inputActions.isNotEmpty()) {
            val nextInputAction = threadSharedState.inputActions.poll()
            // Even though the isNotEmpty check and the poll are not synchronized with one another
            // and in
            // a fully multi-threaded scenario it would be possible for the poll to be null after
            // checking
            // isNotEmpty, in our use case the render thread is the only one removing items from
            // this
            // queue so there should be no way for the queue to be empty by the time we poll it.
            checkNotNull(nextInputAction) {
                "requestRender was called without adding input action."
            }
            handleAction(nextInputAction, systemElapsedTimeNanos)
            renderThreadState.handledActions.add(nextInputAction)
        }
        for (strokeState in renderThreadState.toDrawStrokes.values) {
            if (strokeState.hasUnprocessedInputs || strokeState.inProgressShape.changesWithTime()) {
                strokeState.updateShape(systemElapsedTimeNanos)
            }
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
        for ((strokeIdToDraw, strokeStateToDraw) in renderThreadState.toDrawStrokes) {
            // renderThreadState.strokeStates still contains any canceled strokes so that the space
            // they occupied (as reported by InProgressShape.getUpdatedRegion after calling
            // [InProgressShape.cancel]) is cleared and redrawn with other non-canceled strokes.
            // This
            // conditional `continue` prevents canceled strokes from being drawn here. The canceled
            // strokes will be removed from renderThreadState.strokeStates after drawing is
            // finished.
            if (renderThreadState.canceledStrokes.contains(strokeIdToDraw)) continue
            drawStrokeState(strokeStateToDraw)
        }
        inProgressStrokesRenderHelper.afterDrawInModifiedRegion()
    }

    @WorkerThread
    override fun onDrawComplete() {
        renderThreadState.handledActions.forEach(this::handleActionAfterDraw)
        renderThreadState.handledActions.clear()
        threadSharedState.currentlyHandlingActions.set(false)
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
        postToUiThread(::handOffLatencyDataToClient)
    }

    @UiThread
    private fun handOffLatencyDataToClient() {
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
     * Fill [renderThreadState.updatedRegion] with the region that has been updated and must be
     * redrawn, in stroke coordinates. Return `true` if and only if there is actually a region to be
     * updated.
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
    private fun assertOnRenderThread() {
        inProgressStrokesRenderHelper.assertOnRenderThread()
    }

    /**
     * Hands off a cohort of finished strokes to HWUI.
     *
     * @param finishedStrokes The finished strokes, with map iteration order in stroke z-order from
     *   back to front.
     */
    @UiThread
    private fun handOffFinishedStrokes(
        finishedStrokes: Map<InProgressStrokeId, FinishedStroke<CompletedShapeT>>
    ) {
        uiThreadState.cohortHandoffAsap = false
        uiThreadState.lastStrokeInputCompletedSystemElapsedTimeMillis = Long.MIN_VALUE

        threadSharedState.pauseInputs.set(true)
        // Queue a clear action to take place as soon as inputs are unpaused, to be sure the clear
        // happens before any inputs for the new cohort.
        queueInputToRenderThread(ClearAction)
        inProgressStrokesRenderHelper.requestStrokeCohortHandoffToHwui(finishedStrokes)
    }

    /** An input event that can go in the (future) event queue to hand off across threads. */
    private sealed interface InputAction

    /** Represents the data passed to [startStroke]. */
    private data class StartAction<ShapeSpecT>(
        val strokeInput: StrokeInput,
        val strokeId: InProgressStrokeId,
        val motionEventToStrokeTransform: AndroidMatrix,
        val shapeSpec: ShapeSpecT,
        val latencyData: LatencyData?,
        val startEventTimeMillis: Long,
    ) : InputAction

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
    ) : InputAction

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
    ) : InputAction

    /**
     * Indicates that it's time to update the shape and/or appearance of
     * [renderThreadState.dryingStrokes] and those where
     * [InProgressShape.willTimeUpdatesAffectUpdatedRegion] is true.
     */
    private object AnimationFrameAction : InputAction

    /** Represents the data passed to [cancelStroke]. */
    private data class CancelAction(
        val strokeId: InProgressStrokeId,
        val latencyData: LatencyData,
    ) : InputAction

    /** Represents an update to [motionEventToViewTransform]. */
    private data class MotionEventToViewTransformAction(
        val motionEventToViewTransform: AndroidMatrix
    ) : InputAction

    /**
     * Represents a request to clear the data of a stroke cohort being handed off by
     * [onEndOfStrokeCohortCheck].
     */
    private object ClearAction : InputAction

    /**
     * Represents a request to synchronize across threads, so that the UI thread can block on this
     * operation in the action queue being reached and handled by the render thread.
     */
    private class FlushAction : InputAction {
        val flushCompleted = CountDownLatch(1)
    }

    /**
     * Represents a request to synchronize across threads, so that the UI thread can block on this
     * operation in the action queue being reached and handled by the render thread.
     */
    private class SyncAction : InputAction {
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
     * but [setPauseStrokeCohortHandoffs] is currently preventing handoff.
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
        @Size(min = 1) val finishedStrokes: Map<InProgressStrokeId, FinishedStroke<CompletedShapeT>>
    ) : ClaimStrokesToHandOffResult {
        init {
            require(finishedStrokes.isNotEmpty())
        }
    }

    /** Holds the state for a given stroke, as needed by the render thread. */
    private class RenderThreadStrokeState<
        ShapeSpecT : Any,
        InProgressShapeT : InProgressShape<ShapeSpecT, CompletedShapeT>,
        CompletedShapeT : Any,
    >(
        val inProgressShape: InProgressShapeT,
        val strokeToMotionEventTransform: AndroidMatrix,
        val startEventTimeMillis: Long,
        var hasUnprocessedInputs: Boolean = false,
    )

    /**
     * Holds the state for a given stroke in the current cohort, as needed by the UI thread. New
     * strokes start out in the [Started] state, then move to [InputCompleted] once [finishStroke]
     * is called, or to [Canceled] if [cancelStroke] is called first. Strokes in the
     * [InputCompleted] state move to [Finished] once the stroke has been fully generated by the
     * render thread. Once all strokes in the cohort are either [Canceled] or [Finished], the cohort
     * can be handed off.
     */
    private sealed interface UiStrokeState {

        /** UI thread state for a stroke that has been started, but not yet finished or canceled. */
        class Started(
            val motionEventToStrokeTransform: AndroidMatrix,
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
