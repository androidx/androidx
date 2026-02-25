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

package androidx.xr.arcore.playservices

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorNotTrackingException
import androidx.xr.arcore.runtime.Plane
import androidx.xr.arcore.runtime.Trackable
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import com.google.ar.core.Plane as ARCorePlane
import com.google.ar.core.Trackable as ArCoreTrackable
import com.google.ar.core.exceptions.NotTrackingException

/**
 * Wraps a [com.google.ar.core.Plane] with the [Plane] interface.
 *
 * @property arCorePlane the underlying [ARCorePlane] instance
 * @property resources the [XrResources] instance
 * @property centerPose the [Pose] of the plane's center
 * @property extents the extents of the plane
 * @property label the [Plane.Label] of the plane
 * @property subsumedBy the plane that this plane is subsumed by
 * @property trackingState the [TrackingState] of the plane
 * @property type the [Plane.Type] of the plane
 * @property vertices the vertices of the plane
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCorePlane
internal constructor(internal val _arCorePlane: ARCorePlane, private val resources: XrResources) :
    Plane, Trackable {
    @UnsupportedArCoreCompatApi public fun arCorePlane(): ARCorePlane = _arCorePlane

    /**
     * Creates an anchor on the plane.
     *
     * This method calls the [ARCorePlane.createAnchor] method.
     *
     * @param pose the [Pose] of the anchor
     * @return the created [Anchor]
     */
    override fun createAnchor(pose: Pose): Anchor {
        try {
            return ArCoreAnchor(_arCorePlane.createAnchor(pose.toARCorePose()))
        } catch (e: NotTrackingException) {
            throw AnchorNotTrackingException(e)
        }
    }

    override val centerPose: Pose
        get() = _arCorePlane.centerPose.toRuntimePose()

    override val extents: FloatSize2d
        get() = FloatSize2d(_arCorePlane.extentX, _arCorePlane.extentZ)

    override val label: Plane.Label = Plane.Label.UNKNOWN

    override val subsumedBy: Plane?
        get() {
            val arCoreTrackable = _arCorePlane.subsumedBy as? ArCoreTrackable ?: return null
            return resources.trackables[arCoreTrackable] as? Plane
        }

    override val trackingState: TrackingState
        get() = TrackingState.fromArCoreTrackingState(_arCorePlane.trackingState)

    override val type: Plane.Type
        get() = Plane.Type.fromArCoreType(_arCorePlane.type)

    override val vertices: List<Vector2>
        get() = _arCorePlane.polygon.array().toList().chunked(2, { Vector2(it[0], it[1]) })
}
