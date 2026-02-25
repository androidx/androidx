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
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.ActivityPanelEntity
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeGltfFeature
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.Node
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class EntityManagerTest {
    private val xrExtensions = getXrExtensions()!!
    private val fakeScheduledExecutorService = FakeScheduledExecutorService()
    private val panelEntityNode: Node = xrExtensions.createNode()
    private val anchorEntityNode: Node = xrExtensions.createNode()
    private val entityManager = EntityManager()
    private lateinit var groupEntityNode: Node
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
                entityManager,
            )
        val taskNode = xrExtensions.createNode()
        activitySpace =
            ActivitySpaceImpl(
                taskNode,
                activity,
                xrExtensions,
                entityManager,
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
    fun creatingEntity_addsEntityToEntityManager() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val groupEntity = createGroupEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        // Entity manager also contains the main panel entity and activity space, which are created
        // when
        // the runtime is created.
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                gltfEntity,
                panelEntity,
                groupEntity,
                anchorEntity,
                activityPanelEntity,
            )
    }

    @Test
    fun getEntityForNode_returnsEntity() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val groupEntity = createGroupEntity()
        val anchorEntity = createAnchorEntity()
        val testNode = xrExtensions.createNode()

        assertThat(entityManager.getEntityForNode(gltfEntityNode)).isEqualTo(gltfEntity)
        assertThat(entityManager.getEntityForNode(panelEntityNode)).isEqualTo(panelEntity)
        assertThat(entityManager.getEntityForNode(groupEntityNode)).isEqualTo(groupEntity)
        assertThat(entityManager.getEntityForNode(anchorEntityNode)).isEqualTo(anchorEntity)
        assertThat(entityManager.getEntityForNode(testNode)).isNull()
    }

    @Test
    fun getEntityByType_returnsEntityOfType() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val groupEntity = createGroupEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        assertThat(entityManager.getEntitiesOfType(GltfEntity::class.java))
            .containsExactly(gltfEntity)
        // MainPanel is also a PanelEntity.
        assertThat(entityManager.getEntitiesOfType(PanelEntity::class.java)).contains(panelEntity)
        // Base class of all entities.
        assertThat(entityManager.getEntitiesOfType<Entity>(Entity::class.java))
            .contains(groupEntity)
        assertThat(entityManager.getEntitiesOfType<AnchorEntity>(AnchorEntity::class.java))
            .containsExactly(anchorEntity)
        assertThat(
                entityManager.getEntitiesOfType<ActivityPanelEntity>(
                    ActivityPanelEntity::class.java
                )
            )
            .containsExactly(activityPanelEntity)
    }

    @Test
    fun removeEntity_removesFromEntityManager() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val groupEntity = createGroupEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                gltfEntity,
                panelEntity,
                groupEntity,
                anchorEntity,
                activityPanelEntity,
            )

        entityManager.removeEntityForNode(groupEntityNode)

        assertThat(entityManager.getAllEntities().size).isAtLeast(4)
        assertThat(entityManager.getAllEntities()).doesNotContain(groupEntity)
    }

    @Test
    fun disposeEntity_removesFromEntityManager() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val groupEntity = createGroupEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                gltfEntity,
                panelEntity,
                groupEntity,
                anchorEntity,
                activityPanelEntity,
            )

        groupEntity.dispose()

        assertThat(entityManager.getAllEntities().size).isAtLeast(4)
        assertThat(entityManager.getAllEntities()).doesNotContain(groupEntity)
    }

    @Test
    fun getAllSystemSpaceScenePoses_returnsAllSystemSpaceScenePoses() {
        assertThat(entityManager.getAllSystemSpaceActivityPoses().size).isAtLeast(2)
        assertThat(entityManager.getAllSystemSpaceActivityPoses())
            .containsAtLeast(
                spatialSceneRuntime.activitySpace,
                spatialSceneRuntime.perceptionSpaceActivityPose,
            )
    }

    @Test
    fun getSystemSpaceScenePoseOfType_returnsSystemSpaceScenePoseOfType() {
        assertThat(entityManager.getSystemSpaceActivityPoseOfType(ActivitySpace::class.java).get(0))
            .isInstanceOf(ActivitySpaceImpl::class.java)
        assertThat(
                entityManager
                    .getSystemSpaceActivityPoseOfType(PerceptionSpaceScenePose::class.java)
                    .get(0)
            )
            .isInstanceOf(PerceptionSpaceScenePoseImpl::class.java)
    }

    @Test
    fun clearEntityManager_removesAllEntityFromEntityManager() {
        val gltfEntity = createGltfEntity()
        val panelEntity = createPanelEntity()
        val groupEntity = createGroupEntity()
        val anchorEntity = createAnchorEntity()
        val activityPanelEntity = createActivityPanelEntity()

        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                gltfEntity,
                panelEntity,
                groupEntity,
                anchorEntity,
                activityPanelEntity,
            )

        entityManager.clear()

        assertThat(entityManager.getAllEntities()).isEmpty()
        assertThat(entityManager.getAllSystemSpaceActivityPoses()).isEmpty()
    }

    @Test
    fun setEntityForMultipleNodes_getEntityForNode_returnsSameEntityForBothNodes() {
        val primaryEntity = createGltfEntity()
        val primaryNode = (primaryEntity as AndroidXrEntity).getNode()

        val aliasNode = xrExtensions.createNode()

        entityManager.setEntityForNode(aliasNode, primaryEntity)

        assertThat(entityManager.getEntityForNode(primaryNode)).isSameInstanceAs(primaryEntity)
        assertThat(entityManager.getEntityForNode(aliasNode)).isSameInstanceAs(primaryEntity)
    }

    @Test
    fun setEntityForMultipleNodes_getAllEntities_returnsNonDuplicateEntities() {
        val primaryEntity = createGltfEntity()

        val aliasNode = xrExtensions.createNode()

        entityManager.setEntityForNode(aliasNode, primaryEntity)

        assertThat(entityManager.getAllEntities()).containsNoDuplicates()
    }

    /** Creates a generic glTF entity. */
    private fun createGltfEntity(): GltfEntity {
        val nodeHolder = NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        val gltfEntity =
            GltfEntityImpl(
                activity,
                FakeGltfFeature(nodeHolder),
                activitySpace,
                xrExtensions,
                entityManager,
                fakeScheduledExecutorService,
            )
        gltfEntityNode = gltfEntity.getNode()
        entityManager.setEntityForNode(gltfEntityNode, gltfEntity)
        return gltfEntity
    }

    private fun createPanelEntity(): PanelEntity {
        val display = activity!!.getSystemService(DisplayManager::class.java).displays[0]
        val displayContext = activity.createDisplayContext(display!!)
        val view = View(displayContext)
        view.setLayoutParams(ViewGroup.LayoutParams(VGA_WIDTH, VGA_HEIGHT))
        val panelEntity =
            PanelEntityImpl(
                displayContext,
                panelEntityNode,
                view,
                xrExtensions,
                entityManager,
                PixelDimensions(VGA_WIDTH, VGA_HEIGHT),
                "panel",
                fakeScheduledExecutorService,
            )
        entityManager.setEntityForNode(panelEntityNode, panelEntity)
        return panelEntity
    }

    private fun createGroupEntity(): Entity {
        val groupEntity =
            spatialSceneRuntime.createGroupEntity(
                Pose(),
                "testGroup",
                spatialSceneRuntime.activitySpace,
            )
        groupEntityNode = (groupEntity as AndroidXrEntity).getNode()
        entityManager.setEntityForNode(groupEntityNode, groupEntity)
        return groupEntity
    }

    private fun createAnchorEntity(): AnchorEntity {
        val anchorEntity =
            AnchorEntityImpl.create(
                activity,
                anchorEntityNode,
                activitySpace,
                xrExtensions,
                entityManager,
                fakeScheduledExecutorService,
            )
        entityManager.setEntityForNode(anchorEntityNode, anchorEntity)
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
