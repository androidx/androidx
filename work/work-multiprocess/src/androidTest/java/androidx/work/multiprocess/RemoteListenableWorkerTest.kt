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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.OneTimeWorkRequest
import androidx.work.ProgressUpdater
import androidx.work.WorkInfo
import androidx.work.WorkInfo.Companion.STOP_REASON_CANCELLED_BY_APP
import androidx.work.WorkInfo.Companion.STOP_REASON_CONSTRAINT_CONNECTIVITY
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.impl.Processor
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.WorkerWrapper
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.utils.SerialExecutorImpl
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.impl.utils.tryDelegateRemoteListenableWorker
import androidx.work.multiprocess.RemoteListenableDelegatingWorker.Companion.ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
public class RemoteListenableWorkerTest {
    private lateinit var mConfiguration: Configuration
    private lateinit var mTaskExecutor: TaskExecutor
    private lateinit var mScheduler: Scheduler
    private lateinit var mProcessor: Processor
    private lateinit var mForegroundProcessor: ForegroundProcessor
    private lateinit var mWorkManager: WorkManagerImpl
    private lateinit var mExecutor: Executor
    private val workerFactory = TrackingRemoteWorkerFactory()

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
            Configuration.Builder()
                .setExecutor(mExecutor)
                .setTaskExecutor(mExecutor)
                .setWorkerFactory(workerFactory)
                .build()
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
        assertEquals(workSpec.output, RemoteSuccessWorker.outputData())
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
        assertEquals(workSpec.output, RemoteFailureWorker.outputData())
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
    public fun testRemoteStopWorker() = runBlocking {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return@runBlocking
        }

        val request = buildRequest<RemoteStopWorker>()
        val wrapper = buildWrapper(request)
        wrapper.launch()
        val remote = workerFactory.awaitRemote(request.id) as RemoteStopWorker
        remote.startRemoteDeferred.await()
        wrapper.interrupt(STOP_REASON_CONSTRAINT_CONNECTIVITY)
        val reason =
            withTimeoutOrNull(2000) { remote.stopDeferred.await() }
                ?: throw AssertionError("Stop wasn't called")
        assertEquals(STOP_REASON_CONSTRAINT_CONNECTIVITY, reason)
    }

    @Test
    @MediumTest
    public fun testUnbindService_successWorker() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }
        val delegatingWorker = buildDelegatingWorker<RemoteSuccessWorker>()
        delegatingWorker.startWork().get()
        assertNull(delegatingWorker.client.connection)
    }

    @Test
    @MediumTest
    public fun testUnbindService_failureWorker() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }
        val delegatingWorker = buildDelegatingWorker<RemoteFailureWorker>()
        delegatingWorker.startWork().get()
        assertNull(delegatingWorker.client.connection)
    }

    @Test
    @MediumTest
    public fun testUnbindService_stopWorker() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }
        val delegatingWorker = buildDelegatingWorker<RemoteStopWorker>()
        delegatingWorker.startWork()
        delegatingWorker.client.connection!!.mFuture.get()
        delegatingWorker.stop(STOP_REASON_CANCELLED_BY_APP)
        assertNull(delegatingWorker.client.connection)
    }

    public inline fun <reified T : RemoteListenableWorker> buildRequest(): OneTimeWorkRequest {
        val inputData =
            Data.Builder()
                .putString(ARGUMENT_PACKAGE_NAME, mContext.packageName)
                .putString(ARGUMENT_CLASS_NAME, RemoteWorkerService::class.java.name)
                .build()

        val request = OneTimeWorkRequest.Builder(T::class.java).setInputData(inputData).build()

        // Delegation
        val workSpec = tryDelegateRemoteListenableWorker(request.workSpec)
        mDatabase.workSpecDao().insertWorkSpec(workSpec)
        return request
    }

    public fun buildWrapper(request: WorkRequest): WorkerWrapper {
        return WorkerWrapper.Builder(
                mContext,
                mConfiguration,
                mTaskExecutor,
                mForegroundProcessor,
                mDatabase,
                mDatabase.workSpecDao().getWorkSpec(request.stringId)!!,
                emptyList(),
            )
            .build()
    }

    private inline fun <reified T : RemoteListenableWorker> buildDelegatingWorker():
        RemoteListenableDelegatingWorker {
        val request = buildRequest<T>()
        val inputData =
            Data.Builder()
                .putString(ARGUMENT_PACKAGE_NAME, mContext.packageName)
                .putString(ARGUMENT_CLASS_NAME, RemoteWorkerService::class.java.name)
                .putString(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME, T::class.java.name)
                .build()
        val progressUpdater = mock(ProgressUpdater::class.java)
        val foregroundUpdater = mock(ForegroundUpdater::class.java)
        val parameters =
            WorkerParameters(
                request.id,
                inputData,
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                0,
                0,
                mConfiguration.executor,
                mConfiguration.workerCoroutineContext,
                mTaskExecutor,
                mConfiguration.workerFactory,
                progressUpdater,
                foregroundUpdater,
            )
        val worker: RemoteListenableDelegatingWorker =
            mConfiguration.workerFactory.createWorkerWithDefaultFallback(
                mContext,
                RemoteListenableDelegatingWorker::class.java.name,
                parameters,
            ) as RemoteListenableDelegatingWorker
        return worker
    }
}

public class RemoteStopWorker(context: Context, parameters: WorkerParameters) :
    RemoteListenableWorker(context, parameters) {

    val startRemoteDeferred = CompletableDeferred<Unit>()
    val stopDeferred = CompletableDeferred<Int>()

    // specially leak completer reference and keep it around.
    // otherwise future will be automatically cancelled.
    lateinit var leakedCompleter: Completer<Result>

    override fun startRemoteWork(): ListenableFuture<Result> {
        startRemoteDeferred.complete(Unit)
        return CallbackToFutureAdapter.getFuture {
            leakedCompleter = it
            "never resolved"
        }
    }

    // in this context stop reason doesn't make difference
    @SuppressLint("NewApi")
    override fun onStopped() {
        super.onStopped()
        stopDeferred.complete(stopReason)
    }
}
