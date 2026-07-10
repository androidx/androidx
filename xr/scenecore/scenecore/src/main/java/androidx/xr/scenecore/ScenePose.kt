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

package androidx.xr.scenecore

import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose as RtPerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.ScenePose as RtScenePose
import androidx.xr.scenecore.runtime.SceneRuntime

/**
 * A [Pose] in the Scene graph, which can be transformed into a Pose relative to another ScenePose.
 */
public interface ScenePose {

    /** The current [Pose] relative to the activity space root. */
    public val poseInActivitySpace: Pose

    /**
     * Returns a [Pose] relative to this ScenePose, transformed into a Pose relative to the
     * destination.
     *
     * @param pose A Pose in this ScenePose's local coordinate space.
     * @param destination The ScenePose which the returned Pose will be relative to.
     * @return The Pose relative to the destination ScenePose.
     */
    public fun transformPoseTo(pose: Pose, destination: ScenePose): Pose

    /**
     * Transforms a position from this ScenePose's local space to the destination ScenePose's local
     * space.
     *
     * This operation is affected by both ScenePose's position, rotation, and scale.
     *
     * @param position The position in this ScenePose's local coordinate space
     * @param destination The ScenePose which the returned position will be relative to.
     * @return The position in the destination ScenePose's local space.
     */
    public fun transformPositionTo(position: Vector3, destination: ScenePose): Vector3

    /**
     * Transforms a vector from this ScenePose's local space to the destination ScenePose's local
     * space. This operation accounts for scale. The magnitude of the output vector might be
     * different from the magnitude of the input vector.
     *
     * This operation is not affected by either ScenePose's position.
     *
     * @param vector The vector in this ScenePose's local coordinate space
     * @param destination The ScenePose which the returned vector will be relative to.
     * @return The vector in the destination ScenePose's local space. The returned magnitude will be
     *   affected by destination scale.
     */
    public fun transformVectorTo(vector: Vector3, destination: ScenePose): Vector3

    /**
     * Transforms a direction from this ScenePose's local space to the destination ScenePose's local
     * space. This operation ignores relative scaling; the output vector will have the same
     * magnitude as [direction].
     *
     * This operation is not affected by either ScenePose's scale or position.
     *
     * @param direction The direction in this ScenePose's local coordinate space
     * @param destination The ScenePose which the returned direction will be relative to.
     * @return The direction in the destination ScenePose's local space. It will have the same
     *   magnitude as the input direction.
     */
    public fun transformDirectionTo(direction: Vector3, destination: ScenePose): Vector3

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
     * Perform a hit test from the specified origin in the specified direction into the Scene.
     *
     * @param origin The translation of the origin of the hit test relative to this ScenePose.
     * @param direction The direction for the hit test ray from the origin.
     * @return The [HitTestResult], or null if the hit test did not find an intersection. The
     *   HitTestResult describes the location and normal of the object closest to the hit, relative
     *   to this ScenePose.
     */
    public suspend fun hitTest(origin: Vector3, direction: Vector3): HitTestResult?

    /**
     * Creates a hit test from the specified origin in the specified direction into the scene.
     *
     * @param origin The translation of the origin of the hit test relative to this ScenePose.
     * @param direction The direction for the hit test ray from the origin
     * @param hitTestFilter Filter for which scenes to hit test. Hitting other scenes is only
     *   allowed for apps with the `com.android.extensions.xr.ACCESS_XR_OVERLAY_SPACE` permission.
     * @return The [HitTestResult], or null if the hit test did not find an intersection. The
     *   HitTestResult describes the location and normal of the object closest to the hit, relative
     *   to this ScenePose.
     */
    public suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult?
}

/** The BaseScenePose implements the [ScenePose] interface. */
public abstract class BaseScenePose<out RtScenePoseType : RtScenePose>
protected constructor(internal val rtScenePose: RtScenePoseType) : ScenePose {
    private companion object {
        private const val TAG = "BaseScenePose"
    }

    override val poseInActivitySpace: Pose
        get() = rtScenePose.activitySpacePose

    override fun transformPoseTo(pose: Pose, destination: ScenePose): Pose {
        if (destination !is BaseScenePose<RtScenePose>) {
            Log.e(TAG, "Destination must be a subclass of BaseScenePose!")
            return Pose.Identity
        }
        return rtScenePose.transformPoseTo(pose, destination.rtScenePose)
    }

    override fun transformPositionTo(position: Vector3, destination: ScenePose): Vector3 {
        if (destination !is BaseScenePose<RtScenePose>) {
            Log.e(TAG, "Destination must be a subclass of BaseScenePose!")
            return Vector3.Zero
        }
        return rtScenePose.transformPositionTo(position, destination.rtScenePose)
    }

    override fun transformVectorTo(vector: Vector3, destination: ScenePose): Vector3 {
        if (destination !is BaseScenePose<RtScenePose>) {
            Log.e(TAG, "Destination must be a subclass of BaseScenePose!")
            return Vector3.Zero
        }
        return rtScenePose.transformVectorTo(vector, destination.rtScenePose)
    }

    override fun transformDirectionTo(direction: Vector3, destination: ScenePose): Vector3 {
        if (destination !is BaseScenePose<RtScenePose>) {
            Log.e(TAG, "Destination must be a subclass of BaseScenePose!")
            return Vector3.Zero
        }
        return rtScenePose.transformDirectionTo(direction, destination.rtScenePose)
    }

    override suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @ScenePose.HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult? {
        val hitTestRt =
            this.rtScenePose.hitTest(origin, direction, hitTestFilter.toRtHitTestFilter())
        return hitTestRt.toHitTestResult()
    }

    override suspend fun hitTest(origin: Vector3, direction: Vector3): HitTestResult? {
        return hitTest(origin, direction, ScenePose.HitTestFilter.SELF_SCENE.toRtHitTestFilter())
    }
}

/**
 * PerceptionSpace is an [ScenePose] used to track the origin of the space used by ARCore for
 * Jetpack XR APIs.
 */
public class PerceptionSpace private constructor(private val sceneRuntime: SceneRuntime) :
    BaseScenePose<RtPerceptionSpaceScenePose>(sceneRuntime.perceptionSpaceActivityPose) {

    /**
     * Returns a [ScenePose] from a [Pose] relative to this [PerceptionSpace].
     *
     * @param pose a Pose relative to the perceptionSpace.
     * @return a ScenePose containing the position in the [PerceptionSpace].
     */
    public fun getScenePoseFromPerceptionPose(pose: Pose): ScenePose {
        return PerceptionScenePose.create(sceneRuntime, pose)
    }

    internal companion object {

        /** Factory function for creating [PerceptionSpace] instance. */
        internal fun create(sceneRuntime: SceneRuntime): PerceptionSpace =
            PerceptionSpace(sceneRuntime)
    }
}

/** A ScenePose that is created based on a position in [PerceptionSpace]. */
internal class PerceptionScenePose private constructor(rtScenePose: RtScenePose) :
    BaseScenePose<RtScenePose>(rtScenePose) {

    internal companion object {
        /** Factory function for creating PerceptionScenePose instance. */
        internal fun create(sceneRuntime: SceneRuntime, pose: Pose): ScenePose =
            PerceptionScenePose(sceneRuntime.getScenePoseFromPerceptionPose(pose))
    }
}
