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

package androidx.xr.scenecore.testrule

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.ResizableComponentTester
import androidx.xr.scenecore.testing.SceneCoreTestRule
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
class TestRuleResizableComponentTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    private lateinit var session: Session

    /**
     * A helper class for testing that acts as a [Consumer] for [ResizeEvent]. It counts how many
     * times it has been called and stores the last event it received.
     */
    private class TestResizeListener : Consumer<ResizeEvent> {
        var callCount = 0
            private set

        var lastEvent: ResizeEvent? = null
            private set

        override fun accept(event: ResizeEvent) {
            callCount++
            lastEvent = event
        }
    }

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
    fun addResizableComponent_addsRuntimeResizableComponent() {
        val entity = Entity.create(session, "test")
        val resizableComponent =
            ResizableComponent.create(session, FloatSize3d(), FloatSize3d(), directExecutor()) {}

        assertThat(entity.addComponent(resizableComponent)).isTrue()

        // Verifies addition by ensuring a tester can be successfully created for the component.
        assertThat(scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent))
            .isNotNull()
    }

    @Test
    fun removeResizableComponent_removesRuntimeResizableComponent() {
        val entity = Entity.create(session, "test")
        val resizableComponent =
            ResizableComponent.create(session, FloatSize3d(), FloatSize3d(), directExecutor()) {}

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        assertThat(scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent))
            .isNotNull()

        entity.removeComponent(resizableComponent)

        assertThat(entity.getComponents()).doesNotContain(resizableComponent)

        // Verifies removal by ensuring the component can be added to another entity.
        // This indirectly confirms it's no longer attached to the original entity.
        val entity2 = Entity.create(session, "test2")
        assertThat(entity2.addComponent(resizableComponent)).isTrue()
    }

    @Test
    fun resizableComponent_canAttachOnlyOnce() {
        val entity = Entity.create(session, "test")
        val entity2 = Entity.create(session, "test")

        val resizableComponent = ResizableComponent.create(session) {}

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        assertThat(entity2.addComponent(resizableComponent)).isFalse()
    }

    @Test
    fun createResizableComponentWithListener_invokesRuntimeAddResizeEventListener() {
        val entity = Entity.create(session, "test")
        val initialListener = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                session,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
                initialListener,
            )

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val tester = scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent)

        val resizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event via tester.
        tester.triggerOnResizeEvent(resizeEvent)

        assertThat(initialListener.callCount).isEqualTo(1)
        assertThat(initialListener.lastEvent).isEqualTo(resizeEvent)

        val ongoingResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.ONGOING, FloatSize3d(2f, 2f, 2f))
        tester.triggerOnResizeEvent(ongoingResizeEvent)
        tester.triggerOnResizeEvent(ongoingResizeEvent)

        assertThat(initialListener.callCount).isEqualTo(3)
        assertThat(initialListener.lastEvent).isEqualTo(ongoingResizeEvent)

        val endResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.END, FloatSize3d(2f, 2f, 2f))
        tester.triggerOnResizeEvent(endResizeEvent)

        assertThat(initialListener.callCount).isEqualTo(4)
        assertThat(initialListener.lastEvent).isEqualTo(endResizeEvent)
    }

    @Test
    fun addResizeListener_invokesRuntimeAddResizeEventListener() {
        val entity = Entity.create(session, "test")
        val resizeListener2 = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(session, FloatSize3d(), FloatSize3d(), directExecutor()) {}

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val tester = scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent)

        resizableComponent.addResizeEventListener(directExecutor(), resizeListener2)

        val resizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event via tester.
        tester.triggerOnResizeEvent(resizeEvent)

        assertThat(resizeListener2.callCount).isEqualTo(1)
        assertThat(resizeListener2.lastEvent).isEqualTo(resizeEvent)

        val ongoingResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.ONGOING, FloatSize3d(2f, 2f, 2f))
        tester.triggerOnResizeEvent(ongoingResizeEvent)
        tester.triggerOnResizeEvent(ongoingResizeEvent)

        assertThat(resizeListener2.callCount).isEqualTo(3)
        assertThat(resizeListener2.lastEvent).isEqualTo(ongoingResizeEvent)

        val endResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.END, FloatSize3d(2f, 2f, 2f))
        tester.triggerOnResizeEvent(endResizeEvent)

        assertThat(resizeListener2.callCount).isEqualTo(4)
        assertThat(resizeListener2.lastEvent).isEqualTo(endResizeEvent)
    }

    @Test
    fun addMultipleResizeEventListeners_invokesAllListeners() {
        val entity = Entity.create(session, "test")
        val initialListener = TestResizeListener()
        val resizeListener2 = TestResizeListener()
        val resizeListener3 = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                session,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
                initialListener,
            )

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val tester = scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent)

        resizableComponent.addResizeEventListener(directExecutor(), resizeListener2)
        resizableComponent.addResizeEventListener(directExecutor(), resizeListener3)

        val resizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event via tester.
        tester.triggerOnResizeEvent(resizeEvent)
        tester.triggerOnResizeEvent(resizeEvent)
        tester.triggerOnResizeEvent(resizeEvent)

        assertThat(initialListener.callCount).isEqualTo(3)
        assertThat(initialListener.lastEvent).isEqualTo(resizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(3)
        assertThat(resizeListener2.lastEvent).isEqualTo(resizeEvent)
        assertThat(resizeListener3.callCount).isEqualTo(3)
        assertThat(resizeListener3.lastEvent).isEqualTo(resizeEvent)
    }

    @Test
    fun removeResizeEventListener_invokesRuntimeRemoveResizeEventListener() {
        val entity = Entity.create(session, "test")
        val initialListener = TestResizeListener()
        val resizeListener2 = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                session,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
                initialListener,
            )

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val tester = scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent)

        resizableComponent.addResizeEventListener(directExecutor(), resizeListener2)

        val resizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event via tester.
        tester.triggerOnResizeEvent(resizeEvent)
        tester.triggerOnResizeEvent(resizeEvent)

        assertThat(initialListener.callCount).isEqualTo(2)
        assertThat(initialListener.lastEvent).isEqualTo(resizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(2)
        assertThat(resizeListener2.lastEvent).isEqualTo(resizeEvent)

        resizableComponent.removeResizeEventListener(initialListener)
        resizableComponent.removeResizeEventListener(resizeListener2)

        tester.triggerOnResizeEvent(resizeEvent)
        // Listener counts should not increase.
        assertThat(initialListener.callCount).isEqualTo(2)
        assertThat(resizeListener2.callCount).isEqualTo(2)
    }

    @Test
    fun resizableComponent_canAttachAgainAfterDetach() {
        val entity = Entity.create(session, "test")
        val resizableComponent = ResizableComponent.create(session) {}

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        entity.removeComponent(resizableComponent)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
    }

    @Test
    fun resizableComponent_attachAfterDetachPreservesListeners() {
        val entity = Entity.create(session, "test")
        val initialListener = TestResizeListener()
        val resizeListener2 = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                session,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
                initialListener,
            )

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        var tester = scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent)

        resizableComponent.addResizeEventListener(directExecutor(), resizeListener2)

        val resizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event via tester.
        tester.triggerOnResizeEvent(resizeEvent)
        tester.triggerOnResizeEvent(resizeEvent)

        assertThat(initialListener.callCount).isEqualTo(2)
        assertThat(initialListener.lastEvent).isEqualTo(resizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(2)
        assertThat(resizeListener2.lastEvent).isEqualTo(resizeEvent)

        // Detach and reattach the resizable component.
        entity.removeComponent(resizableComponent)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        tester = scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent)

        // Invoke the runtime resize event listener with a resize event via tester.
        tester.triggerOnResizeEvent(resizeEvent)
        tester.triggerOnResizeEvent(resizeEvent)

        // initialListener and resizeListener2 should each have been called 4 times total.
        assertThat(initialListener.callCount).isEqualTo(4)
        assertThat(initialListener.lastEvent).isEqualTo(resizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(4)
        assertThat(resizeListener2.lastEvent).isEqualTo(resizeEvent)
    }

    @Test
    fun createResizableComponent_callsRuntimeCreateResizableComponent() {
        val resizableComponent = ResizableComponent.create(session) {}
        val view = TextView(activity)
        val panelEntity =
            PanelEntity.create(
                session,
                view,
                IntSize2d(720, 480),
                "test",
                parent = session.scene.activitySpace,
            )

        assertThat(panelEntity.addComponent(resizableComponent)).isTrue()
        assertThat(scenecoreTestRule.createTester<ResizableComponentTester>(resizableComponent))
            .isNotNull()
    }
}
