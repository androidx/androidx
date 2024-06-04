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

package androidx.window.rxjava3.layout

import android.app.Activity
import android.content.Context
import androidx.window.core.ExperimentalWindowApi
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test for the adapter functions that convert to [io.reactivex.rxjava3.core.Observable] or
 * [io.reactivex.rxjava3.core.Flowable] and ensure that data is forwarded appropriately.
 *
 * @see WindowInfoTracker
 */
@OptIn(ExperimentalWindowApi::class)
class WindowInfoTrackerRxTest {

    @Test
    fun testWindowLayoutInfoObservable() {
        val activity = mock<Activity>()
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(flowOf(expected))

        val testSubscriber = mockTracker.windowLayoutInfoObservable(activity).test()

        testSubscriber.assertValue(expected)
    }

    @Test
    fun testWindowLayoutInfoFlowable() {
        val activity = mock<Activity>()
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(flowOf(expected))

        val testSubscriber = mockTracker.windowLayoutInfoFlowable(activity).test()

        testSubscriber.assertValue(expected)
    }

    @Test
    fun testWindowLayoutInfoObservable_context() {
        val activity = mock<Context>()
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(flowOf(expected))

        val testSubscriber = mockTracker.windowLayoutInfoObservable(activity).test()

        testSubscriber.assertValue(expected)
    }

    @Test
    fun testWindowLayoutInfoFlowable_context() {
        val activity = mock<Context>()
        val feature = mock<FoldingFeature>()
        val expected = WindowLayoutInfo(listOf(feature))
        val mockTracker = mock<WindowInfoTracker>()
        whenever(mockTracker.windowLayoutInfo(activity)).thenReturn(flowOf(expected))

        val testSubscriber = mockTracker.windowLayoutInfoFlowable(activity).test()

        testSubscriber.assertValue(expected)
    }
}
