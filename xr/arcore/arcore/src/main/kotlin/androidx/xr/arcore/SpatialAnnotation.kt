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

package androidx.xr.arcore

import androidx.xr.arcore.runtime.SpatialAnnotation as RuntimeSpatialAnnotation
import androidx.xr.arcore.runtime.SpatialAnnotationId as RuntimeSpatialAnnotationId
import androidx.xr.arcore.runtime.SpatialAnnotationImageFormat as RuntimeSpatialAnnotationImageFormat
import androidx.xr.arcore.runtime.SpatialAnnotationQuadAlignment as RuntimeSpatialAnnotationQuadAlignment
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SpatialAnnotationTrackingMode
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

/**
 * Base class for all tracked Spatial Annotations (Quads, Points, Masks, etc.).
 *
 * @property id the caller-defined unique [SpatialAnnotationId] associated with this annotation
 * @property quadAlignment the runtime alignment configuration bound to this [SpatialAnnotation], it
 *   will only have a value when the spatial tracking mode is set to quad
 */
@SuppressWarnings("HiddenSuperclass")
@ExperimentalSpatialAnnotationsApi
public class SpatialAnnotation
internal constructor(internal val runtimeSpatialAnnotation: RuntimeSpatialAnnotation) :
    Trackable<SpatialAnnotation.State>, Updatable() {

    public val id: SpatialAnnotationId =
        SpatialAnnotationId.fromString(runtimeSpatialAnnotation.id.toString())

    public val quadAlignment: SpatialAnnotationQuadAlignment? =
        when (runtimeSpatialAnnotation.alignment?.value) {
            0 -> SpatialAnnotationQuadAlignment.SCREEN
            1 -> SpatialAnnotationQuadAlignment.OBJECT
            else -> null
        }

    public companion object {

        /**
         * Emits the Spatial Annotations that are currently being tracked in the [session].
         *
         * Only instances of [SpatialAnnotation] that are
         * [androidx.xr.arcore.TrackingState.TRACKING] will be emitted in the [Collection].
         * Instances of the same [SpatialAnnotation] will remain between subsequent emits to the
         * [StateFlow] as long as they remain tracking.
         *
         * @param session the [Session] to subscribe to
         * @return a [StateFlow] that emits a collection of tracked annotations
         * @throws [IllegalStateException] if [SpatialAnnotationTrackingMode] is set to
         *   [SpatialAnnotationTrackingMode.DISABLED]
         */
        @JvmStatic
        public fun subscribe(session: Session): StateFlow<List<SpatialAnnotation>> {
            check(
                session.perceptionRuntime.config.getSpatialAnnotationTracking() !=
                    SpatialAnnotationTrackingMode.DISABLED
            ) {
                "Config.SpatialAnnotationTrackingMode is set to DISABLED."
            }

            return session.state
                .transform { state ->
                    state.perceptionState?.let { perceptionState ->
                        emit(
                            perceptionState.trackableStates
                                .filterIsInstance<SpatialAnnotation.State>()
                                .map { it.owner }
                        )
                    }
                }
                .stateIn(
                    session.coroutineScope,
                    SharingStarted.WhileSubscribed(),
                    session.state.value.perceptionState
                        ?.trackableStates
                        ?.filterIsInstance<SpatialAnnotation.State>()
                        ?.map { it.owner } ?: emptyList(),
                )
        }

        /**
         * Starts tracking one or more Spatial Annotations in 3D physical space.
         *
         * @param session the active JXR session
         * @param options configuration generated via [SpatialAnnotationTrackingOptions.Builder]
         */
        @JvmStatic
        public fun startTracking(session: Session, options: SpatialAnnotationTrackingOptions) {
            val extender = getPerceptionStateExtender(session)

            val runtimeFormat =
                when (options.format) {
                    SpatialAnnotationImageFormat.RGBA -> RuntimeSpatialAnnotationImageFormat.RGBA
                    SpatialAnnotationImageFormat.GRAYSCALE ->
                        RuntimeSpatialAnnotationImageFormat.GRAYSCALE
                    else -> throw IllegalArgumentException("Unknown format!")
                }

            val runtimeAlignment =
                when (options.alignment) {
                    SpatialAnnotationQuadAlignment.SCREEN ->
                        RuntimeSpatialAnnotationQuadAlignment.SCREEN
                    SpatialAnnotationQuadAlignment.OBJECT ->
                        RuntimeSpatialAnnotationQuadAlignment.OBJECT
                    else -> throw IllegalArgumentException("Unknown alignment!")
                }

            extender.perceptionManager.startSpatialAnnotationTracking(
                options.imageBuffer,
                options.imageSize,
                options.rowStride,
                runtimeFormat,
                runtimeAlignment,
                options.quads.mapKeys { RuntimeSpatialAnnotationId.fromString(it.key.toString()) },
                options.timestampNanos,
            )
        }

        /**
         * Stops tracking all actively tracked Spatial Annotations in the active [session].
         *
         * @param session the [Session] executing the XR tracking
         */
        @JvmStatic
        public fun stopTrackingAllAnnotations(session: Session) {
            getPerceptionStateExtender(session)
                .perceptionManager
                .stopSpatialAnnotationTracking(emptyList())
        }

        /**
         * Stops tracking the provided Spatial Annotations.
         *
         * @param session the [Session] executing the XR tracking
         * @param ids the specific list of [SpatialAnnotationId]s to cease tracking
         * @throws IllegalArgumentException if the provided list of ids is empty, use
         *   [stopTrackingAllAnnotations] to halt all active tracking
         */
        @JvmStatic
        public fun stopTracking(session: Session, ids: List<SpatialAnnotationId>) {
            require(ids.isNotEmpty()) {
                "The list of ids to stop tracking must not be empty. Use stopTrackingAllAnnotations() to stop all tracking."
            }
            getPerceptionStateExtender(session)
                .perceptionManager
                .stopSpatialAnnotationTracking(
                    ids.map { RuntimeSpatialAnnotationId.fromString(it.toString()) }
                )
        }

        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().firstOrNull()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }
    }

    /**
     * State containing physical properties that update frame-to-frame.
     *
     * @property trackingState the [TrackingState] of the SpatialAnnotation
     * @property centerPose the [Pose] of the center of the annotation's active physical state, the
     *   +Y axis relative to the [centerPose] is equivalent to the normal of the bounded region
     * @property quad the physical spatial geometry (in meters) residing on the XZ plane relative to
     *   the [centerPose], this will only have a value when the spatial tracking mode is set to quad
     * @property owner the [SpatialAnnotation] that owns this state
     */
    public class State
    internal constructor(
        public override val trackingState: TrackingState,
        public val centerPose: Pose,
        public val quad: Quad?,
        public val owner: SpatialAnnotation,
    ) : Trackable.State {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return trackingState == other.trackingState &&
                centerPose == other.centerPose &&
                quad == other.quad &&
                owner == other.owner
        }

        override fun hashCode(): Int {
            var result = trackingState.hashCode()
            result = 31 * result + centerPose.hashCode()
            result = 31 * result + (quad?.hashCode() ?: 0)
            result = 31 * result + owner.hashCode()
            return result
        }

        override fun toString(): String =
            "State(trackingState=$trackingState, centerPose=$centerPose, quad=$quad)"
    }

    private val _state =
        MutableStateFlow(
            State(
                trackingState = runtimeSpatialAnnotation.trackingState.toTrackingState(),
                centerPose = runtimeSpatialAnnotation.centerPose,
                quad = runtimeSpatialAnnotation.quad,
                owner = this,
            )
        )

    /** The current reactive state of the [SpatialAnnotation] */
    public override val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Reads the most recent physical boundaries and poses (State) for this single Spatial
     * Annotation from the C++ OpenXR engine and pushes them to developers.
     */
    internal override suspend fun update() {
        _state.emit(
            State(
                trackingState = runtimeSpatialAnnotation.trackingState.toTrackingState(),
                centerPose = runtimeSpatialAnnotation.centerPose,
                quad = runtimeSpatialAnnotation.quad,
                owner = this,
            )
        )
    }

    override fun toString(): String = "SpatialAnnotation(id=$id, state=${state.value})"
}
