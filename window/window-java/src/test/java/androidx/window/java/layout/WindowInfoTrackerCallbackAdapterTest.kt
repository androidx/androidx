/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.core.util.Consumer
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class WindowInfoTrackerCallbackAdapterTest {

    @Test
    fun testAddListenerSubscribes() {
        val tracker = mock<WindowInfoTracker>()
        val adapter = WindowInfoTrackerCallbackAdapter(tracker)
        val mutableFlow = MutableSharedFlow<WindowLayoutInfo>()
        val consumer = mock<Consumer<WindowLayoutInfo>>()
        whenever(tracker.windowLayoutInfo(any())).thenReturn(mutableFlow)

        adapter.addWindowLayoutInfoListener(mock(), Runnable::run, consumer)
        runBlocking {
            mutableFlow.emit(WindowLayoutInfo(emptyList()))
        }

        verify(consumer).accept(WindowLayoutInfo(emptyList()))
    }

    @Test
    fun testRemoveListenerStops() {
        val tracker = mock<WindowInfoTracker>()
        val adapter = WindowInfoTrackerCallbackAdapter(tracker)
        val mutableFlow = MutableSharedFlow<WindowLayoutInfo>()
        val consumer = mock<Consumer<WindowLayoutInfo>>()
        whenever(tracker.windowLayoutInfo(any())).thenReturn(mutableFlow)

        adapter.addWindowLayoutInfoListener(mock(), Runnable::run, consumer)
        adapter.removeWindowLayoutInfoListener(consumer)
        runBlocking {
            mutableFlow.emit(WindowLayoutInfo(emptyList()))
        }

        verifyNoMoreInteractions(consumer)
    }
}
