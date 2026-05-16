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

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.NodeHolder
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeMeshFeatureTest {

    private lateinit var underTest: FakeMeshFeature

    private val nodeHolder: NodeHolder<*> =
        NodeHolder<FakeNode>(object : FakeNode {}, FakeNode::class.java)

    private val initialMaterial = FakeResource()
    private val initialBoneCount = 1

    @Before
    fun setUp() {
        underTest =
            FakeMeshFeature(nodeHolder = nodeHolder, initialMaterials = listOf(initialMaterial))
                .apply { this.boneCount = initialBoneCount }
    }

    @Test
    fun verifyInitialStatus() {
        assertThat(underTest.materials).hasSize(1)
        assertThat(underTest.meshBoundingBox)
            .isEqualTo(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        assertThat(underTest.materials[0]).isSameInstanceAs(initialMaterial)
        assertThat(underTest.boneCount).isEqualTo(initialBoneCount)
        assertThat(underTest.meshEntity).isNull()
        assertThat(underTest.reformAffordanceEnabled).isFalse()
        assertThat(underTest.executor).isNull()
        assertThat(underTest.systemMovable).isFalse()
    }

    @Test
    fun setMaterial_setsCorrectly() {
        val subsetIndex = 0
        val expectedResource = FakeResource()

        underTest.setMaterial(expectedResource, subsetIndex)

        assertThat(underTest.materials[subsetIndex]).isSameInstanceAs(expectedResource)
    }

    @Test
    fun setMaterial_withOutOfBoundsIndex_isIgnored() {
        val outOfBoundsIndex = 5
        val newResource = FakeResource(1)
        val initialSize = underTest.materials.size

        underTest.setMaterial(newResource, outOfBoundsIndex)

        assertThat(underTest.materials).hasSize(initialSize)
        assertThat(underTest.materials[0]).isSameInstanceAs(initialMaterial)
        assertThat(underTest.materials).doesNotContain(newResource)
    }

    @Test
    fun setBoneTransforms_withMoreTransformsThanBoneCount_ignoresExtraTransforms() {
        val transform1 = Matrix4.Identity
        val transform2 = Matrix4.Identity
        val transform3 = Matrix4.Identity

        val transforms = listOf(transform1, transform2, transform3)

        underTest.setBoneTransforms(transforms)

        assertThat(underTest.boneTransforms).hasSize(1)
        assertThat(underTest.boneTransforms).containsExactly(transform1).inOrder()
    }

    @Test
    fun setBoneTransforms_respectsUpdatedBoneCount() {
        underTest.boneCount = 2

        val transform1 = Matrix4.Identity
        val transform2 = Matrix4.Identity
        val transform3 = Matrix4.Identity
        val transforms = listOf(transform1, transform2, transform3)

        underTest.setBoneTransforms(transforms)

        assertThat(underTest.boneTransforms).hasSize(2)
        assertThat(underTest.boneTransforms).containsExactly(transform1, transform2).inOrder()
    }

    @Test
    fun setReformAffordanceEnabled_updatesProperties() {
        val fakeEntity = FakeMeshEntity(underTest)
        val fakeExecutor = Executor { it.run() }
        val isEnabled = true
        val isSystemMovable = true

        underTest.setReformAffordanceEnabled(
            entity = fakeEntity,
            enabled = isEnabled,
            executor = fakeExecutor,
            systemMovable = isSystemMovable,
        )

        assertThat(underTest.meshEntity).isSameInstanceAs(fakeEntity)
        assertThat(underTest.reformAffordanceEnabled).isEqualTo(isEnabled)
        assertThat(underTest.executor).isSameInstanceAs(fakeExecutor)
        assertThat(underTest.systemMovable).isEqualTo(isSystemMovable)
    }
}
