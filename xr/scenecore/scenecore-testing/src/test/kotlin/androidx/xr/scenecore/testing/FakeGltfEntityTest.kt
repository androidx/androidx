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

package androidx.xr.scenecore.testing

import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GltfEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeGltfEntityTest {

    lateinit var underTest: FakeGltfEntity

    @Before
    fun setUp() {
        underTest = FakeGltfEntity()
    }

    @Test
    fun getInitialState_returnsDefaultValues() {
        check(underTest.animationState == GltfEntity.AnimationState.STOPPED)
        check(!underTest.isLooping)
        check(underTest.currentAnimationName == null)
        check(underTest.gltfModelBoundingBox.center == Vector3(0.5f, 0.5f, 0.5f))
        check(underTest.gltfModelBoundingBox.halfExtents == FloatSize3d(0.5f, 0.5f, 0.5f))
    }

    @Test
    fun startAnimationWithSupportedAnimation_setsAnimationStateToPlaying() = runBlocking {
        check(underTest.supportedAnimationNames.contains("animation_name"))

        underTest.startAnimation(false, "test_animation")

        // startAnimation doesn't work on an unsupported animation name.
        assertThat(underTest.isLooping).isFalse()
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
        assertThat(underTest.currentAnimationName).isNull()

        underTest.startAnimation(loop = true, animationName = "animation_name")

        assertThat(underTest.isLooping).isTrue()
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        assertThat(underTest.currentAnimationName).isEqualTo("animation_name")

        underTest.supportedAnimationNames.add("test_animation")
        underTest.startAnimation(false, "test_animation")

        // startAnimation doesn't work when the animationState is PLAYING.
        assertThat(underTest.isLooping).isTrue()
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        assertThat(underTest.currentAnimationName).isEqualTo("animation_name")

        underTest.stopAnimation()

        // stopAnimation resets all states.
        assertThat(underTest.isLooping).isFalse()
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
        assertThat(underTest.currentAnimationName).isNull()

        underTest.startAnimation(false, "test_animation")

        // Verifies that startAnimation works on the added supported animation name.
        assertThat(underTest.isLooping).isFalse()
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        assertThat(underTest.currentAnimationName).isEqualTo("test_animation")
    }

    @Test
    fun startAnimationWithSupportedAnimation_setsAnimationStateToPlayingAfterPause() {
        check(underTest.supportedAnimationNames.contains("animation_name"))

        underTest.startAnimation(loop = true, animationName = "animation_name")

        assertThat(underTest.isLooping).isTrue()
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        assertThat(underTest.currentAnimationName).isEqualTo("animation_name")

        underTest.pauseAnimation()

        // Verifies that pauseAnimation works by the animation state.
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PAUSED)

        underTest.resumeAnimation()

        // Verifies that resumeAnimation works by the animation state.
        assertThat(underTest.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
    }

    @Test
    fun getAnimations_returnsEmptyList() {
        assertThat(underTest.animations).isEmpty()
    }
}
