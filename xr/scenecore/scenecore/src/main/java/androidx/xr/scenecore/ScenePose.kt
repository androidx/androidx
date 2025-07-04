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
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.internal.ActivityPose as RtActivityPose
import androidx.xr.runtime.internal.CameraViewActivityPose as RtCameraViewActivityPose
import androidx.xr.runtime.internal.HeadActivityPose as RtHeadActivityPose
import androidx.xr.runtime.internal.HitTestResult as RtHitTestResult
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PerceptionSpaceActivityPose as RtPerceptionSpaceActivityPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * A [Pose] in the Scene graph, which can be transformed into a Pose relative to another ScenePose.
 */
public interface ScenePose {

    /** The current [Pose] relative to the activity space root. */
    public val activitySpacePose: Pose

    /**
     * Returns a [Pose] relative to this ScenePose, transformed into a Pose relative to the
     * destination.
     *
     * @param pose A Pose in this ScenePose's local coordinate space.
     * @param destination The ScenePose which the returned Pose will be relative to.
     * @return The Pose relative to the destination ScenePose.
     */
    public fun transformPoseTo(pose: Pose, destination: ScenePose): Pose

    /** A filter for which Scenes to hit test with [ScenePose.hitTest]. */
    public object HitTestFilter {

        /** Register hit tests for the scene which this ScenePose belongs. */
        public const val SELF_SCENE: Int = 1 shl 0

        /**
         * Register hit tests only for other scenes. A process will only have access to other scenes
         * if it has the `com.android.extensions.xr.ACCESS_XR_OVERLAY_SPACE` permission.
         */
        public const val OTHER_SCENES: Int = 1 shl 1
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(flag = true, value = [HitTestFilter.SELF_SCENE, HitTestFilter.OTHER_SCENES])
    public annotation class HitTestFilterValue

    /**
     * Creates a hit test from the specified origin in the specified direction into the Scene.
     *
     * @param origin The translation of the origin of the hit test relative to this ScenePose.
     * @param direction The direction for the hit test ray from the origin.
     * @return a HitResult. The HitResult describes if it hit something and where relative to this
     *   ScenePose.
     */
    public suspend fun hitTest(origin: Vector3, direction: Vector3): HitTestResult

    /**
     * Creates a hit test from the specified origin in the specified direction into the scene.
     *
     * @param origin The translation of the origin of the hit test relative to this ScenePose.
     * @param direction The direction for the hit test ray from the origin
     * @param hitTestFilter Filter for which scenes to hit test. Hitting other scenes is only
     *   allowed for apps with the `com.android.extensions.xr.ACCESS_XR_OVERLAY_SPACE` permission.
     * @return a HitResult. The HitResult describes if it hit something and where relative to this
     *   ScenePose.
     */
    public suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult
}

/** The BaseScenePose implements the [ScenePose] interface. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public abstract class BaseScenePose<out RtActivityPoseType : RtActivityPose>(
    internal val rtActivityPose: RtActivityPoseType
) : ScenePose {
    private companion object {
        private const val TAG = "BaseScenePose"
    }

    override val activitySpacePose: Pose
        get() = rtActivityPose.activitySpacePose

    override fun transformPoseTo(pose: Pose, destination: ScenePose): Pose {
        if (destination !is BaseScenePose<RtActivityPose>) {
            Log.e(TAG, "Destination must be a subclass of BaseActivityPose!")
            return Pose.Identity
        }
        return rtActivityPose.transformPoseTo(pose, destination.rtActivityPose)
    }

    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult {
        val hitTestRtFuture =
            this.rtActivityPose.hitTest(origin, direction, hitTestFilter.toRtHitTestFilter())
        val deferredHitTestResult: RtHitTestResult = hitTestRtFuture.awaitSuspending()
        return deferredHitTestResult.toHitTestResult()
    }

    override suspend fun hitTest(origin: Vector3, direction: Vector3): HitTestResult {
        return hitTest(origin, direction, ScenePose.HitTestFilter.SELF_SCENE.toRtHitTestFilter())
    }
}

/** An [ScenePose] which tracks a camera view's position and view into physical space. */
@Suppress("HiddenSuperclass")
public class CameraView
private constructor(private val rtCameraViewActivityPose: RtCameraViewActivityPose) :
    BaseScenePose<RtCameraViewActivityPose>(rtCameraViewActivityPose) {

    internal companion object {
        internal fun createLeft(platformAdapter: JxrPlatformAdapter): CameraView? {
            val cameraViewActivityPose =
                platformAdapter.getCameraViewActivityPose(
                    RtCameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE
                )
            return cameraViewActivityPose?.let { CameraView(it) }
        }

        internal fun createRight(platformAdapter: JxrPlatformAdapter): CameraView? {
            val cameraViewActivityPose =
                platformAdapter.getCameraViewActivityPose(
                    RtCameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE
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

    /** Gets the FOV for the camera. */
    public val fov: FieldOfView
        get() {
            val rtFov = rtCameraViewActivityPose.fov
            return FieldOfView(rtFov.angleLeft, rtFov.angleRight, rtFov.angleUp, rtFov.angleDown)
        }
}

/**
 * Head is an [ScenePose] used to track the position of the user's head. If there is a left and
 * right camera it is calculated as the position between the two.
 */
@Suppress("HiddenSuperclass")
public class Head private constructor(rtActivityPose: RtHeadActivityPose) :
    BaseScenePose<RtHeadActivityPose>(rtActivityPose) {

    internal companion object {

        /** Factory function for creating [Head] instance. */
        internal fun create(platformAdapter: JxrPlatformAdapter): Head? {
            return platformAdapter.headActivityPose?.let { Head(it) }
        }
    }
}

/**
 * PerceptionSpace is an [ScenePose] used to track the origin of the space used by ARCore for
 * Jetpack XR APIs.
 */
@Suppress("HiddenSuperclass")
public class PerceptionSpace private constructor(rtActivityPose: RtPerceptionSpaceActivityPose) :
    BaseScenePose<RtPerceptionSpaceActivityPose>(rtActivityPose) {

    internal companion object {

        /** Factory function for creating [PerceptionSpace] instance. */
        internal fun create(platformAdapter: JxrPlatformAdapter): PerceptionSpace =
            PerceptionSpace(platformAdapter.perceptionSpaceActivityPose)
    }
}
