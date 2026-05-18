/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class InteractableComponentTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(
                context = activity,
                coroutineContext = testDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val component = InteractableComponent.create(session) {}
        val tester1 = InteractableComponentTester.create(component)
        val tester2 = InteractableComponentTester.create(component)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun interactableComponent_propagatesHitInfoInInputEvents() {
        var inputEvent: InputEvent? = null
        val inputEventListener = Consumer<InputEvent> { event -> inputEvent = event }
        val interactableComponent =
            InteractableComponent.create(session, directExecutor(), inputEventListener)
        val tester = testRule.createTester<InteractableComponentTester>(interactableComponent)
        val entity = Entity.create(session)

        assertThat(entity.addComponent(interactableComponent)).isTrue()

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

        tester.triggerOnInputEvent(expectedInputEvent)

        val event = requireNotNull(inputEvent) { "Input event should not be null" }

        assertThat(event.source).isEqualTo(InputEvent.Source.HANDS)
        assertThat(event.pointerType).isEqualTo(InputEvent.Pointer.RIGHT)
        assertThat(event.timestamp).isEqualTo(123456789L)
        assertThat(event.action).isEqualTo(InputEvent.Action.DOWN)
        assertThat(event.hitInfoList).isNotEmpty()
        assertThat(event.hitInfoList).hasSize(1)

        val hitInfo = event.hitInfoList[0]
        assertThat(hitInfo).isNotNull()
        assertThat(hitInfo.inputEntity).isEqualTo(entity)
        assertThat(hitInfo.hitPosition).isEqualTo(Vector3.One)
        assertThat(hitInfo.transform).isEqualTo(Matrix4.Identity)
    }
}
