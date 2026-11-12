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

package androidx.xr.scenecore.integration.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.GltfModelNode
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Automated integration tests for SceneCore glTF node hierarchy and transformation matrices. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class GltfNodeHierarchyTransformTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun nodeHierarchy_inspection_matchesGltfAssetSchema() {
        sessionRule.requestFullSpace()
        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", DRAGON_MODEL_ASSET))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )

            val nodes = entity.nodes

            // The flattened list must match the source asset's `nodes` array in size and order.
            assertThat(nodes).hasSize(DRAGON_NODE_COUNT)
            assertThat(nodes[HEAD_NODE_INDEX].name).isEqualTo(HEAD_NODE_NAME)
            assertThat(nodes[NECK_NODE_INDEX].name).isEqualTo(NECK_NODE_NAME)
            assertThat(nodes[TORSO_NODE_INDEX].name).isEqualTo(TORSO_NODE_NAME)
            assertThat(nodes[ROOT_NODE_INDEX].name).isEqualTo(ROOT_NODE_NAME)
            assertThat(nodes[DRAGON_NODE_INDEX].name).isEqualTo(DRAGON_NODE_NAME)
            assertThat(nodes[ARMATURE_NODE_INDEX].name).isEqualTo(ARMATURE_NODE_NAME)
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun nodeTransforms_localVsModelAccumulation_computesCorrectly() {
        sessionRule.requestFullSpace()
        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", DRAGON_MODEL_ASSET))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )

            // `Head` is a child of `Neck` in the source asset, so its local and model
            // transforms differ and the model transform accumulates the parent chain.
            val head = entity.nodes[HEAD_NODE_INDEX]
            val neck = entity.nodes[NECK_NODE_INDEX]
            assertThat(head.name).isEqualTo(HEAD_NODE_NAME)
            assertThat(neck.name).isEqualTo(NECK_NODE_NAME)

            val initialLocalRotation = head.localPose.rotation
            val targetLocalPose =
                Pose(translation = Vector3(0.5f, 0.2f, -0.3f), rotation = initialLocalRotation)
            head.localPose = targetLocalPose
            assertPose(head.localPose, targetLocalPose, 1e-4f)
            // Changing the local pose must be reflected in the accumulated model pose.
            assertModelPoseAccumulatesFromParent(child = head, parent = neck)

            val targetLocalScale = Vector3(1.2f, 1.2f, 1.2f)
            head.localScale = targetLocalScale
            assertVector3(head.localScale, targetLocalScale, 1e-4f)

            val initialModelRotation = head.modelPose.rotation
            val targetModelPose =
                Pose(translation = Vector3(1.0f, 0.0f, -2.0f), rotation = initialModelRotation)
            head.modelPose = targetModelPose
            assertPose(head.modelPose, targetModelPose, 1e-4f)
            // Setting the model pose must recompute the local pose relative to `Neck`.
            assertModelPoseAccumulatesFromParent(child = head, parent = neck)

            val targetModelScale = Vector3(2.0f, 2.0f, 2.0f)
            head.modelScale = targetModelScale
            assertVector3(head.modelScale, targetModelScale, 1e-4f)
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun nodeTransforms_modifiedWhileEntityDisabled_persistsWhenEnabled() {
        sessionRule.requestFullSpace()
        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", DRAGON_MODEL_ASSET))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )

            entity.setEnabled(false)

            // `Head` is a non-root node (child of `Neck`).
            val node = entity.nodes[HEAD_NODE_INDEX]
            assertThat(node.name).isEqualTo(HEAD_NODE_NAME)
            val initialRotation = node.localPose.rotation
            val hiddenPose =
                Pose(translation = Vector3(0.3f, 0.4f, 0.5f), rotation = initialRotation)
            node.localPose = hiddenPose
            val hiddenModelPose = node.modelPose

            val entityNewPose = Pose(translation = Vector3(1f, 2f, 3f))
            entity.setPose(entityNewPose)

            entity.setEnabled(true)
            assertThat(entity.isEnabled(includeParents = false)).isTrue()

            assertPose(node.localPose, hiddenPose, 1e-4f)
            assertPose(node.modelPose, hiddenModelPose, 1e-4f)
            assertPose(entity.getPose(), entityNewPose, 1e-4f)
        }
    }

    /**
     * Asserts that [child]'s model pose equals [parent]'s model transform applied to [child]'s
     * local pose. The parent's model scale is applied to the child's local translation (TRS order),
     * and rotations are compared up to quaternion sign since both are decomposed from Impress
     * matrices. Must be called on the main thread, i.e. inside `sessionRule.onMainThread { }`.
     */
    private fun assertModelPoseAccumulatesFromParent(child: GltfModelNode, parent: GltfModelNode) {
        val parentModelPose = parent.modelPose
        val parentModelScale = parent.modelScale
        val childLocalPose = child.localPose

        val expectedTranslation =
            parentModelPose.translation +
                parentModelPose.rotation * childLocalPose.translation.scale(parentModelScale)
        val expectedRotation = parentModelPose.rotation * childLocalPose.rotation

        val actualModelPose = child.modelPose
        assertVector3(actualModelPose.translation, expectedTranslation, ACCUMULATION_EPSILON)
        assertRotationEquivalent(actualModelPose.rotation, expectedRotation, ACCUMULATION_EPSILON)
    }

    /** Asserts that [actual] and [expected] are the same rotation (`q` and `-q` are equivalent). */
    private fun assertRotationEquivalent(actual: Quaternion, expected: Quaternion, epsilon: Float) {
        val aligned = if ((actual dot expected) < 0f) -expected else expected
        assertThat(actual.x).isWithin(epsilon).of(aligned.x)
        assertThat(actual.y).isWithin(epsilon).of(aligned.y)
        assertThat(actual.z).isWithin(epsilon).of(aligned.z)
        assertThat(actual.w).isWithin(epsilon).of(aligned.w)
    }

    private companion object {
        const val DRAGON_MODEL_ASSET = "Dragon_Evolved.gltf"

        // Node layout of `models/Dragon_Evolved.gltf` (`nodes` array of the source asset).
        const val DRAGON_NODE_COUNT = 48
        const val HEAD_NODE_INDEX = 0
        const val HEAD_NODE_NAME = "Head"
        const val NECK_NODE_INDEX = 1 // Parent of `Head`.
        const val NECK_NODE_NAME = "Neck"
        const val TORSO_NODE_INDEX = 40
        const val TORSO_NODE_NAME = "Torso"
        const val ROOT_NODE_INDEX = 45
        const val ROOT_NODE_NAME = "Root"
        const val DRAGON_NODE_INDEX = 46
        const val DRAGON_NODE_NAME = "Dragon"
        const val ARMATURE_NODE_INDEX = 47 // Scene root node.
        const val ARMATURE_NODE_NAME = "CharacterArmature"

        /** Tolerance for comparisons that compose several decomposed Impress matrices. */
        const val ACCUMULATION_EPSILON = 1e-3f
    }
}
