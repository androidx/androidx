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
        check(underTest.getGltfModelBoundingBox().center == Vector3(0.5f, 0.5f, 0.5f))
        check(underTest.getGltfModelBoundingBox().halfExtents == FloatSize3d(0.5f, 0.5f, 0.5f))
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
    fun setMaterialOverride_setMaterialCorrectly() {
        val material = FakeResource(123)
        val nodeName = "glTF node"
        val primitiveIndex = 0

        check(underTest.node.materialArray[primitiveIndex] == FakeResource(1))

        underTest.setMaterialOverride(material, nodeName, primitiveIndex)

        assertThat(underTest.node.materialArray[primitiveIndex]).isEqualTo(material)
    }

    @Test
    fun clearMaterialOverride_setAndClearOverrideMaterial_getMaterialAfterClearCorrectly() {
        val material = FakeResource(123)
        val nodeName = "glTF node"
        val primitiveIndex = 0

        check(underTest.node.materialArray[primitiveIndex] == FakeResource(1))

        underTest.setMaterialOverride(material, nodeName, primitiveIndex)

        assertThat(underTest.node.materialArray[primitiveIndex]).isEqualTo(material)

        underTest.clearMaterialOverride(nodeName, primitiveIndex)

        assertThat(underTest.node.materialArray[primitiveIndex])
            .isEqualTo(FakeResource(primitiveIndex.toLong()))
    }
}
