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

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import java.util.concurrent.Executor

/** Provide the rendering implementation for [GltfEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface GltfFeature : RenderingFeature {

    /**
     * The unscaled size of the glTF model's axis-aligned bounding box in the entity's local space,
     * in meters.
     *
     * @return A [FloatSize3d] object representing the width (x-axis), height (y-axis), and depth
     *   (z-axis).
     */
    @get:MainThread public val size: FloatSize3d

    /** Returns the current animation state of the glTF entity. */
    public val animationState: Int

    /**
     * Retrieves the axis-aligned bounding box (AABB) of an instanced glTF model.
     *
     * The bounding box is defined in the model's local coordinate space, before any transformations
     * (like scaling) from the entity are applied.
     *
     * @return A [BoundingBox] object representing the model's bounding box. The
     *   [BoundingBox.center] defines the geometric center of the box, and the
     *   [BoundingBox.halfExtents] defines the distance from the center to each face. The total size
     *   of the box is twice the half-extent. All values are in meters.
     */
    @MainThread public fun getGltfModelBoundingBox(): BoundingBox

    /**
     * Starts the animation with the given name.
     *
     * @param animationName The name of the animation to start. If null is supplied, will play the
     *   first animation found in the glTF.
     * @param loop Whether the animation should loop.
     * @param executor The Entity's executor to use for the animation.
     */
    @MainThread public fun startAnimation(loop: Boolean, animationName: String?, executor: Executor)

    /** Stops the animation of the glTF entity. */
    @MainThread public fun stopAnimation()

    /**
     * Sets a material override for a specific mesh of a node.
     *
     * @param material The material to use for the mesh primitive.
     * @param nodeName The name of the node containing the mesh to override.
     * @param primitiveIndex The zero-based index of the mesh in the node.
     */
    @MainThread
    public fun setMaterialOverride(
        material: MaterialResource,
        nodeName: String,
        primitiveIndex: Int,
    )

    /**
     * Clears a material override for a specific mesh of a node.
     *
     * @param nodeName The name of the node containing the mesh for which to clear the override.
     * @param primitiveIndex The zero-based index of the mesh in the node.
     */
    @MainThread public fun clearMaterialOverride(nodeName: String, primitiveIndex: Int)

    /**
     * Sets whether the collider is enabled.
     *
     * @param enableCollider Whether the collider is enabled.
     */
    @MainThread public fun setColliderEnabled(enableCollider: Boolean)
}
