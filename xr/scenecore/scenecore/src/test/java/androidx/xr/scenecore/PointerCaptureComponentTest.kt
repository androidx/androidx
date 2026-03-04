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
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.InputEvent as RtInputEvent
import androidx.xr.scenecore.runtime.PointerCaptureComponent as RtPointerCaptureComponent
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakePointerCaptureComponent
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PointerCaptureComponentTest {
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var sceneRuntime: SceneRuntime

    private lateinit var session: Session

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
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
    }

    @Test
    fun addComponent_addsRuntimeComponent() {
        val entity = Entity.create(session, "test")
        val rtEntity = (entity as BaseEntity<*>).rtEntity
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)

        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        assertThat(rtEntity?.getComponents()).hasSize(1)
        assertThat(rtEntity?.getComponents()[0])
            .isInstanceOf(FakePointerCaptureComponent::class.java)
    }

    @Test
    fun addComponent_failsIfAlreadyAttached() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        assertThat(entity.addComponent(pointerCaptureComponent)).isFalse()
    }

    @Test
    fun stateListener_propagatesCorrectlyFromRuntime() {
        val entity = Entity.create(session, "test")
        val rtEntity = (entity as BaseEntity<*>).rtEntity
        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)

        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        assertThat(rtEntity?.getComponents()).hasSize(1)
        assertThat(rtEntity?.getComponents()[0])
            .isInstanceOf(FakePointerCaptureComponent::class.java)

        // Verify all states are properly converted and propagated.
        val stateListenerCaptured = rtEntity?.getComponents()[0] as FakePointerCaptureComponent
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
        val entity = Entity.create(session, "test")
        val rtEntity = (entity as BaseEntity<*>).rtEntity
        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)

        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        assertThat(rtEntity?.getComponents()).hasSize(1)
        assertThat(rtEntity?.getComponents()[0])
            .isInstanceOf(FakePointerCaptureComponent::class.java)

        val inputEvent =
            RtInputEvent(
                RtInputEvent.Source.HANDS,
                RtInputEvent.Pointer.LEFT,
                100,
                Vector3(),
                Vector3(0f, 0f, 1f),
                RtInputEvent.Action.DOWN,
                listOf(RtInputEvent.HitInfo(entity.rtEntity!!, Vector3.One, Matrix4.Identity)),
            )
        val rtPointerCaptureComponent =
            entity.rtEntity?.getComponents()[0] as FakePointerCaptureComponent
        rtPointerCaptureComponent.onInputEvent(inputEvent)

        // Only compare non-floating point values for stability
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
        val entity = Entity.create(session, "test")
        val rtEntity = (entity as BaseEntity<*>).rtEntity
        assertThat(entity).isNotNull()

        val pointerCaptureComponent =
            PointerCaptureComponent.create(session, directExecutor(), stateListener, inputListener)
        assertThat(entity.addComponent(pointerCaptureComponent)).isTrue()
        assertThat(rtEntity?.getComponents()).hasSize(1)
        assertThat(rtEntity?.getComponents()[0])
            .isInstanceOf(FakePointerCaptureComponent::class.java)

        entity.removeComponent(pointerCaptureComponent)
        assertThat(rtEntity?.getComponents()).hasSize(0)
    }
}
