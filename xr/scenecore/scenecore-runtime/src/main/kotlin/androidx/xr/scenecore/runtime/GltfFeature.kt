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
import java.util.function.Consumer

/** Provide the rendering implementation for [GltfEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface GltfFeature : RenderingFeature {

    /** The flattened list of all nodes contained within this glTF model. */
    @get:MainThread public val nodes: List<GltfModelNodeFeature>

    /**
     * The unscaled size of the glTF model's axis-aligned bounding box in the entity's local space,
     * in meters.
     *
     * @return A [FloatSize3d] object representing the width (x-axis), height (y-axis), and depth
     *   (z-axis).
     */
    @get:MainThread public val size: FloatSize3d

    /** Returns the animations of the glTF model. */
    @MainThread public fun getAnimations(executor: Executor): List<GltfAnimationFeature>

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

    /* Pause the animation of the glTF entity. */
    @MainThread public fun pauseAnimation()

    /* Resume the animation of the glTF entity. */
    @MainThread public fun resumeAnimation()

    /**
     * Sets whether the collider is enabled.
     *
     * @param enableCollider Whether the collider is enabled.
     */
    @MainThread public fun setColliderEnabled(enableCollider: Boolean)

    /**
     * Adds a listener that will be called whenever the animation state of the glTF is updated.
     *
     * @param executor The executor to run the listener on.
     * @param listener The listener that will be called when the animation state changes.
     */
    @MainThread public fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>)

    /** Removes an animation state updated listener. */
    @MainThread public fun removeAnimationStateListener(listener: Consumer<Int>)

    /**
     * Registers a listener to be notified of changes to the GLTF model's bounds.
     *
     * The listener is invoked on the main thread for each frame that the entity's animation is in
     * the [GltfEntity.AnimationState.PLAYING] state and the bounds has changed since the last
     * frame. To conserve resources, updates are only processed while an animation is actively
     * playing.
     *
     * When the first listener is added, a frame listener is registered with the underlying
     * renderer.
     *
     * @param listener The consumer to be invoked with the updated [BoundingBox].
     */
    @MainThread public fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>)

    /**
     * Unregisters a previously added bounds listener.
     *
     * If this is the last registered listener, the feature will stop monitoring for bounds changes
     * on each frame to conserve resources by unregistering its frame listener from the renderer.
     *
     * @param listener The listener to remove.
     */
    @MainThread public fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>)

    /**
     * Adds reform affordance to the passed GltfEntity.
     *
     * @param entity The GltfEntity to attach the reform affordance to.
     * @param enabled Whether the affordance is enabled.
     * @param executor The executor to run the listener on.
     * @param systemMovable Whether the system should handle move events.
     */
    @MainThread
    public fun setReformAffordanceEnabled(
        entity: GltfEntity,
        enabled: Boolean,
        executor: Executor,
        systemMovable: Boolean,
    )
}
