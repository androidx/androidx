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
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.CheckResult
import androidx.annotation.Size
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.authoring.latency.LatencyDataCallback
import androidx.ink.authoring.latency.LatencyDataPool
import androidx.ink.brush.Brush
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.MutableBox
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
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
 * public-facing [InProgressStrokesView].
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
@OptIn(ExperimentalLatencyDataApi::class)
internal class InProgressStrokesManager(
    private val inProgressStrokesRenderHelper: InProgressStrokesRenderHelper,
    /** A lambda to run a [Runnable] on the next animation frame. */
    private val postOnAnimation: (Runnable) -> Unit,
    /** A lambda to run a [Runnable] on the next run loop of the UI thread. */
    private val postToUiThread: (Runnable) -> Unit,
    /** The callback for reporting latency data to the client. */
    private val latencyDataCallback: LatencyDataCallback = LatencyDataCallback {},
    /** For getting timestamps for latency measurement. Injectable for testing only. */
    private inline val getNanoTime: () -> Long = System::nanoTime,
    /** For getting instances of [InProgressStroke]. Injectable for testing only. */
    inProgressStrokePool: InProgressStrokePool = InProgressStrokePool.create(),
    /**
     * Allows tests to replace [CountDownLatch.await] with something that yields rather than blocks.
     */
    private val blockingAwait: (CountDownLatch, Long, TimeUnit) -> Boolean =
        { latch, timeout, timeoutUnit ->
            latch.await(timeout, timeoutUnit)
        },
) : InProgressStrokesRenderHelper.Callback {

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

    internal interface Listener {
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
         */
        @UiThread fun onAllStrokesFinished(strokes: Map<InProgressStrokeId, FinishedStroke>)
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
             * Maps stroke IDs to the matrix [motionEventToStrokeTransform] and the stroke's
             * [startEventTimeMillis]. This only contains strokes that have been started, but not
             * yet finished or canceled.
             */
            val startedStrokes = mutableMapOf<InProgressStrokeId, UiStrokeState>()

            /**
             * This contains strokes that have been started and then finished, but not those that
             * were canceled.
             */
            val inputCompletedStrokes = mutableSetOf<InProgressStrokeId>()

            /**
             * Strokes that have been finished and fully generated and are ready to be handed off.
             */
            val strokesAwaitingEndOfCohort = mutableMapOf<InProgressStrokeId, FinishedStroke>()

            /**
             * Runs [onEndOfStrokeCohortCheck] at most once per frame, even if this is passed to
             * [postOnAnimation] more than once during that frame.
             */
            val checkEndOfStrokeCohortOnce = AtMostOnceAfterSetUp(::onEndOfStrokeCohortCheck)

            var cohortHandoffDebounceTimeMs = 0L

            var cohortHandoffAsap = false

            var cohortHandoffPaused = false

            var lastStrokeEndUptimeMs = Long.MIN_VALUE

            val queueUpdateActionOnce = AtMostOnceAfterSetUp(::queueUpdateAction)

            /** Strokes that have been canceled. */
            val canceledStrokes = mutableSetOf<InProgressStrokeId>()

            /** To notify when strokes have been completed. Owned by the UI thread. */
            val listeners = mutableSetOf<Listener>()
        }
        @UiThread
        get() {
            return field
        }

    /** The state that is accessed just by the render thread. */
    private val renderThreadState =
        object {

            /**
             * Strokes that are being drawn by this class. This includes the contents of
             * [generatedStrokes].
             */
            val toDrawStrokes = mutableMapOf<InProgressStrokeId, RenderThreadStrokeState>()

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
            val generatedStrokes = mutableMapOf<InProgressStrokeId, FinishedStroke>()

            /**
             * Strokes that have been canceled, thus should not be drawn or passed to the UI thread
             * for client handoff.
             */
            val canceledStrokes = mutableSetOf<InProgressStrokeId>()

            /**
             * Contains instances of [InProgressStroke] that are not currently in use (does not
             * belong to [toDrawStrokes]) and are ready to be used in a new stroke. This is to reuse
             * memory that has already been allocated to improve performance after the first few
             * strokes, and to minimize memory fragmentation that can affect the health of the app's
             * process over time. This will grow as needed to match the size of the biggest stroke
             * cohort seen in the last N handoffs. A hard limit on the pool size wouldn't be
             * appropriate as each app and each user will have different patterns, and the value of
             * [setHandoffDebounceTimeMs] will influence the number of [InProgressStroke] instances
             * needed at once. But trimming the size of this pool according to recent activity (see
             * [recentCohortSizes]) ensures that an unusually large cohort won't force too much
             * memory to be held for the rest of the inking session.
             */
            val inProgressStrokePool = inProgressStrokePool

            /**
             * The N most recent values for how many [InProgressStroke] instances have been needed
             * at once. Start with all zeroes because so far none have been needed.
             */
            val recentCohortSizes = IntArray(10)

            /** The index in the [recentCohortSizes] circular buffer to update next. */
            var recentCohortSizesNextIndex = 0

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
                ConcurrentLinkedQueue<Map.Entry<InProgressStrokeId, FinishedStroke>>()

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
    fun addListener(listener: Listener) {
        uiThreadState.listeners.add(listener)
    }

    /** Remove a listener for when strokes have been completed. Must be called on the UI thread. */
    @UiThread
    fun removeListener(listener: Listener) {
        uiThreadState.listeners.remove(listener)
    }

    /**
     * Start building a stroke with the [event] data at [pointerIndex].
     *
     * @param event The first [MotionEvent] as part of a Stroke's input data, typically an
     *   ACTION_DOWN.
     * @param pointerIndex The index of the relevant pointer in the [event].
     * @param motionEventToWorldTransform The matrix that transforms [event] coordinates into the
     *   client app's "world" coordinates, which typically is defined by how a client app's document
     *   is panned/zoomed/rotated.
     * @param strokeToWorldTransform An optional matrix that transforms this stroke into the client
     *   app's "world" coordinates, which allows the coordinates of the stroke to be defined in
     *   something other than world coordinates. Defaults to the identity matrix, in which case the
     *   stroke coordinate space is the same as world coordinate space. This matrix must be
     *   invertible.
     * @param brush Brush specification for the stroke being started.
     * @param strokeUnitLengthCm The physical distance that the pointer must travel in order to
     *   produce an input motion of one stroke unit for this particular stroke, in centimeters.
     * @return The Stroke ID of the stroke being built, later used to identify which stroke is being
     *   added to, finished, or canceled.
     * @throws IllegalArgumentException if [strokeToWorldTransform] is not invertible.
     */
    @UiThread
    fun startStroke(
        event: MotionEvent,
        pointerIndex: Int,
        motionEventToWorldTransform: AndroidMatrix,
        strokeToWorldTransform: AndroidMatrix,
        brush: Brush,
        strokeUnitLengthCm: Float,
    ): InProgressStrokeId {
        val receivedActionTimeNanos = getNanoTime()
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
            threadSharedState.strokeInputPool.obtainSingleValueForMotionEvent(
                event,
                pointerIndex,
                motionEventToStrokeTransform,
                event.eventTime,
                strokeUnitLengthCm,
            ),
            brush,
            event.eventTime,
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
     * @param brush Brush specification for the stroke being started.
     * @param startTimeMillis Start time of the stroke, used to determine the relative timing of
     *   later additions of the stroke.
     * @return The Stroke ID of the stroke being built, later used to identify which stroke is being
     *   added to, finished, or canceled.
     */
    @UiThread
    fun startStroke(input: StrokeInput, brush: Brush): InProgressStrokeId {
        // The start time here isn't really relevant unless this override of startStroke is combined
        // with the MotionEvent override of addToStroke or finishStroke.
        return startStrokeInternal(input, brush, getNanoTime() / 1_000_000L)
    }

    @UiThread
    private fun startStrokeInternal(
        input: StrokeInput,
        brush: Brush,
        startTimeMillis: Long,
        strokeId: InProgressStrokeId = InProgressStrokeId.create(),
        inputToStrokeTransform: AndroidMatrix = AndroidMatrix(),
        // TODO: b/364655356 - Add support for collecting LatencyData in the
        // StrokeInput[Batch]-based
        // API.
        latencyData: LatencyData? = null,
    ): InProgressStrokeId {
        inProgressStrokeCounter?.increment()
        val strokeState =
            UiStrokeState(inputToStrokeTransform, startTimeMillis, input.strokeUnitLengthCm)
        uiThreadState.startedStrokes[strokeId] = strokeState
        val startAction =
            StartAction(
                input,
                strokeId,
                inputToStrokeTransform,
                brush,
                latencyData,
                startTimeMillis
            )
        queueInputToRenderThread(startAction)
        return startAction.strokeId
    }

    /**
     * Add [event] data at [pointerIndex] to already started stroke with [strokeId].
     *
     * @param event the next [MotionEvent] as part of a Stroke's input data, typically an
     *   ACTION_MOVE.
     * @param pointerIndex the index of the relevant pointer in the [event].
     * @param strokeId the Stroke that is to be built upon with [event].
     * @param prediction optional predicted MotionEvent containing predicted inputs between event
     *   and the time of the next frame, as generated by MotionEventPredictor::predict.
     */
    @UiThread
    fun addToStroke(
        event: MotionEvent,
        pointerIndex: Int,
        strokeId: InProgressStrokeId,
        prediction: MotionEvent?,
    ) {
        val receivedActionTimeNanos = getNanoTime()
        val strokeState = uiThreadState.startedStrokes[strokeId]
        checkNotNull(strokeState) { "Stroke with ID $strokeId was not found." }
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
                    threadSharedState.strokeInputPool.obtainAllHistoryForMotionEvent(
                        event = prediction,
                        pointerIndex = pointerIndex,
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
                this.strokeId = strokeId
            }
        queueAddActionIfNonEmpty(addAction)
    }

    /**
     * Add [inputs] to already started stroke with [strokeId].
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
        val strokeState = uiThreadState.startedStrokes[strokeId]
        checkNotNull(strokeState) { "Stroke with ID $strokeId was not found." }
        val addAction =
            (threadSharedState.addActionPool.poll() ?: AddAction()).apply {
                check(realInputs.isEmpty())
                check(realInputLatencyDatas.isEmpty())
                check(predictedInputs.isEmpty())
                check(predictedInputLatencyDatas.isEmpty())
                realInputs.addOrIgnore(inputs)
                predictedInputs.addOrIgnore(prediction)
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
     * Complete the building of a stroke.
     *
     * @param event the last [MotionEvent] as part of a stroke, typically an ACTION_UP.
     * @param pointerIndex the index of the relevant pointer.
     * @param strokeId the stroke that is to be finished with the latest event.
     */
    @UiThread
    fun finishStroke(event: MotionEvent, pointerIndex: Int, strokeId: InProgressStrokeId) {
        val receivedActionTimeNanos = getNanoTime()
        val strokeState = uiThreadState.startedStrokes[strokeId] ?: return
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
     * Complete the building of a stroke.
     *
     * @param input the last [StrokeInput] in a stroke.
     * @param strokeId the stroke that is to be finished with that input.
     */
    @UiThread
    fun finishStroke(input: StrokeInput, strokeId: InProgressStrokeId) {
        finishStrokeInternal(input, strokeId, getNanoTime() / 1_000_000L)
    }

    @UiThread
    private fun finishStrokeInternal(
        input: StrokeInput?,
        strokeId: InProgressStrokeId,
        endTimeMs: Long,
        latencyData: LatencyData? = null,
    ) {
        val finishAction = FinishAction(input, strokeId, latencyData)
        uiThreadState.lastStrokeEndUptimeMs = endTimeMs
        uiThreadState.startedStrokes.remove(strokeId)
        uiThreadState.inputCompletedStrokes.add(strokeId)
        queueInputToRenderThread(finishAction)
    }

    /**
     * Cancel the building of a stroke.
     *
     * @param strokeId the stroke to cancel.
     */
    @UiThread
    fun cancelStroke(strokeId: InProgressStrokeId, event: MotionEvent?) {
        val receivedActionTimeNanos = getNanoTime()
        uiThreadState.startedStrokes.remove(strokeId) ?: return
        uiThreadState.lastStrokeEndUptimeMs = receivedActionTimeNanos / 1_000_000
        uiThreadState.canceledStrokes.add(strokeId)
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
            uiThreadState.inputCompletedStrokes.remove(strokeId)
            if (!uiThreadState.canceledStrokes.contains(strokeId)) {
                uiThreadState.strokesAwaitingEndOfCohort[strokeId] = finishedStroke
            }
        }

        // Check that all strokes currently being rendered are finished (input complete and fully
        // generated) and ready to be handed off.
        if (
            uiThreadState.startedStrokes.isEmpty() && uiThreadState.inputCompletedStrokes.isEmpty()
        ) {
            if (uiThreadState.strokesAwaitingEndOfCohort.isEmpty()) {
                return NoneInProgressOrFinished
            }
            if (uiThreadState.cohortHandoffPaused) {
                return NoneInProgressButHandoffsPaused
            }
            if (
                inProgressStrokesRenderHelper.supportsDebounce &&
                    !uiThreadState.cohortHandoffAsap &&
                    getNanoTime() / 1_000_000 <
                        uiThreadState.lastStrokeEndUptimeMs +
                            uiThreadState.cohortHandoffDebounceTimeMs
            ) {
                return NoneInProgressButDebouncing
            }
            val handingOff = uiThreadState.strokesAwaitingEndOfCohort.toMap()
            uiThreadState.strokesAwaitingEndOfCohort.clear()
            return Finished(handingOff)
        }
        return StillInProgress
    }

    @UiThread
    private fun onEndOfStrokeCohortCheck() {
        val claimStrokesToHandOffResult = claimStrokesToHandOff()
        if (claimStrokesToHandOffResult !is Finished) {
            if (claimStrokesToHandOffResult is NoneInProgressButDebouncing) {
                potentialEndOfStrokeCohort()
            }
            return
        }

        uiThreadState.cohortHandoffAsap = false
        uiThreadState.lastStrokeEndUptimeMs = Long.MIN_VALUE

        uiThreadState.strokesAwaitingEndOfCohort.clear()

        threadSharedState.pauseInputs.set(true)
        // Queue a clear action to take place as soon as inputs are unpaused, to be sure the clear
        // happens before any inputs for the new cohort.
        queueInputToRenderThread(ClearAction)
        inProgressStrokesRenderHelper.requestStrokeCohortHandoffToHwui(
            claimStrokesToHandOffResult.finishedStrokes
        )
    }

    @UiThread
    fun setHandoffDebounceTimeMs(debounceTimeMs: Long) {
        if (!inProgressStrokesRenderHelper.supportsDebounce) {
            return
        }
        uiThreadState.cohortHandoffDebounceTimeMs = debounceTimeMs
        potentialEndOfStrokeCohort()
    }

    /**
     * Request that the value passed to [setHandoffDebounceTimeMs] be temporarily ignored to hand
     * off rendering to the client's dry layer via
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
        // cancelStroke/finishStroke will modify uiThreadState.startedStrokes, so make a copy to
        // avoid
        // a ConcurrentModificationException.
        for (id in uiThreadState.startedStrokes.keys.toList()) {
            if (cancelAllInProgress) {
                cancelStroke(id, event = null)
            } else {
                finishStrokeInternal(
                    input = null,
                    strokeId = id,
                    endTimeMs = getNanoTime() / 1_000_000
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
            is Finished -> {
                onStrokeCohortHandoffToHwui(claimStrokesToHandOffResult.finishedStrokes)
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
        strokeCohort: Map<InProgressStrokeId, FinishedStroke>
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
    private fun handleAction(action: InputAction) {
        assertOnRenderThread()
        when (action) {
            is StartAction -> handleStartStroke(action)
            is AddAction -> handleAddToStroke(action)
            is FinishAction -> handleFinishStroke(action)
            is UpdateAction -> handleUpdateStrokes()
            is CancelAction -> handleCancelStroke(action)
            is MotionEventToViewTransformAction -> handleMotionEventToViewTransformAction(action)
            is ClearAction -> handleClear()
            is FlushAction -> handleFlushAction(action)
            // Nothing to do before drawing for [SyncAction].
            else -> {}
        }
    }

    @WorkerThread
    private fun handleActionAfterDraw(action: InputAction) {
        assertOnRenderThread()
        when (action) {
            is FinishAction -> handleFinishStrokeAfterDraw()
            is UpdateAction -> handleUpdateStrokesAfterDraw()
            is CancelAction -> handleCancelStrokeAfterDraw(action)
            is SyncAction -> handleSyncActionAfterDraw(action)
            // Nothing to do after drawing for the other actions.
            else -> {}
        }
    }

    /** Handle an action that was initiated by [startStroke]. */
    @WorkerThread
    private fun handleStartStroke(action: StartAction) {
        assertOnRenderThread()
        val strokeToMotionEventTransform =
            AndroidMatrix().apply { action.motionEventToStrokeTransform.invert(this) }
        val strokeState = run {
            val stroke = renderThreadState.inProgressStrokePool.obtain()
            stroke.start(action.brush)
            stroke
                .enqueueInputs(
                    MutableStrokeInputBatch().addOrIgnore(action.strokeInput),
                    ImmutableStrokeInputBatch.EMPTY,
                )
                .onFailure {
                    // TODO(b/306361370): Throw here once input is more sanitized.
                    Log.w(
                        InProgressStrokesManager::class.simpleName,
                        "Error during InProgressStroke.enqueueInputs",
                        it,
                    )
                }
            stroke.updateShape(0).onFailure {
                // TODO(b/306361370): Throw here once input is more sanitized.
                Log.w(
                    InProgressStrokesManager::class.simpleName,
                    "Error during InProgressStroke.updateShape",
                    it,
                )
            }
            RenderThreadStrokeState(
                stroke,
                strokeToMotionEventTransform,
                startEventTimeMillis = action.startEventTimeMillis,
            )
        }
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
        check(!renderThreadState.generatedStrokes.contains(action.strokeId)) {
            "Stroke with ID ${action.strokeId} was already finished."
        }
        check(!renderThreadState.canceledStrokes.contains(action.strokeId)) {
            "Stroke with ID ${action.strokeId} was canceled."
        }
        strokeState.inProgressStroke.apply {
            enqueueInputs(action.realInputs, action.predictedInputs).onFailure {
                // TODO(b/306361370): Throw here once input is more sanitized.
                Log.w(
                    InProgressStrokesManager::class.simpleName,
                    "Error during InProgressStroke.enqueueInputs",
                    it,
                )
            }
            // TODO: b/287041801 - Don't necessarily always immediately [updateShape] after
            // [enqueueInputs].
            updateShape(getNanoTime() / 1_000_000L - strokeState.startEventTimeMillis).onFailure {
                // TODO(b/306361370): Throw here once input is more sanitized.
                Log.w(
                    InProgressStrokesManager::class.simpleName,
                    "Error during InProgressStroke.updateShape",
                    it,
                )
            }
        }
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
    private fun handleFinishStroke(action: FinishAction) {
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
            strokeState.inProgressStroke
                .enqueueInputs(
                    MutableStrokeInputBatch().addOrIgnore(action.strokeInput),
                    ImmutableStrokeInputBatch.EMPTY,
                )
                .onFailure {
                    // TODO(b/306361370): Throw here once input is more sanitized.
                    Log.w(
                        InProgressStrokesManager::class.simpleName,
                        "Error during InProgressStroke.enqueueInputs",
                        it,
                    )
                }
            // TODO: b/287041801 - Don't necessarily always immediately [updateShape] after
            // [enqueueInputs].
            strokeState.inProgressStroke
                .updateShape(getNanoTime() / 1_000_000L - strokeState.startEventTimeMillis)
                .onFailure {
                    // TODO(b/306361370): Throw here once input is more sanitized.
                    Log.w(
                        InProgressStrokesManager::class.simpleName,
                        "Error during InProgressStroke.updateShape",
                        it,
                    )
                }
        }
        strokeState.inProgressStroke.finishInput()
        if (strokeState.inProgressStroke.getNeedsUpdate()) {
            renderThreadState.dryingStrokes.add(action.strokeId)
            postToUiThread(::scheduleUpdateAction)
        } else {
            renderThreadState.generatedStrokes[action.strokeId] =
                FinishedStroke(
                    stroke = strokeState.inProgressStroke.toImmutable(),
                    copiedStrokeToViewTransform,
                )
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

    @WorkerThread
    private fun handleUpdateStrokes() {
        val nowMillis = getNanoTime() / 1_000_000L
        val dryingStrokesIterator = renderThreadState.dryingStrokes.iterator()
        for (strokeId in dryingStrokesIterator) {
            val strokeState = renderThreadState.toDrawStrokes[strokeId]
            checkNotNull(strokeState) { "Stroke state with ID ${strokeId} was not found." }
            val inProgressStroke = strokeState.inProgressStroke

            inProgressStroke.updateShape(nowMillis - strokeState.startEventTimeMillis).onFailure {
                // TODO(b/306361370): Throw here once input is more sanitized.
                Log.w(
                    InProgressStrokesManager::class.simpleName,
                    "Error during InProgressStroke.updateShape",
                    it,
                )
            }

            // If the stroke is now fully dry, remove it from [dryingStrokes] and mark it finished.
            if (!inProgressStroke.getNeedsUpdate()) {
                dryingStrokesIterator.remove()
                fillStrokeToViewTransform(strokeState)
                val copiedStrokeToViewTransform =
                    AndroidMatrix().apply { set(renderThreadState.strokeToViewTransform) }
                renderThreadState.generatedStrokes[strokeId] =
                    FinishedStroke(
                        stroke = inProgressStroke.toImmutable(),
                        copiedStrokeToViewTransform,
                    )
            }
        }

        // Schedule another [UpdateAction] if needed.
        if (!renderThreadState.dryingStrokes.isEmpty()) {
            postToUiThread(::scheduleUpdateAction)
        }
    }

    /**
     * Arranges to queue an [UpdateAction] on the next animation frame. If this is called multiple
     * times between animation frames, only one [UpdateAction] will be queued.
     */
    @UiThread
    private fun scheduleUpdateAction() {
        postOnAnimation(uiThreadState.queueUpdateActionOnce.setUp())
    }

    /**
     * Queues an [UpdateAction] to the render thread. This is the implementation for
     * [queueUpdateActionOnce]; use that instead of calling this directly.
     */
    @UiThread
    private fun queueUpdateAction() {
        queueInputToRenderThread(UpdateAction)
    }

    @WorkerThread
    private fun handleUpdateStrokesAfterDraw() {
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
        checkNotNull(renderThreadState.toDrawStrokes[action.strokeId]) {
            "Stroke state with ID ${action.strokeId} was not found."
        }
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
            renderThreadState.inProgressStrokePool.recycle(removedStrokeState.inProgressStroke)
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
        val cohortSize = renderThreadState.toDrawStrokes.size
        // Recycle instances of InProgressStroke.
        for (strokeState in renderThreadState.toDrawStrokes.values) {
            renderThreadState.inProgressStrokePool.recycle(strokeState.inProgressStroke)
        }

        // Clear state.
        renderThreadState.toDrawStrokes.clear()
        renderThreadState.generatedStrokes.clear()
        renderThreadState.canceledStrokes.clear()
        if (inProgressStrokesRenderHelper.contentsPreservedBetweenDraws) {
            inProgressStrokesRenderHelper.clear()
        }

        // Make sure we're holding onto a reasonable number of InProgressStroke instances, as
        // determined
        // by recent data on how many are needed simultaneously based on app and user behavior.
        renderThreadState.recentCohortSizes[renderThreadState.recentCohortSizesNextIndex] =
            cohortSize
        renderThreadState.recentCohortSizesNextIndex++
        if (
            renderThreadState.recentCohortSizesNextIndex >= renderThreadState.recentCohortSizes.size
        ) {
            renderThreadState.recentCohortSizesNextIndex = 0
        }
        val maxRecentCohortSize = renderThreadState.recentCohortSizes.max()
        renderThreadState.inProgressStrokePool.trimToSize(maxRecentCohortSize)
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
            handleAction(nextInputAction)
            renderThreadState.handledActions.add(nextInputAction)
        }

        if (inProgressStrokesRenderHelper.contentsPreservedBetweenDraws) {
            // The updated region for each stroke must be drawn into for all strokes, not just
            // itself, to
            // handle when a live stroke intersects another live stroke. Without this nested loop
            // (if
            // scissor+draw happened for each stroke in isolation), a live stroke A drawing over
            // another
            // live stroke B would clear a rectangle where B was previously drawn and only draw A in
            // that
            // space - but that part of B needs to be filled in again.
            for ((strokeIdToScissor, strokeStateToScissor) in renderThreadState.toDrawStrokes) {
                fillUpdatedStrokeRegion(strokeIdToScissor, strokeStateToScissor)
                val updatedRegionBox = renderThreadState.updatedRegion.box
                if (updatedRegionBox != null) {
                    renderThreadState.scratchRect.populateFrom(updatedRegionBox)
                    // Change updatedRegion from stroke coordinates to view coordinates.
                    fillStrokeToViewTransform(strokeStateToScissor)
                    renderThreadState.scratchRect.transform(renderThreadState.strokeToViewTransform)
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
                Float.POSITIVE_INFINITY
            )
            renderThreadState.scratchRect.setYBounds(
                Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY
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
            // they occupied can be cleared, but don't draw them again here. The canceled strokes
            // will
            // be removed from renderThreadState.strokeStates after drawing is finished.
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
        val time = getNanoTime()
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
    private fun fillUpdatedStrokeRegion(
        strokeId: InProgressStrokeId,
        strokeState: RenderThreadStrokeState,
    ) {
        if (renderThreadState.canceledStrokes.contains(strokeId)) {
            // Any space occupied by a canceled stroke must be redrawn to clear that stroke.
            renderThreadState.updatedRegion.reset()
            for (coatIndex in 0 until strokeState.inProgressStroke.getBrushCoatCount()) {
                strokeState.inProgressStroke.populateMeshBounds(
                    coatIndex,
                    renderThreadState.scratchEnvelope,
                )
                renderThreadState.updatedRegion.add(renderThreadState.scratchEnvelope)
            }
        } else {
            strokeState.inProgressStroke.populateUpdatedRegion(renderThreadState.updatedRegion)
            strokeState.inProgressStroke.resetUpdatedRegion()
        }
    }

    /** Draw a live stroke. */
    @WorkerThread
    private fun drawStrokeState(strokeState: RenderThreadStrokeState) {
        fillStrokeToViewTransform(strokeState)
        inProgressStrokesRenderHelper.drawInModifiedRegion(
            strokeState.inProgressStroke,
            renderThreadState.strokeToViewTransform,
        )
    }

    /** Calculate and update strokeToViewTransform by combining other transform matrices. */
    @WorkerThread
    private fun fillStrokeToViewTransform(strokeState: RenderThreadStrokeState) {
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

    /** An input event that can go in the (future) event queue to hand off across threads. */
    private sealed interface InputAction

    /** Represents the data passed to [startStroke]. */
    private data class StartAction(
        val strokeInput: StrokeInput,
        val strokeId: InProgressStrokeId,
        val motionEventToStrokeTransform: AndroidMatrix,
        val brush: Brush,
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
        val latencyData: LatencyData?,
    ) : InputAction

    /** Indicates that it's time to call [updateShape] on strokes in [dryingStrokes]. */
    private object UpdateAction : InputAction

    /** Represents the data passed to [cancelStroke]. */
    private data class CancelAction(
        val strokeId: InProgressStrokeId,
        val latencyData: LatencyData
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
     * @param finishedStrokes The finished strokes, which cannot be empty.
     */
    private data class Finished(
        @Size(min = 1) val finishedStrokes: Map<InProgressStrokeId, FinishedStroke>
    ) : ClaimStrokesToHandOffResult {
        init {
            require(finishedStrokes.isNotEmpty())
        }
    }

    /** Holds the state for a given stroke, as needed by the render thread. */
    private data class RenderThreadStrokeState(
        val inProgressStroke: InProgressStroke,
        val strokeToMotionEventTransform: AndroidMatrix,
        val startEventTimeMillis: Long,
    )

    /** Holds the state for a given stroke, as needed by the UI thread. */
    private data class UiStrokeState(
        val motionEventToStrokeTransform: AndroidMatrix,
        val startEventTimeMillis: Long,
        val strokeUnitLengthCm: Float,
    )
}
