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
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.InputEvent as RtInputEvent
import androidx.xr.runtime.internal.InputEventListener as RtInputEventListener
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PointerCaptureComponent as RtPointerCaptureComponent
import androidx.xr.runtime.internal.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PointerCaptureComponentTest {
    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private lateinit var session: Session
    private val mockActivitySpace = mock<RtActivitySpace>()
    private val mockRtEntity = mock<RtEntity>()
    private val mockRtComponent = mock<RtPointerCaptureComponent>()

    private val stateListener =
        object : PointerCaptureComponent.StateListener {
            var lastState: Int = -1

            override fun onStateChanged(newState: Int) {
                lastState = newState
            }
        }

    private val inputListener =
        object : InputEventListener {
            lateinit var lastEvent: InputEvent

            override fun onInputEvent(inputEvent: InputEvent) {
                lastEvent = inputEvent
            }
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
        whenever(mockRuntime.createGroupEntity(any(), any(), any())).thenReturn(mockRtEntity)
        whenever(mockRtEntity.addComponent(any())).thenReturn(true)
        whenever(mockRuntime.createPointerCaptureComponent(any(), any(), any()))
            .thenReturn(mockRtComponent)

        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockRuntime)
    }

    @Test
    fun addComponent_addsRuntimeComponent() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()

        verify(mockRtEntity).addComponent(any())
        verify(mockRuntime).createPointerCaptureComponent(any(), any(), any())
    }

    @Test
    fun addComponent_failsIfAlreadyAttached() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        assertThat(entity.addComponent(pointerCaptureComponent)).isFalse()
    }

    @Test
    fun stateListener_propagatesCorrectlyFromRuntime() {
        val entity = GroupEntity.create(session, "test")
        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        val stateListenerCaptor = argumentCaptor<RtPointerCaptureComponent.StateListener>()

        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        verify(mockRuntime)
            .createPointerCaptureComponent(any(), stateListenerCaptor.capture(), any())

        // Verify all states are properly converted and propagated.
        val stateListenerCaptured: RtPointerCaptureComponent.StateListener =
            stateListenerCaptor.lastValue
        stateListenerCaptured.onStateChanged(
            RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.Companion.POINTER_CAPTURE_STATE_ACTIVE)

        stateListenerCaptured.onStateChanged(
            RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_PAUSED
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.Companion.POINTER_CAPTURE_STATE_PAUSED)

        stateListenerCaptured.onStateChanged(
            RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.Companion.POINTER_CAPTURE_STATE_STOPPED)
    }

    @Test
    fun inputEventListener_propagatesFromRuntime() {
        val entity = GroupEntity.create(session, "test")
        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        val inputListenerCaptor = argumentCaptor<RtInputEventListener>()

        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        verify(mockRuntime)
            .createPointerCaptureComponent(any(), any(), inputListenerCaptor.capture())

        val inputEvent =
            RtInputEvent(
                RtInputEvent.Source.HANDS,
                RtInputEvent.Pointer.LEFT,
                100,
                Vector3(),
                Vector3(0f, 0f, 1f),
                RtInputEvent.Action.DOWN,
                listOf(RtInputEvent.HitInfo(mockRtEntity, Vector3.One, Matrix4.Identity)),
            )

        // Only compare non-floating point values for stability
        inputListenerCaptor.lastValue.onInputEvent(inputEvent)
        assertThat(inputListener.lastEvent.source).isEqualTo(InputEvent.Source.SOURCE_HANDS)
        assertThat(inputListener.lastEvent.pointerType)
            .isEqualTo(InputEvent.Pointer.POINTER_TYPE_LEFT)
        assertThat(inputListener.lastEvent.timestamp).isEqualTo(inputEvent.timestamp)
        assertThat(inputListener.lastEvent.action).isEqualTo(InputEvent.Action.ACTION_DOWN)
        assertThat(inputListener.lastEvent.hitInfoList).isNotEmpty()
        val hitInfoList = inputListener.lastEvent.hitInfoList
        assertThat(hitInfoList).isNotEmpty()
        assertThat(hitInfoList.size).isEqualTo(1)

        val hitInfo = hitInfoList[0]

        assertThat(hitInfo.inputEntity).isEqualTo(entity)
        assertThat(hitInfo.hitPosition).isEqualTo(Vector3.One)
        assertThat(hitInfo.transform).isEqualTo(Matrix4.Identity)
    }

    @Test
    fun removeComponent_removesRuntimeComponent() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()

        entity.removeComponent(pointerCaptureComponent)
        verify(mockRtEntity).removeComponent(mockRtComponent)
    }
}
