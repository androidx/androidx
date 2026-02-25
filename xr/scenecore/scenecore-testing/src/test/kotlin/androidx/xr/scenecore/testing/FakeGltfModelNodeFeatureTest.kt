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

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeGltfModelNodeFeatureTest {

    private lateinit var underTest: FakeGltfModelNodeFeature
    private val nodeName = "test_node"

    @Before
    fun setUp() {
        underTest = FakeGltfModelNodeFeature(name = nodeName)
    }

    @Test
    fun getInitialState_returnsDefaultValues() {
        assertThat(underTest.name).isEqualTo(nodeName)
        assertThat(underTest.localPose).isEqualTo(Pose.Identity)
        assertThat(underTest.localScale).isEqualTo(Vector3(1f, 1f, 1f))
        assertThat(underTest.modelPose).isEqualTo(Pose.Identity)
        assertThat(underTest.modelScale).isEqualTo(Vector3(1f, 1f, 1f))
        assertThat(underTest.materialOverrides).isEmpty()
    }

    @Test
    fun localPose_updatesStateCorrectly() {
        val newPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 1f, 0f, 0f))
        underTest.localPose = newPose
        assertThat(underTest.localPose).isEqualTo(newPose)
    }

    @Test
    fun localScale_updatesStateCorrectly() {
        val newScale = Vector3(2f, 2f, 2f)
        underTest.localScale = newScale
        assertThat(underTest.localScale).isEqualTo(newScale)
    }

    @Test
    fun modelPose_updatesStateCorrectly() {
        val relativePose = Pose(Vector3(10f, 0f, 0f), Quaternion.Identity)
        underTest.modelPose = relativePose
        assertThat(underTest.modelPose).isEqualTo(relativePose)
    }

    @Test
    fun modelScale_updatesStateCorrectly() {
        val relativeScale = Vector3(0.5f, 0.5f, 0.5f)
        underTest.modelScale = relativeScale
        assertThat(underTest.modelScale).isEqualTo(relativeScale)
    }

    @Test
    fun setMaterialOverride_updatesMaterialOverridesMap() {
        val material = FakeResource(123)
        val primitiveIndex = 0

        underTest.setMaterialOverride(material, primitiveIndex)

        assertThat(underTest.materialOverrides[primitiveIndex]).isEqualTo(material)
    }

    @Test
    fun clearMaterialOverride_removesMaterialFromMap() {
        val material = FakeResource(123)
        val primitiveIndex = 0

        underTest.setMaterialOverride(material, primitiveIndex)
        assertThat(underTest.materialOverrides[primitiveIndex]).isEqualTo(material)

        underTest.clearMaterialOverride(primitiveIndex)

        assertThat(underTest.materialOverrides).doesNotContainKey(primitiveIndex)
    }

    @Test
    fun clearMaterialOverrides_removesAllMaterials() {
        val material1 = FakeResource(123)
        val material2 = FakeResource(456)

        underTest.setMaterialOverride(material1, 0)
        underTest.setMaterialOverride(material2, 1)

        assertThat(underTest.materialOverrides).hasSize(2)

        underTest.clearMaterialOverrides()

        assertThat(underTest.materialOverrides).isEmpty()
    }
}
