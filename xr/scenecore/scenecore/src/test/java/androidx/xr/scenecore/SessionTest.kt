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

@file:Suppress("UNUSED_VARIABLE")

package androidx.xr.scenecore

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.widget.TextView
import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the JXRCore SDK Session Interface.
 *
 * TODO(b/329902726): Add a TestRuntime and verify CPM Integration.
 */
@RunWith(RobolectricTestRunner::class)
class SessionTest {
    private val activityController = Robolectric.buildActivity(Activity::class.java)
    private val activity = activityController.create().start().get()
    private val mockPlatformAdapter = mock<JxrPlatformAdapter>()
    private val mockAnchorEntity = mock<JxrPlatformAdapter.AnchorEntity>()
    lateinit var session: Session

    @Before
    fun setUp() {
        whenever(mockPlatformAdapter.spatialEnvironment).thenReturn(mock())
        val mockActivitySpace = mock<JxrPlatformAdapter.ActivitySpace>()
        whenever(mockPlatformAdapter.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockPlatformAdapter.headActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.activitySpaceRootImpl).thenReturn(mockActivitySpace)
        whenever(mockPlatformAdapter.mainPanelEntity).thenReturn(mock())
        whenever(mockPlatformAdapter.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockAnchorEntity.state)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.State.UNANCHORED)
        whenever(mockAnchorEntity.persistState)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.PersistState.PERSIST_NOT_REQUESTED)
        session = Session.create(activity, mockPlatformAdapter)
    }

    @Test
    fun createSession_runtimeProvided_createsSessionWithProvidedRuntime() {
        assertThat(session).isNotNull()
        assertThat(session.platformAdapter).isNotNull()
        assertThat(session.platformAdapter).isEqualTo(mockPlatformAdapter)
    }

    @Test
    fun createSession_twiceFromSameActivity_returnsSameInstance() {
        val newSession = Session.create(activity, mockPlatformAdapter)
        assertThat(session).isEqualTo(newSession)
    }

    @Test
    fun createSession_differentActivities_returnsUniqueInstances() {
        val newActivity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val newSession = Session.create(newActivity, mockPlatformAdapter)
        assertThat(session).isNotEqualTo(newSession)
    }

    @Test
    fun createGltfResourceAsync_callsRuntimeLoadGltf() {
        val mockGltfModelResource = mock<GltfModelResource>()
        whenever(mockPlatformAdapter.loadGltfByAssetNameSplitEngine(anyString()))
            .thenReturn(Futures.immediateFuture(mockGltfModelResource))
        val unused = GltfModel.create(session, "test.glb")

        verify(mockPlatformAdapter).loadGltfByAssetNameSplitEngine("test.glb")
    }

    @Test
    fun createGltfEntity_callsRuntimeCreateGltfEntity() {
        whenever(mockPlatformAdapter.loadGltfByAssetNameSplitEngine(anyString()))
            .thenReturn(Futures.immediateFuture(mock()))
        whenever(mockPlatformAdapter.createGltfEntity(any(), any(), any())).thenReturn(mock())
        val gltfModelFuture = GltfModel.create(session, "test.glb")
        val unused = GltfModelEntity.create(session, gltfModelFuture.get())

        verify(mockPlatformAdapter).loadGltfByAssetNameSplitEngine(eq("test.glb"))
        verify(mockPlatformAdapter).createGltfEntity(any(), any(), any())
    }

    @Test
    fun createPanelEntity_callsRuntimeCreatePanelEntity() {
        val view = TextView(activity)
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            )
            .thenReturn(mock())
        val unused =
            PanelEntity.create(
                session,
                view,
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test",
            )

        verify(mockPlatformAdapter)
            .createPanelEntity(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun createAnchorEntity_callsRuntimeCreateAnchorEntity() {
        whenever(mockPlatformAdapter.createAnchorEntity(any(), any(), any(), anyOrNull()))
            .thenReturn(mockAnchorEntity)
        val unused = AnchorEntity.create(session, Dimensions(), PlaneType.ANY, PlaneSemantic.ANY)

        verify(mockPlatformAdapter).createAnchorEntity(any(), any(), any(), anyOrNull())
    }

    @Test
    fun getActivitySpace_returnsActivitySpace() {
        val activitySpace = session.activitySpace

        assertThat(activitySpace).isNotNull()
    }

    @Test
    fun getActivitySpaceTwice_returnsSameSpace() {
        val activitySpace1 = session.activitySpace
        val activitySpace2 = session.activitySpace

        assertThat(activitySpace1).isEqualTo(activitySpace2)
    }

    @Test
    fun getActivitySpaceRoot_returnsActivitySpaceRoot() {
        val activitySpaceRoot = session.activitySpaceRoot

        assertThat(activitySpaceRoot).isNotNull()
    }

    @Test
    fun getActivitySpaceRootTwice_returnsSameSpace() {
        val activitySpaceRoot1 = session.activitySpaceRoot
        val activitySpaceRoot2 = session.activitySpaceRoot

        assertThat(activitySpaceRoot1).isEqualTo(activitySpaceRoot2)
    }

    @Test
    fun getSpatialUser_returnsSpatialUser() {
        val spatialUser = session.spatialUser

        assertThat(spatialUser).isNotNull()
    }

    @Test
    fun getSpatialUserTwice_returnsSameUser() {
        val spatialUser1 = session.spatialUser
        val spatialUser2 = session.spatialUser

        assertThat(spatialUser1).isEqualTo(spatialUser2)
    }

    @Test
    fun getPerceptionSpace_returnPerceptionSpace() {
        val perceptionSpace = session.perceptionSpace

        assertThat(perceptionSpace).isNotNull()
    }

    @Test
    fun createActivityPanelEntity_callsRuntimeCreateActivityPanelEntity() {
        whenever(mockPlatformAdapter.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mock())
        val unused = ActivityPanelEntity.create(session, Rect(0, 0, 640, 480), "test")

        verify(mockPlatformAdapter).createActivityPanelEntity(any(), any(), any(), any(), any())
    }

    @Test
    fun getMainPanelEntity_returnsPanelEntity() {
        val unused = session.mainPanelEntity
        val unusedAgain = session.mainPanelEntity

        verify(mockPlatformAdapter, times(1)).mainPanelEntity
    }

    @Test
    fun createInteractableComponent_callsRuntimeCreateInteractableComponent() {
        whenever(mockPlatformAdapter.createInteractableComponent(any(), any())).thenReturn(mock())

        val interactableComponent = InteractableComponent.create(session, directExecutor(), mock())
        val view = TextView(activity)
        val mockPanelEntity = mock<JxrPlatformAdapter.PanelEntity>()
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            )
            .thenReturn(mockPanelEntity)
        whenever(mockPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity =
            PanelEntity.create(
                session,
                view,
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test",
            )
        assertThat(panelEntity.addComponent(interactableComponent)).isTrue()

        verify(mockPlatformAdapter).createInteractableComponent(any(), anyOrNull())
    }

    @Test
    fun createMovableComponent_callsRuntimeCreateMovableComponent() {
        whenever(mockPlatformAdapter.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mock())

        val movableComponent = MovableComponent.create(session)
        val view = TextView(activity)
        val mockRtPanelEntity = mock<JxrPlatformAdapter.PanelEntity>()
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            )
            .thenReturn(mockRtPanelEntity)
        whenever(mockRtPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity =
            PanelEntity.create(
                session,
                view,
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test",
            )
        assertThat(panelEntity.addComponent(movableComponent)).isTrue()

        verify(mockPlatformAdapter).createMovableComponent(any(), any(), any(), any())
    }

    @Test
    fun createResizableComponent_callsRuntimeCreateResizableComponent() {
        whenever(mockPlatformAdapter.createResizableComponent(any(), any())).thenReturn(mock())

        val resizableComponent = ResizableComponent.create(session)
        val view = TextView(activity)
        val mockRtPanelEntity = mock<JxrPlatformAdapter.PanelEntity>()
        whenever(mockRtPanelEntity.getSize()).thenReturn(JxrPlatformAdapter.Dimensions(1f, 1f, 1f))
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            )
            .thenReturn(mockRtPanelEntity)
        whenever(mockRtPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity =
            PanelEntity.create(
                session,
                view,
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test",
            )
        assertThat(panelEntity.addComponent(resizableComponent)).isTrue()

        verify(mockPlatformAdapter).createResizableComponent(any(), any())
    }

    @Test
    fun setFullSpaceMode_callsThrough() {
        // Test that Session calls into the runtime.
        val bundle = Bundle().apply { putString("testkey", "testval") }
        whenever(mockPlatformAdapter.setFullSpaceMode(any())).thenReturn(bundle)
        val unused = session.setFullSpaceMode(bundle)
        verify(mockPlatformAdapter).setFullSpaceMode(bundle)
    }

    @Test
    fun setFullSpaceModeWithEnvironmentInherited_callsThrough() {
        // Test that Session calls into the runtime.
        val bundle = Bundle().apply { putString("testkey", "testval") }
        whenever(mockPlatformAdapter.setFullSpaceModeWithEnvironmentInherited(any()))
            .thenReturn(bundle)
        val unused = session.setFullSpaceModeWithEnvironmentInherited(bundle)
        verify(mockPlatformAdapter).setFullSpaceModeWithEnvironmentInherited(bundle)
    }

    @Test
    fun setPreferredAspectRatio_callsThrough() {
        // Test that Session calls into the runtime.
        session.setPreferredAspectRatio(activity, 1.23f)
        verify(mockPlatformAdapter).setPreferredAspectRatio(activity, 1.23f)
    }

    @Test
    fun getPanelEntityType_returnsAllPanelEntities() {
        val mockPanelEntity1 = mock<JxrPlatformAdapter.PanelEntity>()
        val mockActivityPanelEntity = mock<JxrPlatformAdapter.ActivityPanelEntity>()
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            )
            .thenReturn(mockPanelEntity1)
        whenever(mockPlatformAdapter.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mockActivityPanelEntity)
        val panelEntity =
            PanelEntity.create(
                session,
                TextView(activity),
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test1",
            )
        val activityPanelEntity = ActivityPanelEntity.create(session, Rect(0, 0, 640, 480), "test2")

        assertThat(session.getEntitiesOfType(PanelEntity::class.java))
            .containsAtLeast(panelEntity, activityPanelEntity)
    }

    @Test
    fun getEntitiesBaseType_returnsAllEntities() {
        val mockPanelEntity = mock<JxrPlatformAdapter.PanelEntity>()
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            )
            .thenReturn(mockPanelEntity)
        whenever(mockPlatformAdapter.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntity)
        val panelEntity =
            PanelEntity.create(
                session,
                TextView(activity),
                Dimensions(720f, 480f),
                Dimensions(0.1f, 0.1f, 0.1f),
                "test1",
            )
        val anchorEntity =
            AnchorEntity.create(session, Dimensions(), PlaneType.ANY, PlaneSemantic.ANY)

        assertThat(session.getEntitiesOfType(Entity::class.java))
            .containsAtLeast(panelEntity, anchorEntity)
    }

    @Test
    fun addAndRemoveSpatialCapabilitiesChangedListener_callsRuntimeAddAndRemove() {
        val listener = Consumer<SpatialCapabilities> { _ -> }
        session.addSpatialCapabilitiesChangedListener(listener = listener)
        verify(mockPlatformAdapter).addSpatialCapabilitiesChangedListener(any(), any())
        session.removeSpatialCapabilitiesChangedListener(listener)
        verify(mockPlatformAdapter).removeSpatialCapabilitiesChangedListener(any())
    }

    @Test
    fun onDestroy_callsRuntimeDispose() {
        activityController.destroy()
        verify(mockPlatformAdapter).dispose()
    }
}
