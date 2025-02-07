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
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.secondValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResizableComponentTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockRuntime = mock<JxrPlatformAdapter>()
    private lateinit var session: Session
    private val mockContentlessEntity = mock<JxrPlatformAdapter.Entity>()

    @Before
    fun setUp() {
        whenever(mockRuntime.spatialEnvironment).thenReturn(mock())
        whenever(mockRuntime.activitySpace).thenReturn(mock())
        whenever(mockRuntime.activitySpaceRootImpl).thenReturn(mock())
        whenever(mockRuntime.headActivityPose).thenReturn(mock())
        whenever(mockRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockRuntime.getMainPanelEntity()).thenReturn(mock())
        whenever(mockRuntime.createEntity(any(), any(), any())).thenReturn(mockContentlessEntity)
        session = Session.create(activity, mockRuntime)
    }

    @Test
    fun addResizableComponent_addsRuntimeResizableComponent() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createResizableComponent(any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime, Dimensions(), Dimensions())

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        verify(mockRuntime).createResizableComponent(any(), any())
        verify(mockContentlessEntity).addComponent(any())
    }

    @Test
    fun addResizableComponentDefaultArguments_addsRuntimeResizableComponentWithDefaults() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createResizableComponent(any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.Dimensions::class.java)
        verify(mockRuntime).createResizableComponent(captor.capture(), captor.capture())
        val rtMinDimensions = captor.firstValue
        val rtMaxDimensions = captor.secondValue
        assertThat(rtMinDimensions.width).isEqualTo(0f)
        assertThat(rtMinDimensions.height).isEqualTo(0f)
        assertThat(rtMinDimensions.depth).isEqualTo(0f)
        assertThat(rtMaxDimensions.width).isEqualTo(10f)
        assertThat(rtMaxDimensions.height).isEqualTo(10f)
        assertThat(rtMaxDimensions.depth).isEqualTo(10f)
        verify(mockContentlessEntity).addComponent(any())
    }

    @Test
    fun removeResizableComponent_removesRuntimeResizableComponent() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createResizableComponent(any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        entity.removeComponent(resizableComponent)
        verify(mockContentlessEntity).removeComponent(any())
    }

    @Test
    fun resizableComponent_canAttachOnlyOnce() {
        val entity = ContentlessEntity.create(session, "test")
        val entity2 = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createResizableComponent(any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        assertThat(entity2.addComponent(resizableComponent)).isFalse()
    }

    @Test
    fun resizableComponent_setSizeInvokesRuntimeResizableComponentSetSize() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val testSize = Dimensions(2f, 2f, 0f)
        resizableComponent.size = testSize

        assertThat(resizableComponent.size).isEqualTo(testSize)
        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.Dimensions::class.java)
        verify(mockRtResizableComponent).setSize(captor.capture())
        val rtDimensions = captor.value
        assertThat(rtDimensions.width).isEqualTo(2f)
        assertThat(rtDimensions.height).isEqualTo(2f)
        assertThat(rtDimensions.depth).isEqualTo(0f)
    }

    @Test
    fun resizableComponent_setMinimumSizeInvokesRuntimeResizableComponentSetMinimumSize() {
        val entity = ContentlessEntity.create(session, "test")
        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.Dimensions::class.java)
        assertThat(entity).isNotNull()

        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val testSize = Dimensions(0.5f, 0.6f, 0.7f)
        resizableComponent.minimumSize = testSize

        assertThat(resizableComponent.minimumSize).isEqualTo(testSize)
        verify(mockRtResizableComponent).setMinimumSize(captor.capture())
        val rtDimensions = captor.value
        assertThat(rtDimensions.width).isEqualTo(0.5f)
        assertThat(rtDimensions.height).isEqualTo(0.6f)
        assertThat(rtDimensions.depth).isEqualTo(0.7f)
    }

    @Test
    fun resizableComponent_setMaximumSizeInvokesRuntimeResizableComponentSetMaximumSize() {
        val entity = ContentlessEntity.create(session, "test")
        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.Dimensions::class.java)
        assertThat(entity).isNotNull()

        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val testSize = Dimensions(5f, 6f, 7f)
        resizableComponent.maximumSize = testSize

        assertThat(resizableComponent.maximumSize).isEqualTo(testSize)
        verify(mockRtResizableComponent).setMaximumSize(captor.capture())
        val rtDimensions = captor.value
        assertThat(rtDimensions.width).isEqualTo(5f)
        assertThat(rtDimensions.height).isEqualTo(6f)
        assertThat(rtDimensions.depth).isEqualTo(7f)
    }

    @Test
    fun resizableComponent_setFixedAspectRatioInvokesRuntimeResizableComponentSetFixedAspectRatio() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        val testAspectRatio = 1.23f
        resizableComponent.fixedAspectRatio = testAspectRatio

        assertThat(resizableComponent.fixedAspectRatio).isEqualTo(testAspectRatio)
        val captor = ArgumentCaptor.forClass(Float::class.java)
        verify(mockRtResizableComponent).setFixedAspectRatio(captor.capture())
        assertThat(captor.value).isEqualTo(testAspectRatio)
    }

    @Test
    fun resizableComponent_setAutoHideContentInvokesRuntimeResizableComponentSetAutoHideContent() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        resizableComponent.autoHideContent = false // default is true

        assertThat(resizableComponent.autoHideContent).isFalse()
        val captor = ArgumentCaptor.forClass(Boolean::class.java)
        verify(mockRtResizableComponent).setAutoHideContent(captor.capture())
        assertThat(captor.value).isFalse()
    }

    @Test
    fun resizableComponent_setAutoUpdateSizeInvokesRuntimeResizableComponentSetAutoUpdateSize() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        resizableComponent.autoUpdateSize = false // default is true

        assertThat(resizableComponent.autoUpdateSize).isFalse()
        val captor = ArgumentCaptor.forClass(Boolean::class.java)
        verify(mockRtResizableComponent).setAutoUpdateSize(captor.capture())
        assertThat(captor.value).isFalse()
    }

    @Test
    fun resizableComponent_setForceShowResizeOverlayInvokesRuntimeResizableComponentSetForceShowResizeOverlay() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()

        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()

        resizableComponent.forceShowResizeOverlay = true // default is false

        assertThat(resizableComponent.forceShowResizeOverlay).isTrue()
        val captor = ArgumentCaptor.forClass(Boolean::class.java)
        verify(mockRtResizableComponent).setForceShowResizeOverlay(captor.capture())
        assertThat(captor.value).isTrue()
    }

    @Test
    fun addResizeListener_invokesRuntimeAddResizeEventListener() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val mockResizeListener = mock<ResizeListener>()
        resizableComponent.addResizeListener(directExecutor(), mockResizeListener)

        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.ResizeEventListener::class.java)
        // Capture the runtime resize event listener that is provided to the runtime resizable
        // component.
        verify(mockRtResizableComponent).addResizeEventListener(any(), captor.capture())
        val rtResizeEventListener = captor.value
        var rtResizeEvent =
            JxrPlatformAdapter.ResizeEvent(
                JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_START,
                JxrPlatformAdapter.Dimensions(1f, 1f, 1f),
            )
        // Invoke the runtime resize event listener with a resize event.
        rtResizeEventListener.onResizeEvent(rtResizeEvent)
        verify(mockResizeListener).onResizeStart(any(), any())
        rtResizeEvent =
            JxrPlatformAdapter.ResizeEvent(
                JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_ONGOING,
                JxrPlatformAdapter.Dimensions(2f, 2f, 2f),
            )
        rtResizeEventListener.onResizeEvent(rtResizeEvent)
        rtResizeEventListener.onResizeEvent(rtResizeEvent)
        verify(mockResizeListener, times(2)).onResizeUpdate(any(), any())
        rtResizeEvent =
            JxrPlatformAdapter.ResizeEvent(
                JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_END,
                JxrPlatformAdapter.Dimensions(2f, 2f, 2f),
            )
        rtResizeEventListener.onResizeEvent(rtResizeEvent)
        verify(mockResizeListener).onResizeEnd(any(), any())
    }

    @Test
    fun addMultipleResizeEventListeners_invokesAllListeners() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val mockResizeListener = mock<ResizeListener>()
        resizableComponent.addResizeListener(directExecutor(), mockResizeListener)
        val mockResizeListener2 = mock<ResizeListener>()
        resizableComponent.addResizeListener(directExecutor(), mockResizeListener2)

        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.ResizeEventListener::class.java)
        // Capture the runtime resize event listener that is provided to the runtime resizable
        // component.
        verify(mockRtResizableComponent, times(2)).addResizeEventListener(any(), captor.capture())
        val rtResizeEventListener1 = captor.allValues[0]
        val rtResizeEventListener2 = captor.allValues[1]
        val rtResizeEvent =
            JxrPlatformAdapter.ResizeEvent(
                JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_START,
                JxrPlatformAdapter.Dimensions(1f, 1f, 1f),
            )
        // Invoke the runtime resize event listener with a resize event.
        rtResizeEventListener1.onResizeEvent(rtResizeEvent)
        rtResizeEventListener2.onResizeEvent(rtResizeEvent)
        verify(mockResizeListener).onResizeStart(any(), any())
        verify(mockResizeListener2).onResizeStart(any(), any())
    }

    @Test
    fun removeResizeEventListener_invokesRuntimeRemoveResizeEventListener() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val mockResizeListener = mock<ResizeListener>()
        resizableComponent.addResizeListener(directExecutor(), mockResizeListener)
        val mockResizeListener2 = mock<ResizeListener>()
        resizableComponent.addResizeListener(directExecutor(), mockResizeListener2)

        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.ResizeEventListener::class.java)
        // Capture the runtime resize event listener that is provided to the runtime resizable
        // component.
        verify(mockRtResizableComponent, times(2)).addResizeEventListener(any(), captor.capture())
        val rtResizeEventListener1 = captor.allValues[0]
        val rtResizeEventListener2 = captor.allValues[1]
        val rtResizeEvent =
            JxrPlatformAdapter.ResizeEvent(
                JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_START,
                JxrPlatformAdapter.Dimensions(1f, 1f, 1f),
            )
        // Invoke the runtime resize event listener with a resize event.
        rtResizeEventListener1.onResizeEvent(rtResizeEvent)
        rtResizeEventListener2.onResizeEvent(rtResizeEvent)
        verify(mockResizeListener).onResizeStart(any(), any())
        verify(mockResizeListener2).onResizeStart(any(), any())

        resizableComponent.removeResizeListener(mockResizeListener)
        resizableComponent.removeResizeListener(mockResizeListener2)
        verify(mockRtResizableComponent).removeResizeEventListener(rtResizeEventListener1)
        verify(mockRtResizableComponent).removeResizeEventListener(rtResizeEventListener2)
    }

    @Test
    fun resizableComponent_canAttachAgainAfterDetach() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        whenever(mockRuntime.createResizableComponent(any(), any())).thenReturn(mock())
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        entity.removeComponent(resizableComponent)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
    }

    @Test
    fun resizableComponent_attachAfterDetachPreservesListeners() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
        val mockRtResizableComponent = mock<JxrPlatformAdapter.ResizableComponent>()
        whenever(mockRuntime.createResizableComponent(any(), any()))
            .thenReturn(mockRtResizableComponent)
        whenever(mockContentlessEntity.addComponent(any())).thenReturn(true)
        val resizableComponent = ResizableComponent.create(mockRuntime)

        assertThat(entity.addComponent(resizableComponent)).isTrue()
        val mockResizeListener = mock<ResizeListener>()
        resizableComponent.addResizeListener(directExecutor(), mockResizeListener)
        val mockResizeListener2 = mock<ResizeListener>()
        resizableComponent.addResizeListener(directExecutor(), mockResizeListener2)

        val captor = ArgumentCaptor.forClass(JxrPlatformAdapter.ResizeEventListener::class.java)
        // Capture the runtime resize event listener that is provided to the runtime resizable
        // component.
        verify(mockRtResizableComponent, times(2)).addResizeEventListener(any(), captor.capture())
        val rtResizeEventListener1 = captor.allValues[0]
        val rtResizeEventListener2 = captor.allValues[1]
        val rtResizeEvent =
            JxrPlatformAdapter.ResizeEvent(
                JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_START,
                JxrPlatformAdapter.Dimensions(1f, 1f, 1f),
            )
        // Invoke the runtime resize event listener with a resize event.
        rtResizeEventListener1.onResizeEvent(rtResizeEvent)
        rtResizeEventListener2.onResizeEvent(rtResizeEvent)
        verify(mockResizeListener).onResizeStart(any(), any())
        verify(mockResizeListener2).onResizeStart(any(), any())

        // Detach and reattach the resizable component.
        entity.removeComponent(resizableComponent)
        assertThat(entity.addComponent(resizableComponent)).isTrue()
        // Invoke the runtime resize event listener with a resize event.
        rtResizeEventListener1.onResizeEvent(rtResizeEvent)
        rtResizeEventListener2.onResizeEvent(rtResizeEvent)
        verify(mockResizeListener, times(2)).onResizeStart(any(), any())
        verify(mockResizeListener2, times(2)).onResizeStart(any(), any())
    }
}
