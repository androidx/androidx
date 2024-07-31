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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CaptureRequest
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.core.Preview
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.streamsharing.StreamSharing
import javax.inject.Inject
import kotlin.collections.removeFirst as removeFirstKt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

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
class UseCaseCameraState
@Inject
constructor(
    useCaseGraphConfig: UseCaseGraphConfig,
    private val threads: UseCaseThreads,
    private val sessionProcessorManager: SessionProcessorManager?,
    private val templateParamsOverride: TemplateParamsOverride,
) {
    private val lock = Any()

    private val cameraGraph = useCaseGraphConfig.graph

    @GuardedBy("lock") private var updateSignal: CompletableDeferred<Unit>? = null

    @GuardedBy("lock") private val submittedRequestCounter = atomic(0)

    data class RequestSignal(val requestNo: Int, val signal: CompletableDeferred<Unit>)

    @GuardedBy("lock") private var updateSignals = ArrayDeque<RequestSignal>()

    @GuardedBy("lock") private var updating = false

    @GuardedBy("lock") private val currentParameters = mutableMapOf<CaptureRequest.Key<*>, Any>()

    @GuardedBy("lock") private val currentInternalParameters = mutableMapOf<Metadata.Key<*>, Any>()

    @GuardedBy("lock") private val currentStreams = mutableSetOf<StreamId>()

    @GuardedBy("lock") private val currentListeners = mutableSetOf<Request.Listener>()

    @GuardedBy("lock") private var currentTemplate: RequestTemplate? = null

    @GuardedBy("lock") private var currentSessionConfig: SessionConfig? = null

    private val requestListener = RequestListener()

    /**
     * Updates the camera state by applying the provided parameters to a repeating request and
     * returns a [Deferred] signal that is completed only when a capture request with equal or
     * larger request number is completed or failed.
     *
     * In case the corresponding capture request of a signal is aborted, it is not completed right
     * then. This is because a quick succession of update requests may lead to the previous request
     * being aborted while the request parameters should still be applied unless it was changed in
     * the new request. If the new request has a value change for some parameter, it is the
     * responsibility of the caller to keep track of that and take necessary action.
     *
     * @return A [Deferred] signal to represent if the update operation has been completed.
     */
    fun updateAsync(
        parameters: Map<CaptureRequest.Key<*>, Any>? = null,
        appendParameters: Boolean = true,
        internalParameters: Map<Metadata.Key<*>, Any>? = null,
        appendInternalParameters: Boolean = true,
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
        listeners: Set<Request.Listener>? = null,
        sessionConfig: SessionConfig? = null,
    ): Deferred<Unit> {
        val result: Deferred<Unit>
        synchronized(lock) {
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
                sessionConfig
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

    fun update(
        parameters: Map<CaptureRequest.Key<*>, Any>? = null,
        appendParameters: Boolean = true,
        internalParameters: Map<Metadata.Key<*>, Any>? = null,
        appendInternalParameters: Boolean = true,
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
        listeners: Set<Request.Listener>? = null
    ) {
        synchronized(lock) {
            // See updateAsync for details.
            updateState(
                parameters,
                appendParameters,
                internalParameters,
                appendInternalParameters,
                streams,
                template,
                listeners
            )
            if (updating) {
                return
            }
            updating = true
        }
        submitLatest()
    }

    fun capture(requests: List<Request>) {
        threads.sequentialScope.launch(start = CoroutineStart.UNDISPATCHED) {
            cameraGraph.acquireSession().use { it.submit(requests) }
        }
    }

    @GuardedBy("lock")
    private inline fun updateState(
        parameters: Map<CaptureRequest.Key<*>, Any>? = null,
        appendParameters: Boolean = true,
        internalParameters: Map<Metadata.Key<*>, Any>? = null,
        appendInternalParameters: Boolean = true,
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
        listeners: Set<Request.Listener>? = null,
        sessionConfig: SessionConfig? = null,
    ) {
        // TODO: Consider if this should detect changes and only invoke an update if state has
        //  actually changed.
        debug {
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
        if (sessionConfig != null) {
            currentSessionConfig = sessionConfig
        }
    }

    /**
     * Tries to invoke [androidx.camera.camera2.pipe.CameraGraph.Session.startRepeating] with
     * current (the most recent) set of values.
     */
    fun tryStartRepeating() = submitLatest()

    private fun submitLatest() {
        if (sessionProcessorManager != null) {
            submitLatestWithSessionProcessor()
            return
        }

        // Update the cameraGraph with the most recent set of values.
        // Since acquireSession is a suspending function, it's possible that subsequent updates
        // can occur while waiting for the acquireSession call to complete. If this happens,
        // updates to the internal state are aggregated together, and the Request is built
        // synchronously with the latest values. The startRepeating/stopRepeating call happens
        // outside of the synchronized block to avoid holding a lock while updating the camera
        // state.
        threads.sequentialScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result: CompletableDeferred<Unit>?
            val request: Request?
            try {
                    cameraGraph.acquireSession()
                } catch (e: CancellationException) {
                    debug(e) { "Cannot acquire session at ${this@UseCaseCameraState}" }
                    null
                }
                .let { session ->
                    synchronized(lock) {
                        request =
                            if (currentStreams.isEmpty() || session == null) {
                                null
                            } else {
                                Request(
                                    template = currentTemplate,
                                    streams = currentStreams.toList(),
                                    parameters =
                                        templateParamsOverride.getOverrideParams(currentTemplate) +
                                            currentParameters.toMap(),
                                    extras =
                                        currentInternalParameters.toMutableMap().also { parameters
                                            ->
                                            parameters[USE_CASE_CAMERA_STATE_CUSTOM_TAG] =
                                                submittedRequestCounter.incrementAndGet()
                                        },
                                    listeners =
                                        currentListeners.toMutableList().also { listeners ->
                                            listeners.add(requestListener)
                                        }
                                )
                            }
                        result = updateSignal
                        updating = false
                        updateSignal = null
                    }
                    session?.use {
                        if (request == null) {
                            it.stopRepeating()
                        } else {
                            result?.let { result ->
                                synchronized(lock) {
                                    updateSignals.add(
                                        RequestSignal(submittedRequestCounter.value, result)
                                    )
                                }
                            }
                            debug { "Update RepeatingRequest: $request" }
                            it.startRepeating(request)
                            // TODO: Invoke update3A only if required e.g. a 3A value has changed
                            it.update3A(request.parameters)
                        }
                    }
                }

            // complete the result instantly only when the request was not submitted
            if (request == null) {
                // Complete the result after the session closes to allow other threads to acquire a
                // lock. This also avoids cases where complete() synchronously invokes expensive
                // calls.
                result?.complete(Unit)
            }
        }
    }

    private fun submitLatestWithSessionProcessor() {
        checkNotNull(sessionProcessorManager)
        synchronized(lock) {
            updating = false
            val signal = updateSignal
            updateSignal = null

            if (currentSessionConfig == null) {
                signal?.complete(Unit)
                return
            }

            // Here we're intentionally building a new SessionConfig. Various request parameters,
            // such as zoom or 3A are directly translated to corresponding CameraPipe types and
            // APIs. As such, we need to build a new, "combined" SessionConfig that has these
            // updated request parameters set. Otherwise, certain settings like zoom would be
            // disregarded.
            SessionConfig.Builder()
                .apply {
                    currentTemplate?.let { setTemplateType(it.value) }
                    setImplementationOptions(
                        Camera2ImplConfig.Builder()
                            .apply {
                                for ((key, value) in currentParameters) {
                                    setCaptureRequestOptionWithType(key, value)
                                }
                            }
                            .build()
                    )
                    currentInternalParameters[CAMERAX_TAG_BUNDLE]?.let {
                        val tagBundleMap = (it as TagBundle).toMap()
                        for ((tag, value) in tagBundleMap) {
                            addTag(tag, value)
                        }
                    }
                }
                .build()
                .also { sessionConfig -> sessionProcessorManager.sessionConfig = sessionConfig }

            if (
                currentSessionConfig!!.repeatingCaptureConfig.surfaces.any {
                    it.containerClass == Preview::class.java ||
                        it.containerClass == StreamSharing::class.java
                }
            ) {
                sessionProcessorManager.startRepeating(
                    currentSessionConfig!!.repeatingCaptureConfig.tagBundle
                )
            } else {
                sessionProcessorManager.stopRepeating()
            }
            signal?.complete(Unit)
        }
    }

    private fun CameraGraph.Session.update3A(parameters: Map<CaptureRequest.Key<*>, Any>?) {
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

        if (aeMode != null || afMode != null || awbMode != null) {
            update3A(aeMode = aeMode, afMode = afMode, awbMode = awbMode)
        }
    }

    private fun Map<CaptureRequest.Key<*>, Any>?.getIntOrNull(key: CaptureRequest.Key<*>): Int? =
        this?.get(key) as? Int

    @Suppress("UNCHECKED_CAST")
    private fun <T> Camera2ImplConfig.Builder.setCaptureRequestOptionWithType(
        key: CaptureRequest.Key<T>,
        value: Any
    ) {
        setCaptureRequestOption(key, value as T)
    }

    inner class RequestListener : Request.Listener {
        override fun onTotalCaptureResult(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            totalCaptureResult: FrameInfo,
        ) {
            super.onTotalCaptureResult(requestMetadata, frameNumber, totalCaptureResult)
            threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                requestMetadata[USE_CASE_CAMERA_STATE_CUSTOM_TAG]?.let { requestNo ->
                    synchronized(lock) { updateSignals.complete(requestNo) }
                }
            }
        }

        override fun onFailed(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            requestFailure: RequestFailure,
        ) {
            @Suppress("DEPRECATION") super.onFailed(requestMetadata, frameNumber, requestFailure)
            completeExceptionally(requestMetadata, requestFailure)
        }

        private fun completeExceptionally(
            requestMetadata: RequestMetadata,
            requestFailure: RequestFailure? = null
        ) {
            threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                requestMetadata[USE_CASE_CAMERA_STATE_CUSTOM_TAG]?.let { requestNo ->
                    synchronized(lock) {
                        updateSignals.completeExceptionally(
                            requestNo,
                            Throwable(
                                "Failed in framework level" +
                                    (requestFailure?.reason?.let {
                                        " with CaptureFailure.reason = $it"
                                    } ?: "")
                            )
                        )
                    }
                }
            }
        }

        private fun ArrayDeque<RequestSignal>.complete(requestNo: Int) {
            while (isNotEmpty() && first().requestNo <= requestNo) {
                first().signal.complete(Unit)
                removeFirstKt()
            }
        }

        private fun ArrayDeque<RequestSignal>.completeExceptionally(
            requestNo: Int,
            throwable: Throwable
        ) {
            while (isNotEmpty() && first().requestNo <= requestNo) {
                first().signal.completeExceptionally(throwable)
                removeFirstKt()
            }
        }
    }
}
