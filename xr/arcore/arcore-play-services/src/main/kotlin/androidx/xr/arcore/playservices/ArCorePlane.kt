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
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.internal.AnchorNotTrackingException
import androidx.xr.runtime.internal.Plane
import androidx.xr.runtime.internal.Trackable
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import com.google.ar.core.Plane as ARCorePlane
import com.google.ar.core.Trackable as ArCoreTrackable
import com.google.ar.core.exceptions.NotTrackingException

/**
 * Wraps the [ARCorePlane] with an implementation of the [Plane] interface.
 *
 * @property arCorePlane The underlying [ARCorePlane] instance.
 * @property resources The [XrResources] instance.
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
     * @param pose The pose of the anchor.
     * @return The created anchor.
     */
    override fun createAnchor(pose: Pose): Anchor {
        try {
            return ArCoreAnchor(_arCorePlane.createAnchor(pose.toARCorePose()))
        } catch (e: NotTrackingException) {
            throw AnchorNotTrackingException(e)
        }
    }

    /**
     * The pose of the plane's center.
     *
     * This property gets the pose from the underlying [ARCorePlane] instance, and converts it to a
     * [Pose].
     *
     * @return The pose of the plane's center.
     */
    override val centerPose: Pose
        get() = _arCorePlane.centerPose.toRuntimePose()

    /**
     * The extents of the plane.
     *
     * This property gets the extents from the underlying [ARCorePlane] instance, and converts it to
     * a [Vector2].
     *
     * @return The extents of the plane.
     */
    override val extents: Vector2
        get() = Vector2(_arCorePlane.extentX, _arCorePlane.extentZ)

    /**
     * ARCore 1.x does not support plane labels; this property is always [UNKNOWN].
     *
     * @return [Plane.Label.UNKNOWN]
     */
    override val label: Plane.Label = Plane.Label.UNKNOWN

    /**
     * The plane that this plane is subsumed by.
     *
     * If this plane has no subsuming plane, this property is null.
     *
     * @return The plane that this plane is subsumed by.
     */
    override val subsumedBy: Plane?
        get() {
            val arCoreTrackable = _arCorePlane.subsumedBy as? ArCoreTrackable ?: return null
            return resources.trackables[arCoreTrackable] as? Plane
        }

    /**
     * The tracking state of the plane.
     *
     * This property gets the tracking state from the underlying [ARCorePlane] instance, and
     * converts it to a [TrackingState].
     *
     * @return The tracking state of the plane.
     */
    override val trackingState: TrackingState
        get() = TrackingState.fromArCoreTrackingState(_arCorePlane.trackingState)

    /**
     * The type of the plane.
     *
     * This property gets the type from the underlying [ARCorePlane] instance, and converts it to a
     * [Plane.Type].
     *
     * @return The type of the plane.
     */
    override val type: Plane.Type
        get() = Plane.Type.fromArCoreType(_arCorePlane.type)

    /**
     * The vertices of the plane.
     *
     * This property gets the vertices from the underlying [ARCorePlane] instance, and converts it
     * to a [List] of [Vector2].
     *
     * @return The vertices of the plane.
     */
    override val vertices: List<Vector2>
        get() = _arCorePlane.polygon.array().toList().chunked(2, { Vector2(it[0], it[1]) })
}
