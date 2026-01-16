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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeInteractableComponentTest {
    lateinit var underTest: FakeInteractableComponent
    lateinit var fakeExecutor: Executor
    private val tasks = ConcurrentLinkedQueue<Runnable>()

    @Before
    fun setUp() {
        underTest = FakeInteractableComponent()
        fakeExecutor = Executor { command -> tasks.add(command) }
    }

    @Test
    fun setInputEventListener_propagatesInputEvent() {
        var inputEvent: InputEvent? = null
        val inputEventListener = InputEventListener { event -> inputEvent = event }
        underTest.inputEventListenersMap[inputEventListener] = fakeExecutor

        val entity = FakeEntity()
        val expectedInputEvent =
            InputEvent(
                InputEvent.Source.HANDS,
                InputEvent.Pointer.RIGHT,
                123456789L,
                Vector3.Zero,
                Vector3.One,
                InputEvent.Action.DOWN,
                listOf(InputEvent.HitInfo(entity, Vector3.One, Matrix4.Identity)),
            )
        underTest.onInputEvent(expectedInputEvent)

        assertThat(inputEvent).isNull()

        tasks.forEach { it.run() }

        assertThat(inputEvent?.source).isEqualTo(expectedInputEvent.source)
        assertThat(inputEvent?.pointerType).isEqualTo(expectedInputEvent.pointerType)
        assertThat(inputEvent?.timestamp).isEqualTo(expectedInputEvent.timestamp)
        assertThat(inputEvent?.origin).isEqualTo(expectedInputEvent.origin)
        assertThat(inputEvent?.direction).isEqualTo(expectedInputEvent.direction)
        assertThat(inputEvent?.action).isEqualTo(expectedInputEvent.action)
        assertThat(inputEvent?.hitInfoList).isNotEmpty()
        assertThat(inputEvent?.hitInfoList).hasSize(1)

        val hitInfo = inputEvent?.hitInfoList[0]
        assertThat(hitInfo).isNotNull()
        assertThat(hitInfo?.inputEntity).isEqualTo(entity)
        assertThat(hitInfo?.hitPosition).isEqualTo(Vector3.One)
        assertThat(hitInfo?.transform).isEqualTo(Matrix4.Identity)
    }
}
