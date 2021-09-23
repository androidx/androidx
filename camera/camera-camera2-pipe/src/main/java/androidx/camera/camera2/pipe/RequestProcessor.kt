/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.RequiresApi

/**
 * An instance of a [RequestProcessor] exists for the duration of a CameraCaptureSession and must be
 * created for each new CameraCaptureSession. It is responsible for low level interactions with the
 * CameraCaptureSession and for shimming the interfaces and callbacks to make them easier to work
 * with.
 *
 * There are some important design considerations:
 * - Instances class is not thread safe, and must be synchronized outside of this class.
 * - Implementations can and will assume that calls will not overlap across threads.
 * - Implementations must take special care to reduce the number objects and wrappers that are
 *   created, and to reduce the number of loops and overhead in wrapper objects. Its better to do
 *   work during initialization than during a callback.
 * - Implementations are expected to interact directly, and synchronously, with the Camera.
 * - Callbacks are expected to be invoked at *very* high frequency.
 * - One RequestProcessor instance per CameraCaptureSession
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
interface RequestProcessor {

    /**
     * Set the repeating [Request] with an optional set of parameters and listeners. Parameters are
     * applied, in order, to each request in the list:
     *
     *   [Request.template] -> defaultParameters -> [Request.parameters] -> requiredParameters
     *
     * Parameters where values are set to null will physically remove a particular key from the
     * final map of parameters.
     *
     * @param request the [Request] to submit to the camera.
     * @param defaultParameters will not override parameters specified in the [Request].
     * @param requiredParameters will override parameters specified in the [Request].
     * @param defaultListeners are internal and/or global listeners that should be invoked in
     * addition to listeners that are specified on each [Request]
     * @return false if the repeating request was not successfully updated.
     */
    fun startRepeating(
        request: Request,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        defaultListeners: List<Request.Listener>
    ): Boolean

    /**
     * Stops the current repeating request, but does *not* close the session. The current
     * repeating request can be resumed by invoking [startRepeating] again.
     */
    fun stopRepeating()

    /**
     * Submit a single [Request] with optional sets of parameters and listeners. Parameters are
     * applied, in order, to each request in the list:
     *
     *   [Request.template] -> defaultParameters -> [Request.parameters] -> requiredParameters
     *
     * Parameters where values are set to null will physically remove a particular key from the
     * final map of parameters.
     *
     * @param request the requests to submit to the camera.
     * @param defaultParameters will not override parameters specified in the [Request].
     * @param requiredParameters will override parameters specified in the [Request].
     * @param defaultListeners are internal and/or global listeners that should be invoked in
     * addition to listeners that are specified on each [Request]
     * @return false if this request was not submitted to the camera for any reason.
     */
    fun submit(
        request: Request,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        defaultListeners: List<Request.Listener>
    ): Boolean

    /**
     * Submit a list of [Request]s with optional sets of parameters and listeners. Parameters are
     * applied, in order, to each request in the list:
     *
     *   [Request.template] -> defaultParameters -> [Request.parameters] -> requiredParameters
     *
     * Parameters where values are set to null will physically remove a particular key from the
     * final map of parameters.
     *
     * @param requests the requests to submit to the camera.
     * @param defaultParameters will not override parameters specified in the [Request].
     * @param requiredParameters will override parameters specified in the [Request].
     * @param defaultListeners are internal and/or global listeners that should be invoked in
     * addition to listeners that are specified on each [Request]
     * @return false if these requests were not submitted to the camera for any reason.
     */
    fun submit(
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        defaultListeners: List<Request.Listener>
    ): Boolean

    /**
     * Abort requests that have been submitted but not completed.
     */
    fun abortCaptures()

    /**
     * Puts the RequestProcessor into a closed state where it should immediately reject all
     * incoming requests. This should NOT call stopRepeating() or abortCaptures().
     */
    fun close()
}
