/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.multiprocess

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkContinuationImpl
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.utils.SerialExecutor
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.multiprocess.parcelable.ParcelConverters.marshall
import androidx.work.multiprocess.parcelable.ParcelConverters.unmarshall
import androidx.work.multiprocess.parcelable.ParcelableWorkContinuationImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
public class ParcelableWorkContinuationImplTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManagerImpl

    @Before
    public fun setUp() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        context = ApplicationProvider.getApplicationContext<Context>()
        val taskExecutor = object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean {
                return true
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }
        }
        ArchTaskExecutor.getInstance().setDelegate(taskExecutor)

        val scheduler = mock(Scheduler::class.java)
        val configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()

        workManager = spy(
            WorkManagerImpl(
                context,
                configuration,
                object : androidx.work.impl.utils.taskexecutor.TaskExecutor {
                    val executor = Executor {
                        it.run()
                    }
                    val serialExecutor = SerialExecutor(executor)
                    override fun postToMainThread(runnable: Runnable) {
                        serialExecutor.execute(runnable)
                    }

                    override fun getMainThreadExecutor(): Executor {
                        return serialExecutor
                    }

                    override fun executeOnBackgroundThread(runnable: Runnable) {
                        serialExecutor.execute(runnable)
                    }

                    override fun getBackgroundExecutor(): SerialExecutor {
                        return serialExecutor
                    }
                }
            )
        )
        `when`<List<Scheduler>>(workManager.schedulers).thenReturn(listOf(scheduler))
        WorkManagerImpl.setDelegate(workManager)
    }

    @Test
    @MediumTest
    public fun basicContinuationTest() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val first = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val second = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val continuation = workManager.beginWith(listOf(first)).then(second)
        val parcelable = ParcelableWorkContinuationImpl(continuation as WorkContinuationImpl)
        assertOn(parcelable)
    }

    @Test
    @MediumTest
    public fun continuationTests2() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val first = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val second = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val third = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val continuation = workManager.beginWith(listOf(first, second)).then(third)
        val parcelable = ParcelableWorkContinuationImpl(continuation as WorkContinuationImpl)
        assertOn(parcelable)
    }

    @Test
    @MediumTest
    public fun continuationTest3() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val first = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val second = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val continuation = workManager.beginUniqueWork(
            "test", ExistingWorkPolicy.REPLACE, listOf(first)
        ).then(second)
        val parcelable = ParcelableWorkContinuationImpl(continuation as WorkContinuationImpl)
        assertOn(parcelable)
    }

    @Test
    @MediumTest
    public fun continuationTest4() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val first = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val second = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val continuation = workManager.beginUniqueWork(
            "test", ExistingWorkPolicy.REPLACE, listOf(first)
        ).then(second)
        val parcelable = ParcelableWorkContinuationImpl(continuation as WorkContinuationImpl)
        val continuation2 = parcelable.info.toWorkContinuationImpl(workManager)
        equal(
            ParcelableWorkContinuationImpl(continuation).info,
            ParcelableWorkContinuationImpl(continuation2).info
        )
    }

    @Test
    @MediumTest
    public fun combineContinuationTests() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val first = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val second = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val third = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val continuation1 = workManager.beginWith(listOf(first, second)).then(third)

        val fourth = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val fifth = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val sixth = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val continuation2 = workManager.beginWith(listOf(fourth, fifth)).then(sixth)

        val continuation = WorkContinuationImpl.combine(listOf(continuation1, continuation2))
        val parcelable = ParcelableWorkContinuationImpl(continuation as WorkContinuationImpl)
        assertOn(parcelable)
    }

    // Utilities

    private fun assertOn(parcelable: ParcelableWorkContinuationImpl) {
        val parcelable2 = unmarshall(marshall(parcelable), ParcelableWorkContinuationImpl.CREATOR)
        equal(parcelable.info, parcelable2.info)
    }

    private fun equal(
        first: ParcelableWorkContinuationImpl.WorkContinuationImplInfo,
        second: ParcelableWorkContinuationImpl.WorkContinuationImplInfo
    ) {
        assertEquals(first.name, second.name)
        assertEquals(first.existingWorkPolicy, second.existingWorkPolicy)
        assertRequests(first.work, second.work)
        assertEquals(first.parentInfos?.size, first.parentInfos?.size)
        first.parentInfos?.forEachIndexed { i, info -> equal(info, second.parentInfos!![i]) }
    }

    private fun assertRequest(first: WorkRequest, second: WorkRequest) {
        assertEquals(first.id, second.id)
        assertEquals(first.workSpec, second.workSpec)
        assertEquals(first.tags, second.tags)
    }

    private fun assertRequests(listOne: List<WorkRequest>, listTwo: List<WorkRequest>) {
        listOne.forEachIndexed { i, workRequest ->
            assertRequest(workRequest, listTwo[i])
        }
    }
}
