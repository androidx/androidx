/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.openxr

import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.Plane
import androidx.xr.runtime.internal.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

/** Wraps the native [XrTrackableANDROID] with the [Plane] interface. */
public class OpenXrPlane
internal constructor(
    internal val planeId: Long,
    override val type: Plane.Type,
    internal val timeSource: OpenXrTimeSource,
    private val xrResources: XrResources,
) : Plane, Updatable {
    override var label: Plane.Label = Plane.Label.Unknown
        private set

    override var centerPose: Pose = Pose()
        private set

    override var vertices: List<Vector2> = emptyList()
        private set

    override var extents: Vector2 = Vector2.Zero
        private set

    override var subsumedBy: Plane? = null
        private set

    override var trackingState: TrackingState = TrackingState.Paused
        private set

    override fun createAnchor(pose: Pose): Anchor {
        val xrTime = timeSource.getXrTime(timeSource.markNow())
        val anchorNativePointer = nativeCreateAnchorForPlane(planeId, pose, xrTime)
        checkNativeAnchorIsValid(anchorNativePointer)
        val anchor: Anchor = OpenXrAnchor(anchorNativePointer, xrResources)
        xrResources.addUpdatable(anchor as Updatable)
        return anchor
    }

    override fun update(xrTime: Long) {
        val planeState: PlaneState =
            nativeGetPlaneState(planeId, xrTime)
                ?: throw IllegalStateException("Could latest plane state. Is the plane ID valid?")
        label = planeState.label
        trackingState = planeState.trackingState
        centerPose = planeState.centerPose
        extents = planeState.extents
        vertices = planeState.vertices.toList()

        if (planeState.subsumedByPlaneId != 0L) {
            subsumedBy = xrResources.trackablesMap[planeState.subsumedByPlaneId] as OpenXrPlane?
        }
    }

    private fun checkNativeAnchorIsValid(nativeAnchor: Long) {
        when (nativeAnchor) {
            -2L -> throw IllegalStateException("Failed to create anchor.") // kErrorRuntimeFailure
            -10L -> throw AnchorResourcesExhaustedException() // kErrorLimitReached
        }
    }

    private external fun nativeGetPlaneState(planeId: Long, timestampNs: Long): PlaneState?

    private external fun nativeCreateAnchorForPlane(
        planeId: Long,
        pose: Pose,
        timeStampNs: Long,
    ): Long
}

/** Create a [Plane.Type] from an integer value corresponding to an [XrPlaneTypeANDROID]. */
internal fun Plane.Type.Companion.fromOpenXrType(type: Int): Plane.Type =
    when (type) {
        0 -> Plane.Type.HorizontalDownwardFacing // XR_PLANE_TYPE_HORIZONTAL_DOWNWARD_FACING_ANDROID
        1 -> Plane.Type.HorizontalUpwardFacing // XR_PLANE_TYPE_HORIZONTAL_UPWARD_FACING_ANDROID
        2 -> Plane.Type.Vertical // XR_PLANE_TYPE_VERTICAL_ANDROID
        else -> {
            throw IllegalArgumentException("Invalid plane type.")
        }
    }

/** Create a [Plane.Label] from an integer value corresponding to an [XrPlaneLabelANDROID]. */
internal fun Plane.Label.Companion.fromOpenXrLabel(label: Int): Plane.Label =
    when (label) {
        0 -> Plane.Label.Unknown // XR_PLANE_LABEL_UNKNOWN_ANDROID
        1 -> Plane.Label.Wall // XR_PLANE_LABEL_WALL_ANDROID
        2 -> Plane.Label.Floor // XR_PLANE_LABEL_FLOOR_ANDROID
        3 -> Plane.Label.Ceiling // XR_PLANE_LABEL_CEILING_ANDROID
        4 -> Plane.Label.Table // XR_PLANE_LABEL_TABLE_ANDROID
        else -> {
            throw IllegalArgumentException("Invalid plane label.")
        }
    }
