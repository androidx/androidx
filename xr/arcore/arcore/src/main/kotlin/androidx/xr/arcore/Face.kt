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

package androidx.xr.arcore

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.Face as RuntimeFace
import androidx.xr.arcore.runtime.Mesh
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

/**
 * Contains the tracking information of a detected human face.
 *
 * @property state the current [State] of this face
 */
public class Face
internal constructor(
    internal val runtimeFace: RuntimeFace,
    internal val xrResourceManager: XrResourcesManager,
) : Anchorable<Face.State>, Updatable {

    public companion object {
        /**
         * Returns the Face object that corresponds to the user.
         *
         * @param session the currently active [Session]
         * @throws [IllegalStateException] if [FaceTrackingMode] is set to
         *   [FaceTrackingMode.DISABLED]
         */
        @JvmStatic
        public fun getUserFace(session: Session): Face? {
            val config = session.config
            check(config.faceTracking == FaceTrackingMode.BLEND_SHAPES) {
                "Config.FaceTrackingMode must be set to USER to read the user's face."
            }

            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender.xrResourcesManager.userFace
        }

        /**
         * Emits the faces that are currently being tracked in the [Session].
         *
         * @param session the [Session] to track faces from
         */
        @JvmStatic
        public fun subscribe(session: Session): StateFlow<Collection<Face>> {
            check(session.config.faceTracking == FaceTrackingMode.MESHES) {
                "Config.FaceTrackingMode must be set to MESHES to track face meshes."
            }

            return session.state
                .transform { state ->
                    state.perceptionState?.let { perceptionState ->
                        emit(perceptionState.trackables.filterIsInstance<Face>())
                    }
                }
                .stateIn(
                    session.coroutineScope,
                    SharingStarted.Eagerly,
                    session.state.value.perceptionState?.trackables?.filterIsInstance<Face>()
                        ?: emptyList(),
                )
        }

        internal val blendShapeMapKeys: List<FaceBlendShapeType> =
            listOf(
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_BROW_LOWERER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_BROW_LOWERER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHEEK_PUFF_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHEEK_PUFF_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHEEK_RAISER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHEEK_RAISER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHEEK_SUCK_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHEEK_SUCK_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHIN_RAISER_BOTTOM,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHIN_RAISER_TOP,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_DIMPLER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_DIMPLER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_CLOSED_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_CLOSED_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_LOOK_DOWN_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_LOOK_DOWN_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_LOOK_LEFT_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_LOOK_LEFT_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_LOOK_RIGHT_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_LOOK_RIGHT_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_LOOK_UP_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_LOOK_UP_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_INNER_BROW_RAISER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_INNER_BROW_RAISER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_JAW_DROP,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_JAW_SIDEWAYS_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_JAW_SIDEWAYS_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_JAW_THRUST,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LID_TIGHTENER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LID_TIGHTENER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_CORNER_DEPRESSOR_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_CORNER_DEPRESSOR_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_CORNER_PULLER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_CORNER_PULLER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_FUNNELER_LEFT_BOTTOM,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_FUNNELER_LEFT_TOP,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_FUNNELER_RIGHT_BOTTOM,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_FUNNELER_RIGHT_TOP,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_PRESSOR_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_PRESSOR_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_PUCKER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_PUCKER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_STRETCHER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_STRETCHER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_SUCK_LEFT_BOTTOM,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_SUCK_LEFT_TOP,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_SUCK_RIGHT_BOTTOM,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_SUCK_RIGHT_TOP,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_TIGHTENER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_TIGHTENER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIPS_TOWARD,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LOWER_LIP_DEPRESSOR_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LOWER_LIP_DEPRESSOR_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_MOUTH_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_MOUTH_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_NOSE_WRINKLER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_NOSE_WRINKLER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_OUTER_BROW_RAISER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_OUTER_BROW_RAISER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_UPPER_LID_RAISER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_UPPER_LID_RAISER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_UPPER_LIP_RAISER_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_UPPER_LIP_RAISER_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_TONGUE_OUT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_TONGUE_LEFT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_TONGUE_RIGHT,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_TONGUE_UP,
                FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_TONGUE_DOWN,
            )

        internal val confidenceRegions: List<FaceConfidenceRegion> =
            listOf(
                FaceConfidenceRegion.FACE_CONFIDENCE_REGION_LOWER,
                FaceConfidenceRegion.FACE_CONFIDENCE_REGION_LEFT_UPPER,
                FaceConfidenceRegion.FACE_CONFIDENCE_REGION_RIGHT_UPPER,
            )
    }

    /**
     * The representation of the current state of [Face].
     *
     * @property trackingState the current [TrackingState] of the face.
     * @property centerPose the pose at the center of the face, defined to have the origin located
     *   behind the nose and between the two cheek bones
     *
     *   Z+ is forward out of the nose, Y+ is upwards, and X+ is towards the left. The units are in
     *   meters.
     *
     *   [centerPose] will be null if the Session is not configured with [FaceTrackingMode.MESHES].
     *
     * @property mesh the polygonal representation of the face as observed by the perception system
     *
     *   [mesh] will be null if the Session is not configured with [FaceTrackingMode.MESHES].
     */
    public class State
    internal constructor(
        public override val trackingState: TrackingState,
        public val centerPose: Pose? = null,
        public val mesh: Mesh? = null,
        internal val blendShapeValues: FloatArray? = null,
        internal val confidenceValues: FloatArray? = null,
        internal val noseTipPose: Pose? = null,
        internal val foreheadLeftPose: Pose? = null,
        internal val foreheadRightPose: Pose? = null,
    ) : Trackable.State {

        public val blendShapes: Map<FaceBlendShapeType, Float> =
            blendShapeMapKeys.zip(blendShapeValues?.toList() ?: emptyList()).toMap()

        /**
         * Gets the confidence value of the face tracker for the given region.
         *
         * @param region the [FaceConfidenceRegion] to get the confidence value for
         * @return the confidence value in the range `[0.0, 1.0]` of the face tracker for the given
         *   region
         * @throws IllegalArgumentException if the region does not exist
         * @throws IllegalStateException if the Face does not provide confidence values
         */
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
        public fun getConfidence(region: FaceConfidenceRegion): Float {
            check(confidenceValues != null) { "The Face does not contain confidenceValues." }
            return when (region) {
                FaceConfidenceRegion.FACE_CONFIDENCE_REGION_LOWER -> confidenceValues[0]
                FaceConfidenceRegion.FACE_CONFIDENCE_REGION_LEFT_UPPER -> confidenceValues[1]
                FaceConfidenceRegion.FACE_CONFIDENCE_REGION_RIGHT_UPPER -> confidenceValues[2]
                else -> throw IllegalArgumentException("Unknown confidence for region ${region}.")
            }
        }

        /**
         * Map of [Pose] values on the Face for each [FaceMeshRegion]
         *
         * Each [Pose] value in the Map will be null if the Session is not configured with
         * [FaceTrackingMode.MESHES].
         */
        public val regionPoses: Map<FaceMeshRegion, Pose?> =
            mapOf(
                FaceMeshRegion.NOSE_TIP to noseTipPose,
                FaceMeshRegion.FOREHEAD_LEFT to foreheadLeftPose,
                FaceMeshRegion.FOREHEAD_RIGHT to foreheadRightPose,
            )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return trackingState == other.trackingState &&
                blendShapeValues contentEquals other.blendShapeValues &&
                confidenceValues contentEquals other.confidenceValues &&
                centerPose == other.centerPose &&
                mesh == other.mesh &&
                noseTipPose == other.noseTipPose &&
                foreheadLeftPose == other.foreheadLeftPose &&
                foreheadRightPose == other.foreheadRightPose
        }

        override fun hashCode(): Int {
            var result = trackingState.hashCode()
            result = 31 * result + blendShapeValues.contentHashCode()
            result = 31 * result + confidenceValues.contentHashCode()
            result = 31 * result + centerPose.hashCode()
            result = 31 * result + mesh.hashCode()
            result = 31 * result + noseTipPose.hashCode()
            result = 31 * result + foreheadLeftPose.hashCode()
            result = 31 * result + foreheadRightPose.hashCode()
            return result
        }

        override fun toString(): String {
            return "State(trackingState=$trackingState, " +
                "blendShapeValues=${blendShapeValues.contentToString()}," +
                "confidenceValues=${confidenceValues.contentToString()})"
        }
    }

    private val _state =
        MutableStateFlow<State>(
            State(
                TrackingState.PAUSED,
                blendShapeValues = FloatArray(blendShapeMapKeys.size),
                confidenceValues = FloatArray(confidenceRegions.size),
            )
        )

    /** The current [State] of this Face. */
    public override val state: StateFlow<State> = _state.asStateFlow()

    /** Create and attach an [Anchor] to the Face at the given [Pose] in world space. */
    public override fun createAnchor(pose: Pose): AnchorCreateResult {
        val runtimeAnchor: RuntimeAnchor
        try {
            runtimeAnchor = runtimeFace.createAnchor(pose)
        } catch (e: AnchorResourcesExhaustedException) {
            return AnchorCreateResourcesExhausted()
        } catch (e: IllegalStateException) {
            throw UnsupportedOperationException("The Face does not support anchors.", e)
        }
        val anchor = Anchor(runtimeAnchor, xrResourceManager)
        xrResourceManager.addUpdatable(anchor)
        return AnchorCreateSuccess(anchor)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public override suspend fun update() {
        if (!runtimeFace.isValid) return
        _state.emit(
            State(
                runtimeFace.trackingState,
                runtimeFace.centerPose,
                runtimeFace.mesh,
                runtimeFace.blendShapeValues,
                runtimeFace.confidenceValues,
                runtimeFace.noseTipPose,
                runtimeFace.foreheadLeftPose,
                runtimeFace.foreheadRightPose,
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Face) return false
        return runtimeFace == other.runtimeFace
    }

    override fun hashCode(): Int = runtimeFace.hashCode()

    override fun toString(): String = "Face(runtimeFace=$runtimeFace, state=${state.value})"
}
