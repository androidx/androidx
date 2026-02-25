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
import androidx.xr.arcore.runtime.Eye as RuntimeEye
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A representation of a user's eye.
 *
 * An [Eye] instance provides the state of the eye (shut or gazing), as well as a [Pose] indicating
 * where the user is currently looking.
 */
public class Eye internal constructor(internal val runtimeEye: RuntimeEye) :
    Trackable<Eye.State>, Updatable {

    public companion object {
        /**
         * Returns the left eye, if available.
         *
         * @param session the [Session] to retrieve the eye from
         * @sample androidx.xr.arcore.samples.getLeftEye
         */
        @JvmStatic
        public fun left(session: Session): Eye? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            val config = perceptionStateExtender.xrResourcesManager.lifecycleManager.config
            check(config.eyeTracking != EyeTrackingMode.DISABLED) {
                "Config.EyeTrackingMode is set to DISABLED."
            }
            return perceptionStateExtender.xrResourcesManager.leftEye
        }

        /**
         * Returns the right eye, if available.
         *
         * @param session the [Session] to retrieve the eye from
         * @sample androidx.xr.arcore.samples.getRightEye
         */
        @JvmStatic
        public fun right(session: Session): Eye? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            val config = perceptionStateExtender.xrResourcesManager.lifecycleManager.config
            check(config.eyeTracking != EyeTrackingMode.DISABLED) {
                "Config.EyeTrackingMode is set to DISABLED."
            }
            return perceptionStateExtender.xrResourcesManager.rightEye
        }

        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }
    }

    /**
     * The representation of the current state of an [Eye].
     *
     * The [Pose]s provided are the position and rotation of the eye itself, relative to the head
     * pose.
     *
     * @property isOpen a flag indicating whether the eye is open
     * @property pose the [Pose] of the eye
     * @property trackingState the [TrackingState] of the eye
     */
    public class State(
        public val isOpen: Boolean,
        public val pose: Pose,
        public override val trackingState: TrackingState,
    ) : Trackable.State {}

    private var _state =
        MutableStateFlow(State(runtimeEye.isOpen, runtimeEye.pose, runtimeEye.trackingState))

    /** A [StateFlow] that contains the latest [State] of an [Eye]. */
    public override val state: StateFlow<State> = _state.asStateFlow()

    /**
     * This function is used by the runtime to propagate internal state changes. It is not intended
     * to be called directly by a developer.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override suspend fun update() {
        _state.emit(State(runtimeEye.isOpen, runtimeEye.pose, runtimeEye.trackingState))
    }
}
