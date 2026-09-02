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

package androidx.xr.scenecore.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import org.junit.Test
import org.junit.runner.RunWith

/** Automated integration tests for SceneCore glTF node hierarchy and transformation matrices. */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SceneCoreGltfNodeHierarchyTransformTest {

    @Test
    fun nodeHierarchy_inspection_matchesGltfAssetSchema() = runTestWithSession { session ->
        val gltfModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
        val entity =
            GltfModelEntity.create(
                session = session,
                model = gltfModel,
                parent = session.scene.activitySpace,
            )

        val nodes = entity.nodes
        assertThat(nodes).isNotEmpty()

        nodes.forEachIndexed { idx, node -> assertThat(node.index).isEqualTo(idx) }

        val namedNode = nodes.firstOrNull { it.name != null }
        assertThat(namedNode).isNotNull()

        entity.parent = null
    }

    @Test
    fun nodeTransforms_localVsModelAccumulation_computesCorrectly() =
        runTestWithSession { session ->
            val gltfModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )

            val node = entity.nodes.first()

            val targetLocalPose = Pose(translation = Vector3(0.5f, 0.2f, -0.3f))
            node.localPose = targetLocalPose
            assertPose(node.localPose, targetLocalPose, 1e-4f)

            val targetLocalScale = Vector3(1.2f, 1.2f, 1.2f)
            node.localScale = targetLocalScale
            assertVector3(node.localScale, targetLocalScale, 1e-4f)

            val targetModelPose = Pose(translation = Vector3(1.0f, 0.0f, -2.0f))
            node.modelPose = targetModelPose
            assertPose(node.modelPose, targetModelPose, 1e-4f)

            val targetModelScale = Vector3(2.0f, 2.0f, 2.0f)
            node.modelScale = targetModelScale
            assertVector3(node.modelScale, targetModelScale, 1e-4f)

            entity.parent = null
        }

    @Test
    fun nodeTransforms_modifiedWhileEntityDisabled_persistsWhenEnabled() =
        runTestWithSession { session ->
            val gltfModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )

            entity.setEnabled(false)

            val node = entity.nodes.first()
            val hiddenPose = Pose(translation = Vector3(0.3f, 0.4f, 0.5f))
            node.localPose = hiddenPose

            val entityNewPose = Pose(translation = Vector3(1f, 2f, 3f))
            entity.setPose(entityNewPose)

            entity.setEnabled(true)

            assertPose(node.localPose, hiddenPose, 1e-4f)
            assertPose(entity.getPose(), entityNewPose, 1e-4f)

            entity.parent = null
        }
}
