/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.Face
import androidx.xr.arcore.runtime.Mesh
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose

/** Wraps the native XrFaceStateANDROID with the [androidx.xr.arcore.runtime.Face] interface. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrFace : Updatable, Face {

    public override fun createAnchor(pose: Pose): Anchor = throw UnsupportedOperationException()

    public override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    public override var blendShapeValues: FloatArray = FloatArray(XR_FACE_PARAMETER_COUNT_ANDROID)
        private set

    public override var confidenceValues: FloatArray =
        FloatArray(XR_FACE_REGION_CONFIDENCE_COUNT_ANDROID)
        private set

    public override val centerPose: Pose? = null

    public override val mesh: Mesh? = null

    public override val noseTipPose: Pose? = null

    public override val foreheadLeftPose: Pose? = null

    public override val foreheadRightPose: Pose? = null

    public override var isValid: Boolean = false

    /** Updatable */
    override fun update(xrTime: Long) {
        val faceState = nativeGetFaceState(xrTime)
        if (faceState == null) {
            trackingState = TrackingState.PAUSED
            isValid = false
            return
        }
        trackingState = faceState.trackingState

        if (trackingState == TrackingState.TRACKING) {
            isValid = faceState.isValid
            blendShapeValues = faceState.parameters
            confidenceValues = faceState.regionConfidences
        }
    }

    /** Native method */
    private external fun nativeGetFaceState(timestampNs: Long): FaceState?

    /** Holds OpenXR constants for reference */
    internal companion object {
        internal const val XR_FACE_PARAMETER_COUNT_ANDROID: Int = 68
        internal const val XR_FACE_REGION_CONFIDENCE_COUNT_ANDROID: Int = 3
    }
}
