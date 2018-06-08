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

package androidx.work

import android.support.test.filters.SdkSuppress
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import androidx.work.workers.TestWorker
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SmallTest
class PeriodicWorkRequestTest {
    @Test
    fun testPeriodicWorkRequestBuilder() {
        val builder = PeriodicWorkRequestBuilder<TestWorker>(
                repeatInterval = 15L,
                repeatIntervalTimeUnit = TimeUnit.MINUTES)
        val workRequest = builder.build()
        assertEquals(workRequest.workSpec.workerClassName, TestWorker::class.java.name)
        assertEquals(workRequest.workSpec.isPeriodic, true)
        assertEquals(workRequest.workSpec.intervalDuration, TimeUnit.MINUTES.toMillis(15))
        assertEquals(workRequest.workSpec.flexDuration, TimeUnit.MINUTES.toMillis(15))
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPeriodicWorkRequestBuilder_withDuration() {
        val repeatInterval = Duration.ofDays(2).plusHours(3)
        val builder = PeriodicWorkRequestBuilder<TestWorker>(repeatInterval)
        val workRequest = builder.build()
        assertEquals(workRequest.workSpec.workerClassName, TestWorker::class.java.name)
        assertEquals(workRequest.workSpec.isPeriodic, true)
        assertEquals(workRequest.workSpec.intervalDuration, repeatInterval.toMillis())
        assertEquals(workRequest.workSpec.flexDuration, repeatInterval.toMillis())
    }

    @Test
    fun testPeriodicWorkRequestBuilder_withFlexTime() {
        val builder = PeriodicWorkRequestBuilder<TestWorker>(
                repeatInterval = 15L,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 10L,
                flexTimeIntervalUnit = TimeUnit.MINUTES)
        val workRequest = builder.build()
        assertEquals(workRequest.workSpec.workerClassName, TestWorker::class.java.name)
        assertEquals(workRequest.workSpec.isPeriodic, true)
        assertEquals(workRequest.workSpec.flexDuration, TimeUnit.MINUTES.toMillis(10))
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPeriodicWorkRequestBuilder_withFlexTimeandDuration() {
        val repeatInterval = Duration.ofHours(3).plusMinutes(25)
        val flexInterval = repeatInterval.minusMinutes(15)
        val builder = PeriodicWorkRequestBuilder<TestWorker>(
                repeatInterval = repeatInterval,
                flexTimeInterval = flexInterval)
        val workRequest = builder.build()
        assertEquals(workRequest.workSpec.workerClassName, TestWorker::class.java.name)
        assertEquals(workRequest.workSpec.isPeriodic, true)
        assertEquals(workRequest.workSpec.intervalDuration, repeatInterval.toMillis())
        assertEquals(workRequest.workSpec.flexDuration, flexInterval.toMillis())
    }
}
