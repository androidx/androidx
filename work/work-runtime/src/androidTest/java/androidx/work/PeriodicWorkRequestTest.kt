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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.worker.TestWorker
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.HOURS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PeriodicWorkRequestTest {
    @Test
    fun testPeriodicWorkRequestBuilder() {
        val builder =
            PeriodicWorkRequestBuilder<TestWorker>(
                repeatInterval = 15L,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
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
        val builder =
            PeriodicWorkRequestBuilder<TestWorker>(
                repeatInterval = 15L,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 10L,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
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
        val builder =
            PeriodicWorkRequestBuilder<TestWorker>(
                repeatInterval = repeatInterval,
                flexTimeInterval = flexInterval
            )
        val workRequest = builder.build()
        assertEquals(workRequest.workSpec.workerClassName, TestWorker::class.java.name)
        assertEquals(workRequest.workSpec.isPeriodic, true)
        assertEquals(workRequest.workSpec.intervalDuration, repeatInterval.toMillis())
        assertEquals(workRequest.workSpec.flexDuration, flexInterval.toMillis())
    }

    @Test
    fun testPeriodicWorkRequestBuilder_noNextScheduleTimeOverride_noGeneration() {
        val builder = PeriodicWorkRequestBuilder<TestWorker>(HOURS.toMillis(15L), HOURS)
        assertEquals(builder.build().workSpec.nextScheduleTimeOverride, Long.MAX_VALUE)
        assertEquals(builder.build().workSpec.nextScheduleTimeOverrideGeneration, 0)
    }

    @Test
    fun testPeriodicWorkRequestBuilder_nextScheduleTimeOverride_setsGeneration() {
        val builder = PeriodicWorkRequestBuilder<TestWorker>(HOURS.toMillis(15L), HOURS)
        builder.setNextScheduleTimeOverride(123456)
        assertEquals(builder.build().workSpec.nextScheduleTimeOverride, 123456L)
        assertEquals(builder.build().workSpec.nextScheduleTimeOverrideGeneration, 1)
    }

    @Test
    fun testPeriodicWorkRequestBuilder_nextScheduleTimeOverride_maxLongNotAllowed() {
        val builder = PeriodicWorkRequestBuilder<TestWorker>(HOURS.toMillis(15L), HOURS)
        assertThrows(IllegalArgumentException::class.java) {
            builder.setNextScheduleTimeOverride(Long.MAX_VALUE)
        }
    }

    @Test
    fun testPeriodicWorkRequest_clearNextScheduleTimeOverride_setsGeneration() {
        val plainRequest =
            PeriodicWorkRequestBuilder<TestWorker>(HOURS.toMillis(15L), HOURS).build()

        val clearedRequest =
            PeriodicWorkRequestBuilder<TestWorker>(HOURS.toMillis(15L), HOURS)
                .setNextScheduleTimeOverride(123456)
                .clearNextScheduleTimeOverride()
                .build()

        // The generation should stay incremented, since the request still has to clear any override
        // that has been already set in the database.
        assertNotEquals(plainRequest, clearedRequest)
    }
}
