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

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config.FaceTrackingMode
import androidx.xr.runtime.FaceBlendShapeType
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Face as RuntimeFace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contains the tracking information of a detected human face. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Face internal constructor(internal val runtimeFace: RuntimeFace) : Updatable {
    public companion object {
        /**
         * Returns the Face object that corresponds to the user
         *
         * @param session the currently active [Session].
         * @throws [IllegalStateException] if [FaceTrackingMode] is set to
         *   [FaceTrackingMode.DISABLED].
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun getUserFace(session: Session): Face? {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }

            val config = perceptionStateExtender.xrResourcesManager.lifecycleManager.config
            check(config.faceTracking != FaceTrackingMode.DISABLED) {
                "Config.FaceTrackingMode is set to Disabled."
            }
            return perceptionStateExtender.xrResourcesManager.userFace
        }
    }

    /**
     * The representation of the current state of [Face].
     *
     * @param trackingState the current [TrackingState] of the face.
     * @param blendShapeValues the values measuring the blend shapes of the face.
     * @param confidenceValues the confidence values of the face tracker at different regions.
     */
    public class State(
        public val trackingState: TrackingState,
        internal val blendShapeValues: FloatArray,
        internal val confidenceValues: FloatArray,
    ) {
        /**
         * Represents the blend shapes of the face.
         *
         * @return a map of [FaceBlendShapeType] to the corresponding blend shape value.
         */
        // TODO: b/326655571 - Consider manually parsing the map for each entry rather than
        // generating it for the entire map on each access.
        public val blendShapes: Map<FaceBlendShapeType, Float>
            get() = FaceBlendShapeType.values().zip(blendShapeValues.toList()).toMap()

        /**
         * Gets the confidence value of the face tracker at the specified region index.
         *
         * @param regionIndex the index of the region to get the confidence value for.
         * @return the confidence value of the face tracker at the specified region index. If the
         *   region index does not exist, returns 0.
         */
        public fun getConfidence(regionIndex: Int): Float {
            return if (regionIndex < confidenceValues.size) confidenceValues[regionIndex] else 0f
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return trackingState == other.trackingState &&
                blendShapeValues contentEquals other.blendShapeValues &&
                confidenceValues contentEquals other.confidenceValues
        }

        override fun hashCode(): Int {
            var result = trackingState.hashCode()
            result = 31 * result + blendShapeValues.contentHashCode()
            result = 31 * result + confidenceValues.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "State(trackingState=$trackingState, " +
                "blendShapeValues=${blendShapeValues.contentToString()}," +
                "confidenceValues=${confidenceValues.contentToString()})"
        }
    }

    private val _state =
        MutableStateFlow<State>(State(TrackingState.PAUSED, FloatArray(0), FloatArray(0)))

    /** The current [State] of this Face. */
    public val state: StateFlow<State> = _state.asStateFlow()

    public override suspend fun update() {
        if (!runtimeFace.isValid) {
            return
        }
        _state.emit(
            State(
                runtimeFace.trackingState,
                runtimeFace.blendShapeValues,
                runtimeFace.confidenceValues,
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
