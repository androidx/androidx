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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PointerCaptureComponentTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private lateinit var session: Session
    private val mockRtEntity = mock<JxrPlatformAdapter.Entity>()
    private val mockRtComponent = mock<JxrPlatformAdapter.PointerCaptureComponent>()

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
        whenever(mockRuntime.activitySpace).thenReturn(mock())
        whenever(mockRuntime.activitySpaceRootImpl).thenReturn(mock())
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockRuntime.createEntity(any(), any(), any())).thenReturn(mockRtEntity)
        whenever(mockRtEntity.addComponent(any())).thenReturn(true)
        whenever(mockRuntime.createPointerCaptureComponent(any(), any(), any()))
            .thenReturn(mockRtComponent)

        session = Session.create(activity, mockRuntime)
    }

    @Test
    fun addComponent_addsRuntimeComponent() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()

        verify(mockRtEntity).addComponent(any())
        verify(mockRuntime).createPointerCaptureComponent(any(), any(), any())
    }

    @Test
    fun addComponent_failsIfAlreadyAttached() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        assertThat(entity.addComponent(pointerCaptureComponent)).isFalse()
    }

    @Test
    fun stateListener_propagatesCorrectlyFromRuntime() {
        val entity = ContentlessEntity.create(session, "test")
        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        val stateListenerCaptor =
            argumentCaptor<JxrPlatformAdapter.PointerCaptureComponent.StateListener>()

        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        verify(mockRuntime)
            .createPointerCaptureComponent(any(), stateListenerCaptor.capture(), any())

        // Verify all states are properly converted and propagated.
        val stateListenerCaptured: JxrPlatformAdapter.PointerCaptureComponent.StateListener =
            stateListenerCaptor.lastValue
        stateListenerCaptured.onStateChanged(
            JxrPlatformAdapter.PointerCaptureComponent.POINTER_CAPTURE_STATE_ACTIVE
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.POINTER_CAPTURE_STATE_ACTIVE)

        stateListenerCaptured.onStateChanged(
            JxrPlatformAdapter.PointerCaptureComponent.POINTER_CAPTURE_STATE_PAUSED
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.POINTER_CAPTURE_STATE_PAUSED)

        stateListenerCaptured.onStateChanged(
            JxrPlatformAdapter.PointerCaptureComponent.POINTER_CAPTURE_STATE_STOPPED
        )
        assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.POINTER_CAPTURE_STATE_STOPPED)
    }

    @Test
    fun inputEventListener_propagatesFromRuntime() {
        val entity = ContentlessEntity.create(session, "test")
        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        val inputListenerCaptor = argumentCaptor<JxrPlatformAdapter.InputEventListener>()

        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        verify(mockRuntime)
            .createPointerCaptureComponent(any(), any(), inputListenerCaptor.capture())

        val inputEvent =
            JxrPlatformAdapter.InputEvent(
                JxrPlatformAdapter.InputEvent.SOURCE_HANDS,
                JxrPlatformAdapter.InputEvent.POINTER_TYPE_LEFT,
                100,
                Vector3(),
                Vector3(0f, 0f, 1f),
                JxrPlatformAdapter.InputEvent.ACTION_DOWN,
                JxrPlatformAdapter.InputEvent.HitInfo(mockRtEntity, Vector3.One, Matrix4.Identity),
                null,
            )

        // Only compare non-floating point values for stability
        inputListenerCaptor.lastValue.onInputEvent(inputEvent)
        assertThat(inputListener.lastEvent.source).isEqualTo(InputEvent.SOURCE_HANDS)
        assertThat(inputListener.lastEvent.pointerType).isEqualTo(InputEvent.POINTER_TYPE_LEFT)
        assertThat(inputListener.lastEvent.timestamp).isEqualTo(inputEvent.timestamp)
        assertThat(inputListener.lastEvent.action).isEqualTo(InputEvent.ACTION_DOWN)
        assertThat(inputListener.lastEvent.hitInfo).isNotNull()
        val hitInfo = inputListener.lastEvent.hitInfo!!
        assertThat(hitInfo.inputEntity).isEqualTo(entity)
        assertThat(hitInfo.hitPosition).isEqualTo(Vector3.One)
        assertThat(hitInfo.transform).isEqualTo(Matrix4.Identity)
        assertThat(inputListener.lastEvent.secondaryHitInfo).isNull()
    }

    @Test
    fun removeComponent_removesRuntimeComponent() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()

        entity.removeComponent(pointerCaptureComponent)
        verify(mockRtEntity).removeComponent(mockRtComponent)
    }
}
