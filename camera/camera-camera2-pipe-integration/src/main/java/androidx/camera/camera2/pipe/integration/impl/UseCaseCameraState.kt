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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This object keeps track of the state of the current [UseCaseCamera].
 *
 * Updates to the camera from this class are batched together. That is, if multiple updates
 * happen while some other system is holding the cameraGraph session, those updates will be
 * aggregated together and applied when the session becomes available. This also serves as a form
 * of primitive rate limiting that ensures that updates arriving too quickly are only sent to the
 * underlying camera graph as fast as the camera is capable of consuming them.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@UseCaseCameraScope
class UseCaseCameraState @Inject constructor(
    private val cameraGraph: CameraGraph,
    private val threads: UseCaseThreads
) {
    private val lock = Any()

    @GuardedBy("lock")
    private var updateSignal: CompletableDeferred<Unit>? = null

    @GuardedBy("lock")
    private var updating = false

    @GuardedBy("lock")
    private val currentParameters = mutableMapOf<CaptureRequest.Key<*>, Any>()

    @GuardedBy("lock")
    private val currentInternalParameters = mutableMapOf<Metadata.Key<*>, Any>()

    @GuardedBy("lock")
    private val currentStreams = mutableSetOf<StreamId>()

    @GuardedBy("lock")
    private val currentListeners = mutableSetOf<Request.Listener>()

    @GuardedBy("lock")
    private var currentTemplate: RequestTemplate? = null

    fun updateAsync(
        parameters: Map<CaptureRequest.Key<*>, Any>? = null,
        appendParameters: Boolean = true,
        internalParameters: Map<Metadata.Key<*>, Any>? = null,
        appendInternalParameters: Boolean = true,
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
        listeners: Set<Request.Listener>? = null
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
                parameters, appendParameters, internalParameters,
                appendInternalParameters, streams, template,
                listeners
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
                parameters, appendParameters, internalParameters,
                appendInternalParameters, streams, template,
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
        threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            cameraGraph.acquireSession().use {
                it.submit(requests)
            }
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
        listeners: Set<Request.Listener>? = null
    ) {
        // TODO: Consider if this should detect changes and only invoke an update if state has
        //  actually changed.

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun submitLatest() {
        // Update the cameraGraph with the most recent set of values.
        // Since acquireSession is a suspending function, it's possible that subsequent updates
        // can occur while waiting for the acquireSession call to complete. If this happens,
        // updates to the internal state are aggregated together, and the Request is built
        // synchronously with the latest values. The startRepeating/stopRepeating call happens
        // outside of the synchronized block to avoid holding a lock while updating the camera
        // state.

        threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result: CompletableDeferred<Unit>?
            cameraGraph.acquireSession().use {
                val request: Request?
                synchronized(lock) {
                    request = if (currentStreams.isEmpty()) {
                        null
                    } else {
                        Request(
                            template = currentTemplate,
                            streams = currentStreams.toList(),
                            parameters = currentParameters.toMap(),
                            extras = currentInternalParameters.toMap(),
                            listeners = currentListeners.toList()
                        )
                    }
                    result = updateSignal
                    updateSignal = null
                    updating = false
                }

                if (request == null) {
                    it.stopRepeating()
                } else {
                    it.startRepeating(request)
                }
            }

            // Complete the result after the session closes to allow other threads to acquire a
            // lock. This also avoids cases where complete() synchronously invokes expensive calls.
            result?.complete(Unit)
        }
    }
}