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
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeMeshEntityTest {

    private lateinit var meshFeature: FakeMeshFeature
    private lateinit var underTest: FakeMeshEntity

    private val nodeHolder: NodeHolder<*> =
        NodeHolder<FakeNode>(object : FakeNode {}, FakeNode::class.java)

    private val initialMaterial = FakeResource()
    private val initialBoneCount = 1

    @Before
    fun setUp() {
        meshFeature =
            FakeMeshFeature(nodeHolder = nodeHolder, initialMaterials = listOf(initialMaterial))
                .apply { this.boneCount = initialBoneCount }

        underTest = FakeMeshEntity(meshFeature, directExecutor())
    }

    @Test
    fun verifyInitialStatus() {
        assertThat(underTest.materials).hasSize(1)
        assertThat(underTest.meshBoundingBox)
            .isEqualTo(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        assertThat(underTest.materials[0]).isSameInstanceAs(initialMaterial)
        assertThat(underTest.reformAffordanceEnabled).isFalse()
        assertThat(underTest.systemMovable).isFalse()
    }

    @Test
    fun setMaterial_setsCorrectly() {
        val subsetIndex = 0
        val expectedResource2 = FakeResource()

        underTest.setMaterial(expectedResource2, subsetIndex)

        assertThat(underTest.materials[subsetIndex]).isSameInstanceAs(expectedResource2)
    }

    @Test
    fun setBoneTransforms_updatesTransforms() {
        val transform = Matrix4.Identity
        val transforms = listOf(transform)

        underTest.setBoneTransforms(transforms)

        assertThat(underTest.boneTransforms).hasSize(1)
        assertThat(underTest.boneTransforms).containsExactly(transform).inOrder()
    }

    @Test
    fun setReformAffordanceEnabled_withExecutor_updatesProperties() {
        val isEnabled = true
        val isSystemMovable = true

        underTest.setReformAffordanceEnabled(enabled = isEnabled, systemMovable = isSystemMovable)

        assertThat(underTest.reformAffordanceEnabled).isEqualTo(isEnabled)
        assertThat(underTest.systemMovable).isEqualTo(isSystemMovable)
    }

    @Test
    fun setReformAffordanceEnabled_withoutExecutor_doesNothing() {
        val entityWithoutExecutor = FakeMeshEntity(meshFeature, executor = null)

        entityWithoutExecutor.setReformAffordanceEnabled(enabled = true, systemMovable = true)

        assertThat(entityWithoutExecutor.reformAffordanceEnabled).isFalse()
        assertThat(entityWithoutExecutor.systemMovable).isFalse()
    }
}
