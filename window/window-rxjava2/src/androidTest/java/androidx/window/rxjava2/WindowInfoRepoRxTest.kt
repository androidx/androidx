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

package androidx.window.rxjava2

import android.graphics.Rect
import androidx.window.FoldingFeature
import androidx.window.WindowInfoRepo
import androidx.window.WindowLayoutInfo
import androidx.window.WindowMetrics
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

/**
 * Tests for the RxJava 2 adapters.
 */
public class WindowInfoRepoRxTest {
    @Test
    public fun testCurrentWindowMetricsObservable() {
        val expected = WindowMetrics(Rect(0, 1, 2, 3))
        val mockRepo = mock<WindowInfoRepo>()
        whenever(mockRepo.currentWindowMetrics).thenReturn(flowOf(expected))

        val testSubscriber = mockRepo.currentWindowMetricsObservable().test()

        testSubscriber.assertValue(expected)
    }

    @Test
    public fun testCurrentWindowMetricsFlowable() {
        val expected = WindowMetrics(Rect(0, 1, 2, 3))
        val mockRepo = mock<WindowInfoRepo>()
        whenever(mockRepo.currentWindowMetrics).thenReturn(flowOf(expected))

        val testSubscriber = mockRepo.currentWindowMetricsFlowable().test()

        testSubscriber.assertValue(expected)
    }

    @Test
    public fun testWindowLayoutInfoObservable() {
        val feature = FoldingFeature(
            Rect(0, 100, 100, 100),
            FoldingFeature.Type.HINGE,
            FoldingFeature.State.HALF_OPENED
        )
        val expected = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(feature)).build()
        val mockRepo = mock<WindowInfoRepo>()
        whenever(mockRepo.windowLayoutInfo).thenReturn(flowOf(expected))

        val testSubscriber = mockRepo.windowLayoutInfoObservable().test()

        testSubscriber.assertValue(expected)
    }

    @Test
    public fun testWindowLayoutInfoFlowable() {
        val feature = FoldingFeature(
            Rect(0, 100, 100, 100),
            FoldingFeature.Type.HINGE,
            FoldingFeature.State.HALF_OPENED
        )
        val expected = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(feature)).build()
        val mockRepo = mock<WindowInfoRepo>()
        whenever(mockRepo.windowLayoutInfo).thenReturn(flowOf(expected))

        val testSubscriber = mockRepo.windowLayoutInfoFlowable().test()

        testSubscriber.assertValue(expected)
    }
}