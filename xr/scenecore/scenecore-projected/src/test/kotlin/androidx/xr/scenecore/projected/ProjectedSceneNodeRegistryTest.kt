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

package androidx.xr.scenecore.projected

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
class ProjectedSceneNodeRegistryTest {

    // TODO: b/492129708 - Test with real concrete ProjectedEntity (and ScenePose) types
    // Helper interfaces for testing type filtering
    interface TestEntity1 : Entity

    interface TestEntity2 : Entity

    interface TestScenePose1 : ScenePose

    interface TestScenePose2 : ScenePose

    private lateinit var nodeRegistry: ProjectedSceneNodeRegistry

    @Before
    fun setUp() {
        nodeRegistry = ProjectedSceneNodeRegistry()
    }

    @Test
    fun setEntityForNode_and_getEntityForNode_workCorrectly() {
        val node = mock<IProjectedNode>()
        val entity = mock<Entity>()

        nodeRegistry.setEntityForNode(node, entity)

        assertThat(nodeRegistry.getEntityForNode(node)).isEqualTo(entity)
    }

    @Test
    fun getEntityForNode_returnsNullForUnknownNode() {
        val node = mock<IProjectedNode>()
        assertThat(nodeRegistry.getEntityForNode(node)).isNull()
    }

    @Test
    fun getEntitiesOfType_returnsFilteringResults() {
        val node1 = mock<IProjectedNode>()
        val entity1 = mock<TestEntity1>()
        val node2 = mock<IProjectedNode>()
        val entity2 = mock<TestEntity2>()

        nodeRegistry.setEntityForNode(node1, entity1)
        nodeRegistry.setEntityForNode(node2, entity2)

        val entities1 = nodeRegistry.getEntitiesOfType(TestEntity1::class.java)
        assertThat(entities1).containsExactly(entity1)

        val entities2 = nodeRegistry.getEntitiesOfType(TestEntity2::class.java)
        assertThat(entities2).containsExactly(entity2)

        val allEntities = nodeRegistry.getEntitiesOfType(Entity::class.java)
        assertThat(allEntities).containsExactly(entity1, entity2)
    }

    @Test
    fun getEntitiesOfType_returnsEmptyListWhenNoMatches() {
        val node = mock<IProjectedNode>()
        val entity = mock<TestEntity1>()
        nodeRegistry.setEntityForNode(node, entity)

        // Query for a type that doesn't exist in the registry
        val results = nodeRegistry.getEntitiesOfType(TestEntity2::class.java)
        assertThat(results).isEmpty()
    }

    @Test
    fun getAllEntities_returnsAllStoredEntities() {
        val node1 = mock<IProjectedNode>()
        val entity1 = mock<Entity>()
        val node2 = mock<IProjectedNode>()
        val entity2 = mock<Entity>()

        nodeRegistry.setEntityForNode(node1, entity1)
        nodeRegistry.setEntityForNode(node2, entity2)

        assertThat(nodeRegistry.getAllEntities()).containsExactly(entity1, entity2)
    }

    @Test
    fun removeEntityForNode_removesMapping() {
        val node = mock<IProjectedNode>()
        val entity = mock<Entity>()

        nodeRegistry.setEntityForNode(node, entity)
        nodeRegistry.removeEntityForNode(node)

        assertThat(nodeRegistry.getEntityForNode(node)).isNull()
    }

    @Test
    fun removeEntityForNode_doesNothingForUnknownNode() {
        val node = mock<IProjectedNode>()

        // Should not throw exception
        nodeRegistry.removeEntityForNode(node)

        assertThat(nodeRegistry.getAllEntities()).isEmpty()
    }

    @Test
    fun addSystemSpaceScenePose_and_getAllSystemSpaceScenePoses_workCorrectly() {
        val scenePose = mock<ScenePose>()
        nodeRegistry.addSystemSpaceScenePose(scenePose)
        assertThat(nodeRegistry.getAllSystemSpaceScenePoses()).containsExactly(scenePose)
    }

    @Test
    fun getSystemSpaceScenePoseOfType_returnsFilteredResults() {
        val scenePose1 = mock<TestScenePose1>()
        val scenePose2 = mock<TestScenePose2>()

        nodeRegistry.addSystemSpaceScenePose(scenePose1)
        nodeRegistry.addSystemSpaceScenePose(scenePose2)

        val scenePoses1 = nodeRegistry.getSystemSpaceScenePoseOfType(TestScenePose1::class.java)
        assertThat(scenePoses1).containsExactly(scenePose1)

        val scenePoses2 = nodeRegistry.getSystemSpaceScenePoseOfType(TestScenePose2::class.java)
        assertThat(scenePoses2).containsExactly(scenePose2)
    }

    @Test
    fun getSystemSpaceScenePoseOfType_returnsEmptyListWhenNoMatches() {
        val pose = mock<TestScenePose1>()
        nodeRegistry.addSystemSpaceScenePose(pose)

        // Query for a type that doesn't exist
        val results = nodeRegistry.getSystemSpaceScenePoseOfType(TestScenePose2::class.java)
        assertThat(results).isEmpty()
    }

    @Test
    fun clear_removesAllData() {
        val node = mock<IProjectedNode>()
        val entity = mock<Entity>()
        val scenePose = mock<ScenePose>()

        nodeRegistry.setEntityForNode(node, entity)
        nodeRegistry.addSystemSpaceScenePose(scenePose)

        nodeRegistry.clear()

        assertThat(nodeRegistry.getEntityForNode(node)).isNull()
        assertThat(nodeRegistry.getAllEntities()).isEmpty()
        assertThat(nodeRegistry.getAllSystemSpaceScenePoses()).isEmpty()
    }
}
