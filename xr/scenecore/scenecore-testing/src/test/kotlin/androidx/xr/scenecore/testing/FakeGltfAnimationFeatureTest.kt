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

import androidx.xr.scenecore.runtime.GltfEntity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeGltfAnimationFeatureTest {

    private lateinit var underTest: FakeGltfAnimationFeature

    @Before
    fun setUp() {
        underTest =
            FakeGltfAnimationFeature(
                animationName = "test_anim",
                animationIndex = 1,
                animationDuration = 5.0f,
            )
    }

    @Test
    fun getInitialState_returnsDefaultValues() {
        assertThat(underTest.animationName).isEqualTo("test_anim")
        assertThat(underTest.animationIndex).isEqualTo(1)
        assertThat(underTest.animationDuration).isEqualTo(5.0f)
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
        assertThat(underTest.isLooping).isFalse()
        assertThat(underTest.speed).isEqualTo(1.0f)
        assertThat(underTest.seekStartTimeSeconds).isEqualTo(0.0f)
    }

    @Test
    fun startAnimation_setsStateCorrectly() {
        underTest.startAnimation(loop = true, speed = 2.0f, seekStartTimeSeconds = 1.5f)

        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        assertThat(underTest.isLooping).isTrue()
        assertThat(underTest.speed).isEqualTo(2.0f)
        assertThat(underTest.seekStartTimeSeconds).isEqualTo(1.5f)
    }

    @Test
    fun stopAnimation_resetsState() {
        underTest.startAnimation(loop = true, speed = 1.0f, seekStartTimeSeconds = 0.0f)
        underTest.stopAnimation()

        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
        assertThat(underTest.isLooping).isFalse()
    }

    @Test
    fun pauseAndResumeAnimation_updatesState() {
        underTest.startAnimation(loop = false, speed = 1.0f, seekStartTimeSeconds = 0.0f)

        underTest.pauseAnimation()
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PAUSED)

        underTest.resumeAnimation()
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
    }

    @Test
    fun seekAnimation_updatesSeekTime() {
        underTest.seekAnimation(10.0f)
        assertThat(underTest.seekStartTimeSeconds).isEqualTo(10.0f)
    }

    @Test
    fun setAnimationSpeed_updatesSpeed() {
        underTest.setAnimationSpeed(0.5f)
        assertThat(underTest.speed).isEqualTo(0.5f)
    }

    @Test
    fun addAnimationStateListener_invokesListener() {
        var lastState = -1
        val listener = Consumer<Int> { state -> lastState = state }
        val executor = Executor { it.run() }

        underTest.addAnimationStateListener(executor, listener)

        underTest.startAnimation(loop = true, speed = 1.0f, seekStartTimeSeconds = 0.0f)
        assertThat(lastState).isEqualTo(GltfEntity.AnimationState.PLAYING)

        underTest.pauseAnimation()
        assertThat(lastState).isEqualTo(GltfEntity.AnimationState.PAUSED)

        underTest.stopAnimation()
        assertThat(lastState).isEqualTo(GltfEntity.AnimationState.STOPPED)
    }

    @Test
    fun removeAnimationStateListener_removesListener() {
        var lastState = -1
        val listener = Consumer<Int> { state -> lastState = state }
        val executor = Executor { it.run() }

        underTest.addAnimationStateListener(executor, listener)
        underTest.removeAnimationStateListener(listener)

        underTest.startAnimation(loop = true, speed = 1.0f, seekStartTimeSeconds = 0.0f)
        assertThat(lastState).isEqualTo(-1) // Should not have been updated
    }
}
