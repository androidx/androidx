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

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.GltfEntity as RtGltfEntity
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.Collections
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * GltfModelEntity is a concrete implementation of Entity that hosts a glTF model.
 *
 * Note: The size property of this Entity is always reported as {0, 0, 0}, regardless of the actual
 * size of the model.
 */
public class GltfModelEntity
private constructor(rtEntity: RtGltfEntity, entityManager: EntityManager) :
    BaseEntity<RtGltfEntity>(rtEntity, entityManager) {
    // TODO: b/417750821 - Add an OnAnimationEvent() Listener interface

    private val mAnimationStateListeners: MutableMap<Consumer<AnimationState>, Executor> =
        Collections.synchronizedMap(mutableMapOf())

    /** Specifies the current animation state of the GltfModelEntity. */
    public class AnimationState private constructor(private val name: String) {

        public companion object {
            /** The animation is currently playing. */
            @JvmField public val PLAYING: AnimationState = AnimationState("PLAYING")
            /** The animation is currently stopped. */
            @JvmField public val STOPPED: AnimationState = AnimationState("STOPPED")
        }

        public override fun toString(): String = name
    }

    /**
     * The current animation state of the GltfModelEntity.
     *
     * @return The current animation state.
     */
    public val animationState: AnimationState
        get() {
            checkNotDisposed()
            return when (rtEntity!!.animationState) {
                RtGltfEntity.AnimationState.PLAYING -> AnimationState.PLAYING
                RtGltfEntity.AnimationState.STOPPED -> AnimationState.STOPPED
                else -> AnimationState.STOPPED
            }
        }

    public companion object {
        /**
         * Factory method for GltfModelEntity.
         *
         * @param sceneRuntime SceneRuntime.
         * @param renderingRuntime RenderingRuntime.
         * @param model [GltfModel] which this entity will display.
         * @param pose Pose for this [GltfModelEntity], relative to its parent.
         */
        internal fun create(
            sceneRuntime: SceneRuntime,
            renderingRuntime: RenderingRuntime,
            entityManager: EntityManager,
            model: GltfModel,
            pose: Pose = Pose.Identity,
        ): GltfModelEntity =
            GltfModelEntity(
                renderingRuntime.createGltfEntity(pose, model.model, sceneRuntime.activitySpace),
                entityManager,
            )

        /**
         * Public factory function for a [GltfModelEntity].
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session [Session] to create the [GltfModel] in.
         * @param model The [GltfModel] this [Entity] is referencing.
         * @param pose The initial [Pose] of the [Entity].
         */
        @MainThread
        @JvmStatic
        @JvmOverloads
        public fun create(
            session: Session,
            model: GltfModel,
            pose: Pose = Pose.Identity,
        ): GltfModelEntity =
            create(
                session.sceneRuntime,
                session.renderingRuntime,
                session.scene.entityManager,
                model,
                pose,
            )
    }

    /**
     * Starts the animation with the given name. Only one animation can be playing at a time.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param loop If true, the animation plays in a loop indefinitely until [stopAnimation] is
     *   called. If false, the animation plays once and then stops.
     * @param animationName The name of the animation to start.
     * @throws IllegalArgumentException if the underlying model doesn't contain an animation with
     *   the given name.
     */
    @MainThread
    public fun startAnimation(loop: Boolean, animationName: String) {
        checkNotDisposed()
        try {
            rtEntity!!.startAnimation(loop, animationName)
        } catch (_: Exception) {
            throw IllegalArgumentException("Animation name is invalid.")
        }
    }

    /**
     * Starts animating the glTF with the first animation found in the model.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param loop Whether the animation should loop over or stop after animating once. Defaults to
     *   true.
     * @throws IllegalArgumentException if the underlying model doesn't contain any animations.
     */
    @MainThread
    @JvmOverloads
    public fun startAnimation(loop: Boolean = true) {
        checkNotDisposed()
        try {
            rtEntity!!.startAnimation(loop, null)
        } catch (_: Exception) {
            throw IllegalArgumentException("Model doesn't contain any animations.")
        }
    }

    /**
     * Stops the currently active animation.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     */
    @MainThread
    public fun stopAnimation() {
        checkNotDisposed()
        rtEntity!!.stopAnimation()
    }

    /**
     * Sets a material override for a primitive of a node within the glTF graph.
     *
     * This function searches for the first node in the glTF scene graph with a matching [nodeName].
     * The override is then applied to a primitive of that node at the specified [primitiveIndex].
     *
     * @param material The new [Material] to apply to the primitive.
     * @param nodeName The name of the node as defined in the glTF graph, containing the primitive
     *   to override.
     * @param primitiveIndex The zero-based index for the primitive of the specified node, as
     *   defined in the glTF graph. Default is the first primitive of that node.
     * @throws IllegalArgumentException if the provided [material] is invalid or if no node with the
     *   given [nodeName] is found in the model.
     * @throws IndexOutOfBoundsException if the [primitiveIndex] is out of bounds.
     */
    @JvmOverloads
    @MainThread
    public fun setMaterialOverride(material: Material, nodeName: String, primitiveIndex: Int = 0) {
        checkNotDisposed()
        rtEntity!!.setMaterialOverride(material.material!!, nodeName, primitiveIndex)
    }

    /**
     * Clears a previously set material override for a specific primitive of a node within the glTF
     * graph.
     *
     * If no override was previously set for that primitive, this call has no effect.
     *
     * @param nodeName The name of the node containing the primitive whose material override will be
     *   cleared.
     * @param primitiveIndex The zero-based index for the primitive of the specified node, as
     *   defined in the glTF graph. Default is the first primitive of that node.
     * @throws IllegalArgumentException if the provided [Material] is invalid or if no node with the
     *   given [nodeName] is found in the model.
     * @throws IndexOutOfBoundsException if the [primitiveIndex] is out of bounds.
     */
    @JvmOverloads
    @MainThread
    public fun clearMaterialOverride(nodeName: String, primitiveIndex: Int = 0) {
        checkNotDisposed()
        rtEntity!!.clearMaterialOverride(nodeName, primitiveIndex)
    }

    /**
     * Retrieves the axis-aligned bounding box (AABB) of an instanced glTF model in meters in the
     * model's local coordinate space.
     *
     * @return A [BoundingBox] object representing the model's bounding box. The
     *   [BoundingBox.center] defines the geometric center of the box, and the
     *   [BoundingBox.halfExtents] defines the distance from the center to each face. The total size
     *   of the box is twice the half-extent. All values are in meters.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @MainThread
    public fun getGltfModelBoundingBox(): BoundingBox {
        checkNotDisposed()
        return rtEntity!!.getGltfModelBoundingBox()
    }

    /**
     * Registers a listener to be invoked when the animation state of the GltfModelEntity changes.
     *
     * The only intended client is currently XR Compose. See b/457481325.
     *
     * @param executor The executor on which the listener will be invoked.
     * @param listener The listener to be invoked.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun addAnimationStateListener(executor: Executor, listener: Consumer<AnimationState>) {
        checkNotDisposed()
        if (mAnimationStateListeners.isEmpty()) {
            rtEntity!!.addAnimationStateListener(
                executor = Dispatchers.Main.asExecutor(),
                listener = this::onAnimationStateUpdated,
            )
        }
        mAnimationStateListeners[listener] = executor
    }

    /**
     * Registers a listener to be invoked on the main thread when the animation state of the
     * GltfModelEntity changes.
     *
     * The only intended client is currently XR Compose. See b/457481325.
     *
     * @param listener The listener to be invoked.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun addAnimationStateListener(listener: Consumer<AnimationState>) {
        addAnimationStateListener(executor = Dispatchers.Main.asExecutor(), listener = listener)
    }

    /**
     * Unregisters a previously registered animation state update listener.
     *
     * The only intended client is currently XR Compose. See b/457481325.
     *
     * @param listener The listener to be removed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun removeAnimationStateListener(listener: Consumer<AnimationState>) {
        mAnimationStateListeners.remove(listener)
        if (mAnimationStateListeners.isEmpty()) {
            rtEntity?.removeAnimationStateListener(this::onAnimationStateUpdated)
        }
    }

    private fun onAnimationStateUpdated(@RtGltfEntity.AnimationStateValue animationState: Int) {
        val result =
            when (animationState) {
                RtGltfEntity.AnimationState.PLAYING -> AnimationState.PLAYING
                RtGltfEntity.AnimationState.STOPPED -> AnimationState.STOPPED
                else -> AnimationState.STOPPED
            }
        for ((listener, executor) in mAnimationStateListeners.entries) {
            executor.execute { listener.accept(result) }
        }
    }
}
