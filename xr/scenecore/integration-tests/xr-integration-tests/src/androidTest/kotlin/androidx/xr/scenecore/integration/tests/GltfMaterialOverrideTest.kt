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
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import java.nio.file.Paths
import org.junit.Assert.assertThrows
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Automated integration tests for glTF material overrides. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class GltfMaterialOverrideTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun materialOverride_setClearAndReapply_succeeds() {
        // GltfModelEntity is only rendered in Full Space Mode.
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
            val pbrMaterial = KhronosPbrMaterial.create(session, AlphaMode.OPAQUE)
            pbrMaterial.setMetallicFactor(0.9f)
            pbrMaterial.setRoughnessFactor(0.2f)

            val modelEntity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = MODEL_POSE,
                    parent = session.scene.activitySpace,
                )

            // In Dragon_Evolved.gltf, node 'Dragon' references mesh 0, which has exactly one
            // primitive.
            val dragonNode =
                checkNotNull(modelEntity.nodes.firstOrNull { it.name == "Dragon" }) {
                    "Expected node 'Dragon' in Dragon_Evolved.gltf"
                }

            // Clearing a primitive that has no override is documented as a no-op.
            dragonNode.clearMaterialOverride()

            dragonNode.setMaterialOverride(pbrMaterial)
            dragonNode.clearMaterialOverride()

            // An override can be re-applied after it has been cleared.
            dragonNode.setMaterialOverride(pbrMaterial, primitiveIndex = 0)
            dragonNode.clearMaterialOverride(primitiveIndex = 0)

            // The documented error contract is covered separately by
            // materialOverride_invalidPrimitiveOrMeshlessNode_throwsDocumentedExceptions.
        }
    }

    // TODO(b/579240898): Remove @Ignore once the runtime matches the GltfModelNode KDoc (or update
    //  the expected exceptions to the finalized contract). On device, an out-of-range
    //  primitiveIndex throws nothing (KDoc: IndexOutOfBoundsException), and a node without a mesh
    //  throws IllegalArgumentException (KDoc: IllegalStateException).
    @Ignore("b/579240898: runtime does not match GltfModelNode KDoc error contract")
    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun materialOverride_invalidPrimitiveOrMeshlessNode_throwsDocumentedExceptions() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
            val pbrMaterial = KhronosPbrMaterial.create(session, AlphaMode.OPAQUE)
            val modelEntity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = MODEL_POSE,
                    parent = session.scene.activitySpace,
                )

            // In Dragon_Evolved.gltf, node 'Dragon' references mesh 0, which has exactly one
            // primitive, and node 'Head' has no mesh.
            val dragonNode =
                checkNotNull(modelEntity.nodes.firstOrNull { it.name == "Dragon" }) {
                    "Expected node 'Dragon' in Dragon_Evolved.gltf"
                }
            val headNode =
                checkNotNull(modelEntity.nodes.firstOrNull { it.name == "Head" }) {
                    "Expected node 'Head' in Dragon_Evolved.gltf"
                }

            // primitiveIndex must be within [0, primitiveCount - 1] of the associated mesh.
            assertThrows(IndexOutOfBoundsException::class.java) {
                dragonNode.setMaterialOverride(pbrMaterial, primitiveIndex = 1)
            }
            assertThrows(IndexOutOfBoundsException::class.java) {
                dragonNode.clearMaterialOverride(primitiveIndex = 1)
            }

            // Nodes without an associated mesh reject material override calls.
            assertThrows(IllegalStateException::class.java) {
                headNode.setMaterialOverride(pbrMaterial)
            }
            assertThrows(IllegalStateException::class.java) { headNode.clearMaterialOverride() }
        }
    }

    private companion object {
        val MODEL_POSE = Pose(translation = Vector3(0f, 0.5f, -1.5f))
    }
}
