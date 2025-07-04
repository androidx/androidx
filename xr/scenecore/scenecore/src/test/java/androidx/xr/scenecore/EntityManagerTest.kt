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
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivityPanelEntity as RtActivityPanelEntity
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.AnchorEntity as RtAnchorEntity
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.GltfEntity as RtGltfEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PanelEntity as RtPanelEntity
import androidx.xr.runtime.internal.PixelDimensions as RtPixelDimensions
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking
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
    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockPlatformAdapter = mock<JxrPlatformAdapter>()
    private val mockActivitySpace = mock<RtActivitySpace>()
    private val mockGltfModelEntityImpl = mock<RtGltfEntity>()
    private val mockPanelEntityImpl = mock<RtPanelEntity>()
    private val mockAnchorEntityImpl = mock<RtAnchorEntity>()
    private val mockActivityPanelEntity = mock<RtActivityPanelEntity>()
    private val mockGroupEntity = mock<RtEntity>()
    private val entityManager = EntityManager()
    private lateinit var session: Session
    private lateinit var activitySpace: ActivitySpace
    private lateinit var gltfModel: GltfModel
    private lateinit var gltfModelEntity: GltfModelEntity
    private lateinit var panelEntity: PanelEntity
    private lateinit var anchorEntity: AnchorEntity
    private lateinit var activityPanelEntity: ActivityPanelEntity
    private lateinit var groupEntity: Entity

    @Before
    fun setUp() {
        whenever(mockPlatformAdapter.spatialEnvironment).thenReturn(mock())
        whenever(mockPlatformAdapter.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockPlatformAdapter.activitySpaceRootImpl).thenReturn(mockActivitySpace)
        whenever(mockPlatformAdapter.headActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        whenever(mockPlatformAdapter.loadGltfByAssetName(Mockito.anyString()))
            .thenReturn(Futures.immediateFuture(mock()))
        whenever(mockPlatformAdapter.createGltfEntity(any(), any(), any()))
            .thenReturn(mockGltfModelEntityImpl)
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mockPanelEntityImpl)
        whenever(mockPlatformAdapter.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntityImpl)
        whenever(mockAnchorEntityImpl.state).thenReturn(RtAnchorEntity.State.UNANCHORED)
        whenever(mockPlatformAdapter.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mockActivityPanelEntity)
        whenever(mockPlatformAdapter.createGroupEntity(any(), any(), any()))
            .thenReturn(mockGroupEntity)
        whenever(mockPlatformAdapter.mainPanelEntity).thenReturn(mockPanelEntityImpl)
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)
        activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)
    }

    @Test
    fun creatingEntity_addsEntityToEntityManager() {
        createGroupEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()

        // The entityManager contains activity space.
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                groupEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )
    }

    @Test
    fun getEntityForRtEntity_returnsEntity() {
        createGroupEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()

        assertThat(entityManager.getEntityForRtEntity(mockGroupEntity)).isEqualTo(groupEntity)
        assertThat(entityManager.getEntityForRtEntity(mockPanelEntityImpl)).isEqualTo(panelEntity)
        assertThat(entityManager.getEntityForRtEntity(mockAnchorEntityImpl)).isEqualTo(anchorEntity)
        assertThat(entityManager.getEntityForRtEntity(mockGltfModelEntityImpl))
            .isEqualTo(gltfModelEntity)
        assertThat(entityManager.getEntityForRtEntity(mockActivityPanelEntity))
            .isEqualTo(activityPanelEntity)
    }

    @Test
    fun getEntityForRtEntity_returnsNullWhenNoRtEntityFound() {
        assertThat(entityManager.getEntityForRtEntity(mockGroupEntity)).isNull()
        assertThat(entityManager.getEntityForRtEntity(mockPanelEntityImpl)).isNull()
        assertThat(entityManager.getEntityForRtEntity(mockAnchorEntityImpl)).isNull()
        assertThat(entityManager.getEntityForRtEntity(mockGltfModelEntityImpl)).isNull()
        assertThat(entityManager.getEntityForRtEntity(mockActivityPanelEntity)).isNull()
    }

    @Test
    fun getEntityByType_returnsEntityOfType() {
        createGroupEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()

        assertThat(entityManager.getEntities<Entity>())
            .containsAtLeast(
                groupEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )
        assertThat(entityManager.getEntities<GroupEntity>()).containsExactly(groupEntity)
        assertThat(entityManager.getEntities<PanelEntity>()).contains(panelEntity)
        assertThat(entityManager.getEntities<AnchorEntity>()).containsExactly(anchorEntity)
        assertThat(entityManager.getEntities<ActivityPanelEntity>())
            .containsExactly(activityPanelEntity)
        assertThat(entityManager.getEntities<GltfModelEntity>()).containsExactly(gltfModelEntity)
    }

    @Test
    fun disposeEntity_removesEntityFromEntityManager() {
        createGroupEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                groupEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )

        groupEntity.dispose()

        assertThat(entityManager.getAllEntities().size).isAtLeast(4)
        assertThat(entityManager.getAllEntities()).doesNotContain(groupEntity)
    }

    @Test
    fun clearEntityManager_removesAllEntityFromEntityManager() {
        createGroupEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                groupEntity,
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
        createGroupEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityManager.getAllEntities().size).isAtLeast(5)
        assertThat(entityManager.getAllEntities())
            .containsAtLeast(
                groupEntity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )

        entityManager.removeEntity(mockGroupEntity)

        assertThat(entityManager.getAllEntities().size).isAtLeast(4)
        assertThat(entityManager.getAllEntities()).doesNotContain(groupEntity)
    }

    private fun createPanelEntity() {
        panelEntity =
            PanelEntity.create(
                session.runtime.lifecycleManager,
                activity,
                mockPlatformAdapter,
                entityManager,
                TextView(activity),
                IntSize2d(720, 480),
                "test",
            )
    }

    private fun createGltfEntity() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        }
        gltfModelEntity = GltfModelEntity.create(mockPlatformAdapter, entityManager, gltfModel)
    }

    private fun createAnchorEntity() {
        anchorEntity =
            AnchorEntity.create(
                mockPlatformAdapter,
                entityManager,
                FloatSize2d(),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
                10.seconds.toJavaDuration(),
            )
    }

    private fun createActivityPanelEntity() {
        activityPanelEntity =
            ActivityPanelEntity.create(
                session.runtime.lifecycleManager,
                mockPlatformAdapter,
                entityManager,
                IntSize2d(640, 480),
                "test",
                activity,
            )
    }

    private fun createGroupEntity() {
        groupEntity = GroupEntity.create(mockPlatformAdapter, entityManager, "test")
    }
}
