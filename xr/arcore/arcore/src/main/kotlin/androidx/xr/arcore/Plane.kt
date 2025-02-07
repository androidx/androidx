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

import androidx.xr.runtime.Session
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
        /** Emits the planes that are currently being tracked in the [session]. */
        @JvmStatic
        public fun subscribe(session: Session): StateFlow<Collection<Plane>> =
            session.state
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

    /**
     * The representation of the current state of a [Plane].
     *
     * @property trackingState whether this plane is being tracked or not.
     * @property label The [Label] associated with the plane.
     * @property centerPose The pose of the center of the detected plane.
     * @property extents The dimensions of the detected plane.
     * @property subsumedBy If this plane has been subsumed, returns the plane this plane was merged
     *   into.
     * @property vertices The 2D vertices of a convex polygon approximating the detected plane.
     */
    public class State(
        public override val trackingState: TrackingState,
        public val label: Label,
        public val centerPose: Pose,
        public val extents: Vector2,
        public val subsumedBy: Plane?,
        public val vertices: List<Vector2>,
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
            @JvmField public val HorizontalUpwardFacing: Type = Type(0)

            /** A horizontal plane facing downward (e.g. a ceiling). */
            @JvmField public val HorizontalDownwardFacing: Type = Type(1)

            /** A vertical plane (e.g. a wall). */
            @JvmField public val Vertical: Type = Type(2)
        }

        public override fun toString(): String =
            when (this) {
                HorizontalUpwardFacing -> "HorizontalUpwardFacing"
                HorizontalDownwardFacing -> "HorizontalDownwardFacing"
                Vertical -> "Vertical"
                else -> "Unknown"
            }
    }

    /** A semantic description of a [Plane]. */
    public class Label private constructor(private val value: Int) {
        public companion object {
            /** The plane represents an unknown type. */
            @JvmField public val Unknown: Label = Label(0)

            /** The plane represents a wall. */
            @JvmField public val Wall: Label = Label(1)

            /** The plane represents a floor. */
            @JvmField public val Floor: Label = Label(2)

            /** The plane represents a ceiling. */
            @JvmField public val Ceiling: Label = Label(3)

            /** The plane represents a table. */
            @JvmField public val Table: Label = Label(4)
        }

        public override fun toString(): String =
            when (this) {
                Wall -> "Wall"
                Floor -> "Floor"
                Ceiling -> "Ceiling"
                Table -> "Table"
                else -> "Unknown"
            }
    }

    private val _state =
        MutableStateFlow(
            State(
                runtimePlane.trackingState,
                labelFromRuntimeType(),
                runtimePlane.centerPose,
                runtimePlane.extents,
                subsumedByFromRuntimePlane(),
                runtimePlane.vertices,
            )
        )
    /** The current state of the [Plane]. */
    public override val state: StateFlow<Plane.State> = _state.asStateFlow()

    /** The [Type] of the [Plane]. */
    public val type: Type
        get() = typeFromRuntimeType()

    override fun createAnchor(pose: Pose): AnchorCreateResult {
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
                trackingState = runtimePlane.trackingState,
                label = labelFromRuntimeType(),
                centerPose = runtimePlane.centerPose,
                extents = runtimePlane.extents,
                subsumedBy = subsumedByFromRuntimePlane(),
                vertices = runtimePlane.vertices,
            )
        )
    }

    private fun typeFromRuntimeType(): Type =
        when (runtimePlane.type) {
            RuntimePlane.Type.HorizontalUpwardFacing -> Type.HorizontalUpwardFacing
            RuntimePlane.Type.HorizontalDownwardFacing -> Type.HorizontalDownwardFacing
            RuntimePlane.Type.Vertical -> Type.Vertical
            else -> Type.HorizontalUpwardFacing
        }

    private fun labelFromRuntimeType(): Label =
        when (runtimePlane.label) {
            RuntimePlane.Label.Unknown -> Label.Unknown
            RuntimePlane.Label.Wall -> Label.Wall
            RuntimePlane.Label.Floor -> Label.Floor
            RuntimePlane.Label.Ceiling -> Label.Ceiling
            RuntimePlane.Label.Table -> Label.Table
            else -> Label.Unknown
        }

    private fun subsumedByFromRuntimePlane(): Plane? =
        runtimePlane.subsumedBy?.let { xrResourceManager.trackablesMap[it] as Plane? }
}
