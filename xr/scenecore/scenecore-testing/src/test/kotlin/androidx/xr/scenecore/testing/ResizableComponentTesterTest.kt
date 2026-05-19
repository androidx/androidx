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
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
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
class ResizableComponentTesterTest {
    @get:Rule val testRule = SceneCoreTestRule()
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
        val component = ResizableComponent.create(session) {}
        val tester1 = ResizableComponentTester.create(component)
        val tester2 = ResizableComponentTester.create(component)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())

        val component3 = ResizableComponent.create(session) {}
        val tester3 = ResizableComponentTester.create(component3)
        assertThat(tester1).isNotEqualTo(tester3)
        assertThat(tester1.hashCode()).isNotEqualTo(tester3.hashCode())
    }

    @Test
    fun onResizeEvent_triggersListener() {
        val component = ResizableComponent.create(session) {}
        val tester = testRule.createTester<ResizableComponentTester>(component)
        val entity = Entity.create(session)
        assertThat(entity.addComponent(component)).isTrue()

        var capturedEvent: ResizeEvent? = null
        component.addResizeEventListener { capturedEvent = it }

        val resizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(2f, 2f, 2f))
        tester.triggerOnResizeEvent(resizeEvent)
        ShadowLooper.idleMainLooper()

        assertThat(capturedEvent).isEqualTo(resizeEvent)
    }
}
