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

package androidx.xr.scenecore.runtime

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/** Interface for an XR Runtime ScenePose. */
// TODO: b/420684433 This interface name no longer matches the public SceneCore interface name.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface ScenePose {
    /** Returns the pose for this entity, relative to the activity space root. */
    public val activitySpacePose: Pose

    // TODO: b/364303733 - Consider deprecating this method.
    /**
     * Returns the scale of this ScenePose. For base ScenePoses, the scale is (1,1,1). For entities
     * this returns the accumulated scale. This value includes the parent's scale, and is similar to
     * a ActivitySpace scale.
     *
     * @return Total [androidx.xr.runtime.math.Vector3] scale applied to self and children.
     */
    public val worldSpaceScale: Vector3

    /**
     * Returns the scale of this WorldPose relative to the activity space. This returns the
     * accumulated scale which includes the parent's scale, but does not include the scale of the
     * activity space itself.
     *
     * @return Total [androidx.xr.runtime.math.Vector3] scale applied to self and children relative
     *   to the activity space.
     */
    public val activitySpaceScale: Vector3

    /**
     * Returns a pose relative to this entity transformed into a pose relative to the destination.
     *
     * @param pose A pose in this entity's local coordinate space.
     * @param destination The entity which the returned pose will be relative to.
     * @return The pose relative to the destination entity.
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
     * > Warning: This operation does not support non-uniformly scaled ScenePoses.
     *
     * @param direction The direction in this ScenePose's local coordinate space
     * @param destination The ScenePose which the returned direction will be relative to.
     * @return The direction in the destination ScenePose's local space. It will have the same
     *   magnitude as the input direction.
     */
    public fun transformDirectionTo(direction: Vector3, destination: ScenePose): Vector3

    /** A filter for which Scenes to hit test with ScenePose.hitTest */
    public object HitTestFilter {
        /** Register hit tests for the scene which this Scene pose belongs to. */
        public const val SELF_SCENE: Int = 1 shl 0
        /**
         * Register hit tests only for other scenes. An Application will only have access to other
         * scenes if it has the com.android.extensions.xr.ACCESS_XR_OVERLAY_SPACE permission.
         */
        public const val OTHER_SCENES: Int = 1 shl 1
    }

    @Retention(AnnotationRetention.SOURCE)
    @Suppress("PublicTypedef")
    @IntDef(flag = true, value = [HitTestFilter.SELF_SCENE, HitTestFilter.OTHER_SCENES])
    public annotation class HitTestFilterValue

    /**
     * Creates a hit test at the from the specified origin in the specified direction into the
     * scene.
     *
     * @param origin The translation of the origin of the hit test relative to this ScenePose.
     * @param direction The direction for the hit test ray from the ScenePose.
     * @param hitTestFilter The scenes that will be in range for the hit test.
     * @return a {@code HitResult}. The HitResult describes if it hit something and where relative
     *   to this [ScenePose]. Listeners will be called on the main thread if Runnable::run is
     *   supplied.
     */
    public suspend fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @HitTestFilterValue hitTestFilter: Int,
    ): HitTestResult
}
