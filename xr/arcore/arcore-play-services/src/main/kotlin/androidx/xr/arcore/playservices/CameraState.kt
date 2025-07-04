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

package androidx.xr.arcore.playservices

import android.hardware.HardwareBuffer
import androidx.annotation.RestrictTo
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import java.nio.FloatBuffer
import kotlin.time.ComparableTimeMark

/**
 * Represents the state of the ARCore 1.x Session's Camera at a specific point in time.
 *
 * Can be obtained from [CoreState.cameraState].
 *
 * @property timeMark the time at which the state was computed.
 * @property trackingState the tracking state of the camera.
 * @property cameraPose the pose of the physical camera in the world space.
 * @property displayOrientedPose the pose of the virtual camera in the world space (for OpenGL)
 * @property projectionMatrix the projection matrix of the camera.
 * @property viewMatrix the view matrix of the camera.
 * @property hardwareBuffer the hardware buffer of the frame captured by the session.
 * @property transformCoordinates2D a function that transforms coordinates from normalized OpenGL
 *   device coordinates (display-rotated) to normalized texture coordinates.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CameraState
internal constructor(
    public val timeMark: ComparableTimeMark,
    public val trackingState: TrackingState,
    public val cameraPose: Pose? = null,
    public val displayOrientedPose: Pose? = null,
    public val projectionMatrix: Matrix4? = null,
    public val viewMatrix: Matrix4? = null,
    public val hardwareBuffer: HardwareBuffer? = null,
    public val transformCoordinates2D: ((FloatBuffer) -> FloatBuffer)? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CameraState) return false
        if (timeMark != other.timeMark) return false
        if (trackingState != other.trackingState) return false
        if (cameraPose != other.cameraPose) return false
        if (displayOrientedPose != other.displayOrientedPose) return false
        if (projectionMatrix != other.projectionMatrix) return false
        if (viewMatrix != other.viewMatrix) return false
        if (hardwareBuffer != other.hardwareBuffer) return false
        if (transformCoordinates2D != other.transformCoordinates2D) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timeMark.hashCode()
        result = 31 * result + trackingState.hashCode()
        result = 31 * result + cameraPose.hashCode()
        result = 31 * result + displayOrientedPose.hashCode()
        result = 31 * result + projectionMatrix.hashCode()
        result = 31 * result + viewMatrix.hashCode()
        result = 31 * result + hardwareBuffer.hashCode()
        result = 31 * result + transformCoordinates2D.hashCode()
        return result
    }
}
