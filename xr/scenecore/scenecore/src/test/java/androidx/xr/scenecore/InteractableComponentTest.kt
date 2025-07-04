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
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.InputEvent as RtInputEvent
import androidx.xr.runtime.internal.InputEventListener as RtInputEventListener
import androidx.xr.runtime.internal.InteractableComponent as RtInteractableComponent
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PanelEntity as RtPanelEntity
import androidx.xr.runtime.internal.PixelDimensions as RtPixelDimensions
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InteractableComponentTest {
    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private val mockActivitySpace = mock<RtActivitySpace>()
    private lateinit var session: Session
    private val mockGroupEntity = mock<RtEntity>()
    private val entity by lazy { GroupEntity.create(session, "test") }

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
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockRuntime)
    }

    @Test
    fun addInteractableComponent_addsRuntimeInteractableComponent() {
        assertThat(entity).isNotNull()

        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val executor = directExecutor()
        val interactableComponent = InteractableComponent.create(session, executor, mockListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        verify(mockRuntime).createInteractableComponent(any(), anyOrNull())
        verify(mockGroupEntity).addComponent(any())
    }

    @Test
    fun removeInteractableComponent_removesRuntimeInteractableComponent() {
        assertThat(entity).isNotNull()

        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val executor = directExecutor()
        val interactableComponent = InteractableComponent.create(session, executor, mockListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()

        entity.removeComponent(interactableComponent)
        verify(mockGroupEntity).removeComponent(any())
    }

    @Test
    fun interactableComponent_canAttachOnlyOnce() {
        val entity2 = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val executor = directExecutor()
        val interactableComponent = InteractableComponent.create(session, executor, mockListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        assertThat(entity2.addComponent(interactableComponent)).isFalse()
    }

    @Test
    fun interactableComponent_canAttachAgainAfterDetach() {
        assertThat(entity).isNotNull()

        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val executor = directExecutor()
        val interactableComponent = InteractableComponent.create(session, executor, mockListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        entity.removeComponent(interactableComponent)
        assertThat(entity.addComponent(interactableComponent)).isTrue()
    }

    @Test
    fun interactableComponent_propagatesHitInfoInInputEvents() {
        val mockRtInteractableComponent = mock<RtInteractableComponent>()
        whenever(mockRuntime.createInteractableComponent(any(), any()))
            .thenReturn(mockRtInteractableComponent)
        whenever(mockGroupEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val interactableComponent =
            InteractableComponent.create(session, directExecutor(), mockListener)
        assertThat(entity.addComponent(interactableComponent)).isTrue()
        val listenerCaptor = argumentCaptor<RtInputEventListener>()
        verify(mockRuntime).createInteractableComponent(any(), listenerCaptor.capture())
        val rtInputEventListener = listenerCaptor.lastValue
        val rtInputEvent =
            RtInputEvent(
                RtInputEvent.Source.HANDS,
                RtInputEvent.Pointer.RIGHT,
                123456789L,
                Vector3.Zero,
                Vector3.One,
                RtInputEvent.Action.DOWN,
                listOf(RtInputEvent.HitInfo(mockGroupEntity, Vector3.One, Matrix4.Identity)),
            )
        rtInputEventListener.onInputEvent(rtInputEvent)
        val inputEventCaptor = argumentCaptor<InputEvent>()
        verify(mockListener).onInputEvent(inputEventCaptor.capture())
        val inputEvent = inputEventCaptor.lastValue
        assertThat(inputEvent.source).isEqualTo(InputEvent.Source.SOURCE_HANDS)
        assertThat(inputEvent.pointerType).isEqualTo(InputEvent.Pointer.POINTER_TYPE_RIGHT)
        assertThat(inputEvent.timestamp).isEqualTo(rtInputEvent.timestamp)
        assertThat(inputEvent.action).isEqualTo(InputEvent.Action.ACTION_DOWN)
        assertThat(inputEvent.hitInfoList).isNotEmpty()
        assertThat(inputEvent.hitInfoList).hasSize(1)

        val hitInfo = inputEvent.hitInfoList[0]
        assertThat(hitInfo).isNotNull()
        assertThat(hitInfo.inputEntity).isEqualTo(entity)
        assertThat(hitInfo.hitPosition).isEqualTo(Vector3.One)
        assertThat(hitInfo.transform).isEqualTo(Matrix4.Identity)
    }

    @Test
    fun createInteractableComponent_callsRuntimeCreateInteractableComponent() {
        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())

        val interactableComponent = InteractableComponent.create(session, directExecutor(), mock())
        val view = TextView(activity)
        val mockPanelEntity = mock<RtPanelEntity>()
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
            .thenReturn(mockPanelEntity)
        whenever(mockPanelEntity.addComponent(any())).thenReturn(true)
        val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
        assertThat(panelEntity.addComponent(interactableComponent)).isTrue()

        verify(mockRuntime).createInteractableComponent(any(), anyOrNull())
    }
}
