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
import androidx.work.worker.TestWorker
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DelayedCallbackTest {
    private lateinit var mHandler: Handler
    private lateinit var mScheduler: GreedyScheduler
    private lateinit var mDelayedCallback: DelayedWorkTracker.DelayedCallback

    @Before
    fun setUp() {
        mHandler = mock(Handler::class.java)
        mScheduler = mock(GreedyScheduler::class.java)
        mDelayedCallback = DelayedWorkTracker.DelayedCallback(mScheduler)
        mDelayedCallback.setHandler(mHandler)
    }

    @Test
    @MediumTest
    fun testHandleDelayedStartWorkMessage() {
        val builder = newWorkRequestBuilder()
        val now = System.currentTimeMillis()
        val delay = 10L
        builder.setPeriodStartTime(now, TimeUnit.MILLISECONDS)
        builder.setInitialDelay(delay, TimeUnit.MILLISECONDS)
        val request = builder.build()
        val message = DelayedWorkTracker.DelayedCallback.schedule(request.workSpec)
        mDelayedCallback.handleMessage(message)
        // We can't verify that a message was posted on the Handler with the token being
        // the workSpecId because that method is marked final
        val timeCaptor = ArgumentCaptor.forClass(Long::class.java)
        // sendMessage and sendMessageDelayed are final
        Mockito.verify(mHandler, Mockito.times(1))
            .sendMessageAtTime(any<Message>(), timeCaptor.capture())

        assertThat(timeCaptor.value, `is`(now + delay))
    }

    private fun newWorkRequestBuilder(): OneTimeWorkRequest.Builder {
        return OneTimeWorkRequest.Builder(TestWorker::class.java)
    }
}
