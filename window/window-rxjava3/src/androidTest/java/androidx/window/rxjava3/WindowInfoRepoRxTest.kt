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

package androidx.window.rxjava3

import android.graphics.Rect
import androidx.window.FoldingFeature
import androidx.window.WindowInfoRepo
import androidx.window.WindowLayoutInfo
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

/**
 * Test for the adapter functions that convert to [io.reactivex.rxjava3.core.Observable] or
 * [io.reactivex.rxjava3.core.Flowable] and ensure that data is forwarded appropriately.
 * @see WindowInfoRepo
 */
public class WindowInfoRepoRxTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testWindowLayoutInfoObservable() {
        val feature = FoldingFeature(
            Rect(0, 100, 100, 100),
            FoldingFeature.TYPE_HINGE,
            FoldingFeature.STATE_HALF_OPENED
        )
        val expected = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(feature)).build()
        val mockRepo = mock<WindowInfoRepo>()
        whenever(mockRepo.windowLayoutInfo()).thenReturn(flowOf(expected))

        val testSubscriber = mockRepo.windowLayoutInfoObservable().test()

        testSubscriber.assertValue(expected)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testWindowLayoutInfoFlowable() {
        val feature = FoldingFeature(
            Rect(0, 100, 100, 100),
            FoldingFeature.TYPE_HINGE,
            FoldingFeature.STATE_HALF_OPENED
        )
        val expected = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(feature)).build()
        val mockRepo = mock<WindowInfoRepo>()
        whenever(mockRepo.windowLayoutInfo()).thenReturn(flowOf(expected))

        val testSubscriber = mockRepo.windowLayoutInfoFlowable().test()

        testSubscriber.assertValue(expected)
    }
}