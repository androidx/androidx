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

package androidx.xr.scenecore.runtime.impl

import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.ScenePose
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class BaseSceneNodeRegistryTest {

    // Concrete implementation using String as the key type to test the generic base logic
    private class TestSceneNodeRegistry : BaseSceneNodeRegistry<String>()

    // Helper interfaces for testing type filtering
    interface TestEntity1 : Entity

    interface TestEntity2 : Entity

    interface TestScenePose1 : ScenePose

    interface TestScenePose2 : ScenePose

    private lateinit var registry: TestSceneNodeRegistry

    @Before
    fun setUp() {
        registry = TestSceneNodeRegistry()
    }

    @Test
    fun getEntityForNode_returnsNull_whenNodeNotRegistered() {
        val nodeKey = "unknown_node"
        assertThat(registry.getEntityForNode(nodeKey)).isNull()
    }

    @Test
    fun setEntityForNode_and_getEntityForNode_storesAndRetrievesEntity() {
        val nodeKey = "node_1"
        val entity = mock<Entity>()

        registry.setEntityForNode(nodeKey, entity)

        assertThat(registry.getEntityForNode(nodeKey)).isEqualTo(entity)
    }

    @Test
    fun setEntityForNode_updatesExistingMapping() {
        val nodeKey = "node_1"
        val entity1 = mock<Entity>()
        val entity2 = mock<Entity>()

        registry.setEntityForNode(nodeKey, entity1)
        // Overwrite with new entity
        registry.setEntityForNode(nodeKey, entity2)

        assertThat(registry.getEntityForNode(nodeKey)).isEqualTo(entity2)
        // Verify entity1 is no longer in the registry
        assertThat(registry.getAllEntities()).containsExactly(entity2)
    }

    @Test
    fun getEntitiesOfType_filtersCorrectly() {
        val node1 = "node_1"
        val entity1 = mock<TestEntity1>()
        val node2 = "node_2"
        val entity2 = mock<TestEntity2>()

        registry.setEntityForNode(node1, entity1)
        registry.setEntityForNode(node2, entity2)

        val results1 = registry.getEntitiesOfType(TestEntity1::class.java)
        assertThat(results1).containsExactly(entity1)

        val results2 = registry.getEntitiesOfType(TestEntity2::class.java)
        assertThat(results2).containsExactly(entity2)

        val allResults = registry.getEntitiesOfType(Entity::class.java)
        assertThat(allResults).containsExactly(entity1, entity2)
    }

    @Test
    fun getAllEntities_returnsAllDistinctEntities() {
        val node1 = "node_1"
        val node2 = "node_2"
        val entity1 = mock<Entity>()
        val entity2 = mock<Entity>()

        registry.setEntityForNode(node1, entity1)
        registry.setEntityForNode(node2, entity2)

        assertThat(registry.getAllEntities()).containsExactly(entity1, entity2)
    }

    @Test
    fun getAllEntities_deduplicatesEntities() {
        val node1 = "node_1"
        val node2 = "node_2"
        val sharedEntity = mock<Entity>()

        // Map two different nodes to the exact same entity instance
        registry.setEntityForNode(node1, sharedEntity)
        registry.setEntityForNode(node2, sharedEntity)

        // Ensure the entity is only returned once
        assertThat(registry.getAllEntities()).containsExactly(sharedEntity)
    }

    @Test
    fun removeEntityForNode_removesMapping() {
        val nodeKey = "node_1"
        val entity = mock<Entity>()

        registry.setEntityForNode(nodeKey, entity)
        registry.removeEntityForNode(nodeKey)

        assertThat(registry.getEntityForNode(nodeKey)).isNull()
        assertThat(registry.getAllEntities()).isEmpty()
    }

    @Test
    fun addSystemSpaceScenePose_storesPose() {
        val scenePose = mock<ScenePose>()
        registry.addSystemSpaceScenePose(scenePose)
        assertThat(registry.getAllSystemSpaceScenePoses()).containsExactly(scenePose)
    }

    @Test
    fun getAllSystemSpaceScenePoses_returnsAllPoses() {
        val scenePose1 = mock<ScenePose>()
        val scenePose2 = mock<ScenePose>()

        registry.addSystemSpaceScenePose(scenePose1)
        registry.addSystemSpaceScenePose(scenePose2)

        assertThat(registry.getAllSystemSpaceScenePoses()).containsExactly(scenePose1, scenePose2)
    }

    @Test
    fun getSystemSpaceScenePoseOfType_filtersCorrectly() {
        val scenePose1 = mock<TestScenePose1>()
        val scenePose2 = mock<TestScenePose2>()

        registry.addSystemSpaceScenePose(scenePose1)
        registry.addSystemSpaceScenePose(scenePose2)

        assertThat(registry.getSystemSpaceScenePoseOfType(TestScenePose1::class.java))
            .containsExactly(scenePose1)
        assertThat(registry.getSystemSpaceScenePoseOfType(TestScenePose2::class.java))
            .containsExactly(scenePose2)
    }

    @Test
    fun clear_removesAllNodesAndSystemSpaces() {
        val nodeKey = "node_1"
        val entity = mock<Entity>()
        val scenePose = mock<ScenePose>()

        registry.setEntityForNode(nodeKey, entity)
        registry.addSystemSpaceScenePose(scenePose)

        registry.clear()

        assertThat(registry.getEntityForNode(nodeKey)).isNull()
        assertThat(registry.getAllEntities()).isEmpty()
        assertThat(registry.getAllSystemSpaceScenePoses()).isEmpty()
    }
}
