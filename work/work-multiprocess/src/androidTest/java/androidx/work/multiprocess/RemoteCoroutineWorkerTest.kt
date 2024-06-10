/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkRequest
import androidx.work.buildDelegatedRemoteRequestData
import androidx.work.impl.Processor
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.WorkerWrapper
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.utils.SerialExecutorImpl
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.isRemoteWorkRequest
import androidx.work.multiprocess.RemoteListenableDelegatingWorker.Companion.ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
public class RemoteCoroutineWorkerTest {
    private lateinit var mConfiguration: Configuration
    private lateinit var mTaskExecutor: TaskExecutor
    private lateinit var mScheduler: Scheduler
    private lateinit var mProcessor: Processor
    private lateinit var mForegroundProcessor: ForegroundProcessor
    private lateinit var mWorkManager: WorkManagerImpl
    private lateinit var mExecutor: Executor

    // Necessary for the reified function
    public lateinit var mContext: Context
    public lateinit var mDatabase: WorkDatabase

    @Before
    public fun setUp() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        mContext = InstrumentationRegistry.getInstrumentation().context
        mExecutor = Executor { it.run() }
        mConfiguration =
            Configuration.Builder().setExecutor(mExecutor).setTaskExecutor(mExecutor).build()
        mTaskExecutor =
            object : TaskExecutor {
                override fun getMainThreadExecutor() = mExecutor

                override fun getSerialTaskExecutor() = SerialExecutorImpl(mExecutor)
            }
        mScheduler = mock(Scheduler::class.java)
        mForegroundProcessor = mock(ForegroundProcessor::class.java)
        mWorkManager = mock(WorkManagerImpl::class.java)
        mDatabase = WorkDatabase.create(mContext, mExecutor, mConfiguration.clock, true)
        val schedulers = listOf(mScheduler)
        // Processor
        mProcessor = Processor(mContext, mConfiguration, mTaskExecutor, mDatabase)
        // WorkManagerImpl
        `when`(mWorkManager.configuration).thenReturn(mConfiguration)
        `when`(mWorkManager.workTaskExecutor).thenReturn(mTaskExecutor)
        `when`(mWorkManager.workDatabase).thenReturn(mDatabase)
        `when`(mWorkManager.schedulers).thenReturn(schedulers)
        `when`(mWorkManager.processor).thenReturn(mProcessor)
        WorkManagerImpl.setDelegate(mWorkManager)
        RemoteWorkManagerInfo.clearInstance()
    }

    @Test
    @MediumTest
    public fun testRemoteSuccessWorker() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val request = buildRequest<RemoteSuccessWorker>()
        val wrapper = buildWrapper(request)
        wrapper.launch().get()
        val workSpec = mDatabase.workSpecDao().getWorkSpec(request.stringId)!!
        assertEquals(workSpec.state, WorkInfo.State.SUCCEEDED)
    }

    @Test
    @MediumTest
    public fun testSuccessWorker() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val request = buildRequest<TestWorker>()
        val wrapper = buildWrapper(request)
        wrapper.launch().get()
        val workSpec = mDatabase.workSpecDao().getWorkSpec(request.stringId)!!
        assertEquals(workSpec.state, WorkInfo.State.SUCCEEDED)
    }

    @Test
    @MediumTest
    public fun testRemoteFailureWorker() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val request = buildRequest<RemoteFailureWorker>()
        val wrapper = buildWrapper(request)
        wrapper.launch().get()
        val workSpec = mDatabase.workSpecDao().getWorkSpec(request.stringId)!!
        assertEquals(workSpec.state, WorkInfo.State.FAILED)
    }

    @Test
    @MediumTest
    public fun testRemoteRetryWorker() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val request = buildRequest<RemoteRetryWorker>()
        val wrapper = buildWrapper(request)
        wrapper.launch().get()
        val workSpec = mDatabase.workSpecDao().getWorkSpec(request.stringId)!!
        assertEquals(workSpec.state, WorkInfo.State.ENQUEUED)
    }

    @Test
    @MediumTest
    public fun testSwitchRemoteProcess() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val packageName = "PACKAGE_NAME"
        val className = "CLASS_NAME"
        val inputKey = "INPUT_KEY"
        val inputValue = "InputValue"
        val inputData = Data.Builder().putString(inputKey, inputValue).build()
        val data =
            buildDelegatedRemoteRequestData(
                delegatedWorkerName = RemoteSuccessWorker::class.java.name,
                componentName = ComponentName(packageName, className),
                inputData
            )
        assertEquals(data.isRemoteWorkRequest(), true)
        assertEquals(
            data.getString(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME),
            RemoteSuccessWorker::class.java.name
        )
        assertEquals(data.getString(RemoteListenableWorker.ARGUMENT_PACKAGE_NAME), packageName)
        assertEquals(data.getString(RemoteListenableWorker.ARGUMENT_CLASS_NAME), className)
        assertEquals(data.getString(inputKey), inputValue)
    }

    private inline fun <reified T : ListenableWorker> buildRequest(): OneTimeWorkRequest {
        val inputData =
            Data.Builder()
                .putString(RemoteListenableWorker.ARGUMENT_PACKAGE_NAME, mContext.packageName)
                .putString(
                    RemoteListenableWorker.ARGUMENT_CLASS_NAME,
                    RemoteWorkerService::class.java.name
                )
                .putString(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME, T::class.java.name)
                .build()

        val request =
            OneTimeWorkRequest.Builder(RemoteListenableDelegatingWorker::class.java)
                .setInputData(inputData)
                .build()

        mDatabase.workSpecDao().insertWorkSpec(request.workSpec)
        return request
    }

    private fun buildWrapper(request: WorkRequest): WorkerWrapper {
        return WorkerWrapper.Builder(
                mContext,
                mConfiguration,
                mTaskExecutor,
                mForegroundProcessor,
                mDatabase,
                mDatabase.workSpecDao().getWorkSpec(request.stringId)!!,
                emptyList()
            )
            .build()
    }
}
