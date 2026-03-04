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

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.InputEvent as RtInputEvent
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeInteractableComponent
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
class InteractableComponentTest {
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var sceneRuntime: SceneRuntime

    private lateinit var session: Session
    private val entity by lazy { Entity.create(session, "test") }

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
    }

    @Test
    fun addInteractableComponent_addsRuntimeInteractableComponent() {
        assertThat(entity).isNotNull()

        val inputEventListener = Consumer<InputEvent> {}
        val executor = directExecutor()
        val interactableComponent =
            InteractableComponent.create(session, executor, inputEventListener)
        val rtEntity = (entity as BaseEntity<*>).rtEntity

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        assertThat(rtEntity?.getComponents()?.toList()[0])
            .isInstanceOf(FakeInteractableComponent::class.java)
    }

    @Test
    fun removeInteractableComponent_removesRuntimeInteractableComponent() {
        assertThat(entity).isNotNull()

        val inputEventListener = Consumer<InputEvent> {}
        val executor = directExecutor()
        val interactableComponent =
            InteractableComponent.create(session, executor, inputEventListener)
        val rtEntity = (entity as BaseEntity<*>).rtEntity

        assertThat(entity.addComponent(interactableComponent)).isTrue()

        entity.removeComponent(interactableComponent)

        assertThat(rtEntity?.getComponents()).hasSize(0)
    }

    @Test
    fun interactableComponent_canAttachOnlyOnce() {
        val entity2 = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val inputEventListener = Consumer<InputEvent> {}
        val executor = directExecutor()
        val interactableComponent =
            InteractableComponent.create(session, executor, inputEventListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        assertThat(entity2.addComponent(interactableComponent)).isFalse()
    }

    @Test
    fun interactableComponent_canAttachAgainAfterDetach() {
        assertThat(entity).isNotNull()

        val inputEventListener = Consumer<InputEvent> {}
        val executor = directExecutor()
        val interactableComponent =
            InteractableComponent.create(session, executor, inputEventListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        entity.removeComponent(interactableComponent)
        assertThat(entity.addComponent(interactableComponent)).isTrue()
    }

    @Test
    fun interactableComponent_propagatesHitInfoInInputEvents() {
        var inputEvent: InputEvent? = null
        val inputEventListener = Consumer<InputEvent> { event -> inputEvent = event }
        val interactableComponent =
            InteractableComponent.create(session, directExecutor(), inputEventListener)
        val rtEntity = (entity as BaseEntity<*>).rtEntity

        assertThat(entity.addComponent(interactableComponent)).isTrue()

        val rtInputEvent =
            RtInputEvent(
                RtInputEvent.Source.HANDS,
                RtInputEvent.Pointer.RIGHT,
                123456789L,
                Vector3.Zero,
                Vector3.One,
                RtInputEvent.Action.DOWN,
                listOf(RtInputEvent.HitInfo(rtEntity!!, Vector3.One, Matrix4.Identity)),
            )
        // Simulates an input event from runtime.
        (rtEntity.getComponents()[0] as FakeInteractableComponent).onInputEvent(rtInputEvent)

        assertThat(inputEvent).isNotNull()
        assertThat(inputEvent!!.source).isEqualTo(InputEvent.Source.HANDS)
        assertThat(inputEvent.pointerType).isEqualTo(InputEvent.Pointer.RIGHT)
        assertThat(inputEvent.timestamp).isEqualTo(rtInputEvent.timestamp)
        assertThat(inputEvent.action).isEqualTo(InputEvent.Action.DOWN)
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
        val inputEventListener = Consumer<InputEvent> {}
        val interactableComponent =
            InteractableComponent.create(session, directExecutor(), inputEventListener)
        val view = TextView(activity)
        val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")

        assertThat(panelEntity.addComponent(interactableComponent)).isTrue()
        assertThat(panelEntity.rtEntity?.getComponents()?.toList()[0])
            .isInstanceOf(FakeInteractableComponent::class.java)
    }
}
