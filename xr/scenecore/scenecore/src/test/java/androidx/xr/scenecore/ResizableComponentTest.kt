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
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.runtime.Dimensions as RtDimensions
import androidx.xr.scenecore.runtime.ResizeEvent as RtResizeEvent
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeResizableComponent
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
class ResizableComponentTest {
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var sceneRuntime: SceneRuntime

    private lateinit var session: Session

    /**
     * A helper class for testing that acts as a [Consumer] for [ResizeEvent]. It counts how many
     * times it has been called and stores the last event it received.
     */
    private class TestResizeListener : Consumer<ResizeEvent> {
        var callCount = 0
            private set // Make the setter private to prevent external modification

        var lastEvent: ResizeEvent? = null
            private set

        override fun accept(event: ResizeEvent) {
            callCount++
            lastEvent = event
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
    fun addResizableComponent_addsRuntimeResizableComponent() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)
    }

    @Test
    fun addResizableComponentDefaultArguments_addsRuntimeResizableComponentWithDefaults() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()
        val resizableComponent = ResizableComponent.create(session) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        val rtMinDimensions = rtResizableComponent.minimumSize
        val rtMaxDimensions = rtResizableComponent.maximumSize

        assertThat(rtMinDimensions.width).isEqualTo(0f)
        assertThat(rtMinDimensions.height).isEqualTo(0f)
        assertThat(rtMinDimensions.depth).isEqualTo(0f)
        assertThat(rtMaxDimensions.width).isEqualTo(10f)
        assertThat(rtMaxDimensions.height).isEqualTo(10f)
        assertThat(rtMaxDimensions.depth).isEqualTo(10f)
    }

    @Test
    fun removeResizableComponent_removesRuntimeResizableComponent() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        entity.removeComponent(resizableComponent)

        assertThat((entity as BaseEntity<*>).rtEntity?.getComponents()).hasSize(0)
        // The listeners map will not be reset after removing the component.
        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)
    }

    @Test
    fun resizableComponent_canAttachOnlyOnce() {
        val entity = Entity.create(session, "test")
        val entity2 = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        assertThat(entity2.addComponent(resizableComponent)).isFalse()
    }

    @Test
    fun resizableComponent_setSizeInvokesRuntimeResizableComponentSetSize() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        val testSize = FloatSize3d(2f, 2f, 0f)
        resizableComponent.affordanceSize = testSize

        assertThat(resizableComponent.affordanceSize).isEqualTo(testSize)
        assertThat(rtResizableComponent.size).isEqualTo(testSize.toRtDimensions())
    }

    @Test
    fun resizableComponent_setMinimumSizeInvokesRuntimeResizableComponentSetMinimumSize() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        val testSize = FloatSize3d(0.5f, 0.6f, 0.7f)
        resizableComponent.minimumEntitySize = testSize

        assertThat(resizableComponent.minimumEntitySize).isEqualTo(testSize)
        assertThat(rtResizableComponent.minimumSize).isEqualTo(testSize.toRtDimensions())
    }

    @Test
    fun resizableComponent_setMaximumSizeInvokesRuntimeResizableComponentSetMaximumSize() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        val testSize = FloatSize3d(5f, 6f, 7f)
        resizableComponent.maximumEntitySize = testSize

        assertThat(resizableComponent.maximumEntitySize).isEqualTo(testSize)
        assertThat(rtResizableComponent.maximumSize).isEqualTo(testSize.toRtDimensions())
    }

    @Test
    fun resizableComponent_setFixedAspectRatioInvokesRuntimeResizableComponentSetFixedAspectRatio() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        val testAspectRatio = true
        resizableComponent.isFixedAspectRatioEnabled = testAspectRatio

        assertThat(resizableComponent.isFixedAspectRatioEnabled).isEqualTo(testAspectRatio)
        assertThat(rtResizableComponent.isFixedAspectRatioEnabled).isEqualTo(testAspectRatio)
    }

    @Test
    fun resizableComponent_setAutoHideContentWhileResizingInvokesRuntimeResizableComponentSetAutoHideContent() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)
        assertThat(resizableComponent.isAutoHideContentWhileResizingEnabled).isTrue()

        resizableComponent.isAutoHideContentWhileResizingEnabled = false

        assertThat(resizableComponent.isAutoHideContentWhileResizingEnabled).isFalse()
        assertThat(rtResizableComponent.autoHideContent).isFalse()
    }

    @Test
    fun resizableComponent_setAutoUpdateOverlayInvokesRuntimeResizableComponentSetAutoUpdateSize() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)
        assertThat(resizableComponent.shouldAutoUpdateOverlay).isTrue()

        resizableComponent.shouldAutoUpdateOverlay = false // default is true

        assertThat(resizableComponent.shouldAutoUpdateOverlay).isFalse()
        assertThat(rtResizableComponent.autoUpdateSize).isFalse()
    }

    @Test
    fun resizableComponent_setAlwaysShowOverlayInvokesRuntimeResizableComponentSetForceShowResizeOverlay() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)
        assertThat(resizableComponent.isAlwaysShowOverlayEnabled).isFalse()

        resizableComponent.isAlwaysShowOverlayEnabled = true // default is false

        assertThat(resizableComponent.isAlwaysShowOverlayEnabled).isTrue()
        assertThat(rtResizableComponent.forceShowResizeOverlay).isTrue()
    }

    @Test
    fun createResizableComponentWithListener_invokesRuntimeAddResizeEventListener() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val initialListener = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
                initialListener,
            )
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        var rtResizeEvent =
            RtResizeEvent(RtResizeEvent.RESIZE_STATE_START, RtDimensions(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event.
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        val expectedStartResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))

        assertThat(initialListener.callCount).isEqualTo(1)
        assertThat(initialListener.lastEvent).isEqualTo(expectedStartResizeEvent)

        rtResizeEvent = RtResizeEvent(RtResizeEvent.RESIZE_STATE_ONGOING, RtDimensions(2f, 2f, 2f))
        val expectedOngoingResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.ONGOING, FloatSize3d(2f, 2f, 2f))
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        rtResizableComponent.onResizeEvent(rtResizeEvent)

        assertThat(initialListener.callCount).isEqualTo(3)
        assertThat(initialListener.lastEvent).isEqualTo(expectedOngoingResizeEvent)

        rtResizeEvent = RtResizeEvent(RtResizeEvent.RESIZE_STATE_END, RtDimensions(2f, 2f, 2f))
        val expectedEndResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.END, FloatSize3d(2f, 2f, 2f))
        rtResizableComponent.onResizeEvent(rtResizeEvent)

        assertThat(initialListener.callCount).isEqualTo(4)
        assertThat(initialListener.lastEvent).isEqualTo(expectedEndResizeEvent)
    }

    @Test
    fun addResizeListener_invokesRuntimeAddResizeEventListener() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizeListener2 = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        resizableComponent.addResizeEventListener(directExecutor(), resizeListener2)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(2)

        var rtResizeEvent =
            RtResizeEvent(RtResizeEvent.RESIZE_STATE_START, RtDimensions(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event.
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        val expectedStartResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))

        assertThat(resizeListener2.callCount).isEqualTo(1)
        assertThat(resizeListener2.lastEvent).isEqualTo(expectedStartResizeEvent)

        rtResizeEvent = RtResizeEvent(RtResizeEvent.RESIZE_STATE_ONGOING, RtDimensions(2f, 2f, 2f))
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        val expectedOngoingResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.ONGOING, FloatSize3d(2f, 2f, 2f))

        assertThat(resizeListener2.callCount).isEqualTo(3)
        assertThat(resizeListener2.lastEvent).isEqualTo(expectedOngoingResizeEvent)

        rtResizeEvent = RtResizeEvent(RtResizeEvent.RESIZE_STATE_END, RtDimensions(2f, 2f, 2f))
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        val expectedEndResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.END, FloatSize3d(2f, 2f, 2f))

        assertThat(resizeListener2.callCount).isEqualTo(4)
        assertThat(resizeListener2.lastEvent).isEqualTo(expectedEndResizeEvent)
    }

    @Test
    fun addMultipleResizeEventListeners_invokesAllListeners() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val initialListener = TestResizeListener()
        val resizeListener2 = TestResizeListener()
        val resizeListener3 = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
                initialListener,
            )
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        resizableComponent.addResizeEventListener(directExecutor(), resizeListener2)
        resizableComponent.addResizeEventListener(directExecutor(), resizeListener3)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(3)

        val rtResizeEvent =
            RtResizeEvent(RtResizeEvent.RESIZE_STATE_START, RtDimensions(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event.
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        val expectedStartResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))

        assertThat(initialListener.callCount).isEqualTo(3)
        assertThat(initialListener.lastEvent).isEqualTo(expectedStartResizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(3)
        assertThat(resizeListener2.lastEvent).isEqualTo(expectedStartResizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(3)
        assertThat(resizeListener2.lastEvent).isEqualTo(expectedStartResizeEvent)
    }

    @Test
    fun removeResizeEventListener_invokesRuntimeRemoveResizeEventListener() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val initialListener = TestResizeListener()
        val resizeListener2 = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
                initialListener,
            )
        val rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        resizableComponent.addResizeEventListener(directExecutor(), resizeListener2)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(2)

        val rtResizeEvent =
            RtResizeEvent(RtResizeEvent.RESIZE_STATE_START, RtDimensions(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event.
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        rtResizableComponent.onResizeEvent(rtResizeEvent)

        val expectedStartResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))

        assertThat(initialListener.callCount).isEqualTo(2)
        assertThat(initialListener.lastEvent).isNotNull()
        assertThat(initialListener.lastEvent).isEqualTo(expectedStartResizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(2)
        assertThat(resizeListener2.lastEvent).isNotNull()
        assertThat(resizeListener2.lastEvent).isEqualTo(expectedStartResizeEvent)

        resizableComponent.removeResizeEventListener(initialListener)
        resizableComponent.removeResizeEventListener(resizeListener2)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(0)
    }

    @Test
    fun resizableComponent_canAttachAgainAfterDetach() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
            ) {}

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        entity.removeComponent(resizableComponent)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
    }

    @Test
    fun resizableComponent_attachAfterDetachPreservesListeners() {
        val entity = Entity.create(session, "test")
        assertThat(entity).isNotNull()

        val initialListener = TestResizeListener()
        val resizeListener2 = TestResizeListener()
        val resizableComponent =
            ResizableComponent.create(
                sceneRuntime,
                FloatSize3d(),
                FloatSize3d(),
                directExecutor(),
                initialListener,
            )
        var rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)

        resizableComponent.addResizeEventListener(directExecutor(), resizeListener2)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(2)

        val rtResizeEvent =
            RtResizeEvent(RtResizeEvent.RESIZE_STATE_START, RtDimensions(1f, 1f, 1f))
        // Invoke the runtime resize event listener with a resize event.
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        val expectedStartResizeEvent =
            ResizeEvent(entity, ResizeEvent.ResizeState.START, FloatSize3d(1f, 1f, 1f))

        assertThat(initialListener.callCount).isEqualTo(2)
        assertThat(initialListener.lastEvent).isNotNull()
        assertThat(initialListener.lastEvent).isEqualTo(expectedStartResizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(2)
        assertThat(resizeListener2.lastEvent).isNotNull()
        assertThat(resizeListener2.lastEvent).isEqualTo(expectedStartResizeEvent)

        // Detach and reattach the resizable component.
        entity.removeComponent(resizableComponent)

        assertThat((entity as BaseEntity<*>).rtEntity?.getComponents()).hasSize(0)

        rtResizableComponent = addAndGetFakeResizableComponent(entity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(3)

        // Invoke the runtime resize event listener with a resize event.
        rtResizableComponent.onResizeEvent(rtResizeEvent)
        rtResizableComponent.onResizeEvent(rtResizeEvent)

        // addComponent two times so initialListener is added two times.
        assertThat(initialListener.callCount).isEqualTo(6)
        assertThat(initialListener.lastEvent).isNotNull()
        assertThat(initialListener.lastEvent).isEqualTo(expectedStartResizeEvent)
        assertThat(resizeListener2.callCount).isEqualTo(4)
        assertThat(resizeListener2.lastEvent).isNotNull()
        assertThat(resizeListener2.lastEvent).isEqualTo(expectedStartResizeEvent)
    }

    @Test
    fun createResizableComponent_callsRuntimeCreateResizableComponent() {
        val resizableComponent = ResizableComponent.create(session) {}
        val view = TextView(activity)
        val panelEntity = PanelEntity.create(session, view, IntSize2d(720, 480), "test")
        val rtResizableComponent = addAndGetFakeResizableComponent(panelEntity, resizableComponent)

        assertThat(rtResizableComponent.resizeEventListenersMap).hasSize(1)
    }

    /**
     * A helper function to add a ResizableComponent to an entity, perform common assertions, and
     * return the underlying FakeResizableComponent.
     *
     * @param entity The entity to which the component will be added.
     * @param resizableComponent The component to add.
     * @return The underlying [FakeResizableComponent] instance from the runtime.
     */
    private fun addAndGetFakeResizableComponent(
        entity: Entity,
        resizableComponent: ResizableComponent,
    ): FakeResizableComponent {
        // 1. Add the component and assert it was successful.
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        // 2. Get the underlying runtime components.
        val rtComponents = (entity as BaseEntity<*>).rtEntity?.getComponents()
        assertThat(rtComponents).isNotNull()
        assertThat(rtComponents).hasSize(1)

        // 3. Assert the component is the correct fake type.
        val rtComponent = rtComponents!![0]
        assertThat(rtComponent).isInstanceOf(FakeResizableComponent::class.java)

        // 4. Return the casted component.
        return rtComponent as FakeResizableComponent
    }
}
