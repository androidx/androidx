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

import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.Plane as RuntimePlane
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

/**
 * Describes the system's current best knowledge of a real-world planar surface.
 *
 * @property state the current [State] of the plane
 * @property type the [PlaneType] of the plane
 */
@SuppressWarnings("HiddenSuperclass")
public class Plane
internal constructor(
    internal val runtimePlane: RuntimePlane,
    private val xrResourceManager: XrResourcesManager,
) : Anchorable<Plane.State>, Updatable() {

    public companion object {
        /**
         * Emits the planes that are currently being tracked in the [session].
         *
         * Only [Plane]s that are [androidx.xr.arcore.TrackingState.TRACKING] will be emitted in the
         * [Collection]. Instances of the same [Plane] will remain between subsequent emits to the
         * [StateFlow] as long as they remain tracking.
         *
         * @param session the [Session] to track planes from
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [androidx.xr.runtime.PlaneTrackingMode.DISABLED]
         * @sample androidx.xr.arcore.samples.getPlanes
         */
        @JvmStatic
        public fun subscribe(session: Session): StateFlow<Collection<Plane>> {
            check(session.perceptionRuntime.config.planeTracking != PlaneTrackingMode.DISABLED) {
                "Config.PlaneTrackingMode is set to DISABLED."
            }

            return session.state
                .transform { state ->
                    state.perceptionState?.let { perceptionState ->
                        emit(
                            perceptionState.trackableStates.filterIsInstance<Plane.State>().map {
                                it.owner
                            }
                        )
                    }
                }
                .stateIn(
                    session.coroutineScope,
                    SharingStarted.Eagerly,
                    session.state.value.perceptionState
                        ?.trackableStates
                        ?.filterIsInstance<Plane.State>()
                        ?.map { it.owner } ?: emptyList(),
                )
        }
    }

    /**
     * The representation of the current state of a [Plane]. A [Plane] is represented as a finite
     * polygon with an arbitrary amount of [vertices] around a [centerPose].
     *
     * @property trackingState whether this plane is being tracked or not
     * @property label the [PlaneLabel] associated with the plane
     * @property centerPose the [Pose] of the center of the detected plane's bounding box in the
     *   world coordinate space
     *
     * The +Y axis relative to the [centerPose] is equivalent to the normal of the [Plane].
     *
     * @property extents the dimensions of the bounding box of the detected plane
     * @property vertices the 2D vertices of a convex polygon approximating the detected plane
     * @property subsumedBy if this plane has been subsumed, returns the plane this plane was merged
     *   into
     *
     * If the subsuming plane is also subsumed by another plane, this plane will continue to be
     * subsumed by the former.
     *
     * @property owner self-reference to the object that owns this state.
     */
    public class State
    internal constructor(
        public override val trackingState: TrackingState,
        public val label: PlaneLabel,
        public val centerPose: Pose,
        public val extents: FloatSize2d,
        public val vertices: List<Vector2>,
        public val subsumedBy: Plane?,
        public val owner: Plane,
    ) : Trackable.State {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return trackingState == other.trackingState &&
                label == other.label &&
                centerPose == other.centerPose &&
                extents == other.extents &&
                subsumedBy == other.subsumedBy &&
                vertices == other.vertices &&
                owner == other.owner
        }

        override fun hashCode(): Int {
            var result = trackingState.hashCode()
            result = 31 * result + label.hashCode()
            result = 31 * result + centerPose.hashCode()
            result = 31 * result + extents.hashCode()
            result = 31 * result + subsumedBy.hashCode()
            result = 31 * result + vertices.hashCode()
            result = 31 * result + owner.hashCode()
            return result
        }
    }

    private val _state =
        MutableStateFlow(
            State(
                runtimePlane.trackingState.toTrackingState(),
                labelFromRuntimeType(),
                runtimePlane.centerPose,
                runtimePlane.extents,
                runtimePlane.vertices,
                subsumedByFromRuntimePlane(),
                owner = this,
            )
        )

    public override val state: StateFlow<Plane.State> = _state.asStateFlow()

    public val type: PlaneType
        get() = typeFromRuntimeType()

    /**
     * Creates an [Anchor] that is attached to this trackable, using the given initial [pose] in the
     * world coordinate space.
     *
     * @param pose the initial [Pose] of the [Anchor]
     * @throws [IllegalStateException] if [Session.config] is set to [PlaneTrackingMode.DISABLED]
     */
    override fun createAnchor(pose: Pose): AnchorResult {
        check(
            xrResourceManager.perceptionRuntime.config.planeTracking != PlaneTrackingMode.DISABLED
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

    override suspend fun update() {
        _state.emit(
            State(
                trackingState = runtimePlane.trackingState.toTrackingState(),
                label = labelFromRuntimeType(),
                centerPose = runtimePlane.centerPose,
                extents = runtimePlane.extents,
                vertices = runtimePlane.vertices,
                subsumedBy = subsumedByFromRuntimePlane(),
                owner = this,
            )
        )
    }

    private fun typeFromRuntimeType(): PlaneType =
        when (runtimePlane.type) {
            RuntimePlane.Type.HORIZONTAL_UPWARD_FACING -> PlaneType.HORIZONTAL_UPWARD_FACING
            RuntimePlane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneType.HORIZONTAL_DOWNWARD_FACING
            RuntimePlane.Type.VERTICAL -> PlaneType.VERTICAL
            else -> PlaneType.HORIZONTAL_UPWARD_FACING
        }

    private fun labelFromRuntimeType(): PlaneLabel =
        when (runtimePlane.label) {
            RuntimePlane.Label.UNKNOWN -> PlaneLabel.UNKNOWN
            RuntimePlane.Label.WALL -> PlaneLabel.WALL
            RuntimePlane.Label.FLOOR -> PlaneLabel.FLOOR
            RuntimePlane.Label.CEILING -> PlaneLabel.CEILING
            RuntimePlane.Label.TABLE -> PlaneLabel.TABLE
            else -> PlaneLabel.UNKNOWN
        }

    private fun subsumedByFromRuntimePlane(): Plane? =
        runtimePlane.subsumedBy?.let { xrResourceManager.trackablesMap[it] as Plane? }
}
