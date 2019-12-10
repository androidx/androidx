/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.background.greedy

import android.os.Handler
import android.os.Message
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.OneTimeWorkRequest
import androidx.work.impl.model.WorkSpec
import androidx.work.worker.TestWorker
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class DelayedWorkTrackerTest {

    private lateinit var mGreedyScheduler: GreedyScheduler
    private lateinit var mHandler: Handler
    private lateinit var mDelayedCallback: DelayedWorkTracker.DelayedCallback
    private lateinit var mDelayedWorkTracker: DelayedWorkTracker

    @Before
    fun setUp() {
        mGreedyScheduler = mock(GreedyScheduler::class.java)
        mHandler = mock(Handler::class.java)
        mDelayedCallback = spy(DelayedWorkTracker.DelayedCallback(mGreedyScheduler))
        mDelayedCallback.setHandler(mHandler)
        mDelayedWorkTracker = DelayedWorkTracker(mDelayedCallback)
    }

    @Test
    @MediumTest
    fun testDelayedStartWork() {
        val request = newWorkRequest()
        mDelayedWorkTracker.schedule(request.workSpec)
        val messageCaptor = ArgumentCaptor.forClass(Message::class.java)
        // sendMessage and sendMessageDelayed are final
        verify(mHandler, times(2)).sendMessageAtTime(messageCaptor.capture(), anyLong())
        assertThat(messageCaptor.allValues.size, `is`(2))
        val cancelCaptor = messageCaptor.allValues[0]
        val delayedCaptor = messageCaptor.allValues[1]
        assertThat(
            cancelCaptor.what,
            `is`(DelayedWorkTracker.DelayedCallback.UNSCHEDULE)
        )
        assertThat(
            delayedCaptor.what,
            `is`(DelayedWorkTracker.DelayedCallback.SCHEDULE)
        )
        assertThat(
            delayedCaptor.obj as? WorkSpec,
            `is`(request.workSpec)
        )
    }

    @Test
    @MediumTest
    fun testCancelStartWork() {
        val request = newWorkRequest()
        mDelayedWorkTracker.unschedule(request.stringId)
        val messageCaptor = ArgumentCaptor.forClass(Message::class.java)
        // sendMessage and sendMessageDelayed are final
        verify(mHandler, times(1)).sendMessageAtTime(messageCaptor.capture(), anyLong())
        val cancelCaptor = messageCaptor.allValues[0]
        assertThat(
            cancelCaptor.what,
            `is`(DelayedWorkTracker.DelayedCallback.UNSCHEDULE)
        )
    }

    private fun newWorkRequest(): OneTimeWorkRequest {
        val request = OneTimeWorkRequest.Builder(TestWorker::class.java)
        return request.build()
    }
}
