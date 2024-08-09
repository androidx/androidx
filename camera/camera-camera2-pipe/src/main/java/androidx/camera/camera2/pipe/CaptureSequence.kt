/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.core.Debug

/**
 * An ordered list of [TCaptureRequest] objects, listeners, and associated metadata that will be
 * submitted and captured together when submitted to the camera.
 *
 * A CaptureSequence should be created from a [CaptureSequenceProcessor].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CaptureSequence<out TCaptureRequest> {
    public val cameraId: CameraId
    public val repeating: Boolean
    public val captureRequestList: List<TCaptureRequest>
    public val captureMetadataList: List<RequestMetadata>
    public val listeners: List<Request.Listener>
    public val sequenceListener: CaptureSequenceListener

    /** This value must be set to the return value of [CaptureSequenceProcessor.submit] */
    public var sequenceNumber: Int

    public interface CaptureSequenceListener {
        public fun onCaptureSequenceComplete(captureSequence: CaptureSequence<*>)
    }
}

/** Utility functions for interacting with [CaptureSequence] callbacks and listeners. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object CaptureSequences {
    /**
     * Efficient, inlined utility function for invoking a call on each of the listeners defined on a
     * [CaptureSequence] instance using the provided [RequestMetadata] object.
     */
    public inline fun <T> CaptureSequence<T>.invokeOnRequests(
        crossinline fn: (RequestMetadata, Int, Request.Listener) -> Any
    ) {
        Debug.traceStart { "InvokeInternalListeners" }
        // Always invoke the internal listener first on all of the internal listeners for the
        // entire sequence before invoking the listeners specified in the specific requests
        for (i in captureMetadataList.indices) {
            val request = captureMetadataList[i]
            for (listenerIndex in listeners.indices) {
                fn(request, i, listeners[listenerIndex])
            }
        }

        Debug.traceStop()
        Debug.traceStart { "InvokeRequestListeners" }

        // Invoke the listeners that were defined on the individual requests.
        for (i in captureMetadataList.indices) {
            val request = captureMetadataList[i]
            for (listenerIndex in request.request.listeners.indices) {
                fn(request, i, request.request.listeners[listenerIndex])
            }
        }

        Debug.traceStop()
    }

    /**
     * Efficient, inlined utility function for invoking a call on each of the listeners defined on a
     * [CaptureSequence] instance using the provided [RequestMetadata] object.
     */
    public inline fun <T> CaptureSequence<T>.invokeOnRequest(
        request: RequestMetadata,
        crossinline fn: (Request.Listener) -> Any
    ) {
        Debug.traceStart { "InvokeInternalListeners" }

        // Always invoke the sequence listeners first so that internal state can be updated before
        // specific requests receive the callback.
        for (i in listeners.indices) {
            fn(listeners[i])
        }

        Debug.traceStop()
        Debug.traceStart { "InvokeRequestListeners" }

        // Invoke the listeners that were defined on this request.
        for (i in request.request.listeners.indices) {
            fn(request.request.listeners[i])
        }

        Debug.traceStop()
    }
}
