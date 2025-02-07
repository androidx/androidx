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
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MovableComponentTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private lateinit var session: Session
    private val mockActivitySpace = mock<JxrPlatformAdapter.ActivitySpace>()
    private val mockContentlessEntity = mock<JxrPlatformAdapter.Entity>()
    private val mockAnchorEntity = mock<JxrPlatformAdapter.AnchorEntity>()
    private val entityManager = EntityManager()

    @Before
    fun setUp() {
        whenever(mockRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockRuntime.activitySpaceRootImpl).thenReturn(mock())
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockRuntime.getMainPanelEntity()).thenReturn(mock())
        whenever(mockRuntime.createEntity(any(), any(), any())).thenReturn(mockContentlessEntity)
        whenever(mockRuntime.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntity)
        whenever(mockAnchorEntity.state)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.State.UNANCHORED)
        whenever(mockAnchorEntity.persistState)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.PersistState.PERSIST_NOT_REQUESTED)
        session = Session.create(activity, mockRuntime)
    }

    @Test
    fun addMovableComponent_addsRuntimeMovableComponent() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockAnchorPlacement = mock<JxrPlatformAdapter.AnchorPlacement>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        whenever(
                mockRuntime.createAnchorPlacementForPlanes(
                    setOf(JxrPlatformAdapter.PlaneType.HORIZONTAL),
                    setOf(JxrPlatformAdapter.PlaneSemantic.WALL),
                )
            )
            .thenReturn(mockAnchorPlacement)

        val anchorPlacement =
            AnchorPlacement.createForPlanes(setOf(PlaneType.HORIZONTAL), setOf(PlaneSemantic.WALL))

        val movableComponent =
            MovableComponent.create(
                session,
                systemMovable = false,
                scaleInZ = false,
                anchorPlacement = setOf(anchorPlacement),
                shouldDisposeParentAnchor = false,
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockRuntime).createMovableComponent(false, false, setOf(mockAnchorPlacement), false)
        verify(mockContentlessEntity).addComponent(any())
    }

    @Test
    fun addMovableComponentDefaultArguments_addsRuntimeMovableComponentWithDefaults() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.create(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockRuntime)
            .createMovableComponent(
                /*systemMovable=*/ true,
                /*scaleInZ=*/ true,
                /*anchorPlacement=*/ emptySet(),
                /*shouldDisposeParentAnchor=*/ true,
            )
        verify(mockContentlessEntity).addComponent(any())
    }

    @Test
    fun removeMovableComponent_removesRuntimeMovableComponent() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.create(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()

        entity.removeComponent(movableComponent)
        verify(mockContentlessEntity).removeComponent(any())
    }

    @Test
    fun movableComponent_canAttachOnlyOnce() {
        val entity = ContentlessEntity.create(session, "test")
        val entity2 = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.create(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity2.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun movableComponent_setSizeInvokesRuntimeMovableComponentSetSize() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val mockRtMovableComponent = mock<JxrPlatformAdapter.MovableComponent>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.create(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()

        val testSize = Dimensions(2f, 2f, 0f)
        movableComponent.size = testSize

        assertThat(movableComponent.size).isEqualTo(testSize)
        verify(mockRtMovableComponent).setSize(any())
    }

    @Test
    fun movableComponent_addMoveListenerInvokesRuntimeMovableComponentAddMoveEventListener() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<JxrPlatformAdapter.MovableComponent>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.create(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockMoveListener = mock<MoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockMoveListener)

        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.MoveEventListener::class.java)
        verify(mockRtMovableComponent).addMoveEventListener(any(), captor.capture())
        val rtMoveEventListener = captor.value
        var rtMoveEvent =
            JxrPlatformAdapter.MoveEvent(
                MoveEvent.MOVE_STATE_START,
                JxrPlatformAdapter.Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                JxrPlatformAdapter.Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                /* updatedParent= */ null,
                /* disposedEntity= */ null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockMoveListener).onMoveStart(any(), any(), any(), any(), any())

        rtMoveEvent =
            JxrPlatformAdapter.MoveEvent(
                MoveEvent.MOVE_STATE_ONGOING,
                JxrPlatformAdapter.Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                JxrPlatformAdapter.Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                /* updatedParent= */ null,
                /* disposedEntity= */ null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockMoveListener).onMoveUpdate(any(), any(), any(), any())

        rtMoveEvent =
            JxrPlatformAdapter.MoveEvent(
                MoveEvent.MOVE_STATE_END,
                JxrPlatformAdapter.Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                JxrPlatformAdapter.Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                mockAnchorEntity,
                /* disposedEntity= */ null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockMoveListener).onMoveEnd(any(), any(), any(), any(), isA<AnchorEntity>())
    }

    @Test
    fun movableComponent_addMultipleMoveEventListenersInvokesAllListeners() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<JxrPlatformAdapter.MovableComponent>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.create(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockMoveListener = mock<MoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockMoveListener)
        val mockMoveListener2 = mock<MoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockMoveListener2)

        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.MoveEventListener::class.java)
        verify(mockRtMovableComponent, times(2)).addMoveEventListener(any(), captor.capture())
        val rtMoveEventListener1 = captor.allValues[0]
        val rtMoveEventListener2 = captor.allValues[1]
        val rtMoveEvent =
            JxrPlatformAdapter.MoveEvent(
                MoveEvent.MOVE_STATE_START,
                JxrPlatformAdapter.Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                JxrPlatformAdapter.Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                /* updatedParent= */ null,
                /* disposedEntity= */ null,
            )

        rtMoveEventListener1.onMoveEvent(rtMoveEvent)
        rtMoveEventListener2.onMoveEvent(rtMoveEvent)

        verify(mockMoveListener).onMoveStart(any(), any(), any(), any(), any())
        verify(mockMoveListener2).onMoveStart(any(), any(), any(), any(), any())
    }

    @Test
    fun movableComponent_removeMoveEventListenerInvokesRuntimeRemoveMoveEventListener() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<JxrPlatformAdapter.MovableComponent>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.create(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockMoveListener = mock<MoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockMoveListener)
        val mockMoveListener2 = mock<MoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockMoveListener2)

        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.MoveEventListener::class.java)
        verify(mockRtMovableComponent, times(2)).addMoveEventListener(any(), captor.capture())
        val rtMoveEventListener1 = captor.allValues[0]
        val rtMoveEventListener2 = captor.allValues[1]
        val rtMoveEvent =
            JxrPlatformAdapter.MoveEvent(
                MoveEvent.MOVE_STATE_START,
                JxrPlatformAdapter.Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                JxrPlatformAdapter.Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                /* updatedParent= */ null,
                /* disposedEntity= */ null,
            )

        rtMoveEventListener1.onMoveEvent(rtMoveEvent)
        rtMoveEventListener2.onMoveEvent(rtMoveEvent)

        verify(mockMoveListener).onMoveStart(any(), any(), any(), any(), any())
        verify(mockMoveListener2).onMoveStart(any(), any(), any(), any(), any())

        movableComponent.removeMoveListener(mockMoveListener)
        verify(mockRtMovableComponent).removeMoveEventListener(rtMoveEventListener1)

        rtMoveEventListener2.onMoveEvent(rtMoveEvent)
        // The first listener, which we removed, should not be called again.
        verify(mockMoveListener, times(1)).onMoveStart(any(), any(), any(), any(), any())
        verify(mockMoveListener2, times(2)).onMoveStart(any(), any(), any(), any(), any())

        movableComponent.removeMoveListener(mockMoveListener2)
        verify(mockRtMovableComponent).removeMoveEventListener(rtMoveEventListener2)
    }

    @Test
    fun movablecomponent_canAttachAgainAfterDetach() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.create(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        entity.removeComponent(movableComponent)
        assertThat(entity.addComponent(movableComponent)).isTrue()
    }
}
