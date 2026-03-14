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

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import java.util.function.Consumer

/** Interface for a XR Runtime [GltfEntity]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface GltfEntity : Entity {

    /** The flattened list of all nodes contained within this glTF model entity. */
    public val nodes: List<GltfModelNodeFeature>

    /**
     * Retrieves the axis-aligned bounding box (AABB) of an instanced glTF model in meters in the
     * model's local coordinate space.
     *
     * Note that this bounding box can change over time, for example, if the glTF model contains
     * animations that alter the bounds of the geometry. There is currently no listener mechanism to
     * be notified of such changes. Follow b/451424385 for updates on making this observable.
     *
     * @return A [BoundingBox] object representing the model's bounding box. The
     *   [BoundingBox.center] defines the geometric center of the box, and the
     *   [BoundingBox.halfExtents] defines the distance from the center to each face. The total size
     *   of the box is twice the half-extent. All values are in meters.
     */
    public val gltfModelBoundingBox: BoundingBox

    /** Returns a list of all animations in the model. */
    public val animations: List<GltfAnimationFeature>

    /**
     * Enable/disable the collider for the glTF entity.
     *
     * @param enabled Whether the collider should be enabled.
     */
    public fun setColliderEnabled(enabled: Boolean)

    /**
     * Adds a listener to observe the glTF entity's AA-bounds updates.
     *
     * @param listener The listener to add.
     */
    public fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>)

    /**
     * Removes provided listener from registered listeners of glTF entity's AA-bounds updates.
     *
     * @param listener The listener to remove.
     */
    public fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>)

    /**
     * Enable/disable the reform affordances for glTF entity.
     *
     * @param enabled Whether the reform affordances should be enabled.
     * @param systemMovable Whether the entity should be movable by the system.
     */
    public fun setReformAffordanceEnabled(enabled: Boolean, systemMovable: Boolean)

    /** Specifies the current animation state of the [GltfEntity]. */
    public annotation class AnimationStateValue

    /** Specifies the current animation state of the [GltfEntity]. */
    public object AnimationState {
        public const val PLAYING: Int = 0
        public const val STOPPED: Int = 1
        public const val PAUSED: Int = 2
    }
}
