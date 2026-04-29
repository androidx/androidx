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
package androidx.xr.scenecore.spatial.core

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.ActivityPanelEntity
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import androidx.xr.scenecore.testing.FakeGltfFeature.Companion.createWithMockFeature
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.Node
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SceneNodeRegistryTest {
    private val xrExtensions = SpatialCoreXrExtensionsHolderProvider.extensionsLegacy
    private val fakeScheduledExecutorService = FakeScheduledExecutorService()
    private val panelEntityNode: Node = xrExtensions.createNode()
    private val anchorEntityNode: Node = xrExtensions.createNode()
    private val sceneNodeRegistry = SceneNodeRegistry()
    private val mockGltfFeature: GltfFeature = mock(GltfFeature::class.java)
    private lateinit var entityNode: Node
    private lateinit var gltfEntityNode: Node
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private lateinit var spatialSceneRuntime: SpatialSceneRuntime
    private lateinit var activitySpace: ActivitySpaceImpl

    @Before
    fun setUp() {
        spatialSceneRuntime =
            SpatialSceneRuntime.create(
                activity,
                fakeScheduledExecutorService,
                xrExtensions,
                sceneNodeRegistry,
            )
        val taskNode = xrExtensions.createNode()
        activitySpace =
            ActivitySpaceImpl(
                taskNode,
                activity,
                xrExtensions,
                sceneNodeRegistry,
                { xrExtensions.getSpatialState(activity) },
                fakeScheduledExecutorService,
            )
        val currentTimeMillis = 1000000000L
        SystemClock.setCurrentTimeMillis(currentTimeMillis)

        // By default, set the activity space to the root of the underlying OpenXR reference space.
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        spatialSceneRuntime.destroy()
    }

    @Test
    fun creatingEntity_addsEntityToSceneNodeRegistry() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val entity = createEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        // SceneNodeRegistry also contains the main panel entity and activity space, which are
        // created when the runtime is created.
        assertThat(sceneNodeRegistry.getAllEntities().size).isAtLeast(5)
        assertThat(sceneNodeRegistry.getAllEntities())
            .containsAtLeast(gltfEntity, panelEntity, entity, anchorEntity, activityPanelEntity)
    }

    @Test
    fun getEntityForNode_returnsEntity() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val entity = createEntity()
        val anchorEntity = createAnchorEntity()
        val testNode = xrExtensions.createNode()

        assertThat(sceneNodeRegistry.getEntityForNode(gltfEntityNode)).isEqualTo(gltfEntity)
        assertThat(sceneNodeRegistry.getEntityForNode(panelEntityNode)).isEqualTo(panelEntity)
        assertThat(sceneNodeRegistry.getEntityForNode(entityNode)).isEqualTo(entity)
        assertThat(sceneNodeRegistry.getEntityForNode(anchorEntityNode)).isEqualTo(anchorEntity)
        assertThat(sceneNodeRegistry.getEntityForNode(testNode)).isNull()
    }

    @Test
    fun getEntityByType_returnsEntityOfType() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val entity = createEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        assertThat(sceneNodeRegistry.getEntitiesOfType(GltfEntity::class.java))
            .containsExactly(gltfEntity)
        // MainPanel is also a PanelEntity.
        assertThat(sceneNodeRegistry.getEntitiesOfType(PanelEntity::class.java))
            .contains(panelEntity)
        // Base class of all entities.
        assertThat(sceneNodeRegistry.getEntitiesOfType(Entity::class.java)).contains(entity)
        assertThat(sceneNodeRegistry.getEntitiesOfType(AnchorEntity::class.java))
            .containsExactly(anchorEntity)
        assertThat(sceneNodeRegistry.getEntitiesOfType(ActivityPanelEntity::class.java))
            .containsExactly(activityPanelEntity)
    }

    @Test
    fun removeEntity_removesFromSceneNodeRegistry() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val entity = createEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        assertThat(sceneNodeRegistry.getAllEntities().size).isAtLeast(5)
        assertThat(sceneNodeRegistry.getAllEntities())
            .containsAtLeast(gltfEntity, panelEntity, entity, anchorEntity, activityPanelEntity)

        sceneNodeRegistry.removeEntityForNode(entityNode)

        assertThat(sceneNodeRegistry.getAllEntities().size).isAtLeast(4)
        assertThat(sceneNodeRegistry.getAllEntities()).doesNotContain(entity)
    }

    @Test
    fun disposeEntity_removesFromSceneNodeRegistry() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val entity = createEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        assertThat(sceneNodeRegistry.getAllEntities().size).isAtLeast(5)
        assertThat(sceneNodeRegistry.getAllEntities())
            .containsAtLeast(gltfEntity, panelEntity, entity, anchorEntity, activityPanelEntity)

        entity.dispose()

        assertThat(sceneNodeRegistry.getAllEntities().size).isAtLeast(4)
        assertThat(sceneNodeRegistry.getAllEntities()).doesNotContain(entity)
    }

    @Test
    fun getAllSystemSpaceScenePoses_returnsAllSystemSpaceScenePoses() {
        assertThat(sceneNodeRegistry.getAllSystemSpaceScenePoses().size).isAtLeast(2)
        assertThat(sceneNodeRegistry.getAllSystemSpaceScenePoses())
            .containsAtLeast(
                spatialSceneRuntime.activitySpace,
                spatialSceneRuntime.perceptionSpaceActivityPose,
            )
    }

    @Test
    fun getSystemSpaceScenePoseOfType_returnsSystemSpaceScenePoseOfType() {
        assertThat(sceneNodeRegistry.getSystemSpaceScenePoseOfType(ActivitySpace::class.java)[0])
            .isInstanceOf(ActivitySpaceImpl::class.java)
        assertThat(
                sceneNodeRegistry
                    .getSystemSpaceScenePoseOfType(PerceptionSpaceScenePose::class.java)[0]
            )
            .isInstanceOf(PerceptionSpaceScenePoseImpl::class.java)
    }

    @Test
    fun clearSceneNodeRegistry_removesAllEntityFromSceneNodeRegistry() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val entity = createEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        assertThat(sceneNodeRegistry.getAllEntities().size).isAtLeast(5)
        assertThat(sceneNodeRegistry.getAllEntities())
            .containsAtLeast(gltfEntity, panelEntity, entity, anchorEntity, activityPanelEntity)

        sceneNodeRegistry.clear()

        assertThat(sceneNodeRegistry.getAllEntities()).isEmpty()
        assertThat(sceneNodeRegistry.getAllSystemSpaceScenePoses()).isEmpty()
    }

    @Test
    fun setEntityForMultipleNodes_getEntityForNode_returnsSameEntityForBothNodes() {
        val primaryEntity = createGltfEntity()
        val primaryNode = (primaryEntity as AndroidXrEntity).getNode()

        val aliasNode = xrExtensions.createNode()

        sceneNodeRegistry.setEntityForNode(aliasNode, primaryEntity)

        assertThat(sceneNodeRegistry.getEntityForNode(primaryNode)).isSameInstanceAs(primaryEntity)
        assertThat(sceneNodeRegistry.getEntityForNode(aliasNode)).isSameInstanceAs(primaryEntity)
    }

    @Test
    fun setEntityForMultipleNodes_getAllEntities_returnsNonDuplicateEntities() {
        val primaryEntity = createGltfEntity()

        val aliasNode = xrExtensions.createNode()

        sceneNodeRegistry.setEntityForNode(aliasNode, primaryEntity)

        assertThat(sceneNodeRegistry.getAllEntities()).containsNoDuplicates()
    }

    /** Creates a generic glTF entity. */
    private fun createGltfEntity(): GltfEntity {
        val nodeHolder = NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        val fakeGltfFeature = createWithMockFeature(mockGltfFeature, nodeHolder)
        val gltfEntity =
            GltfEntityImpl(
                activity,
                fakeGltfFeature,
                activitySpace,
                xrExtensions,
                sceneNodeRegistry,
                fakeScheduledExecutorService,
            )
        gltfEntityNode = gltfEntity.getNode()
        sceneNodeRegistry.setEntityForNode(gltfEntityNode, gltfEntity)
        return gltfEntity
    }

    private fun createPanelEntity(): PanelEntity {
        val display = activity!!.getSystemService(DisplayManager::class.java).displays[0]
        val displayContext = activity.createDisplayContext(display!!)
        val view = View(displayContext)
        view.layoutParams = ViewGroup.LayoutParams(VGA_WIDTH, VGA_HEIGHT)
        val panelEntity =
            PanelEntityImpl(
                displayContext,
                panelEntityNode,
                view,
                xrExtensions,
                sceneNodeRegistry,
                PixelDimensions(VGA_WIDTH, VGA_HEIGHT),
                "panel",
                fakeScheduledExecutorService,
            )
        sceneNodeRegistry.setEntityForNode(panelEntityNode, panelEntity)
        return panelEntity
    }

    private fun createEntity(): Entity {
        val entity =
            spatialSceneRuntime.createEntity(Pose(), "testGroup", spatialSceneRuntime.activitySpace)
        entityNode = (entity as AndroidXrEntity).getNode()
        sceneNodeRegistry.setEntityForNode(entityNode, entity)
        return entity
    }

    private fun createAnchorEntity(): AnchorEntity {
        val anchorEntity =
            AnchorEntityImpl.create(
                activity,
                anchorEntityNode,
                activitySpace,
                xrExtensions,
                sceneNodeRegistry,
                fakeScheduledExecutorService,
            )
        sceneNodeRegistry.setEntityForNode(anchorEntityNode, anchorEntity)
        return anchorEntity
    }

    private fun createActivityPanelEntity(): ActivityPanelEntity {
        return spatialSceneRuntime.createActivityPanelEntity(
            Pose(),
            PixelDimensions(VGA_WIDTH, VGA_HEIGHT),
            "test",
            activity!!,
            activitySpace,
        )
    }

    companion object {
        private const val VGA_WIDTH = 640
        private const val VGA_HEIGHT = 480
    }
}
