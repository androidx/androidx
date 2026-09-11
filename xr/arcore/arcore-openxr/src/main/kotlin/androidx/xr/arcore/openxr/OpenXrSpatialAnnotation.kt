/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.xr.arcore.runtime.SpatialAnnotation
import androidx.xr.arcore.runtime.SpatialAnnotationId
import androidx.xr.arcore.runtime.SpatialAnnotationQuadAlignment
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quad

/** Wraps a native XrTrackableSpatialAnnotationANDROID with the [SpatialAnnotation] interface. */
internal class OpenXrSpatialAnnotation
internal constructor(
    internal val nativeSpatialAnnotationId: Long,
    override val id: SpatialAnnotationId,
    override val alignment: SpatialAnnotationQuadAlignment? = null,
) : SpatialAnnotation, Updatable {

    override var centerPose: Pose = Pose()
        private set

    override var quad: Quad? = null
        private set

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    override fun update(xrTime: Long) {
        val spatialAnnotationState =
            try {
                nativeGetSpatialAnnotationState(nativeSpatialAnnotationId, xrTime)
            } catch (_: UnsatisfiedLinkError) {
                // Native method is not linked in JVM host unit tests.
                null
            }
        if (spatialAnnotationState == null) {
            trackingState = TrackingState.PAUSED
            return
        }

        trackingState = spatialAnnotationState.trackingState
        spatialAnnotationState.pose?.let { centerPose = it }

        val ul = spatialAnnotationState.upperLeft
        val ur = spatialAnnotationState.upperRight
        val lr = spatialAnnotationState.lowerRight
        val ll = spatialAnnotationState.lowerLeft
        if (ul != null && ur != null && lr != null && ll != null) {
            quad =
                Quad.fromCorners(
                    upperLeft = ul,
                    upperRight = ur,
                    lowerRight = lr,
                    lowerLeft = ll,
                )
        }
    }

    private external fun nativeGetSpatialAnnotationState(
        spatialAnnotationId: Long,
        timestampNs: Long,
    ): SpatialAnnotationState?
}
