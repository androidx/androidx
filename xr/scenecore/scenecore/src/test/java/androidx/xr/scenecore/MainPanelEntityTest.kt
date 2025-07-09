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

import android.app.Activity
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PixelDimensions as RtPixelDimensions
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
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
    private val fakeRuntimeFactory = FakeRuntimeFactory()
    private val activityController = Robolectric.buildActivity(Activity::class.java)
    private val activity = activityController.create().start().get()
    private val mockPlatformAdapter = mock<JxrPlatformAdapter>()
    lateinit var session: Session

    @Before
    fun setUp() {
        whenever(mockPlatformAdapter.spatialEnvironment).thenReturn(mock())
        val mockActivitySpace = mock<RtActivitySpace>()
        whenever(mockPlatformAdapter.activitySpace).thenReturn(mockActivitySpace)
        whenever(mockPlatformAdapter.headActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.activitySpaceRootImpl).thenReturn(mockActivitySpace)
        whenever(mockPlatformAdapter.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.mainPanelEntity).thenReturn(mock())
        session = Session(activity, fakeRuntimeFactory.createRuntime(activity), mockPlatformAdapter)
    }

    @Test
    fun addPerceivedResolutionChangedListener_callsRuntimeAddPerceivedResolutionChangedListener() {
        val listener = Consumer<IntSize2d> {}
        val executor = directExecutor()
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        verify(mockPlatformAdapter).addPerceivedResolutionChangedListener(eq(executor), any())
    }

    @Test
    fun addPerceivedResolutionChangedListener_withNoExecutor_callsRuntimeWithMainThreadExecutor() {
        val listener = Consumer<IntSize2d> {}
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(listener)
        verify(mockPlatformAdapter)
            .addPerceivedResolutionChangedListener(eq(HandlerExecutor.mainThreadExecutor), any())
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
        verify(mockPlatformAdapter)
            .addPerceivedResolutionChangedListener(any(), rtListenerCaptor.capture())

        session.scene.mainPanelEntity.removePerceivedResolutionChangedListener(listener)
        verify(mockPlatformAdapter)
            .removePerceivedResolutionChangedListener(eq(rtListenerCaptor.firstValue))
    }

    @Test
    fun perceivedResolutionChangedListener_isCalledWithConvertedValues() {
        var receivedDimensions: IntSize2d? = null
        val listener = Consumer<IntSize2d> { dims -> receivedDimensions = dims }
        val rtListenerCaptor = argumentCaptor<Consumer<RtPixelDimensions>>()
        val executor = directExecutor()

        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        verify(mockPlatformAdapter)
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

        verify(mockPlatformAdapter, times(2))
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
}
