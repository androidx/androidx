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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakeGltfEntity(
    private val feature: GltfFeature? = null,
    private val executor: Executor? = null,
) : FakeEntity(), GltfEntity {
    override val nodes: List<GltfModelNodeFeature>
        get() = feature?.nodes ?: emptyList()

    private val _animationStateListeners = mutableMapOf<Consumer<Int>, Executor?>()

    @GltfEntity.AnimationStateValue
    private var _animationState: Int = GltfEntity.AnimationState.STOPPED
        set(value) {
            field = value
            for ((listener, executor) in _animationStateListeners.entries) {
                if (executor != null) {
                    executor.execute { listener.accept(value) }
                } else {
                    listener.accept(value)
                }
            }
        }

    /** Returns the current animation state of the glTF entity. */
    @GltfEntity.AnimationStateValue
    override val animationState: Int
        get() {
            return feature?.animationState ?: _animationState
        }

    override val gltfModelBoundingBox: BoundingBox =
        BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)

    private val _animations = mutableListOf<GltfAnimationFeature>()

    override val animations: List<GltfAnimationFeature>
        get() = (feature?.getAnimations(executor!!) ?: emptyList()) + _animations

    override fun setColliderEnabled(enabled: Boolean) {
        feature?.setColliderEnabled(enabled)
    }

    override fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        feature?.addOnBoundsUpdateListener(listener)
    }

    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        feature?.removeOnBoundsUpdateListener(listener)
    }

    override fun setReformAffordanceEnabled(enabled: Boolean, systemMovable: Boolean) {
        feature?.setReformAffordanceEnabled(this, enabled, executor!!, systemMovable)
    }

    /**
     * Adds an animation to the list of animations.
     *
     * @param animation The animation to add.
     */
    public fun addAnimation(animation: GltfAnimationFeature) {
        _animations.add(animation)
    }

    /**
     * Indicates whether the animation is currently looping. In tests, you can
     * - call [startAnimation] with loop set to true to simulate looping the animation and verify
     *   that your code responds correctly to the animation looping.
     * - call [stopAnimation] to clear the looping state and verify that your code responds
     *   correctly to the animation stopping.
     */
    public var isLooping: Boolean = false

    /**
     * The name of the animation that is currently playing. In tests, you can
     * - call [startAnimation] with a supported animationName and verify that your code responds
     *   correctly to the animation starting.
     * - call [stopAnimation] to clear the value and verify that your code responds correctly to the
     *   animation stopping.
     */
    public var currentAnimationName: String? = null
        private set

    /**
     * A list of supported animation names with a default value of "animation_name" which is used in
     * the scenecore/JxrPlatformAdapterAxrTest unit test. In tests, you can call [startAnimation]
     * with a supported/unsupported animationName and verify that your code responds correctly to
     * the [isLooping] and [currentAnimationName] values.
     */
    public var supportedAnimationNames: MutableList<String> = mutableListOf("animation_name")

    /**
     * Starts the animation with a supported given name when the animation state is STOPPED.
     *
     * @param currentAnimationName The name of the animation to start. If null is supplied, will
     *   play the first animation found in the glTF.
     * @param loop Whether the animation should loop.
     */
    override fun startAnimation(loop: Boolean, animationName: String?) {
        feature?.startAnimation(loop, animationName, executor!!)
        if (
            supportedAnimationNames.contains(animationName) &&
                (_animationState == GltfEntity.AnimationState.STOPPED ||
                    _animationState == GltfEntity.AnimationState.PAUSED)
        ) {

            _animationState = GltfEntity.AnimationState.PLAYING

            isLooping = loop
            currentAnimationName = animationName
        }
    }

    /** Stops the animation of the glTF entity. */
    override fun stopAnimation() {
        feature?.stopAnimation()
        if (
            _animationState == GltfEntity.AnimationState.PLAYING ||
                _animationState == GltfEntity.AnimationState.PAUSED
        ) {
            _animationState = GltfEntity.AnimationState.STOPPED

            isLooping = false
            currentAnimationName = null
        }
    }

    /* Pause the animation of the glTF entity. */
    override fun pauseAnimation() {
        feature?.pauseAnimation()

        if (_animationState == GltfEntity.AnimationState.PLAYING) {
            _animationState = GltfEntity.AnimationState.PAUSED
        }
    }

    /* Resume the animation of the glTF entity. */
    override fun resumeAnimation() {
        feature?.resumeAnimation()

        if (_animationState == GltfEntity.AnimationState.PAUSED) {
            _animationState = GltfEntity.AnimationState.PLAYING
        }
    }

    override fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>) {
        _animationStateListeners.putIfAbsent(listener, executor)
    }

    override fun removeAnimationStateListener(listener: Consumer<Int>) {
        _animationStateListeners.remove(listener)
    }
}
