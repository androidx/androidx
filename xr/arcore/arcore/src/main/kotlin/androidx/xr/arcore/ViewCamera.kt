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
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ArDevice as RuntimeArDevice
import androidx.xr.runtime.internal.ViewCamera as RuntimeViewCamera
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contains view cameras information. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ViewCamera
internal constructor(
    internal val runtimeViewCamera: RuntimeViewCamera,
    internal val runtimeArDevice: RuntimeArDevice,
) : Updatable {

    public companion object {
        /**
         * Returns all view cameras.
         *
         * @param session the currently active [Session].
         */
        @JvmStatic
        public fun getAll(session: Session): List<ViewCamera> {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.viewCameras
        }

        // TODO(b/421240554): Combine getPerceptionStateExtender in different classes.
        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }
    }

    /**
     * Data class that contains the current state of the view camera.
     *
     * @property pose The view camera's pose in perception space, the global coordinate system of
     *   the [Session]. This value is the underlying XR Device's pose plus the localPose offset. Its
     *   update behavior is determined by [Config.HeadTrackingMode]:
     * - **LAST_KNOWN:** The device pose is updated each frame with the latest valid tracking data,
     *   reflecting physical movement.
     * - **DISABLED:** The device pose is not updated. It remains at the origin (an identity pose)
     *   unless this mode is switched from LAST_KNOWN to DISABLED mid-session, which freezes the
     *   pose at its last known state.
     *
     * @property localPose A local offset from the device's central tracking point, used for
     *   scenarios like stereo rendering (left/right eye views).
     * @property fieldOfView Contains the camera's field of view in radians.
     */
    public class State(
        public val pose: Pose,
        public val localPose: Pose,
        public val fieldOfView: FieldOfView,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return pose == other.pose &&
                localPose == other.localPose &&
                fieldOfView == other.fieldOfView
        }

        override fun hashCode(): Int {
            var result = pose.hashCode()
            result = 31 * result + localPose.hashCode()
            result = 31 * result + fieldOfView.hashCode()
            return result
        }

        override fun toString(): String {
            return "State(pose=$pose, localPose=$localPose, fieldOfView=$fieldOfView)"
        }
    }

    private val _state = MutableStateFlow<State>(State(Pose(), Pose(), FieldOfView(0f, 0f, 0f, 0f)))
    /** The current [State] of the view camera. */
    public val state: StateFlow<State> = _state.asStateFlow()

    override suspend fun update() {
        val poseInPerceptionSpace = runtimeArDevice.devicePose.compose(runtimeViewCamera.pose)
        _state.emit(
            State(poseInPerceptionSpace, runtimeViewCamera.pose, runtimeViewCamera.fieldOfView)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewCamera) return false
        return runtimeViewCamera == other.runtimeViewCamera
    }

    override fun hashCode(): Int = runtimeViewCamera.hashCode()
}
