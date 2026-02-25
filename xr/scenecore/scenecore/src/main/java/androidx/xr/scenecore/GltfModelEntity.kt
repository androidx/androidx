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

import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Log
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.GltfEntity as RtGltfEntity
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
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

    @Suppress("DEPRECATION")
    private val mAnimationStateListeners: MutableMap<Consumer<AnimationState>, Executor> =
        Collections.synchronizedMap(mutableMapOf())

    private val _nodes: List<GltfModelNode> by lazy {
        // The unique identifier of a node is their index so we first get the
        // count of the nodes in the model from the native side.
        val features = rtEntity!!.nodes
        val list = ArrayList<GltfModelNode>(features.size)

        for (i in features.indices) {
            // For each node index in the model, query its name from the native side
            // and create a [GltfModelNode]. A node may have no name (`null`).
            val feature = features[i]
            list.add(GltfModelNode(this, feature, i, feature.name))
        }
        list.toList()
    }

    /**
     * A list of all [GltfModelNode]s defined in the [GltfModelEntity]. The list is lazily
     * initialized on the first access.
     *
     * The returned list corresponds to the flattened array of nodes defined in the source glTF
     * file. The order of elements in this list is guaranteed to match the order of nodes in the
     * glTF file's `nodes` array.
     */
    public val nodes: List<GltfModelNode>
        @MainThread
        get() {
            checkNotDisposed()
            return _nodes
        }

    @delegate:RequiresApi(Build.VERSION_CODES.O)
    private val _animations: List<GltfAnimation> by lazy {
        // The unique identifier of an animation is their index so we first get the
        // count of the nodes in the model from the native side.
        val features = rtEntity.animations
        val list = ArrayList<GltfAnimation>(features.size)

        for (i in features.indices) {
            // For each animation index in the model, query its name from the native side
            // and create a [GltfAnimation]. An animation may have no name ("").
            val feature = features[i]
            list.add(
                GltfAnimation(
                    rtGltfEntity = rtEntity,
                    rtGltfAnimation = feature,
                    index = feature.animationIndex,
                    name = feature.animationName,
                    // The animation duration is in seconds [Float]. We convert it to the [Duration]
                    // datatype.
                    duration =
                        java.time.Duration.ofMillis(
                            (feature.animationDuration * TimeUnit.SECONDS.toMillis(1)).toLong()
                        ),
                )
            )
        }
        Collections.unmodifiableList(list)
    }

    /**
     * A list of all [GltfAnimation]s defined in the [GltfModelEntity]. The list is lazily
     * initialized on the first access.
     *
     * The returned list corresponds to the array of animations defined in the source glTF file. The
     * order of elements in this list is guaranteed to match the order of animations in the glTF
     * file's `animations` array.
     */
    @get:RequiresApi(Build.VERSION_CODES.O)
    public val animations: List<GltfAnimation>
        @MainThread
        get() {
            checkNotDisposed()
            return _animations
        }

    /** Specifies the current animation state of the GltfModelEntity. */
    @Deprecated(
        message =
            "Use GltfAnimation.AnimationState instead. Entity-level animation state is deprecated" +
                " since multiple animations can now play simultaneously.",
        replaceWith =
            ReplaceWith("GltfAnimation.AnimationState", "androidx.xr.scenecore.GltfAnimation"),
    )
    public class AnimationState private constructor(private val name: String) {
        @Suppress("DEPRECATION")
        public companion object {
            /** The animation is currently playing. */
            @JvmField public val PLAYING: AnimationState = AnimationState("PLAYING")
            /** The animation is currently stopped. */
            @JvmField public val STOPPED: AnimationState = AnimationState("STOPPED")
            /** The animation is currently paused. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            @JvmField
            public val PAUSED: AnimationState = AnimationState("PAUSED")
        }

        public override fun toString(): String = name
    }

    /**
     * The current animation state of the GltfModelEntity.
     *
     * @return The current animation state.
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message =
            "Entity-level animation state is deprecated. Query the animationState property of" +
                " individual GltfAnimation objects in the 'animations' list instead."
    )
    public val animationState: AnimationState
        get() {
            checkNotDisposed()
            return when (rtEntity!!.animationState) {
                RtGltfEntity.AnimationState.PLAYING -> AnimationState.PLAYING
                RtGltfEntity.AnimationState.STOPPED -> AnimationState.STOPPED
                RtGltfEntity.AnimationState.PAUSED -> AnimationState.PAUSED
                else -> AnimationState.STOPPED
            }
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
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val gltfModelBoundingBox: BoundingBox
        @MainThread
        get() {
            checkNotDisposed()
            return rtEntity!!.gltfModelBoundingBox
        }

    public companion object {
        /**
         * Factory method for GltfModelEntity.
         *
         * @param sceneRuntime SceneRuntime.
         * @param renderingRuntime RenderingRuntime.
         * @param model [GltfModel] which this entity will display.
         * @param pose Pose for this [GltfModelEntity], relative to its parent.
         * @param parent Parent entity. If `null`, the entity is created but not attached to the
         *   scene graph and will not be visible until a parent is set. The default value is
         *   [Scene]'s [ActivitySpace].
         */
        internal fun create(
            sceneRuntime: SceneRuntime,
            renderingRuntime: RenderingRuntime,
            entityManager: EntityManager,
            model: GltfModel,
            pose: Pose = Pose.Identity,
            parent: Entity? = entityManager.getEntityForRtEntity(sceneRuntime.activitySpace),
        ): GltfModelEntity =
            GltfModelEntity(
                renderingRuntime.createGltfEntity(
                    pose,
                    model.model,
                    if (parent != null && parent !is BaseEntity<*>) {
                        Log.warn(
                            "The provided parent is not a BaseEntity. The GltfModelEntity will " +
                                "be created without a parent."
                        )
                        null
                    } else {
                        parent?.rtEntity
                    },
                ),
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

        /**
         * Public factory function for a [GltfModelEntity].
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session [Session] to create the [GltfModel] in.
         * @param model The [GltfModel] this [Entity] is referencing.
         * @param pose The initial [Pose] of the [Entity]. The default value is [Pose.Identity].
         * @param parent Parent entity. If `null`, the entity is created but not attached to the
         *   scene graph and will not be visible until a parent is set. The default value is
         *   [Scene]'s [ActivitySpace].
         */
        @MainThread
        @JvmStatic
        // TODO: b/462865943 - Replace @RestrictTo with @JvmOverloads and remove the other overload
        //  once the API proposal is approved.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun create(
            session: Session,
            model: GltfModel,
            pose: Pose = Pose.Identity,
            parent: Entity? = session.scene.activitySpace,
        ): GltfModelEntity =
            create(
                session.sceneRuntime,
                session.renderingRuntime,
                session.scene.entityManager,
                model,
                pose,
                parent,
            )
    }

    /**
     * Starts the animation with the given name. Only one animation can be playing at a time.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * If this GltfModelEntity currently has an animation that is playing or paused, that animation
     * will be stopped.
     *
     * @param loop If true, the animation plays in a loop indefinitely until [stopAnimation] is
     *   called. If false, the animation plays once and then stops.
     * @param animationName The name of the animation to start.
     * @throws IllegalArgumentException if the underlying model doesn't contain an animation with
     *   the given name.
     */
    @MainThread
    @Deprecated(
        message = "Use GltfAnimation.start() on the specific animation instead.",
        replaceWith =
            ReplaceWith(
                "animations.find { it.name == animationName }?" +
                    ".start(GltfAnimationStartOptions(shouldLoop = loop))",
                "androidx.xr.scenecore.GltfAnimationStartOptions",
            ),
    )
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
    @Deprecated(
        message = "Use GltfAnimation.start() on the specific animation instead.",
        replaceWith =
            ReplaceWith(
                "animations.firstOrNull()?.start(GltfAnimationStartOptions(shouldLoop = loop))",
                "androidx.xr.scenecore.GltfAnimationStartOptions",
            ),
    )
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
    @Deprecated(
        message = "Use GltfAnimation.stop() on individual animations instead.",
        replaceWith = ReplaceWith("animations.forEach { it.stop() }"),
    )
    public fun stopAnimation() {
        checkNotDisposed()
        rtEntity!!.stopAnimation()
    }

    /**
     * Pauses the currently playing animation.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * Use [resumeAnimation] to continue playing. If the AnimationState is not
     * [AnimationState.PLAYING], this method has no effect.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @MainThread
    @Deprecated(
        message = "Use GltfAnimation.pause() on individual animations instead.",
        replaceWith = ReplaceWith("animations.forEach { it.pause() }"),
    )
    public fun pauseAnimation() {
        checkNotDisposed()
        rtEntity!!.pauseAnimation()
    }

    /**
     * Resumes the currently active animation.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * If the AnimationState is not [AnimationState.PAUSED], this method has no effect.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @MainThread
    @Deprecated(
        message = "Use GltfAnimation.resume() on individual animations instead.",
        replaceWith = ReplaceWith("animations.forEach { it.resume() }"),
    )
    public fun resumeAnimation() {
        checkNotDisposed()
        rtEntity!!.resumeAnimation()
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
    @Suppress("DEPRECATION")
    @Deprecated(
        message =
            "Entity-level animation listeners are deprecated. Use" +
                " GltfAnimation.addAnimationStateListener() or removeAnimationStateListener()" +
                " on individual animations instead."
    )
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
    @Suppress("DEPRECATION")
    @Deprecated(
        message =
            "Entity-level animation listeners are deprecated." +
                " Use GltfAnimation.addAnimationStateListener() or" +
                " removeAnimationStateListener() on individual animations instead."
    )
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
    @Suppress("DEPRECATION")
    @Deprecated(
        message =
            "Entity-level animation listeners are deprecated. Use" +
                " GltfAnimation.addAnimationStateListener() or removeAnimationStateListener()" +
                " on individual animations instead."
    )
    public fun removeAnimationStateListener(listener: Consumer<AnimationState>) {
        mAnimationStateListeners.remove(listener)
        if (mAnimationStateListeners.isEmpty()) {
            rtEntity?.removeAnimationStateListener(this::onAnimationStateUpdated)
        }
    }

    @Suppress("DEPRECATION")
    private fun onAnimationStateUpdated(@RtGltfEntity.AnimationStateValue animationState: Int) {
        @Suppress("DEPRECATION")
        val result =
            when (animationState) {
                RtGltfEntity.AnimationState.PLAYING -> AnimationState.PLAYING
                RtGltfEntity.AnimationState.STOPPED -> AnimationState.STOPPED
                RtGltfEntity.AnimationState.PAUSED -> AnimationState.PAUSED
                else -> AnimationState.STOPPED
            }
        for ((listener, executor) in mAnimationStateListeners.entries) {
            executor.execute { listener.accept(result) }
        }
    }
}
