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
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
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
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private lateinit var session: Session
    private val mockContentlessEntity = mock<JxrPlatformAdapter.Entity>()
    private val entity by lazy { ContentlessEntity.create(session, "test") }

    @Before
    fun setUp() {
        whenever(mockRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockRuntime.activitySpace).thenReturn(mock())
        whenever(mockRuntime.activitySpaceRootImpl).thenReturn(mock())
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockRuntime.getMainPanelEntity()).thenReturn(mock())
        whenever(mockRuntime.createEntity(any(), any(), any())).thenReturn(mockContentlessEntity)
        session = Session.create(activity, mockRuntime)
    }

    @Test
    fun addInteractableComponent_addsRuntimeInteractableComponent() {
        assertThat(entity).isNotNull()

        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val executor = directExecutor()
        val interactableComponent = InteractableComponent.create(session, executor, mockListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        verify(mockRuntime).createInteractableComponent(any(), anyOrNull())
        verify(mockContentlessEntity).addComponent(any())
    }

    @Test
    fun removeInteractableComponent_removesRuntimeInteractableComponent() {
        assertThat(entity).isNotNull()

        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val executor = directExecutor()
        val interactableComponent = InteractableComponent.create(session, executor, mockListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()

        entity.removeComponent(interactableComponent)
        verify(mockContentlessEntity).removeComponent(any())
    }

    @Test
    fun interactableComponent_canAttachOnlyOnce() {
        val entity2 = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        whenever(mockRuntime.createInteractableComponent(any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
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
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val executor = directExecutor()
        val interactableComponent = InteractableComponent.create(session, executor, mockListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        entity.removeComponent(interactableComponent)
        assertThat(entity.addComponent(interactableComponent)).isTrue()
    }

    @Test
    fun interactableComponent_propagatesHitInfoInInputEvents() {
        val mockRtInteractableComponent = mock<JxrPlatformAdapter.InteractableComponent>()
        whenever(mockRuntime.createInteractableComponent(any(), any()))
            .thenReturn(mockRtInteractableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val mockListener = mock<InputEventListener>()
        val interactableComponent =
            InteractableComponent.create(session, directExecutor(), mockListener)
        assertThat(entity.addComponent(interactableComponent)).isTrue()
        val listenerCaptor = argumentCaptor<JxrPlatformAdapter.InputEventListener>()
        verify(mockRuntime).createInteractableComponent(any(), listenerCaptor.capture())
        val rtInputEventListener = listenerCaptor.lastValue
        val rtInputEvent =
            JxrPlatformAdapter.InputEvent(
                JxrPlatformAdapter.InputEvent.SOURCE_HANDS,
                JxrPlatformAdapter.InputEvent.POINTER_TYPE_RIGHT,
                123456789L,
                Vector3.Zero,
                Vector3.One,
                JxrPlatformAdapter.InputEvent.ACTION_DOWN,
                JxrPlatformAdapter.InputEvent.HitInfo(
                    mockContentlessEntity,
                    Vector3.One,
                    Matrix4.Identity
                ),
                null,
            )
        rtInputEventListener.onInputEvent(rtInputEvent)
        val inputEventCaptor = argumentCaptor<InputEvent>()
        verify(mockListener).onInputEvent(inputEventCaptor.capture())
        val inputEvent = inputEventCaptor.lastValue
        assertThat(inputEvent.source).isEqualTo(InputEvent.SOURCE_HANDS)
        assertThat(inputEvent.pointerType).isEqualTo(InputEvent.POINTER_TYPE_RIGHT)
        assertThat(inputEvent.timestamp).isEqualTo(rtInputEvent.timestamp)
        assertThat(inputEvent.action).isEqualTo(InputEvent.ACTION_DOWN)
        assertThat(inputEvent.hitInfo).isNotNull()
        assertThat(inputEvent.hitInfo!!.inputEntity).isEqualTo(entity)
        assertThat(inputEvent.hitInfo!!.hitPosition).isEqualTo(Vector3.One)
        assertThat(inputEvent.hitInfo!!.transform).isEqualTo(Matrix4.Identity)
        assertThat(inputEvent.secondaryHitInfo).isNull()
    }
}
