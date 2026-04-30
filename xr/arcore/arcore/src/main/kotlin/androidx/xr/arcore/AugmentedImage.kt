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

import androidx.xr.arcore.runtime.AugmentedImage as RuntimeAugmentedImage
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

/** Describes the system's current best knowledge of a real-world image */
@SuppressWarnings("HiddenSuperclass")
public class AugmentedImage
internal constructor(internal val runtimeAugmentedImage: RuntimeAugmentedImage) :
    Trackable<AugmentedImage.State>, Updatable() {

    public companion object {
        /**
         * Emits the augmented images that are currently being tracked in the [session]
         *
         * Only instances of [AugmentedImage] that are [androidx.xr.arcore.TrackingState.TRACKING]
         * will be emitted in the [Collection]. Instances of the same [AugmentedImage] will remain
         * between subsequent emits to the [StateFlow] as long as they remain tracking
         *
         * @param session the [Session] to subscribe to
         * @return a [StateFlow] that emits a collection of AugmentedImages
         * @throws [IllegalStateException] if the [Config.augmentedImageDatabase] is empty
         */
        @JvmStatic
        public fun subscribe(session: Session): StateFlow<Collection<AugmentedImage>> {
            check(session.config.augmentedImageDatabase?.entries?.isNotEmpty() == true) {
                "Config.augmentedImageDatabase.entries is empty."
            }

            return session.state
                .transform { state ->
                    state.perceptionState?.let { perceptionState ->
                        emit(
                            perceptionState.trackableStates
                                .filterIsInstance<AugmentedImage.State>()
                                .map { it.owner }
                        )
                    }
                }
                .stateIn(
                    session.coroutineScope,
                    SharingStarted.Eagerly,
                    session.state.value.perceptionState
                        ?.trackableStates
                        ?.filterIsInstance<AugmentedImage.State>()
                        ?.map { it.owner } ?: emptyList(),
                )
        }
    }

    /**
     * The representation of the current state of an [AugmentedImage]. An [AugmentedImage] is
     * represented as a finite 2D bounding box around a [centerPose]
     *
     * @property index The id of the detected image which correlates to its zero-based positional
     *   index from its originating image database
     * @property trackingState Whether this image is being tracked or not
     * @property centerPose The [Pose] of the center of the detected image's bounding box in the
     *   world coordinate space. The +Y axis relative to the [centerPose] is equivalent to the
     *   normal of the [AugmentedImage]
     * @property extents The dimensions of the bounding box of the detected image
     * @property owner self-reference to the object that owns this state.
     */
    public class State
    internal constructor(
        public val index: Int,
        public override val trackingState: TrackingState,
        public val centerPose: Pose,
        public val extents: FloatSize2d,
        public val owner: AugmentedImage,
    ) : Trackable.State {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return index == other.index &&
                trackingState == other.trackingState &&
                centerPose == other.centerPose &&
                extents == other.extents &&
                owner == other.owner
        }

        override fun hashCode(): Int {
            var result = index.hashCode()
            result = 31 * result + trackingState.hashCode()
            result = 31 * result + centerPose.hashCode()
            result = 31 * result + extents.hashCode()
            result = 31 * result + owner.hashCode()
            return result
        }
    }

    private val _state =
        MutableStateFlow(
            State(
                runtimeAugmentedImage.index,
                runtimeAugmentedImage.trackingState.toTrackingState(),
                runtimeAugmentedImage.centerPose,
                runtimeAugmentedImage.extents,
                owner = this,
            )
        )
    /** The current state of the [AugmentedImage] */
    public override val state: StateFlow<State> = _state.asStateFlow()

    override suspend fun update() {
        _state.emit(
            State(
                index = runtimeAugmentedImage.index,
                trackingState = runtimeAugmentedImage.trackingState.toTrackingState(),
                centerPose = runtimeAugmentedImage.centerPose,
                extents = runtimeAugmentedImage.extents,
                owner = this,
            )
        )
    }
}
