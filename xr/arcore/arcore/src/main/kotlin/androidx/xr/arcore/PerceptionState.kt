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

import androidx.xr.runtime.CoreState
import kotlin.time.ComparableTimeMark

/**
 * Represents the state of ARCore for Jetpack XR at an specific point in time.
 *
 * Can be obtained from [CoreState.perceptionState].
 *
 * @property timeMark the time at which the state was computed
 * @property trackableStates the state of the trackables at the [timeMark] that are currently being
 *   tracked
 * @property leftHandState the state of the left hand at the [timeMark], or null when not supported
 *   by the current platform
 * @property rightHandState the state of the right hand at the [timeMark], or null when not
 *   supported by the current platform
 * @property arDeviceState the state of the currently tracked device at the [timeMark]
 * @property leftRenderViewpointState the state of the left viewpoint used for rendering at the
 *   [timeMark], or null when not supported by the current platform
 * @property rightRenderViewpointState the state of the right viewpoint used for rendering at the
 *   [timeMark], or null when not supported by the current platform
 * @property monoRenderViewpointState the state of the mono viewpoint used for rendering at the
 *   [timeMark], or null when not supported by the current platform
 * @property leftDepthState the state of the left depth map at the [timeMark], or null when not
 *   supported by the current platform
 * @property rightDepthState the state of the right depth map at the [timeMark], or null when not
 *   supported by the current platform
 * @property monoDepthState the state of the mono depth map at the [timeMark], or null when not
 *   supported by the current platform
 * @property userFaceState the state of the user's face at the [timeMark], or null when not
 *   supported by the current platform
 * @property leftEyeState the state of the user's left eye at the [timeMark], or null when not
 *   supported by the current platform
 * @property rightEyeState the state of the user's right eye at the [timeMark], or null when not
 *   supported by the current platform
 */
public class PerceptionState
internal constructor(
    public val timeMark: ComparableTimeMark,
    public val trackableStates: Collection<Trackable.State>,
    public val leftHandState: Hand.State?,
    public val rightHandState: Hand.State?,
    public val arDeviceState: ArDevice.State,
    public val leftRenderViewpointState: RenderViewpoint.State?,
    public val rightRenderViewpointState: RenderViewpoint.State?,
    public val monoRenderViewpointState: RenderViewpoint.State?,
    public val leftDepthState: Depth.State?,
    public val rightDepthState: Depth.State?,
    public val monoDepthState: Depth.State?,
    public val userFaceState: Face.State?,
    public val leftEyeState: Eye.State?,
    public val rightEyeState: Eye.State?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerceptionState) return false
        if (timeMark != other.timeMark) return false
        if (trackableStates != other.trackableStates) return false
        if (leftHandState != other.leftHandState) return false
        if (rightHandState != other.rightHandState) return false
        if (arDeviceState != other.arDeviceState) return false
        if (leftRenderViewpointState != other.leftRenderViewpointState) return false
        if (rightRenderViewpointState != other.rightRenderViewpointState) return false
        if (monoRenderViewpointState != other.monoRenderViewpointState) return false
        if (leftDepthState != other.leftDepthState) return false
        if (rightDepthState != other.rightDepthState) return false
        if (monoDepthState != other.monoDepthState) return false
        if (userFaceState != other.userFaceState) return false
        if (leftEyeState != other.leftEyeState) return false
        if (rightEyeState != other.rightEyeState) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timeMark.hashCode()
        result = 31 * result + trackableStates.hashCode()
        result = 31 * result + leftHandState.hashCode()
        result = 31 * result + rightHandState.hashCode()
        result = 31 * result + arDeviceState.hashCode()
        result = 31 * result + leftRenderViewpointState.hashCode()
        result = 31 * result + rightRenderViewpointState.hashCode()
        result = 31 * result + monoRenderViewpointState.hashCode()
        result = 31 * result + leftDepthState.hashCode()
        result = 31 * result + rightDepthState.hashCode()
        result = 31 * result + monoDepthState.hashCode()
        result = 31 * result + userFaceState.hashCode()
        result = 31 * result + leftEyeState.hashCode()
        result = 31 * result + rightEyeState.hashCode()
        return result
    }
}
