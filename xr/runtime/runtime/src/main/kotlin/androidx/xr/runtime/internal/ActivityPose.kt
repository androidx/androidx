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

package androidx.xr.runtime.internal

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.google.common.util.concurrent.ListenableFuture

/** Interface for an XR Runtime ActivityPose. */
// TODO: b/420684433 This interface name no longer matches the public SceneCore interface name.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface ActivityPose {
    /** Returns the pose for this entity, relative to the activity space root. */
    public val activitySpacePose: Pose

    // TODO: b/364303733 - Consider deprecating this method.
    /**
     * Returns the scale of this ActivityPose. For base ActivityPoses, the scale is (1,1,1). For
     * entities this returns the accumulated scale. This value includes the parent's scale, and is
     * similar to a ActivitySpace scale.
     *
     * @return Total [Vector3] scale applied to self and children.
     */
    public val worldSpaceScale: Vector3

    /**
     * Returns the scale of this WorldPose relative to the activity space. This returns the
     * accumulated scale which includes the parent's scale, but does not include the scale of the
     * activity space itself.
     *
     * @return Total [Vector3] scale applied to self and children relative to the activity space.
     */
    public val activitySpaceScale: Vector3

    /**
     * Returns a pose relative to this entity transformed into a pose relative to the destination.
     *
     * @param pose A pose in this entity's local coordinate space.
     * @param destination The entity which the returned pose will be relative to.
     * @return The pose relative to the destination entity.
     */
    public fun transformPoseTo(pose: Pose, destination: ActivityPose): Pose

    /** A filter for which Scenes to hit test with ActivityPose.hitTest */
    public object HitTestFilter {
        /** Register hit tests for the scene which this Activity pose belongs to. */
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
     * @param origin The translation of the origin of the hit test relative to this ActivityPose.
     * @param direction The direction for the hit test ray from the ActivityPose.
     * @param hitTestFilter The scenes that will be in range for the hit test.
     * @return a {@code ListenableFuture<HitResult>}. The HitResult describes if it hit something
     *   and where relative to this [ActivityPose]. Listeners will be called on the main thread if
     *   Runnable::run is supplied.
     */
    @Suppress("AsyncSuffixFuture")
    public fun hitTest(
        origin: Vector3,
        direction: Vector3,
        @HitTestFilterValue hitTestFilter: Int,
    ): ListenableFuture<HitTestResult>
}
