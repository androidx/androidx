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
import androidx.xr.arcore.runtime.ArDevice as RuntimeArDevice
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Contains the information of the device that locates it with respect to the real world.
 *
 * @property state the current [State] of the AR device tracking
 */
@SuppressWarnings("HiddenSuperclass")
public class ArDevice internal constructor(internal val runtimeArDevice: RuntimeArDevice) :
    Updatable() {

    public companion object {
        /**
         * Returns the AR device tracking data.
         *
         * @param session the currently active [Session]
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [androidx.xr.runtime.DeviceTrackingMode.DISABLED]
         */
        @JvmStatic
        public fun getInstance(session: Session): ArDevice {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            val config = perceptionStateExtender.xrResourcesManager.lifecycleManager.config
            check(config.deviceTracking != DeviceTrackingMode.DISABLED) {
                "Config.DeviceTrackingMode is set to DISABLED."
            }
            return perceptionStateExtender.xrResourcesManager.arDevice
        }

        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }
    }

    /**
     * Contains the current state of the AR Device tracking.
     *
     * @property devicePose the current [Pose] of the device
     * @property trackingState The current [androidx.xr.runtime.TrackingState]
     * @property owner self-reference to the object that owns this state.
     */
    public class State
    internal constructor(
        public val devicePose: Pose,
        public val trackingState: TrackingState,
        public val owner: ArDevice,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return devicePose == other.devicePose &&
                trackingState == other.trackingState &&
                owner == other.owner
        }

        override fun hashCode(): Int {
            var result = devicePose.hashCode()
            result = 31 * result + trackingState.hashCode()
            result = 31 * result + owner.hashCode()
            return result
        }
    }

    private val _state = MutableStateFlow<State>(State(Pose(), TrackingState.STOPPED, owner = this))

    public val state: StateFlow<State> = _state.asStateFlow()

    // TODO b/482646486: Remove public visibility and unrestrict when no longer used in G3
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public override suspend fun update() {
        _state.emit(
            State(
                runtimeArDevice.devicePose,
                runtimeArDevice.trackingState.toTrackingState(),
                owner = this,
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArDevice) return false
        return runtimeArDevice == other.runtimeArDevice
    }

    override fun hashCode(): Int = runtimeArDevice.hashCode()
}
