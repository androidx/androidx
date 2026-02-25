/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfAnimationFeature] */
// TODO(b/481429599): Audit usage of LIBRARY_GROUP_PREFIX in SceneCore and migrate it over to
// LIBRARY_GROUP.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeGltfAnimationFeature(
    override val animationName: String? = "animation_name",
    override val animationIndex: Int = 0,
    override val animationDuration: Float = 1.0f,
) : GltfAnimationFeature {

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

    override val animationState: Int
        get() = _animationState

    public var isLooping: Boolean = false
        private set

    public var speed: Float = 1.0f
        private set

    public var seekStartTimeSeconds: Float = 0.0f
        private set

    override fun startAnimation(loop: Boolean, speed: Float?, seekStartTimeSeconds: Float?) {
        isLooping = loop
        this.speed = speed ?: 1.0f
        this.seekStartTimeSeconds = seekStartTimeSeconds ?: 0.0f
        _animationState = GltfEntity.AnimationState.PLAYING
    }

    override fun stopAnimation() {
        _animationState = GltfEntity.AnimationState.STOPPED
        isLooping = false
    }

    override fun pauseAnimation() {
        if (_animationState == GltfEntity.AnimationState.PLAYING) {
            _animationState = GltfEntity.AnimationState.PAUSED
        }
    }

    override fun resumeAnimation() {
        if (_animationState == GltfEntity.AnimationState.PAUSED) {
            _animationState = GltfEntity.AnimationState.PLAYING
        }
    }

    override fun seekAnimation(startTime: Float) {
        this.seekStartTimeSeconds = startTime
    }

    override fun setAnimationSpeed(speed: Float) {
        this.speed = speed
    }

    override fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>) {
        _animationStateListeners[listener] = executor
    }

    override fun removeAnimationStateListener(listener: Consumer<Int>) {
        _animationStateListeners.remove(listener)
    }
}
