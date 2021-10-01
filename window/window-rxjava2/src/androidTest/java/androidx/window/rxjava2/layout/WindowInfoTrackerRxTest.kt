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

package androidx.window.rxjava2.layout

import android.app.Activity
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

/**
 * Tests for the RxJava 2 adapters.
 */
public class WindowInfoTrackerRxTest {

    @Test
    public fun testWindowLayoutInfoObservable() {
        val activity = mock<Activity>()
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(flowOf(expected))

        val testSubscriber = mockTracker.windowLayoutInfoObservable(activity).test()

        testSubscriber.assertValue(expected)
    }

    @Test
    public fun testWindowLayoutInfoFlowable() {
        val activity = mock<Activity>()
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(flowOf(expected))

        val testSubscriber = mockTracker.windowLayoutInfoFlowable(activity).test()

        testSubscriber.assertValue(expected)
    }
}
