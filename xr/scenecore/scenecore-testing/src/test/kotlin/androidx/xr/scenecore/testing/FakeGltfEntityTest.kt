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
import com.google.common.truth.Truth.assertThat
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
        check(underTest.gltfModelBoundingBox.center == Vector3(0.5f, 0.5f, 0.5f))
        check(underTest.gltfModelBoundingBox.halfExtents == FloatSize3d(0.5f, 0.5f, 0.5f))
    }

    @Test
    fun getAnimations_returnsEmptyList() {
        assertThat(underTest.animations).isEmpty()
    }

    @Test
    fun addAnimation_addsAnimationToList() {
        val animation = FakeGltfAnimationFeature()
        underTest.addAnimation(animation)

        assertThat(underTest.animations).containsExactly(animation)
    }

    @Test
    fun getNodes_returnsEmptyListByDefault() {
        assertThat(underTest.nodes).isEmpty()
    }
}
