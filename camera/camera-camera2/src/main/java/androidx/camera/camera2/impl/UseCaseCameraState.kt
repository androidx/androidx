/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.camera.camera2.impl

import android.hardware.camera2.CaptureRequest
import androidx.annotation.GuardedBy
import androidx.camera.camera2.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.config.UseCaseCameraContext
import androidx.camera.camera2.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.Metadata
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * This object keeps track of the state of the current [UseCaseCamera].
 *
 * Updates to the camera from this class are batched together. That is, if multiple updates happen
 * while some other system is holding the cameraGraph session, those updates will be aggregated
 * together and applied when the session becomes available. This also serves as a form of primitive
 * rate limiting that ensures that updates arriving too quickly are only sent to the underlying
 * camera graph as fast as the camera is capable of consuming them.
 */
@UseCaseCameraScope
public class UseCaseCameraState
@Inject
constructor(
    private val useCaseCameraContext: UseCaseCameraContext,
    private val templateParamsOverride: TemplateParamsOverride,
) {
    private val lock = Any()

    @GuardedBy("lock") private var updateSignal: CompletableDeferred<Unit>? = null

    @GuardedBy("lock") private val submittedRequestCounter = atomic(0)

    public data class RequestSignal(val requestNo: Int, val signal: CompletableDeferred<Unit>)

    @GuardedBy("lock") private var updateSignals = ArrayDeque<RequestSignal>()

    @GuardedBy("lock") private var updating = false

    @GuardedBy("lock") private var closed = false

    @GuardedBy("lock") private val currentParameters = mutableMapOf<CaptureRequest.Key<*>, Any>()

    @GuardedBy("lock") private val currentInternalParameters = mutableMapOf<Metadata.Key<*>, Any>()

    @GuardedBy("lock") private val currentStreams = mutableSetOf<StreamId>()

    @GuardedBy("lock") private val currentListeners = mutableSetOf<Request.Listener>()

    @GuardedBy("lock") private var currentTemplate: RequestTemplate? = null

    @GuardedBy("lock") private var lastAeMode: AeMode? = null
    @GuardedBy("lock") private var lastAfMode: AfMode? = null
    @GuardedBy("lock") private var lastAwbMode: AwbMode? = null

    private val requestListener = RequestListener()
    private val pendingSignalCount = atomic(0)

    /**
     * Updates the camera state by applying the provided parameters to a repeating request and
     * returns a [Deferred] signal that is completed only when a capture request with equal or
     * larger request number is completed, when repeating is stopped because no streams remain, or
     * when the [CameraGraph.Session] can no longer be acquired.
     *
     * In case the corresponding capture request of a signal is aborted or a single repeating frame
     * fails, it is not completed right then. A quick succession of update requests may lead to the
     * previous request being aborted, and a transient frame drop on a repeating request does not
     * stop subsequent frames from streaming with the applied parameters. The signal is then
     * completed when a newer request is completed or repeating is stopped, or cancelled if this
     * [UseCaseCameraState] is closed. If a newer request has a value change for some parameter, it
     * is the responsibility of the caller to keep track of that and take necessary action.
     *
     * @return A [Deferred] signal to represent if the update operation has been completed.
     */
    public suspend fun updateAsync(
        parameters: Map<CaptureRequest.Key<*>, Any>? = null,
        appendParameters: Boolean = true,
        internalParameters: Map<Metadata.Key<*>, Any>? = null,
        appendInternalParameters: Boolean = true,
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
        listeners: Set<Request.Listener>? = null,
    ): Deferred<Unit> {
        val result: Deferred<Unit>
        synchronized(lock) {
            if (closed) {
                return CompletableDeferred<Unit>().apply {
                    completeExceptionally(CancellationException("UseCaseCameraState closed"))
                }
            }
            // This block does several things while locked, and is paired with another
            // synchronized(lock) section in the submitLatest() method below that prevents these
            // two blocks from ever executing at the same time, even if invoked by multiple
            // threads.
            // 1) Update the internal state (locked)
            // 2) Since a prior update may have happened that didn't need a completion signal,
            //    it is possible that updateSignal is null. Regardless of the need to resubmit or
            //    not, the updateSignal must have a value to be returned.
            // 3) If an update is already dispatched, return existing update signal. This
            //    updateSignal may be the value from #2 (this is fine).
            // 4) If we get this far, we need to dispatch an update. Mark this as updating, and
            //    exit the locked section.
            // 5) If updating, invoke submit without holding the lock.

            updateState(
                parameters,
                appendParameters,
                internalParameters,
                appendInternalParameters,
                streams,
                template,
                listeners,
            )

            if (updateSignal == null) {
                updateSignal = CompletableDeferred()
            }
            if (updating) {
                return updateSignal!!
            }

            // Fall through to submit if there is no pending update.
            updating = true
            result = updateSignal!!
        }

        submitLatest()
        return result
    }

    @GuardedBy("lock")
    private inline fun updateState(
        parameters: Map<CaptureRequest.Key<*>, Any>?,
        appendParameters: Boolean,
        internalParameters: Map<Metadata.Key<*>, Any>?,
        appendInternalParameters: Boolean,
        streams: Set<StreamId>?,
        template: RequestTemplate?,
        listeners: Set<Request.Listener>?,
    ) {
        // TODO: Consider if this should detect changes and only invoke an update if state has
        //  actually changed.
        Camera2Logger.debug {
            "UseCaseCameraState#updateState: parameters = $parameters, internalParameters = " +
                "$internalParameters, streams = $streams, template = $template"
        }

        if (parameters != null) {
            if (!appendParameters) {
                currentParameters.clear()
            }
            currentParameters.putAll(parameters)
        }
        if (internalParameters != null) {
            if (!appendInternalParameters) {
                currentInternalParameters.clear()
            }
            currentInternalParameters.putAll(internalParameters)
        }
        if (streams != null) {
            currentStreams.clear()
            currentStreams.addAll(streams)
        }
        if (template != null) {
            currentTemplate = template
        }
        if (listeners != null) {
            currentListeners.clear()
            currentListeners.addAll(listeners)
        }
    }

    /**
     * Tries to invoke [androidx.camera.camera2.pipe.CameraGraph.Session.startRepeating] with
     * current (the most recent) set of values.
     */
    public suspend fun tryStartRepeating(): Unit = submitLatest()

    private suspend fun submitLatest() {
        // Update the cameraGraph with the most recent set of values.
        // Since acquireSession is a suspending function, it's possible that subsequent updates
        // can occur while waiting for the acquireSession call to complete. If this happens,
        // updates to the internal state are aggregated together, and the Request is built
        // synchronously with the latest values. The startRepeating/stopRepeating call happens
        // outside of the synchronized block to avoid holding a lock while updating the camera
        // state.
        var signalsToComplete: List<CompletableDeferred<Unit>>? = null

        try {
            useCaseCameraContext.useGraphSession { session ->
                val request: Request?

                // Synchronize state reads, signal enqueue/drain, and signal resets atomically
                synchronized(lock) {
                    // close() already drained and cancelled every signal; skip stale submits.
                    if (closed) {
                        return@useGraphSession
                    }
                    if (currentStreams.isEmpty()) {
                        request = null
                        signalsToComplete = takeAllSignalsLocked()
                    } else {
                        val requestNo = submittedRequestCounter.incrementAndGet()
                        request =
                            Request(
                                template = currentTemplate,
                                streams = currentStreams.toList(),
                                parameters =
                                    templateParamsOverride.getOverrideParams(currentTemplate) +
                                        currentParameters.toMap(),
                                extras =
                                    currentInternalParameters.toMutableMap().also { parameters ->
                                        parameters[USE_CASE_CAMERA_STATE_CUSTOM_TAG] = requestNo
                                    },
                                listeners =
                                    currentListeners.toMutableList().also { listeners ->
                                        listeners.add(requestListener)
                                    },
                            )
                        updateSignal?.let { res ->
                            updateSignals.add(RequestSignal(requestNo, res))
                            pendingSignalCount.incrementAndGet()
                        }
                    }
                    updating = false
                    updateSignal = null
                }

                if (request == null) {
                    session.stopRepeating()
                } else {
                    Camera2Logger.debug { "Update RepeatingRequest: $request" }
                    session.startRepeating(request)
                    session.update3A(request.parameters)
                }
            }
        } catch (e: CancellationException) {
            Camera2Logger.debug(e) { "Cannot acquire session at ${this@UseCaseCameraState}" }
            synchronized(lock) {
                updating = false
                signalsToComplete = (signalsToComplete ?: emptyList()) + takeAllSignalsLocked()
            }
        }

        // Complete the result after the session closes to allow other threads to
        // acquire a lock. This also avoids cases where complete() synchronously
        // invokes expensive calls.
        signalsToComplete?.forEach { it.complete(Unit) }
    }

    public fun close() {
        val signalsToCancel =
            synchronized(lock) {
                closed = true
                updating = false
                takeAllSignalsLocked()
            }
        signalsToCancel.forEach {
            it.completeExceptionally(CancellationException("UseCaseCameraState closed"))
        }
    }

    @GuardedBy("lock")
    private fun takeAllSignalsLocked(): List<CompletableDeferred<Unit>> =
        drainUpdateSignalsLocked() + listOfNotNull(updateSignal).also { updateSignal = null }

    @GuardedBy("lock")
    private fun drainUpdateSignalsLocked(): List<CompletableDeferred<Unit>> = buildList {
        while (updateSignals.isNotEmpty()) {
            add(updateSignals.removeFirst().signal)
            pendingSignalCount.decrementAndGet()
        }
    }

    private fun CameraGraph.Session.update3A(parameters: Map<CaptureRequest.Key<*>, Any?>?) {
        val aeMode =
            parameters.getIntOrNull(CaptureRequest.CONTROL_AE_MODE)?.let {
                AeMode.fromIntOrNull(it)
            }
        val afMode =
            parameters.getIntOrNull(CaptureRequest.CONTROL_AF_MODE)?.let {
                AfMode.fromIntOrNull(it)
            }
        val awbMode =
            parameters.getIntOrNull(CaptureRequest.CONTROL_AWB_MODE)?.let {
                AwbMode.fromIntOrNull(it)
            }

        val aeChanged = aeMode != null && aeMode != lastAeMode
        val afChanged = afMode != null && afMode != lastAfMode
        val awbChanged = awbMode != null && awbMode != lastAwbMode

        if (aeChanged || afChanged || awbChanged) {
            Camera2Logger.debug {
                "UseCaseCameraState: Updating 3A modes: " +
                    "AE($aeMode, changed=$aeChanged), " +
                    "AF($afMode, changed=$afChanged), " +
                    "AWB($awbMode, changed=$awbChanged)"
            }

            // Dispatch the update. The underlying implementation handles nulls.
            update3A(aeMode = aeMode, afMode = afMode, awbMode = awbMode)

            // Update the cache *only* for the values that were non-null.
            if (aeMode != null) lastAeMode = aeMode
            if (afMode != null) lastAfMode = afMode
            if (awbMode != null) lastAwbMode = awbMode
        }
    }

    private fun Map<CaptureRequest.Key<*>, Any?>?.getIntOrNull(key: CaptureRequest.Key<*>): Int? =
        this?.get(key) as? Int

    public inner class RequestListener : Request.Listener {
        override fun onTotalCaptureResult(
            requestMetadata: RequestMetadata,
            frameNumber: CameraFrameNumber,
            totalCaptureResult: FrameInfo,
        ) {
            if (pendingSignalCount.value == 0) {
                return
            }
            requestMetadata[USE_CASE_CAMERA_STATE_CUSTOM_TAG]?.let { requestNo ->
                val completedSignals = synchronized(lock) { drainCompletedSignalsLocked(requestNo) }
                completedSignals.forEach { it.complete(Unit) }
            }
        }

        override fun onFailed(
            requestMetadata: RequestMetadata,
            frameNumber: CameraFrameNumber,
            requestFailure: RequestFailure,
        ) {
            // A failed frame on a repeating request is transient: the framework drops only this
            // frame (CaptureFailure.REASON_ERROR / REASON_FLUSHED) and continues streaming the
            // repeating request. Keep pending signals until a frame with an equal or larger
            // request number completes in onTotalCaptureResult, or until close() cancels them.
            Camera2Logger.debug {
                "UseCaseCameraState: frame ${frameNumber.value} failed with " +
                    "CaptureFailure.reason = ${requestFailure.reason}, " +
                    "waiting for a subsequent frame"
            }
        }

        @GuardedBy("lock")
        private fun drainCompletedSignalsLocked(requestNo: Int): List<CompletableDeferred<Unit>> =
            buildList {
                while (updateSignals.isNotEmpty() && updateSignals.first().requestNo <= requestNo) {
                    add(updateSignals.removeFirst().signal)
                    pendingSignalCount.decrementAndGet()
                }
            }
    }
}
