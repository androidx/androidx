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

package androidx.xr.arcore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Anchor as RuntimeAnchor
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.Plane as RuntimePlane
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

/** Describes the system's current best knowledge of a real-world planar surface. */
public class Plane
internal constructor(
    internal val runtimePlane: RuntimePlane,
    private val xrResourceManager: XrResourcesManager,
) : Trackable<Plane.State>, Updatable {

    public companion object {
        /**
         * Emits the planes that are currently being tracked in the [session].
         *
         * Only [Plane]s that are [TrackingState.TRACKING] will be emitted in the [Collection].
         * Instances of the same [Plane] will remain between subsequent emits to the [StateFlow] as
         * long as they remain tracking.
         *
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [Config.PlaneTrackingMode.DISABLED]
         */
        @JvmStatic
        public fun subscribe(session: Session): StateFlow<Collection<Plane>> {
            check(session.config.planeTracking != Config.PlaneTrackingMode.DISABLED) {
                "Config.PlaneTrackingMode is set to DISABLED."
            }

            return session.state
                .transform { state ->
                    state.perceptionState?.let { perceptionState ->
                        emit(perceptionState.trackables.filterIsInstance<Plane>())
                    }
                }
                .stateIn(
                    session.coroutineScope,
                    SharingStarted.Eagerly,
                    session.state.value.perceptionState?.trackables?.filterIsInstance<Plane>()
                        ?: emptyList(),
                )
        }
    }

    /**
     * The representation of the current state of a [Plane]. A [Plane] is represented as a finite
     * polygon with an arbitrary amount of [vertices] around a [centerPose].
     *
     * @property trackingState whether this plane is being tracked or not.
     * @property label The [Label] associated with the plane.
     * @property centerPose The [Pose] of the center of the detected plane's bounding box in the
     *   world coordinate space. The +Y axis relative to the [centerPose] is equivalent to the
     *   normal of the [Plane].
     * @property extents The dimensions of the bounding box of the detected plane.
     * @property vertices The 2D vertices of a convex polygon approximating the detected plane,
     *   relative to its [centerPose] in the X and Z axes.
     * @property subsumedBy If this plane has been subsumed, returns the plane this plane was merged
     *   into. If the subsuming plane is also subsumed by another plane, this plane will continue to
     *   be subsumed by the former.
     */
    public class State
    internal constructor(
        public override val trackingState: TrackingState,
        public val label: Label,
        public val centerPose: Pose,
        public val extents: Vector2,
        public val vertices: List<Vector2>,
        public val subsumedBy: Plane?,
    ) : Trackable.State {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return trackingState == other.trackingState &&
                label == other.label &&
                centerPose == other.centerPose &&
                extents == other.extents &&
                subsumedBy == other.subsumedBy &&
                vertices == other.vertices
        }

        override fun hashCode(): Int {
            var result = trackingState.hashCode()
            result = 31 * result + label.hashCode()
            result = 31 * result + centerPose.hashCode()
            result = 31 * result + extents.hashCode()
            result = 31 * result + subsumedBy.hashCode()
            result = 31 * result + vertices.hashCode()
            return result
        }
    }

    /** A simple summary of the normal vector of a [Plane]. */
    public class Type private constructor(private val value: Int) {
        public companion object {
            /** A horizontal plane facing upward (e.g. floor or tabletop). */
            @JvmField public val HORIZONTAL_UPWARD_FACING: Type = Type(0)

            /** A horizontal plane facing downward (e.g. a ceiling). */
            @JvmField public val HORIZONTAL_DOWNWARD_FACING: Type = Type(1)

            /** A vertical plane (e.g. a wall). */
            @JvmField public val VERTICAL: Type = Type(2)
        }

        public override fun toString(): String =
            when (this) {
                HORIZONTAL_UPWARD_FACING -> "HORIZONTAL_UPWARD_FACING"
                HORIZONTAL_DOWNWARD_FACING -> "HORIZONTAL_DOWNWARD_FACING"
                VERTICAL -> "VERTICAL"
                else -> "UNKNOWN"
            }
    }

    /** A semantic description of a [Plane]. */
    public class Label private constructor(private val value: Int) {
        public companion object {
            /** The plane represents an unknown type. */
            @JvmField public val UNKNOWN: Label = Label(0)

            /** The plane represents a wall. */
            @JvmField public val WALL: Label = Label(1)

            /** The plane represents a floor. */
            @JvmField public val FLOOR: Label = Label(2)

            /** The plane represents a ceiling. */
            @JvmField public val CEILING: Label = Label(3)

            /** The plane represents a table. */
            @JvmField public val TABLE: Label = Label(4)
        }

        public override fun toString(): String =
            when (this) {
                WALL -> "WALL"
                FLOOR -> "FLOOR"
                CEILING -> "CEILING"
                TABLE -> "TABLE"
                else -> "UNKNOWN"
            }
    }

    private val _state =
        MutableStateFlow(
            State(
                runtimePlane.trackingState,
                labelFromRuntimeType(),
                runtimePlane.centerPose,
                runtimePlane.extents,
                runtimePlane.vertices,
                subsumedByFromRuntimePlane(),
            )
        )
    /** The current state of the [Plane]. */
    public override val state: StateFlow<Plane.State> = _state.asStateFlow()

    /** The [Type] of the [Plane]. */
    public val type: Type
        get() = typeFromRuntimeType()

    /**
     * Creates an [Anchor] that is attached to this trackable, using the given initial [pose] in the
     * world coordinate space.
     *
     * @throws [IllegalStateException] if [Session.config] is set to
     *   [Config.PlaneTrackingMode.DISABLED].
     */
    override fun createAnchor(pose: Pose): AnchorCreateResult {
        check(
            xrResourceManager.lifecycleManager.config.planeTracking !=
                Config.PlaneTrackingMode.DISABLED
        ) {
            "Config.PlaneTrackingMode is set to DISABLED."
        }

        val runtimeAnchor: RuntimeAnchor
        try {
            runtimeAnchor = runtimePlane.createAnchor(pose)
        } catch (e: AnchorResourcesExhaustedException) {
            return AnchorCreateResourcesExhausted()
        }
        val anchor = Anchor(runtimeAnchor, xrResourceManager)
        xrResourceManager.addUpdatable(anchor)
        return AnchorCreateSuccess(anchor)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override suspend fun update() {
        _state.emit(
            State(
                trackingState = runtimePlane.trackingState,
                label = labelFromRuntimeType(),
                centerPose = runtimePlane.centerPose,
                extents = runtimePlane.extents,
                vertices = runtimePlane.vertices,
                subsumedBy = subsumedByFromRuntimePlane(),
            )
        )
    }

    private fun typeFromRuntimeType(): Type =
        when (runtimePlane.type) {
            RuntimePlane.Type.HORIZONTAL_UPWARD_FACING -> Type.HORIZONTAL_UPWARD_FACING
            RuntimePlane.Type.HORIZONTAL_DOWNWARD_FACING -> Type.HORIZONTAL_DOWNWARD_FACING
            RuntimePlane.Type.VERTICAL -> Type.VERTICAL
            else -> Type.HORIZONTAL_UPWARD_FACING
        }

    private fun labelFromRuntimeType(): Label =
        when (runtimePlane.label) {
            RuntimePlane.Label.UNKNOWN -> Label.UNKNOWN
            RuntimePlane.Label.WALL -> Label.WALL
            RuntimePlane.Label.FLOOR -> Label.FLOOR
            RuntimePlane.Label.CEILING -> Label.CEILING
            RuntimePlane.Label.TABLE -> Label.TABLE
            else -> Label.UNKNOWN
        }

    private fun subsumedByFromRuntimePlane(): Plane? =
        runtimePlane.subsumedBy?.let { xrResourceManager.trackablesMap[it] as Plane? }
}
