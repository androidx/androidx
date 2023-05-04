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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.OneTimeWorkRequest
import androidx.work.RunnableScheduler
import androidx.work.SystemClock
import androidx.work.worker.TestWorker
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DelayedWorkTrackerTest {
    private lateinit var mScheduler: GreedyScheduler
    private lateinit var mRunnableScheduler: RunnableScheduler
    private lateinit var mDelayedWorkTracker: DelayedWorkTracker

    @Before
    fun setUp() {
        mScheduler = mock(GreedyScheduler::class.java)
        mRunnableScheduler = mock(RunnableScheduler::class.java)
        mDelayedWorkTracker = DelayedWorkTracker(mScheduler, mRunnableScheduler, SystemClock())
    }

    @Test
    @MediumTest
    fun testHandleDelayedStartWorkMessage() {
        val builder = newWorkRequestBuilder()
        val now = System.currentTimeMillis()
        val delay = 10L
        builder.setLastEnqueueTime(now, TimeUnit.MILLISECONDS)
        builder.setInitialDelay(delay, TimeUnit.MILLISECONDS)
        val request = builder.build()
        mDelayedWorkTracker.schedule(request.workSpec, now + delay)

        verify(mRunnableScheduler).scheduleWithDelay(
            anyLong(),
            any(Runnable::class.java)
        )
    }

    private fun newWorkRequestBuilder(): OneTimeWorkRequest.Builder {
        return OneTimeWorkRequest.Builder(TestWorker::class.java)
    }
}
