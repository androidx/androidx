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

package androidx.work.testing

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.workers.TestWorker
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.util.concurrent.ExecutionException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CustomClockTest {
    private val testClock = TestClock(/* timeMillis= */ 1000)

    private lateinit var mContext: Context
    private lateinit var mTestDriver: TestDriver
    private lateinit var mHandler: Handler

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
        mHandler = Handler(Looper.getMainLooper())
        val configuration =
            Configuration.Builder().setExecutor(androidx.work.testing.SynchronousExecutor())
                .setClock(testClock).setMinimumLoggingLevel(Log.DEBUG).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, configuration)

        mTestDriver = WorkManagerTestInitHelper.getTestDriver(mContext)!!
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testScheduleTime_relativeToTestClock() {
        testClock.timeMillis = Duration.ofDays(100).toMillis()
        val initialDelay = Duration.ofHours(1)

        val request: WorkRequest =
            OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setInitialDelay(initialDelay).build()

        val workManagerImpl = WorkManagerImpl.getInstance(mContext)
        workManagerImpl.enqueue(listOf(request)).result.get()

        val status = workManagerImpl.getWorkInfoById(request.id).get()
        assertThat(status.nextScheduleTimeMillis)
            .isEqualTo(testClock.timeMillis + initialDelay.toMillis())
    }
}
