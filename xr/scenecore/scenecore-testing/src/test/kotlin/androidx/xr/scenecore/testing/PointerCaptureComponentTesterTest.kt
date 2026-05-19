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
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.PointerCaptureComponent
import androidx.xr.scenecore.PointerCaptureComponent.PointerCaptureState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PointerCaptureComponentTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()
    private lateinit var session: Session

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

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
        val component = PointerCaptureComponent.create(session, { it.run() }, {}, {})
        val tester1 = PointerCaptureComponentTester.create(component)
        val tester2 = PointerCaptureComponentTester.create(component)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun triggerOnStateChanged_triggersListener() {
        var capturedState: PointerCaptureState? = null
        val component =
            PointerCaptureComponent.create(session, { it.run() }, { capturedState = it }, {})
        val tester = testRule.createTester<PointerCaptureComponentTester>(component)

        tester.triggerOnStateChanged(PointerCaptureState.ACTIVE)
        assertThat(capturedState).isEqualTo(PointerCaptureState.ACTIVE)

        tester.triggerOnStateChanged(PointerCaptureState.PAUSED)
        assertThat(capturedState).isEqualTo(PointerCaptureState.PAUSED)

        tester.triggerOnStateChanged(PointerCaptureState.STOPPED)
        assertThat(capturedState).isEqualTo(PointerCaptureState.STOPPED)
    }

    @Test
    fun triggerOnInputEvent_triggersListener() {
        var capturedEvent: InputEvent? = null
        val component =
            PointerCaptureComponent.create(session, { it.run() }, {}, { capturedEvent = it })
        val tester = testRule.createTester<PointerCaptureComponentTester>(component)
        val entity = Entity.create(session)
        assertThat(entity.addComponent(component)).isTrue()

        val inputEvent =
            InputEvent(
                source = InputEvent.Source.HANDS,
                pointerType = InputEvent.Pointer.DEFAULT,
                timestamp = 1000L,
                origin = Vector3.Zero,
                direction = Vector3.Forward,
                action = InputEvent.Action.DOWN,
                hitInfoList = emptyList(),
            )

        tester.triggerOnInputEvent(inputEvent)
        ShadowLooper.idleMainLooper()

        assertThat(capturedEvent).isEqualTo(inputEvent)
    }
}
