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

import android.app.Activity
import androidx.window.java.TestConsumer
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Add a test for [WindowInfoTrackerCallbackAdapter] to verify adapted methods. Test converting
 * from the kotlin coroutine API to listeners and callbacks.
 * @see WindowInfoTracker
 */
public class WindowInfoTrackerCallbackAdapterTest {

    @Test
    public fun testRegisterListener() {
        val activity = mock<Activity>()
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(flowOf(expected))
        val unitUnderTest = WindowInfoTrackerCallbackAdapter(mockTracker)
        val testConsumer = TestConsumer<WindowLayoutInfo>()

        unitUnderTest.addWindowLayoutInfoListener(activity, Runnable::run, testConsumer)

        testConsumer.assertValue(expected)
    }

    @Test
    public fun testWindowLayoutInfo_registerMultipleIsNoOp() {
        val activity = mock<Activity>()
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(flowOf(expected))
        val unitUnderTest = WindowInfoTrackerCallbackAdapter(mockTracker)
        val testConsumer = TestConsumer<WindowLayoutInfo>()

        unitUnderTest.addWindowLayoutInfoListener(activity, Runnable::run, testConsumer)
        unitUnderTest.addWindowLayoutInfoListener(activity, Runnable::run, testConsumer)

        testConsumer.assertValue(expected)
    }

    @Test
    public fun testWindowLayoutInfo_unregister() {
        val activity = mock<Activity>()
        val feature = mock<FoldingFeature>()
        val info = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        val channel = Channel<WindowLayoutInfo>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(channel.receiveAsFlow())
        val unitUnderTest = WindowInfoTrackerCallbackAdapter(mockTracker)
        val testConsumer = TestConsumer<WindowLayoutInfo>()

        unitUnderTest.addWindowLayoutInfoListener(activity, Runnable::run, testConsumer)
        unitUnderTest.addWindowLayoutInfoListener(activity, Runnable::run, mock())
        unitUnderTest.removeWindowLayoutInfoListener(testConsumer)
        val accepted = channel.trySend(info).isSuccess

        assertTrue(accepted)
        testConsumer.assertEmpty()
    }
}
