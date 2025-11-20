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

import androidx.activity.ComponentActivity
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.InputEvent as RtInputEvent
import androidx.xr.scenecore.runtime.InputEventListener as RtInputEventListener
import androidx.xr.scenecore.runtime.PointerCaptureComponent as RtPointerCaptureComponent
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialCapabilities as RtSpatialCapabilities
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
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
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PointerCaptureComponentTest {
    private val mFakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private val mockSceneRuntime = mock<SceneRuntime>()

    private lateinit var session: Session
    private val mockActivitySpace = mock<RtActivitySpace>()
    private val mockRtEntity = mock<RtEntity>()
    private val mockRtComponent = mock<RtPointerCaptureComponent>()

    private val stateListener =
        object : Consumer<PointerCaptureComponent.PointerCaptureState> {
            var lastState: PointerCaptureComponent.PointerCaptureState? = null

            override fun accept(newState: PointerCaptureComponent.PointerCaptureState) {
                lastState = newState
            }
        }

    private val inputListener =
        object : Consumer<InputEvent> {
            lateinit var lastEvent: InputEvent

            override fun accept(inputEvent: InputEvent) {
                lastEvent = inputEvent
            }
        }

    @Before
    fun setUp() {
        whenever(mockSceneRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockSceneRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockSceneRuntime.headActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockSceneRuntime.spatialCapabilities).thenReturn(RtSpatialCapabilities(0))
        whenever(mockSceneRuntime.createGroupEntity(any(), any(), any())).thenReturn(mockRtEntity)
        whenever(mockRtEntity.addComponent(any())).thenReturn(true)
        whenever(mockSceneRuntime.createPointerCaptureComponent(any(), any(), any()))
            .thenReturn(mockRtComponent)

        session =
            Session(
                activity,
                runtimes =
                    listOf(mFakePerceptionRuntimeFactory.createRuntime(activity), mockSceneRuntime),
            )
    }

    @Test
    fun addComponent_addsRuntimeComponent() {
        val entity = GroupEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()

        verify(mockRtEntity).addComponent(any())
        verify(mockSceneRuntime).createPointerCaptureComponent(any(), any(), any())
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
        verify(mockSceneRuntime)
            .createPointerCaptureComponent(any(), stateListenerCaptor.capture(), any())

        // Verify all states are properly converted and propagated.
        val stateListenerCaptured: RtPointerCaptureComponent.StateListener =
            stateListenerCaptor.lastValue
        stateListenerCaptured.onStateChanged(
            RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.PointerCaptureState.ACTIVE)

        stateListenerCaptured.onStateChanged(
            RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_PAUSED
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.PointerCaptureState.PAUSED)

        stateListenerCaptured.onStateChanged(
            RtPointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.PointerCaptureState.STOPPED)
    }

    @Test
    fun inputEventListener_propagatesFromRuntime() {
        val entity = GroupEntity.create(session, "test")
        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        val inputListenerCaptor = argumentCaptor<RtInputEventListener>()

        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        verify(mockSceneRuntime)
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
        assertThat(inputListener.lastEvent.source).isEqualTo(InputEvent.Source.HANDS)
        assertThat(inputListener.lastEvent.pointerType).isEqualTo(InputEvent.Pointer.LEFT)
        assertThat(inputListener.lastEvent.timestamp).isEqualTo(inputEvent.timestamp)
        assertThat(inputListener.lastEvent.action).isEqualTo(InputEvent.Action.DOWN)
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
