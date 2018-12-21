/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.viewport_offset

import androidx.ui.core.Duration
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ViewportOffsetTest {

    @Test
    fun `moveTo call jumpTo if duration is null test`() {
        val testValue = 123.4f

        val viewportOffset: ViewportOffset = mock()
        whenever(viewportOffset.moveTo(testValue)).thenCallRealMethod()

        val job = viewportOffset.moveTo(testValue)
        verify(viewportOffset, times(1)).jumpTo(testValue)
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun `moveTo call jumpTo if duration is zero test`() {
        val testValue = 123.4f

        val viewportOffset: ViewportOffset = mock()
        whenever(viewportOffset.moveTo(testValue, Duration.zero)).thenCallRealMethod()

        val job = viewportOffset.moveTo(testValue, duration = Duration.zero)
        verify(viewportOffset, times(1)).jumpTo(testValue)
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun `moveTo call animateTo if Duration has time`() {
        val testValue = 123.4f
        val duration = Duration(100)

        val viewportOffset: ViewportOffset = mock()
        whenever(viewportOffset.moveTo(testValue, duration = duration)).thenCallRealMethod()

        viewportOffset.moveTo(testValue, duration = duration)
        verify(viewportOffset, times(1)).animateTo(testValue, duration, null /* curve */)
    }
}