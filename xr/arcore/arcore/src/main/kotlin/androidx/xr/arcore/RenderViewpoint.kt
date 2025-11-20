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
import androidx.xr.arcore.runtime.RenderViewpoint as RuntimeRenderViewpoint
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a single viewpoint used for rendering, such as a left eye, right eye, or a mono view.
 *
 * This class provides access to the [State] of a specific render viewpoint, including its [pose],
 * [localPose], and [fieldOfView].
 */
public class RenderViewpoint
internal constructor(
    internal val runtimeRenderViewpoint: RuntimeRenderViewpoint,
    internal val runtimeArDevice: RuntimeArDevice,
) : Updatable {

    public companion object {
        /**
         * Returns the RenderViewpoint associated with the left display.
         *
         * @param session the currently active [Session].
         * @note Supported only on devices that use stereo displays for rendering.
         */
        @JvmStatic
        public fun left(session: Session): RenderViewpoint? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.leftRenderViewpoint
        }

        /**
         * Returns the RenderViewpoint associated with the right display.
         *
         * @param session the currently active [Session].
         * @note Supported only on devices that use stereo displays for rendering.
         */
        @JvmStatic
        public fun right(session: Session): RenderViewpoint? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.rightRenderViewpoint
        }

        /**
         * Returns the RenderViewpoint associated with the single device display.
         *
         * @param session the currently active [Session].
         * @note When the device uses a single display, this will return the render viewpoint for
         *   that display. When the device uses stereo displays, this will return the render
         *   viewpoint for the center of the two displays.
         */
        @JvmStatic
        public fun mono(session: Session): RenderViewpoint? {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.monoRenderViewpoint
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
     * Class that contains the current state of the render viewpoint.
     *
     * @property pose The render viewpoint's pose in perception space, the global coordinate system
     *   of the [Session]. This value is the underlying AR Device's pose plus the localPose offset.
     *   Its update behavior is determined by [Config.deviceTracking]:
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
    public class State
    internal constructor(
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
    /** The current [State] of the render viewpoint. */
    public val state: StateFlow<State> = _state.asStateFlow()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override suspend fun update() {
        val poseInPerceptionSpace = runtimeArDevice.devicePose.compose(runtimeRenderViewpoint.pose)
        _state.emit(
            State(
                poseInPerceptionSpace,
                runtimeRenderViewpoint.pose,
                runtimeRenderViewpoint.fieldOfView,
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RenderViewpoint) return false
        return runtimeRenderViewpoint == other.runtimeRenderViewpoint
    }

    override fun hashCode(): Int = runtimeRenderViewpoint.hashCode()
}
