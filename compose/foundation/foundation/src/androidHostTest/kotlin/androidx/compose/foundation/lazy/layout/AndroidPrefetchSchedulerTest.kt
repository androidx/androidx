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

@file:Suppress("DEPRECATION") // b/420551535

package androidx.compose.foundation.lazy.layout

import android.os.Trace
import android.view.Choreographer
import android.view.View
import androidx.compose.foundation.AndroidComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class AndroidPrefetchSchedulerTest {
    private val initialLateFrameDetectionFlag =
        AndroidComposeFoundationFlags.isPrefetchSchedulerLateFrameDetectionEnabled
    private val initialFutureDrawTimeClampingFlag =
        AndroidComposeFoundationFlags.isPrefetchSchedulerFutureDrawTimeClampingEnabled

    // Default refresh rate in AndroidPrefetchScheduler when view.display is null is 60fps.
    private val defaultFrameIntervalNanos = TimeUnit.SECONDS.toNanos(1) / 60

    private lateinit var choreographerStaticMock: MockedStatic<Choreographer>
    private lateinit var traceStaticMock: MockedStatic<Trace>
    private val mockChoreographer = mock<Choreographer>()

    @Before
    fun setUp() {
        choreographerStaticMock = Mockito.mockStatic(Choreographer::class.java)
        choreographerStaticMock
            .`when`<Choreographer> { Choreographer.getInstance() }
            .thenReturn(mockChoreographer)
        traceStaticMock = Mockito.mockStatic(Trace::class.java)
    }

    @After
    fun tearDown() {
        traceStaticMock.close()
        choreographerStaticMock.close()
        AndroidComposeFoundationFlags.isPrefetchSchedulerLateFrameDetectionEnabled =
            initialLateFrameDetectionFlag
        AndroidComposeFoundationFlags.isPrefetchSchedulerFutureDrawTimeClampingEnabled =
            initialFutureDrawTimeClampingFlag
    }

    @Test
    fun doFrame_withFutureFrameTimeNanos_clampsAvailableTimeToFrameInterval() {
        AndroidComposeFoundationFlags.isPrefetchSchedulerLateFrameDetectionEnabled = true
        AndroidComposeFoundationFlags.isPrefetchSchedulerFutureDrawTimeClampingEnabled = true
        val view = createAttachedMockView()
        val scheduler = AndroidPrefetchScheduler(view)

        var observedAvailableTimeNanos = -1L
        scheduler.schedulePrefetch(
            createPrefetchRequest {
                observedAvailableTimeNanos = availableTimeNanos()
                false
            }
        )

        val nowNanos = System.nanoTime()
        val futureLeadNanos = TimeUnit.MILLISECONDS.toNanos(100)
        whenever(view.drawingTime).thenReturn(TimeUnit.NANOSECONDS.toMillis(nowNanos))
        scheduler.doFrame(nowNanos + futureLeadNanos)
        scheduler.run()

        assertThat(observedAvailableTimeNanos).isGreaterThan(0L)
        assertThat(observedAvailableTimeNanos).isAtMost(defaultFrameIntervalNanos)
    }

    @Test
    fun run_withFutureViewDrawingTime_clampsAvailableTimeToFrameInterval() {
        AndroidComposeFoundationFlags.isPrefetchSchedulerLateFrameDetectionEnabled = true
        AndroidComposeFoundationFlags.isPrefetchSchedulerFutureDrawTimeClampingEnabled = true
        val view = createAttachedMockView()
        val scheduler = AndroidPrefetchScheduler(view)

        var observedAvailableTimeNanos = -1L
        scheduler.schedulePrefetch(
            createPrefetchRequest {
                observedAvailableTimeNanos = availableTimeNanos()
                false
            }
        )

        val futureDrawingTimeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + 100L
        whenever(view.drawingTime).thenReturn(futureDrawingTimeMillis)
        scheduler.run()

        assertThat(observedAvailableTimeNanos).isGreaterThan(0L)
        assertThat(observedAvailableTimeNanos).isAtMost(defaultFrameIntervalNanos)
    }

    @Test
    fun doFrame_withFutureFrameTimeNanos_flagDisabled_doesNotClampAvailableTime() {
        AndroidComposeFoundationFlags.isPrefetchSchedulerLateFrameDetectionEnabled = true
        AndroidComposeFoundationFlags.isPrefetchSchedulerFutureDrawTimeClampingEnabled = false
        val view = createAttachedMockView()
        val scheduler = AndroidPrefetchScheduler(view)

        var observedAvailableTimeNanos = -1L
        scheduler.schedulePrefetch(
            createPrefetchRequest {
                observedAvailableTimeNanos = availableTimeNanos()
                false
            }
        )

        val nowNanos = System.nanoTime()
        val futureLeadNanos = TimeUnit.MILLISECONDS.toNanos(100)
        whenever(view.drawingTime).thenReturn(TimeUnit.NANOSECONDS.toMillis(nowNanos))
        scheduler.doFrame(nowNanos + futureLeadNanos)
        scheduler.run()

        assertThat(observedAvailableTimeNanos).isGreaterThan(defaultFrameIntervalNanos)
    }

    @Test
    fun doFrame_withPastFrameTimeNanos_subtractsElapsedTimeFromFrameInterval() {
        AndroidComposeFoundationFlags.isPrefetchSchedulerLateFrameDetectionEnabled = true
        AndroidComposeFoundationFlags.isPrefetchSchedulerFutureDrawTimeClampingEnabled = true
        val view = createAttachedMockView()
        val scheduler = AndroidPrefetchScheduler(view)

        var observedAvailableTimeNanos = -1L
        scheduler.schedulePrefetch(
            createPrefetchRequest {
                observedAvailableTimeNanos = availableTimeNanos()
                false
            }
        )

        val elapsedNanos = TimeUnit.MILLISECONDS.toNanos(5)
        val pastFrameTimeNanos = System.nanoTime() - elapsedNanos
        whenever(view.drawingTime).thenReturn(TimeUnit.NANOSECONDS.toMillis(pastFrameTimeNanos))
        scheduler.doFrame(pastFrameTimeNanos)
        scheduler.run()

        assertThat(observedAvailableTimeNanos).isGreaterThan(0L)
        assertThat(observedAvailableTimeNanos).isAtMost(defaultFrameIntervalNanos - elapsedNanos)
    }

    private fun createAttachedMockView(): View {
        val view = mock<View>()
        whenever(view.isAttachedToWindow).thenReturn(true)
        whenever(view.windowVisibility).thenReturn(View.VISIBLE)
        return view
    }

    private fun createPrefetchRequest(
        onExecute: PrefetchRequestScope.() -> Boolean
    ): PrefetchRequest =
        Proxy.newProxyInstance(
            PrefetchRequest::class.java.classLoader,
            arrayOf(PrefetchRequest::class.java),
        ) { _, method, args ->
            if (method.name == "execute" && args?.size == 1 && args[0] is PrefetchRequestScope) {
                (args[0] as PrefetchRequestScope).onExecute()
            } else {
                false
            }
        } as PrefetchRequest
}
