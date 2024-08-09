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

/** Create and submit [CaptureSequence]s to an active camera instance. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CaptureSequenceProcessor<
    out TCaptureRequest,
    TCaptureSequence : CaptureSequence<TCaptureRequest>
> {

    /**
     * Build a [CaptureSequence] instance.
     *
     * @param isRepeating determines if this CaptureSequence should repeat until replaced by another
     *   repeating CaptureSequence, or closed, or stopRepeating is invoked.
     * @param requests the list of [Request] to use when constructing this [CaptureSequence]
     * @param defaultParameters are the parameters to start with when building an individual
     *   [TCaptureRequest] object. Parameters not specified on a [Request] will use these parameters
     *   by default.
     * @param requiredParameters are parameters that will override all [defaultParameters] *and*
     *   parameters that are defined on the [Request].
     * @param listeners are global and internal [Request.Listener]s that should be invoked every
     *   time the listeners on the [Request] are invoked. Since these often track and update
     *   internal state they should be invoked before listeners on the individual [Request].
     * @param sequenceListener is an extra listener that should be invoked whenever a specific
     *   [CaptureSequence] should no longer receive any additional events.
     * @return a [TCaptureSequence] instance that can be used to capture images using the underlying
     *   camera by passing this [submit]. This method will return null if the underlying camera has
     *   been closed or disconnected, and will throw unchecked exceptions if invalid values are
     *   passed to the [build] call.
     */
    public fun build(
        isRepeating: Boolean,
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        listeners: List<Request.Listener>,
        sequenceListener: CaptureSequence.CaptureSequenceListener,
    ): TCaptureSequence?

    /** Issue a previously created [CaptureSequence] to the active camera instance. */
    public fun submit(captureSequence: TCaptureSequence): Int?

    /**
     * Opportunistically abort any ongoing captures by the camera. This may or may not complete
     * quickly depending on the underlying camera.
     */
    public fun abortCaptures()

    /** Opportunistically cancel any currently active repeating [TCaptureSequence]. */
    public fun stopRepeating()

    /**
     * Signal that this [CaptureSequenceProcessor] is no longer in use. Active requests may continue
     * to be processed, and [abortCaptures] and [stopRepeating] may still be invoked.
     */
    public fun close()
}
