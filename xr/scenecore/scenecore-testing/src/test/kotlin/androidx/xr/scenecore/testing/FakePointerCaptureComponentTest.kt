/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.testing

import androidx.kruth.assertThat
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.InputEvent
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.PointerCaptureComponent.PointerCaptureState
import androidx.xr.scenecore.runtime.PointerCaptureComponent.StateListener
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakePointerCaptureComponentTest {
    private lateinit var underTest: FakePointerCaptureComponent

    @Test
    fun onStateChanged_withExecutor_returnsSetPointerCaptureState() {
        // Arrange
        var state = PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED
        val stateListener = StateListener { newState -> state = newState }
        val executor = Executor { commands -> commands.run() }
        underTest = FakePointerCaptureComponent(executor, stateListener)

        // Act
        underTest.onStateChanged(PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE)

        // Assert
        assertThat(state).isEqualTo(PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE)
    }

    @Test
    fun onInputEvent_withExecutor_returnsSetInputEvent() {
        // Arrange
        var inputEvent: InputEvent? = null
        val inputListener = InputEventListener { event -> inputEvent = event }
        val executor = Executor { commands -> commands.run() }
        underTest = FakePointerCaptureComponent(executor)
        underTest.inputListener = inputListener

        // Act
        val entity = FakeEntity()
        val expectedInputEvent =
            InputEvent(
                InputEvent.Source.HANDS,
                InputEvent.Pointer.LEFT,
                100,
                Vector3(),
                Vector3(0f, 0f, 1f),
                InputEvent.Action.DOWN,
                listOf(InputEvent.HitInfo(entity, Vector3.One, Matrix4.Identity)),
            )
        underTest.onInputEvent(expectedInputEvent)

        // Assert
        assertThat(inputEvent).isNotNull()
        assertThat(inputEvent!!.source).isEqualTo(expectedInputEvent.source)
        assertThat(inputEvent.pointerType).isEqualTo(expectedInputEvent.pointerType)
        assertThat(inputEvent.timestamp).isEqualTo(expectedInputEvent.timestamp)
        assertThat(inputEvent.origin).isEqualTo(expectedInputEvent.origin)
        assertThat(inputEvent.direction).isEqualTo(expectedInputEvent.direction)
        assertThat(inputEvent.action).isEqualTo(expectedInputEvent.action)
        assertThat(inputEvent.hitInfoList).isNotEmpty()

        val hitInfoList = inputEvent.hitInfoList
        assertThat(hitInfoList).isNotEmpty()
        assertThat(hitInfoList.size).isEqualTo(1)

        val hitInfo = hitInfoList[0]
        assertThat(hitInfo.inputEntity).isEqualTo(entity)
        assertThat(hitInfo.hitPosition).isEqualTo(Vector3.One)
        assertThat(hitInfo.transform).isEqualTo(Matrix4.Identity)
    }
}
