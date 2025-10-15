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

/** Interface for a XR Runtime [GltfEntity]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface GltfEntity : Entity {

    /** Returns the current animation state of the glTF entity. */
    @AnimationStateValue public val animationState: Int

    /**
     * Starts the animation with the given name.
     *
     * @param animationName The name of the animation to start. If null is supplied, will play the
     *   first animation found in the glTF.
     * @param loop Whether the animation should loop.
     */
    public fun startAnimation(loop: Boolean, animationName: String?)

    /** Stops the animation of the glTF entity. */
    public fun stopAnimation()

    /**
     * Sets a material override for a specific mesh of a node.
     *
     * @param material The material to use for the mesh primitive.
     * @param nodeName The name of the node containing the mesh to override.
     * @param primitiveIndex The zero-based index for the mesh of the node.
     */
    public fun setMaterialOverride(
        material: MaterialResource,
        nodeName: String,
        primitiveIndex: Int,
    )

    /**
     * Clears a material override for a specific mesh of a node.
     *
     * @param nodeName The name of the node containing the mesh for which to clear the override.
     * @param primitiveIndex The zero-based index for the mesh of the node.
     */
    public fun clearMaterialOverride(nodeName: String, primitiveIndex: Int)

    // TODO: b/417750821 - Add an OnAnimationFinished() Listener interface
    //                     Add a getAnimationTimeRemaining() interface

    // TODO: b/451424385 -GltfEntity.getGltfModelBoundingBox() becomes a Flow if the bounding box
    //  can change during animation.
    /**
     * Retrieves the axis-aligned bounding box (AABB) of an instanced glTF model in meters in the
     * model's local coordinate space.
     *
     * @return A [BoundingBox] object representing the model's bounding box. The
     *   [BoundingBox.center] defines the geometric center of the box, and the
     *   [BoundingBox.halfExtents] defines the distance from the center to each face. The total size
     *   of the box is twice the half-extent. All values are in meters.
     */
    public fun getGltfModelBoundingBox(): BoundingBox

    /** Specifies the current animation state of the [GltfEntity]. */
    public annotation class AnimationStateValue

    /** Specifies the current animation state of the [GltfEntity]. */
    public object AnimationState {
        public const val PLAYING: Int = 0
        public const val STOPPED: Int = 1
    }
}
