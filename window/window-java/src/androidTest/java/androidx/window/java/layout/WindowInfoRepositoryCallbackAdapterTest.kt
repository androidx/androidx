/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.java.layout

import android.graphics.Rect
import androidx.window.java.TestConsumer
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoRepository
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Add a test for [WindowInfoRepositoryCallbackAdapter] to verify adapted methods. Test converting
 * from the kotlin coroutine API to listeners and callbacks.
 * @see WindowInfoRepository
 */
public class WindowInfoRepositoryCallbackAdapterTest {

    @Test
    public fun testCurrentWindowMetrics() {
        val expected = WindowMetrics(Rect(0, 1, 2, 3))
        val mockRepo = mock<WindowInfoRepository>()
        whenever(mockRepo.currentWindowMetrics).thenReturn(flowOf(expected))
        val unitUnderTest = WindowInfoRepositoryCallbackAdapter(mockRepo)
        val testConsumer = TestConsumer<WindowMetrics>()

        unitUnderTest.addCurrentWindowMetricsListener(Runnable::run, testConsumer)

        testConsumer.assertValue(expected)
    }

    @Test
    public fun testCurrentWindowMetrics_registerMultipleIsNoOp() {
        val expected = WindowMetrics(Rect(0, 1, 2, 3))
        val mockRepo = mock<WindowInfoRepository>()
        whenever(mockRepo.currentWindowMetrics).thenReturn(flowOf(expected))
        val unitUnderTest = WindowInfoRepositoryCallbackAdapter(mockRepo)
        val testConsumer = TestConsumer<WindowMetrics>()

        unitUnderTest.addCurrentWindowMetricsListener(Runnable::run, testConsumer)
        unitUnderTest.addCurrentWindowMetricsListener(Runnable::run, testConsumer)

        testConsumer.assertValue(expected)
    }

    @Test
    public fun testCurrentWindowMetrics_unregister() {
        val metrics = WindowMetrics(Rect(0, 1, 2, 3))
        val mockRepo = mock<WindowInfoRepository>()
        val channel = Channel<WindowMetrics>()
        whenever(mockRepo.currentWindowMetrics).thenReturn(channel.receiveAsFlow())
        val unitUnderTest = WindowInfoRepositoryCallbackAdapter(mockRepo)
        val testConsumer = TestConsumer<WindowMetrics>()

        unitUnderTest.addCurrentWindowMetricsListener(Runnable::run, testConsumer)
        unitUnderTest.addCurrentWindowMetricsListener(Runnable::run, mock())
        unitUnderTest.removeCurrentWindowMetricsListener(testConsumer)
        val accepted = channel.trySend(metrics).isSuccess

        assertTrue(accepted)
        testConsumer.assertEmpty()
    }

    @Test
    public fun testRegisterListener() {
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockRepo = mock<WindowInfoRepository>()
        whenever(mockRepo.windowLayoutInfo).thenReturn(flowOf(expected))
        val unitUnderTest = WindowInfoRepositoryCallbackAdapter(mockRepo)
        val testConsumer = TestConsumer<WindowLayoutInfo>()

        unitUnderTest.addWindowLayoutInfoListener(Runnable::run, testConsumer)

        testConsumer.assertValue(expected)
    }

    @Test
    public fun testWindowLayoutInfo_registerMultipleIsNoOp() {
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockRepo = mock<WindowInfoRepository>()
        whenever(mockRepo.windowLayoutInfo).thenReturn(flowOf(expected))
        val unitUnderTest = WindowInfoRepositoryCallbackAdapter(mockRepo)
        val testConsumer = TestConsumer<WindowLayoutInfo>()

        unitUnderTest.addWindowLayoutInfoListener(Runnable::run, testConsumer)
        unitUnderTest.addWindowLayoutInfoListener(Runnable::run, testConsumer)

        testConsumer.assertValue(expected)
    }

    @Test
    public fun testWindowLayoutInfo_unregister() {
        val feature = mock<FoldingFeature>()
        val info = WindowLayoutInfo(listOf(feature))
        val mockRepo = mock<WindowInfoRepository>()
        val channel = Channel<WindowLayoutInfo>()
        whenever(mockRepo.windowLayoutInfo).thenReturn(channel.receiveAsFlow())
        val unitUnderTest = WindowInfoRepositoryCallbackAdapter(mockRepo)
        val testConsumer = TestConsumer<WindowLayoutInfo>()

        unitUnderTest.addWindowLayoutInfoListener(Runnable::run, testConsumer)
        unitUnderTest.addWindowLayoutInfoListener(Runnable::run, mock())
        unitUnderTest.removeWindowLayoutInfoListener(testConsumer)
        val accepted = channel.trySend(info).isSuccess

        assertTrue(accepted)
        testConsumer.assertEmpty()
    }
}