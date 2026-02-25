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

package androidx.xr.scenecore.spatial.rendering

import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl
import androidx.xr.scenecore.impl.impress.ImpressNode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GltfModelNodeFeatureImplTest {

    private val fakeImpressApi = FakeImpressApiImpl()
    private lateinit var modelImpressSubNode: ImpressNode
    private lateinit var modelImpressNode: ImpressNode
    private lateinit var nodeFeature: GltfModelNodeFeatureImpl
    private val nodeName = "test_node"

    @Before
    fun setUp() {
        modelImpressSubNode = fakeImpressApi.createImpressNode()
        modelImpressNode = fakeImpressApi.createImpressNode()
        nodeFeature =
            GltfModelNodeFeatureImpl(
                fakeImpressApi,
                modelImpressSubNode,
                modelImpressNode,
                nodeName,
            )
    }

    @Test
    fun name_returnsCorrectName() {
        assertThat(nodeFeature.name).isEqualTo(nodeName)
    }

    @Test
    fun localPose_get_returnsIdentityInitially() {
        val pose = nodeFeature.localPose

        assertThat(pose.translation).isEqualTo(Vector3(0f, 0f, 0f))
        assertThat(pose.rotation).isEqualTo(Quaternion(0f, 0f, 0f, 1f))
    }

    @Test
    fun localPose_set_updatesTransformAndSchedulesReskinning() {
        val newPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 1f, 0f, 0f))

        nodeFeature.localPose = newPose

        val outTransform = fakeImpressApi.getImpressNodeLocalTransform(modelImpressSubNode)

        assertThat(outTransform.pose.translation).isEqualTo(newPose.translation)
        assertThat(outTransform.pose.rotation).isEqualTo(newPose.rotation)

        val nodes = fakeImpressApi.getImpressNodes()
        val nodeData = nodes.keys.first { it.entityId == modelImpressNode.handle }
        assertThat(nodeData.isReskinningScheduled).isTrue()
    }

    @Test
    fun localPose_set_preservesExistingScale() {
        val initialScale = Vector3(2f, 3f, 4f)
        val initialMatrix = Matrix4.fromTrs(Vector3.Zero, Quaternion.Identity, initialScale)
        fakeImpressApi.setImpressNodeLocalTransform(modelImpressSubNode, initialMatrix)

        nodeFeature.localPose = Pose(Vector3(10f, 20f, 30f), Quaternion.Identity)

        val outTransform = fakeImpressApi.getImpressNodeLocalTransform(modelImpressSubNode)
        assertThat(outTransform.scale).isEqualTo(initialScale)
    }

    @Test
    fun localScale_get_returnsIdentityInitially() {
        assertThat(nodeFeature.localScale).isEqualTo(Vector3(1f, 1f, 1f))
    }

    @Test
    fun localScale_set_updatesScaleAndSchedulesReskinning() {
        val newScale = Vector3(0.5f, 2.0f, 3.0f)
        nodeFeature.localScale = newScale

        val outTransform = fakeImpressApi.getImpressNodeLocalTransform(modelImpressSubNode)
        assertThat(outTransform.scale).isEqualTo(newScale)

        val nodes = fakeImpressApi.getImpressNodes()
        val rootNodeData = nodes.keys.first { it.entityId == modelImpressNode.handle }
        assertThat(rootNodeData.isReskinningScheduled).isTrue()
    }

    @Test
    fun modelPose_get_returnsIdentityInitially() {
        val pose = nodeFeature.modelPose

        assertThat(pose.translation).isEqualTo(Vector3(0f, 0f, 0f))
        assertThat(pose.rotation).isEqualTo(Quaternion(0f, 0f, 0f, 1f))
    }

    @Test
    fun modelPose_set_updatesRelativeTransformAndSchedulesReskinning() {
        val newPose = Pose(Vector3(5f, 5f, 5f), Quaternion.Identity)

        nodeFeature.modelPose = newPose

        val outTransform =
            fakeImpressApi.getImpressNodeRelativeTransform(modelImpressSubNode, modelImpressNode)
        assertThat(outTransform.pose.translation).isEqualTo(newPose.translation)

        val nodes = fakeImpressApi.getImpressNodes()
        val rootNodeData = nodes.keys.first { it.entityId == modelImpressNode.handle }
        assertThat(rootNodeData.isReskinningScheduled).isTrue()
    }

    @Test
    fun modelPose_set_preservesExistingModelScale() {
        val initialScale = Vector3(9f, 9f, 9f)
        val initialMatrix = Matrix4.fromTrs(Vector3.Zero, Quaternion.Identity, initialScale)
        fakeImpressApi.setImpressNodeRelativeTransform(
            modelImpressSubNode,
            modelImpressNode,
            initialMatrix,
        )

        nodeFeature.modelPose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)

        val outTransform =
            fakeImpressApi.getImpressNodeRelativeTransform(modelImpressSubNode, modelImpressNode)
        assertThat(outTransform.scale).isEqualTo(initialScale)
    }

    @Test
    fun modelScale_get_returnsIdentityInitially() {
        assertThat(nodeFeature.modelScale).isEqualTo(Vector3(1f, 1f, 1f))
    }

    @Test
    fun modelScale_set_updatesModelScaleAndSchedulesReskinning() {
        val newScale = Vector3(4f, 4f, 4f)

        nodeFeature.modelScale = newScale

        val outTransform =
            fakeImpressApi.getImpressNodeRelativeTransform(modelImpressSubNode, modelImpressNode)
        assertThat(outTransform.scale).isEqualTo(newScale)

        val nodes = fakeImpressApi.getImpressNodes()
        val rootNodeData = nodes.keys.first { it.entityId == modelImpressNode.handle }
        assertThat(rootNodeData.isReskinningScheduled).isTrue()
    }

    @Test
    fun setMaterialOverride_callsApiWithCorrectNodeId() = runBlocking {
        val material = fakeImpressApi.createWaterMaterial(false)
        val primIndex = 1

        nodeFeature.setMaterialOverride(material, primIndex)

        val nodes = fakeImpressApi.getImpressNodes()
        val nodeData = nodes.keys.first { it.entityId == modelImpressSubNode.handle }
        assertThat(nodeData.nodeMaterialOverrides).containsKey(primIndex)
        assertThat(nodeData.nodeMaterialOverrides[primIndex]?.materialHandle)
            .isEqualTo(material.nativeHandle)
    }

    @Test
    fun clearMaterialOverride_callsApiWithCorrectNodeId() = runBlocking {
        val material = fakeImpressApi.createWaterMaterial(false)
        val primIndex = 2
        fakeImpressApi.setGltfModelNodeMaterialOverride(
            modelImpressSubNode,
            material.nativeHandle,
            primIndex,
        )

        nodeFeature.clearMaterialOverride(primIndex)

        val nodes = fakeImpressApi.getImpressNodes()
        val nodeData = nodes.keys.first { it.entityId == modelImpressSubNode.handle }
        assertThat(nodeData.nodeMaterialOverrides).doesNotContainKey(primIndex)
    }

    @Test
    fun name_returnsNull_whenInputIsEmpty() {
        val feature =
            GltfModelNodeFeatureImpl(fakeImpressApi, modelImpressSubNode, modelImpressNode, "")
        assertThat(feature.name).isNull()
    }
}
