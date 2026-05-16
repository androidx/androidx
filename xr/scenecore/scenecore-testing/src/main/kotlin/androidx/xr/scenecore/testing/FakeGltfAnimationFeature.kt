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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.testing.internal.FakeGltfAnimationFeature as InternalFakeGltfAnimationFeature
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfAnimationFeature] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeGltfAnimationFeature
internal constructor(
    override val animationName: String? = "animation_name",
    override val animationIndex: Int = 0,
    override val animationDuration: Float = 1.0f,
    internal val fakeInternal: InternalFakeGltfAnimationFeature,
) : GltfAnimationFeature {

    public constructor(
        animationName: String? = "animation_name",
        animationIndex: Int = 0,
        animationDuration: Float = 1.0f,
    ) : this(
        animationName,
        animationIndex,
        animationDuration,
        InternalFakeGltfAnimationFeature(animationName, animationIndex, animationDuration),
    )

    override val animationState: Int
        get() = fakeInternal.animationState

    public val isLooping: Boolean
        get() = fakeInternal.isLooping

    public val speed: Float
        get() = fakeInternal.speed

    public val seekStartTimeSeconds: Float
        get() = fakeInternal.seekStartTimeSeconds

    override fun startAnimation(loop: Boolean, speed: Float?, seekStartTimeSeconds: Float?) {
        fakeInternal.startAnimation(loop, speed, seekStartTimeSeconds)
    }

    override fun stopAnimation() {
        fakeInternal.stopAnimation()
    }

    override fun pauseAnimation() {
        fakeInternal.pauseAnimation()
    }

    override fun resumeAnimation() {
        fakeInternal.resumeAnimation()
    }

    override fun seekAnimation(startTime: Float) {
        fakeInternal.seekAnimation(startTime)
    }

    override fun setAnimationSpeed(speed: Float) {
        fakeInternal.setAnimationSpeed(speed)
    }

    override fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>) {
        fakeInternal.addAnimationStateListener(executor, listener)
    }

    override fun removeAnimationStateListener(listener: Consumer<Int>) {
        fakeInternal.removeAnimationStateListener(listener)
    }
}
