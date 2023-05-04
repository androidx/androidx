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

package androidx.work.impl.background.gcm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.SystemClock
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.background.gcm.GcmTaskConverter.EXECUTION_WINDOW_SIZE_IN_SECONDS
import com.google.android.gms.gcm.Task
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.Assert.assertEquals
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.spy
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SmallTest
class GcmTaskConverterTest {

    lateinit var mTaskConverter: GcmTaskConverter

    @Before
    fun setUp() {
        mTaskConverter = spy(GcmTaskConverter(SystemClock()))
    }

    @Test
    fun testOneTimeRequest_noInitialDelay() {
        val now = System.currentTimeMillis()
        `when`(mTaskConverter.now()).thenReturn(now)

        val request = OneTimeWorkRequestBuilder<TestWorker>().build()
        val task = mTaskConverter.convert(request.workSpec)

        val expected = request.workSpec.calculateNextRunTime()
        val offset = offset(expected, now)
        val deltaStart = task.windowStart - offset
        val deltaEnd = task.windowEnd - (offset + EXECUTION_WINDOW_SIZE_IN_SECONDS)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        // Account for time unit quantization errors
        assertThat(deltaStart, lessThanOrEqualTo(1L))
        assertThat(deltaEnd, lessThanOrEqualTo(1L))
    }

    @Test
    fun testOneTimeRequest_noInitialDelay_withConstraintNetworkConnected() {
        val now = System.currentTimeMillis()
        `when`(mTaskConverter.now()).thenReturn(now)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.METERED)
            .setRequiresCharging(true)
            .build()

        val request = OneTimeWorkRequestBuilder<TestWorker>()
            .setConstraints(constraints)
            .build()

        val task = mTaskConverter.convert(request.workSpec)
        val expected = request.workSpec.calculateNextRunTime()
        val offset = offset(expected, now)
        val deltaStart = task.windowStart - offset
        val deltaEnd = task.windowEnd - (offset + EXECUTION_WINDOW_SIZE_IN_SECONDS)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_CONNECTED)
        assertEquals(task.requiresCharging, true)
        // Account for time unit quantization errors
        assertThat(deltaStart, lessThanOrEqualTo(1L))
        assertThat(deltaEnd, lessThanOrEqualTo(1L))
    }

    @Test
    fun testOneTimeRequest_noInitialDelay_withConstraintNetworkUnMetered() {
        val now = System.currentTimeMillis()
        `when`(mTaskConverter.now()).thenReturn(now)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val request = OneTimeWorkRequestBuilder<TestWorker>()
            .setConstraints(constraints)
            .build()

        val task = mTaskConverter.convert(request.workSpec)
        val expected = request.workSpec.calculateNextRunTime()
        val offset = offset(expected, now)
        val deltaStart = task.windowStart - offset
        val deltaEnd = task.windowEnd - (offset + EXECUTION_WINDOW_SIZE_IN_SECONDS)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_UNMETERED)
        assertEquals(task.requiresCharging, false)
        // Account for time unit quantization errors
        assertThat(deltaStart, lessThanOrEqualTo(1L))
        assertThat(deltaEnd, lessThanOrEqualTo(1L))
    }

    @Test
    fun testOneTimeRequest_hasInitialDelay() {
        val initialDelay = 10L
        val now = System.currentTimeMillis()
        `when`(mTaskConverter.now()).thenReturn(now)

        val request = OneTimeWorkRequestBuilder<TestWorker>()
            .setInitialDelay(initialDelay, TimeUnit.SECONDS)
            .build()

        val task = mTaskConverter.convert(request.workSpec)
        val expected = request.workSpec.calculateNextRunTime()
        val offset = offset(expected, now)
        val deltaStart = task.windowStart - offset
        val deltaEnd = task.windowEnd - (offset + EXECUTION_WINDOW_SIZE_IN_SECONDS)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        // Account for time unit quantization errors
        assertThat(deltaStart, lessThanOrEqualTo(1L))
        assertThat(deltaEnd, lessThanOrEqualTo(1L))
    }

    @Test
    fun testOneTimeWorkRequest_backedOff() {
        val now = System.currentTimeMillis()
        `when`(mTaskConverter.now()).thenReturn(now)

        val request = OneTimeWorkRequestBuilder<TestWorker>()
            .setInitialRunAttemptCount(1)
            .build()

        val workSpec = request.workSpec
        val task = mTaskConverter.convert(request.workSpec)
        val expected = workSpec.calculateNextRunTime()
        val offset = offset(expected, now)
        val deltaStart = task.windowStart - offset
        val deltaEnd = task.windowEnd - (offset + EXECUTION_WINDOW_SIZE_IN_SECONDS)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        // Account for time unit quantization errors
        assertThat(deltaStart, lessThanOrEqualTo(1L))
        assertThat(deltaEnd, lessThanOrEqualTo(1L))
    }

    @Test
    @SdkSuppress(maxSdkVersion = WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL)
    fun testPeriodicWorkRequest_firstRun() {
        val now = System.currentTimeMillis()
        `when`(mTaskConverter.now()).thenReturn(now)

        val request = PeriodicWorkRequestBuilder<TestWorker>(15L, TimeUnit.MINUTES)
            .build()

        val task = mTaskConverter.convert(request.workSpec)
        val expected = request.workSpec.calculateNextRunTime()
        val offset = offset(expected, now)
        val deltaStart = task.windowStart - offset
        val deltaEnd = task.windowEnd - (offset + EXECUTION_WINDOW_SIZE_IN_SECONDS)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        // Account for time unit quantization errors
        assertThat(deltaStart, lessThanOrEqualTo(1L))
        assertThat(deltaEnd, lessThanOrEqualTo(1L))
    }

    @Test
    @SdkSuppress(
        minSdkVersion = 22, // b/269194015 for minSdkVersion = 22
        maxSdkVersion = WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL
    )
    fun testPeriodicWorkRequest_withFlex_firstRun() {
        val request = PeriodicWorkRequestBuilder<TestWorker>(
            15L, TimeUnit.MINUTES, 5, TimeUnit.MINUTES
        ).build()

        val task = mTaskConverter.convert(request.workSpec)
        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        assertThat(task.windowStart, greaterThan(0L)) // should be in the future
    }

    @Test
    @SdkSuppress(
        minSdkVersion = 22, // b/269194015 for minSdkVersion = 22
        maxSdkVersion = WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL
    )
    fun testPeriodicWorkRequest_withFlex_nextRun() {
        val now = System.currentTimeMillis()
        `when`(mTaskConverter.now()).thenReturn(now)

        val request = PeriodicWorkRequestBuilder<TestWorker>(
            15L, TimeUnit.MINUTES, 5, TimeUnit.MINUTES
        ).build()

        request.workSpec.lastEnqueueTime = now
        val expected = TimeUnit.MINUTES.toSeconds(15L)

        val task = mTaskConverter.convert(request.workSpec)

        assertEquals(task.serviceName, WorkManagerGcmService::class.java.name)
        assertEquals(task.isPersisted, false)
        assertEquals(task.isUpdateCurrent, true)
        assertEquals(task.requiredNetwork, Task.NETWORK_STATE_ANY)
        assertEquals(task.requiresCharging, false)
        assertEquals(task.windowStart, expected)
        assertEquals(task.windowEnd, expected + EXECUTION_WINDOW_SIZE_IN_SECONDS)
    }

    private fun offset(expected: Long, now: Long): Long {
        val delta = Math.max(expected - now, 0)
        return TimeUnit.SECONDS.convert(delta, TimeUnit.MILLISECONDS)
    }
}
