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
import android.view.View
import android.widget.TextView
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.AnchorEntity as RtAnchorEntity
import androidx.xr.runtime.internal.AnchorPlacement as RtAnchorPlacement
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.MovableComponent as RtMovableComponent
import androidx.xr.runtime.internal.MoveEvent as RtMoveEvent
import androidx.xr.runtime.internal.MoveEventListener as RtMoveEventListener
import androidx.xr.runtime.internal.PanelEntity as RtPanelEntity
import androidx.xr.runtime.internal.PixelDimensions as RtPixelDimensions
import androidx.xr.runtime.internal.PlaneSemantic as RtPlaneSemantic
import androidx.xr.runtime.internal.PlaneType as RtPlaneType
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import kotlin.test.assertFailsWith
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
    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private lateinit var session: Session
    private val mockActivitySpace = mock<RtActivitySpace>()
    private val mockGroupEntity = mock<RtEntity>()
    private val mockAnchorEntity = mock<RtAnchorEntity>()
    private val entityManager = EntityManager()

    object MockitoHelper {
        // use this in place of captor.capture() if you are trying to capture an argument that is
        // not nullable
        fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
    }

    @Before
    fun setUp() {
        whenever(mockRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockRuntime.activitySpaceRootImpl).thenReturn(mockActivitySpace)
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockRuntime.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        whenever(mockRuntime.createGroupEntity(any(), any(), any())).thenReturn(mockGroupEntity)
        whenever(mockRuntime.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntity)
        whenever(mockAnchorEntity.state).thenReturn(RtAnchorEntity.State.UNANCHORED)
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockRuntime)
    }

    @Test
    fun addMovableComponent_addsRuntimeMovableComponent() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val mockEntityMoveListener = mock<EntityMoveListener>()

        val movableComponent =
            MovableComponent.createCustomMovable(
                session = session,
                scaleInZ = false,
                directExecutor(),
                mockEntityMoveListener,
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockRuntime)
            .createMovableComponent(
                systemMovable = false,
                scaleInZ = false,
                anchorPlacement = emptySet(),
                shouldDisposeParentAnchor = true,
            )
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun addAutoMovableComponent_addsRuntimeMovableComponent() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)

        val movableComponent =
            MovableComponent.createSystemMovable(session = session, scaleInZ = false)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockRuntime)
            .createMovableComponent(
                systemMovable = true,
                scaleInZ = false,
                anchorPlacement = emptySet(),
                shouldDisposeParentAnchor = true,
            )
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun addMovableAnchorableComponent_addsRuntimeMovableComponent() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockAnchorPlacement = mock<RtAnchorPlacement>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        whenever(
                mockRuntime.createAnchorPlacementForPlanes(
                    setOf(RtPlaneType.HORIZONTAL),
                    setOf(RtPlaneSemantic.WALL),
                )
            )
            .thenReturn(mockAnchorPlacement)

        val anchorPlacement =
            AnchorPlacement.createForPlanes(
                setOf(PlaneOrientation.HORIZONTAL),
                setOf(PlaneSemanticType.WALL),
            )

        val movableComponent =
            MovableComponent.createAnchorable(
                session = session,
                anchorPlacement = setOf(anchorPlacement),
                disposeParentOnReAnchor = false,
            )

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockRuntime)
            .createMovableComponent(
                systemMovable = true,
                scaleInZ = false,
                anchorPlacement = setOf(mockAnchorPlacement),
                shouldDisposeParentAnchor = false,
            )
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun createAnchorableWithEmptySet_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            MovableComponent.createAnchorable(
                session = session,
                anchorPlacement = emptySet(),
                disposeParentOnReAnchor = false,
            )
        }
    }

    @Test
    fun addMovableComponentToAnchorEntity_returnsFalse() {
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)
        assertThat(anchorEntity).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(anchorEntity.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun addMovableComponentToActivitySpace_returnsFalse() {
        val activitySpace = session.scene.activitySpace
        assertThat(activitySpace).isNotNull()
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(activitySpace.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun addMovableComponentDefaultArguments_addsRuntimeMovableComponentWithDefaults() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        verify(mockRuntime)
            .createMovableComponent(
                systemMovable = true,
                scaleInZ = true,
                anchorPlacement = emptySet(),
                shouldDisposeParentAnchor = true,
            )
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun removeMovableComponent_removesRuntimeMovableComponent() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()

        entity.removeComponent(movableComponent)
        verify(mockGroupEntity).removeComponent(any())
    }

    @Test
    fun movableComponent_canAttachOnlyOnce() {
        val entity = GroupEntity.create(session, "test")
        val entity2 = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        assertThat(entity2.addComponent(movableComponent)).isFalse()
    }

    @Test
    fun movableComponent_setSizeInvokesRuntimeMovableComponentSetSize() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val mockRtMovableComponent = mock<RtMovableComponent>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()

        val testSize = FloatSize3d(2f, 2f, 0f)
        movableComponent.size = testSize

        assertThat(movableComponent.size).isEqualTo(testSize)
        verify(mockRtMovableComponent).size = any()
    }

    @Test
    fun movableComponent_addMoveListenerInvokesRuntimeMovableComponentAddMoveEventListener() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<RtMovableComponent>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockEntityMoveListener = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener)

        val captor: ArgumentCaptor<RtMoveEventListener> =
            ArgumentCaptor.forClass(RtMoveEventListener::class.java)

        verify(mockRtMovableComponent).addMoveEventListener(any(), MockitoHelper.capture(captor))
        val rtMoveEventListener = captor.value
        var rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveStart(any(), any(), any(), any(), any())

        rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_ONGOING,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveUpdate(any(), any(), any(), any())

        rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_END,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                mockAnchorEntity,
                disposedEntity = null,
            )
        rtMoveEventListener.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveEnd(any(), any(), any(), any(), isA<AnchorEntity>())
    }

    @Test
    fun movableComponent_addMultipleMoveEventListenersInvokesAllListeners() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<RtMovableComponent>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockEntityMoveListener = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener)
        val mockEntityMoveListener2 = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener2)

        val captor: ArgumentCaptor<RtMoveEventListener> =
            ArgumentCaptor.forClass(RtMoveEventListener::class.java)

        verify(mockRtMovableComponent, times(2))
            .addMoveEventListener(any(), MockitoHelper.capture(captor))
        val rtMoveEventListener1 = captor.allValues[0]
        val rtMoveEventListener2 = captor.allValues[1]
        val rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )

        rtMoveEventListener1.onMoveEvent(rtMoveEvent)
        rtMoveEventListener2.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveStart(any(), any(), any(), any(), any())
        verify(mockEntityMoveListener2).onMoveStart(any(), any(), any(), any(), any())
    }

    @Test
    fun movableComponent_removeMoveEventListenerInvokesRuntimeRemoveMoveEventListener() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtMovableComponent = mock<RtMovableComponent>()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any()))
            .thenReturn(mockRtMovableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)
        assertThat(entity.addComponent(movableComponent)).isTrue()
        val mockEntityMoveListener = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener)
        val mockEntityMoveListener2 = mock<EntityMoveListener>()
        movableComponent.addMoveListener(directExecutor(), mockEntityMoveListener2)

        val captor: ArgumentCaptor<RtMoveEventListener> =
            ArgumentCaptor.forClass(RtMoveEventListener::class.java)

        verify(mockRtMovableComponent, times(2))
            .addMoveEventListener(any(), MockitoHelper.capture(captor))
        val rtMoveEventListener1 = captor.allValues[0]
        val rtMoveEventListener2 = captor.allValues[1]
        val rtMoveEvent =
            RtMoveEvent(
                MoveEvent.MOVE_STATE_START,
                Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f)),
                Ray(Vector3(1f, 1f, 1f), Vector3(2f, 2f, 2f)),
                Pose(),
                Pose(),
                Vector3(1f, 1f, 1f),
                Vector3(1f, 1f, 1f),
                mockActivitySpace,
                updatedParent = null,
                disposedEntity = null,
            )

        rtMoveEventListener1.onMoveEvent(rtMoveEvent)
        rtMoveEventListener2.onMoveEvent(rtMoveEvent)

        verify(mockEntityMoveListener).onMoveStart(any(), any(), any(), any(), any())
        verify(mockEntityMoveListener2).onMoveStart(any(), any(), any(), any(), any())

        movableComponent.removeMoveListener(mockEntityMoveListener)
        verify(mockRtMovableComponent).removeMoveEventListener(rtMoveEventListener1)

        rtMoveEventListener2.onMoveEvent(rtMoveEvent)
        // The first listener, which we removed, should not be called again.
        verify(mockEntityMoveListener, times(1)).onMoveStart(any(), any(), any(), any(), any())
        verify(mockEntityMoveListener2, times(2)).onMoveStart(any(), any(), any(), any(), any())

        movableComponent.removeMoveListener(mockEntityMoveListener2)
        verify(mockRtMovableComponent).removeMoveEventListener(rtMoveEventListener2)
    }

    @Test
    fun movablecomponent_canAttachAgainAfterDetach() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val movableComponent = MovableComponent.createSystemMovable(session)

        assertThat(entity.addComponent(movableComponent)).isTrue()
        entity.removeComponent(movableComponent)
        assertThat(entity.addComponent(movableComponent)).isTrue()
    }

    @Test
    fun createMovableComponent_callsRuntimeCreateMovableComponent() {
        whenever(mockRuntime.createMovableComponent(any(), any(), any(), any())).thenReturn(mock())

        val movableComponent = MovableComponent.createSystemMovable(session)
        val view = TextView(activity)
        val mockRtPanelEntity = mock<RtPanelEntity>()
        whenever(
                mockRuntime.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<RtPixelDimensions>(),
                    any<String>(),
                    any<RtEntity>(),
                )
            )
            .thenReturn(mockRtPanelEntity)
        whenever(mockRtPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
        assertThat(panelEntity.addComponent(movableComponent)).isTrue()

        verify(mockRuntime).createMovableComponent(any(), any(), any(), any())
    }
}
