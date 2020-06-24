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

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.pipe.Request

/**
 * An instance of a RequestProcessor exists for the duration of a CameraCaptureSession and must be
 * created for each new CameraCaptureSession. It is responsible for low level interactions with the
 * CameraCaptureSession and for shimming the interfaces and callbacks to make them easier to work
 * with. Unlike the CameraCaptureSessionProxy interface the RequestProcessor has more liberty to
 * change the standard Camera2 API contract to make it easier to work with.
 *
 * There are some important design considerations:
 * - Instances class is not thread safe, although the companion object has some counters that are
 *   global and *are* thread safe.
 * - Special care is taken to reduce the number objects and wrappers that are created, and to reduce
 *   the number of loops and overhead in wrapper objects.
 * - Callbacks are expected to be invoked at very high frequency.
 * - One RequestProcessor instance per CameraCaptureSession
 */
interface RequestProcessor {

    /**
     * Submit a single [Request] with an optional set of extra parameters.
     *
     * @param request the request to submit to the camera.
     * @param extraRequestParameters extra parameters to apply to the request.
     * @param requireSurfacesForAllStreams if this flag is defined then this method will only submit
     *   the request if all streamIds can be mapped to valid surfaces. At least one surface is
     *   always required. This is useful if (for example) someone needs to quickly submit a
     *   request with a specific trigger or mode key but does not care about modifying the list of
     *   current surfaces.
     * @return false if this request failed to be submitted. If this method returns false, none of
     *   the callbacks on the Request(s) will be invoked.
     */
    fun submit(
        request: Request,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean

    /**
     * Submit a list of [Request]s with an optional set of extra parameters.
     *
     * @param requests the requests to submit to the camera.
     * @param extraRequestParameters extra parameters to apply to the request.
     * @param requireSurfacesForAllStreams if this flag is defined then this method will only submit
     *   the request if all streamIds can be mapped to valid surfaces. At least one surface is
     *   always required. This is useful if (for example) someone needs to quickly submit a
     *   request with a specific trigger or mode key but does not care about modifying the list of
     *   current surfaces.
     * @return false if this request failed to be submitted. If this method returns false, none of
     *   the callbacks on the Request(s) will be invoked.
     */
    fun submit(
        requests: List<Request>,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean

    /**
     * Set the repeating [Request] with an optional set of extra parameters.
     *
     * The current repeating request may not be executed at all, or it may be executed multiple
     * times. The repeating request is used as the base request for all 3A interactions which may
     * cause the request to be used to generate multiple [CaptureRequest]s to the camera.
     *
     * @param request the requests to set as the repeating request.
     * @param extraRequestParameters extra parameters to apply to the request.
     * @param requireSurfacesForAllStreams if this flag is defined then this method will only submit
     *   the request if all streamIds can be mapped to valid surfaces. At least one surface is
     *   always required. This is useful if (for example) someone needs to quickly submit a
     *   request with a specific trigger or mode key but does not care about modifying the list of
     *   current surfaces.
     * @return false if this request failed to be submitted. If this method returns false, none of
     *   the callbacks on the Request(s) will be invoked.
     */
    fun setRepeating(
        request: Request,
        extraRequestParameters: Map<CaptureRequest.Key<*>, Any>,
        requireSurfacesForAllStreams: Boolean
    ): Boolean

    /**
     * Abort requests that have been submitted but not completed.
     */
    fun abort()

    /**
     * Puts the RequestProcessor into a closed state where it will reject all incoming requests, but
     * does not actively stop repeating requests or abort pending captures.
     */
    fun disconnect()

    /**
     * Puts the RequestProcessor into a closed state where it will reject all incoming requests and
     * then actively stops the current repeating request.
     */
    fun stop()
}
