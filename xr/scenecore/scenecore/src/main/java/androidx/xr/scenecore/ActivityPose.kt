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

package androidx.xr.scenecore

import android.util.Log
import androidx.xr.runtime.math.Pose

/**
 * Interface for a ActivityPose.
 *
 * A ActivityPose contains a pose in activity space and it's pose can be transformed into a pose
 * relative to another ActivityPose.
 */
public interface ActivityPose {

    /**
     * Returns the activity space pose for this ActivityPose.
     *
     * @return Current [Pose] relative to the activity space root.
     */
    public fun getActivitySpacePose(): Pose

    /**
     * Returns a pose relative to this ActivityPose transformed into a pose relative to the
     * destination.
     *
     * @param pose A pose in this ActivityPose's local coordinate space.
     * @param destination The ActivityPose which the returned pose will be relative to.
     * @return The pose relative to the destination ActivityPose.
     */
    public fun transformPoseTo(pose: Pose, destination: ActivityPose): Pose
}

/**
 * The BaseActivityPose is an implementation of ActivityPose interface that wraps a platformAdapter
 * ActivityPose.
 */
public sealed class BaseActivityPose<out RtActivityPoseType : JxrPlatformAdapter.ActivityPose>(
    internal val rtActivityPose: RtActivityPoseType
) : ActivityPose {
    private companion object {
        private const val TAG = "BaseRtActivityPose"
    }

    override fun getActivitySpacePose(): Pose {
        return rtActivityPose.activitySpacePose
    }

    override fun transformPoseTo(pose: Pose, destination: ActivityPose): Pose {
        if (destination !is BaseActivityPose<JxrPlatformAdapter.ActivityPose>) {
            Log.e(TAG, "Destination must be a subclass of BaseActivityPose!")
            return Pose.Identity
        }
        return rtActivityPose.transformPoseTo(pose, destination.rtActivityPose)
    }
}

/** A ActivityPose which tracks a camera's position and view into physical space. */
public class CameraView
private constructor(
    private val rtCameraViewActivityPose: JxrPlatformAdapter.CameraViewActivityPose
) : BaseActivityPose<JxrPlatformAdapter.CameraViewActivityPose>(rtCameraViewActivityPose) {

    internal companion object {
        internal fun createLeft(platformAdapter: JxrPlatformAdapter): CameraView? {
            val cameraViewActivityPose =
                platformAdapter.getCameraViewActivityPose(
                    JxrPlatformAdapter.CameraViewActivityPose.CAMERA_TYPE_LEFT_EYE
                )
            return cameraViewActivityPose?.let { CameraView(it) }
        }

        internal fun createRight(platformAdapter: JxrPlatformAdapter): CameraView? {
            val cameraViewActivityPose =
                platformAdapter.getCameraViewActivityPose(
                    JxrPlatformAdapter.CameraViewActivityPose.CAMERA_TYPE_RIGHT_EYE
                )
            return cameraViewActivityPose?.let { CameraView(it) }
        }
    }

    /** Describes the type of camera that this CameraView represents. */
    public enum class CameraType {
        /** This CameraView represents an unknown camera view. */
        UNKNOWN,

        /** This CameraView represents the user's left eye. */
        LEFT_EYE,

        /** This CameraView represents the user's right eye. */
        RIGHT_EYE,
    }

    public val cameraType: CameraType = CameraType.UNKNOWN

    /** Gets the FOV for the camera */
    public val fov: Fov
        get() {
            val rtFov = rtCameraViewActivityPose.fov
            return Fov(rtFov.angleLeft, rtFov.angleRight, rtFov.angleUp, rtFov.angleDown)
        }
}

/**
 * Head is a ActivityPose used to track the position of the user's head. If there is a left and
 * right camera it is calculated as the position between the two.
 */
public class Head private constructor(rtActivityPose: JxrPlatformAdapter.HeadActivityPose) :
    BaseActivityPose<JxrPlatformAdapter.HeadActivityPose>(rtActivityPose) {

    internal companion object {

        /** Factory function for creating [Head] instance. */
        internal fun create(platformAdapter: JxrPlatformAdapter): Head? {
            return platformAdapter.headActivityPose?.let { Head(it) }
        }
    }
}

/**
 * PerceptionSpace is ActivityPose used to track the origin of the space used by ARCore for XR APIs.
 */
// TODO: b/360870690 - Remove suppression annotation when API council review is complete.
public class PerceptionSpace
private constructor(rtActivityPose: JxrPlatformAdapter.PerceptionSpaceActivityPose) :
    BaseActivityPose<JxrPlatformAdapter.PerceptionSpaceActivityPose>(rtActivityPose) {

    internal companion object {

        /** Factory function for creating [PerceptionSpace] instance. */
        internal fun create(platformAdapter: JxrPlatformAdapter): PerceptionSpace =
            PerceptionSpace(platformAdapter.perceptionSpaceActivityPose)
    }
}
