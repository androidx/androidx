/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore

import android.app.Activity
import android.widget.TextView
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EntityManagerTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private val mockGltfModelEntityImpl = mock<JxrPlatformAdapter.GltfEntity>()
    private val mockPanelEntityImpl = mock<JxrPlatformAdapter.PanelEntity>()
    private val mockAnchorEntityImpl = mock<JxrPlatformAdapter.AnchorEntity>()
    private val mockActivityPanelEntity = mock<JxrPlatformAdapter.ActivityPanelEntity>()
    private val mockContentlessEntity = mock<JxrPlatformAdapter.Entity>()
    private val entityManager = EntityManager()
    private lateinit var session: Session
    private lateinit var activitySpace: ActivitySpace
    private lateinit var gltfModel: GltfModel
    private lateinit var gltfModelEntity: GltfModelEntity
    private lateinit var panelEntity: PanelEntity
    private lateinit var anchorEntity: AnchorEntity
    private lateinit var activityPanelEntity: ActivityPanelEntity
    private lateinit var contentlessEntity: Entity

    @Before
    fun setUp() {
        whenever(mockRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockRuntime.activitySpace).thenReturn(mock())
        whenever(mockRuntime.activitySpaceRootImpl).thenReturn(mock())
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockRuntime.loadGltfByAssetNameSplitEngine(Mockito.anyString()))
            .thenReturn(Futures.immediateFuture(mock()))
        whenever(mockRuntime.createGltfEntity(any(), any(), any()))
            .thenReturn(mockGltfModelEntityImpl)
        whenever(mockRuntime.createPanelEntity(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mockPanelEntityImpl)
        whenever(mockRuntime.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntityImpl)
        whenever(mockAnchorEntityImpl.state)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.State.UNANCHORED)
        whenever(mockAnchorEntityImpl.persistState)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.PersistState.PERSIST_NOT_REQUESTED)
        whenever(mockRuntime.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mockActivityPanelEntity)
        whenever(mockRuntime.createEntity(any(), any(), any())).thenReturn(mockContentlessEntity)
        whenever(mockRuntime.mainPanelEntity).thenReturn(mockPanelEntityImpl)
        session = Session.create(activity, mockRuntime)
        activitySpace = ActivitySpace.create(mockRuntime, entityManager)
    }

    @Test
    fun creatingEntity_addsEntityToEntityManager() {
        createContentlessEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()

        // The entityManager contains activity space.
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                contentlessEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )
    }

    @Test
    fun getEntityForRtEntity_returnsEntity() {
        createContentlessEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()

        assertThat(entityManager.getEntityForRtEntity(mockContentlessEntity))
            .isEqualTo(contentlessEntity)
        assertThat(entityManager.getEntityForRtEntity(mockPanelEntityImpl)).isEqualTo(panelEntity)
        assertThat(entityManager.getEntityForRtEntity(mockAnchorEntityImpl)).isEqualTo(anchorEntity)
        assertThat(entityManager.getEntityForRtEntity(mockGltfModelEntityImpl))
            .isEqualTo(gltfModelEntity)
        assertThat(entityManager.getEntityForRtEntity(mockActivityPanelEntity))
            .isEqualTo(activityPanelEntity)
    }

    @Test
    fun getEntityForRtEntity_returnsNullWhenNoRtEntityFound() {
        assertThat(entityManager.getEntityForRtEntity(mockContentlessEntity)).isNull()
        assertThat(entityManager.getEntityForRtEntity(mockPanelEntityImpl)).isNull()
        assertThat(entityManager.getEntityForRtEntity(mockAnchorEntityImpl)).isNull()
        assertThat(entityManager.getEntityForRtEntity(mockGltfModelEntityImpl)).isNull()
        assertThat(entityManager.getEntityForRtEntity(mockActivityPanelEntity)).isNull()
    }

    @Test
    fun getEntityByType_returnsEntityOfType() {
        createContentlessEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()

        assertThat(entityManager.getEntities<Entity>())
            .containsAtLeast(
                contentlessEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )
        assertThat(entityManager.getEntities<ContentlessEntity>())
            .containsExactly(contentlessEntity)
        assertThat(entityManager.getEntities<PanelEntity>()).contains(panelEntity)
        assertThat(entityManager.getEntities<AnchorEntity>()).containsExactly(anchorEntity)
        assertThat(entityManager.getEntities<ActivityPanelEntity>())
            .containsExactly(activityPanelEntity)
        assertThat(entityManager.getEntities<GltfModelEntity>()).containsExactly(gltfModelEntity)
    }

    @Test
    fun disposeEntity_removesEntityFromEntityManager() {
        createContentlessEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                contentlessEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )

        contentlessEntity.dispose()

        assertThat(entityManager.getAllEntities().size).isAtLeast(4)
        assertThat(entityManager.getAllEntities()).doesNotContain(contentlessEntity)
    }

    @Test
    fun clearEntityManager_removesAllEntityFromEntityManager() {
        createContentlessEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                contentlessEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )

        entityManager.clear()

        assertThat(entityManager.getAllEntities()).isEmpty()
    }

    @Test
    fun removeRtEntity_removesEntityFromEntityManager() {
        createContentlessEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                contentlessEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )

        entityManager.removeEntity(mockContentlessEntity)

        assertThat(entityManager.getAllEntities().size).isAtLeast(4)
        assertThat(entityManager.getAllEntities()).doesNotContain(contentlessEntity)
    }

    private fun createPanelEntity() {
        panelEntity =
            PanelEntity.create(
                mockRuntime,
                entityManager,
                TextView(activity),
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test",
                activity,
            )
    }

    private fun createGltfEntity() {
        gltfModel = GltfModel.create(session, "test.glb").get()
        gltfModelEntity = GltfModelEntity.create(mockRuntime, entityManager, gltfModel)
    }

    private fun createAnchorEntity() {
        anchorEntity =
            AnchorEntity.create(
                mockRuntime,
                entityManager,
                Dimensions(),
                PlaneType.ANY,
                PlaneSemantic.ANY,
                10.seconds.toJavaDuration(),
            )
    }

    private fun createActivityPanelEntity() {
        activityPanelEntity =
            ActivityPanelEntity.create(
                mockRuntime,
                entityManager,
                PixelDimensions(640, 480),
                "test",
                activity,
            )
    }

    private fun createContentlessEntity() {
        contentlessEntity = ContentlessEntity.create(mockRuntime, entityManager, "test")
    }
}
