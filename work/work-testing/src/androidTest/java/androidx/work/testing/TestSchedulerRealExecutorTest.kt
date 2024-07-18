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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.testing.WorkManagerTestInitHelper.ExecutorsMode.PRESERVE_EXECUTORS
import androidx.work.testing.workers.CountingTestWorker
import androidx.work.testing.workers.RetryWorker
import androidx.work.testing.workers.TestWorker
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TestSchedulerRealExecutorTest {
    val context = ApplicationProvider.getApplicationContext<Context>()

    init {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, PRESERVE_EXECUTORS)
        CountingTestWorker.COUNT.set(0)
    }

    val wm = WorkManagerImpl.getInstance(context)
    val handler = Handler(Looper.getMainLooper())
    val driver = WorkManagerTestInitHelper.getTestDriver(context)!!

    @Test
    fun testWorker_withDependentWork_shouldSucceedSynchronously() {
        val request = OneTimeWorkRequest.from(TestWorker::class.java)
        val dependentRequest = OneTimeWorkRequest.from(TestWorker::class.java)
        wm.beginWith(request).then(dependentRequest).enqueue()
        awaitSuccess(dependentRequest.id)
    }

    @Test
    fun testWorker_withConstraints_shouldSucceedAfterSetConstraints() {
        val request = OneTimeWorkRequestBuilder<TestWorker>().setConstraints(
            Constraints(requiredNetworkType = NetworkType.UNMETERED)
        ).build()
        wm.enqueue(request).result.get()
        driver.setAllConstraintsMet(request.id)
        awaitSuccess(request.id)
    }

    @Test
    fun testWorker_withPeriodDelay_shouldRunAfterEachSetPeriodDelay() {
        val request = PeriodicWorkRequestBuilder<CountingTestWorker>(10, TimeUnit.DAYS).build()
        wm.enqueue(request).result.get()
        val periodicLatch = CountDownLatch(1)
        // TODO: specifically removing deduplication for periodic workers
        // so runs aren't dedupped. We need periodicity data in workinfo
        val workInfo = wm.workDatabase.workSpecDao().getWorkStatusPojoLiveDataForIds(
            listOf("${request.id}")
        )
        var expectedCounter = 1
        val maxCount = 5
        handler.post {
            workInfo.observeForever {
                val info = it.first()
                val isEnqueued = info.state == ENQUEUED
                val counter = CountingTestWorker.COUNT.get()
                if (isEnqueued && counter == maxCount) {
                    periodicLatch.countDown()
                } else if (isEnqueued && counter == expectedCounter) {
                    expectedCounter++
                    driver.setPeriodDelayMet(request.id)
                }
            }
        }
        assertThat(periodicLatch.await(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testWorker_withPeriodicWorkerWithInitialDelay_shouldRun() {
        val request = PeriodicWorkRequestBuilder<CountingTestWorker>(10, TimeUnit.DAYS)
            .setInitialDelay(10, TimeUnit.DAYS).build()
        wm.enqueue(request).result.get()
        driver.setInitialDelayMet(request.id)
        awaitPeriodicRunOnce(request.id)
        driver.setPeriodDelayMet(request.id)
        awaitCondition(request.id) {
            it.state == WorkInfo.State.ENQUEUED && CountingTestWorker.COUNT.get() == 2
        }
    }

    @Test
    fun testWorker_withPeriodicWorkerFlex_shouldRun() {
        val request = PeriodicWorkRequestBuilder<CountingTestWorker>(
            10, TimeUnit.DAYS, 1, TimeUnit.DAYS
        ).build()
        wm.enqueue(request).result.get()
        awaitPeriodicRunOnce(request.id)
    }

    @Test
    fun testWorker_afterSuccessfulRun_postConditions() {
        val request = OneTimeWorkRequest.from(TestWorker::class.java)
        wm.enqueue(request).result.get()
        awaitSuccess(request.id)
        driver.setAllConstraintsMet(request.id)
        driver.setInitialDelayMet(request.id)
    }

    @Test
    fun testWorkerUnique() {
        val request1 = OneTimeWorkRequestBuilder<TestWorker>()
            .setInitialDelay(1, TimeUnit.DAYS).build()
        wm.enqueueUniqueWork("name", REPLACE, request1).result.get()
        val request2 = OneTimeWorkRequestBuilder<TestWorker>()
            .setInitialDelay(1, TimeUnit.DAYS).build()
        wm.enqueueUniqueWork("name", REPLACE, request2).result.get()

        try {
            driver.setInitialDelayMet(request1.id)
            throw AssertionError()
        } catch (e: IllegalArgumentException) {
            // expected
        }
        driver.setInitialDelayMet(request2.id)
        awaitSuccess(request2.id)
    }

    @Test
    fun testWorker_afterSuccessfulRun_throwsExceptionWhenSetPeriodDelayMet() {
        val request = OneTimeWorkRequest.from(TestWorker::class.java)
        wm.enqueue(request).result.get()
        awaitSuccess(request.id)
        try {
            driver.setPeriodDelayMet(request.id)
            throw AssertionError()
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testSetAllConstraintsDontTriggerSecondRun() {
        val request = PeriodicWorkRequestBuilder<CountingTestWorker>(10, TimeUnit.DAYS)
            .setConstraints(Constraints(requiresCharging = true)).build()
        wm.enqueue(request).result.get()
        val latch = CountDownLatch(1)
        wm.workTaskExecutor.serialTaskExecutor.execute {
            latch.await()
        }
        // double call to setAllConstraint. It shouldn't lead to double execution of worker.
        // It should run once, but second time it shouldn't run because "setPeriodDelayMet" wasn't
        // called.
        driver.setAllConstraintsMet(request.id)
        driver.setAllConstraintsMet(request.id)
        latch.countDown()
        awaitCondition(request.id) {
            it.state == ENQUEUED && CountingTestWorker.COUNT.get() > 0
        }

        drainSerialExecutor()
        assertThat(wm.getWorkInfoById(request.id).get().state).isEqualTo(ENQUEUED)
        assertThat(CountingTestWorker.COUNT.get()).isEqualTo(1)
    }

    @Test
    fun testOneTimeWorkerRetry() {
        val request = OneTimeWorkRequest.from(RetryWorker::class.java)
        wm.enqueue(request).result.get()
        awaitReenqueuedAfterRetry(request.id)
    }

    @Test
    fun testPeriodicWorkerRetry() {
        val request = PeriodicWorkRequestBuilder<RetryWorker>(1, TimeUnit.DAYS).build()
        wm.enqueue(request).result.get()
        awaitReenqueuedAfterRetry(request.id)
    }

    private fun awaitSuccess(id: UUID) = awaitCondition(id) { it.state == WorkInfo.State.SUCCEEDED }

    private fun awaitPeriodicRunOnce(id: UUID) = awaitCondition(id) {
        it.state == ENQUEUED && CountingTestWorker.COUNT.get() == 1
    }

    private fun awaitReenqueuedAfterRetry(id: UUID) = awaitCondition(id) {
        it.state == ENQUEUED && it.runAttemptCount == 1
    }

    private fun awaitCondition(id: UUID, predicate: (WorkSpec.WorkInfoPojo) -> Boolean) {
        val latch = CountDownLatch(1)
        // TODO: specifically removing deduplication for periodic workers
        // so runs aren't dedupped. We need periodicity data in workinfo
        val workInfo = wm.workDatabase.workSpecDao().getWorkStatusPojoLiveDataForIds(listOf("$id"))

        handler.post {
            workInfo.observeForever {
                if (predicate(it.first())) {
                    latch.countDown()
                }
            }
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
    }

    private fun drainSerialExecutor() {
        val latch = CountDownLatch(1)

        class DrainTask(val executor: SerialExecutor) : Runnable {
            override fun run() {
                if (executor.hasPendingTasks()) {
                    executor.execute(this)
                } else {
                    latch.countDown()
                }
            }
        }

        val executor = wm.workTaskExecutor.serialTaskExecutor
        executor.execute(DrainTask(executor))
        latch.await()
    }
}
