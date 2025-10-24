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

package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.PixelDimensions as RtPixelDimensions
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialCapabilities
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainPanelEntityTest {
    private val fakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
    private val activity = activityController.create().start().get()
    private val mockSceneRuntime = mock<SceneRuntime>()

    lateinit var session: Session

    @Before
    fun setUp() {
        whenever(mockSceneRuntime.spatialEnvironment).thenReturn(mock())
        val mockActivitySpace = mock<RtActivitySpace>()
        whenever(mockSceneRuntime.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockSceneRuntime.headActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockSceneRuntime.mainPanelEntity).thenReturn(mock())
        whenever(mockSceneRuntime.spatialCapabilities).thenReturn(SpatialCapabilities(0))
        session =
            Session(
                activity,
                runtimes =
                    listOf(fakePerceptionRuntimeFactory.createRuntime(activity), mockSceneRuntime),
            )
        session.configure(Config(headTracking = Config.HeadTrackingMode.LAST_KNOWN))
    }

    @Test
    fun addPerceivedResolutionChangedListener_callsRuntimeAddPerceivedResolutionChangedListener() {
        val listener = Consumer<IntSize2d> {}
        val executor = directExecutor()
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        verify(mockSceneRuntime).addPerceivedResolutionChangedListener(eq(executor), any())
    }

    @Test
    fun addPerceivedResolutionChangedListener_withNoExecutor_callsRuntimeWithMainThreadExecutor() {
        val listener = Consumer<IntSize2d> {}
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(listener)
        verify(mockSceneRuntime)
            .addPerceivedResolutionChangedListener(eq(HandlerExecutor.mainThreadExecutor), any())
    }

    @Test
    fun addPerceivedResolutionChangedListener_withoutDeviceTracking_throwsIllegalStateException() {
        // Disable head tracking
        session.configure(Config(deviceTracking = Config.DeviceTrackingMode.DISABLED))

        val listener = Consumer<IntSize2d> {}
        val exception =
            assertFailsWith<IllegalStateException> {
                session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(listener)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("Config.DeviceTrackingMode is not set to LastKnown.")
    }

    @Test
    fun removePerceivedResolutionChangedListener_callsRuntimeRemovePerceivedResolutionChangedListener() {
        val listener = Consumer<IntSize2d> {}
        // Add the listener first so there's something to remove
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(
            directExecutor(),
            listener,
        )
        val rtListenerCaptor = argumentCaptor<Consumer<RtPixelDimensions>>()
        verify(mockSceneRuntime)
            .addPerceivedResolutionChangedListener(any(), rtListenerCaptor.capture())

        session.scene.mainPanelEntity.removePerceivedResolutionChangedListener(listener)
        verify(mockSceneRuntime)
            .removePerceivedResolutionChangedListener(eq(rtListenerCaptor.firstValue))
    }

    @Test
    fun perceivedResolutionChangedListener_isCalledWithConvertedValues() {
        var receivedDimensions: IntSize2d? = null
        val listener = Consumer<IntSize2d> { dims -> receivedDimensions = dims }
        val rtListenerCaptor = argumentCaptor<Consumer<RtPixelDimensions>>()
        val executor = directExecutor()

        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        verify(mockSceneRuntime)
            .addPerceivedResolutionChangedListener(eq(executor), rtListenerCaptor.capture())

        val rtListener = rtListenerCaptor.firstValue

        val testRtDimensions = RtPixelDimensions(100, 200)
        rtListener.accept(testRtDimensions)

        assertThat(receivedDimensions).isNotNull()
        assertThat(receivedDimensions!!.width).isEqualTo(100)
        assertThat(receivedDimensions.height).isEqualTo(200)

        // Simulate another callback
        val anotherRtDimensions = RtPixelDimensions(300, 400)
        rtListener.accept(anotherRtDimensions)
        assertThat(receivedDimensions.width).isEqualTo(300)
        assertThat(receivedDimensions.height).isEqualTo(400)
    }

    @Test
    fun addMultiplePerceivedResolutionListeners_allAreRegisteredAndCalled() {
        val listener1 = mock<Consumer<IntSize2d>>()
        val listener2 = mock<Consumer<IntSize2d>>()
        val rtListenerCaptor = argumentCaptor<Consumer<RtPixelDimensions>>()
        val executor = directExecutor()

        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener1)
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener2)

        verify(mockSceneRuntime, times(2))
            .addPerceivedResolutionChangedListener(eq(executor), rtListenerCaptor.capture())

        val rtListeners = rtListenerCaptor.allValues
        assertThat(rtListeners).hasSize(2)

        // Simulate callback for the first registered listener only
        val testRtDimensions1 = RtPixelDimensions(10, 20)
        rtListeners[0].accept(testRtDimensions1)
        verify(listener1).accept(IntSize2d(10, 20))
        verify(listener2, never()).accept(any())

        // Simulate callback for the second registered listener
        val testRtDimensions2 = RtPixelDimensions(30, 40)
        rtListeners[1].accept(testRtDimensions2)
        verify(listener1).accept(IntSize2d(10, 20)) // Still called once
        verify(listener2).accept(IntSize2d(30, 40))
    }

    @Test
    fun dispose_removesPerceivedResolutionChangedListener() {
        val listener = Consumer<IntSize2d> {}
        val executor = directExecutor()
        val mainPanelEntity = session.scene.mainPanelEntity

        mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        val rtListenerCaptor = argumentCaptor<Consumer<RtPixelDimensions>>()
        verify(mockSceneRuntime)
            .addPerceivedResolutionChangedListener(eq(executor), rtListenerCaptor.capture())

        mainPanelEntity.dispose()

        verify(mockSceneRuntime)
            .removePerceivedResolutionChangedListener(rtListenerCaptor.firstValue)
    }
}
