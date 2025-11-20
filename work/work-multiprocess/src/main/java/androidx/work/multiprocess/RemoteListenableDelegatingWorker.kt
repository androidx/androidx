/*
 * Copyright 2024 The Android Open Source Project
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
import android.content.ComponentName
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.SuspendToFutureAdapter.launchFuture
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.Logger
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.awaitWithin
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.multiprocess.parcelable.ParcelConverters
import androidx.work.multiprocess.parcelable.ParcelableForegroundInfo
import androidx.work.multiprocess.parcelable.ParcelableInterruptRequest
import androidx.work.multiprocess.parcelable.ParcelableRemoteWorkRequest
import androidx.work.multiprocess.parcelable.ParcelableResult
import com.google.common.util.concurrent.ListenableFuture

/**
 * A worker which can delegate to an instance of RemoteListenableWorker but importantly only
 * constructs an instance of the RemoteListenableWorker in the remote process.
 */
public class RemoteListenableDelegatingWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters,
) : ListenableWorker(context, workerParameters) {

    internal val client = ListenableWorkerImplClient(context, workerParameters.backgroundExecutor)

    private var componentName: ComponentName? = null

    @Suppress("AsyncSuffixFuture") // Implementing a ListenableWorker
    override fun startWork(): ListenableFuture<Result> {
        return executeRemote(
            block = { iListenableWorkerImpl, callback ->
                val workerClassName = inputData.getString(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME)
                requireNotNull(workerClassName) {
                    "Need to specify a class name for the RemoteListenableWorker to delegate to."
                }
                val remoteWorkRequest =
                    ParcelableRemoteWorkRequest(workerClassName, workerParameters)
                val requestPayload = ParcelConverters.marshall(remoteWorkRequest)
                iListenableWorkerImpl.startWork(requestPayload, callback)
            },
            transformation = { response ->
                val parcelableResult =
                    ParcelConverters.unmarshall(response, ParcelableResult.CREATOR)
                parcelableResult.result
            },
        )
    }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        return executeRemote(
            block = { iListenableWorkerImpl, callback ->
                val workerClassName = inputData.getString(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME)
                requireNotNull(workerClassName) {
                    "Need to specify a class name for the RemoteListenableWorker to delegate to."
                }
                val remoteWorkRequest =
                    ParcelableRemoteWorkRequest(workerClassName, workerParameters)
                val requestPayload = ParcelConverters.marshall(remoteWorkRequest)
                iListenableWorkerImpl.getForegroundInfoAsync(requestPayload, callback)
            },
            transformation = { response ->
                val parcelableResult =
                    ParcelConverters.unmarshall(response, ParcelableForegroundInfo.CREATOR)
                parcelableResult.foregroundInfo
            },
        )
    }

    @SuppressLint("NewApi") // stopReason is actually a safe method to call.
    override fun onStopped() {
        super.onStopped()
        if (componentName != null) {
            client.execute(componentName!!) { iListenableWorkerImpl, callback ->
                val interruptRequest =
                    ParcelableInterruptRequest(workerParameters.id.toString(), stopReason)
                val request = ParcelConverters.marshall(interruptRequest)
                iListenableWorkerImpl.interrupt(request, callback)
                client.unbindService()
            }
        }
    }

    private inline fun <T> executeRemote(
        crossinline block:
            (
                iListenableWorkerImpl: IListenableWorkerImpl, callback: IWorkManagerImplCallback,
            ) -> Unit,
        crossinline transformation: (input: ByteArray) -> T,
    ): ListenableFuture<T> {
        val workManager = WorkManagerImpl.getInstance(context.applicationContext)
        val dispatcher = workManager.workTaskExecutor.taskCoroutineDispatcher
        return launchFuture(context = dispatcher) {
            val servicePackageName = inputData.getString(ARGUMENT_PACKAGE_NAME)
            val serviceClassName = inputData.getString(ARGUMENT_CLASS_NAME)
            requireNotNull(servicePackageName) {
                "Need to specify a package name for the Remote Service."
            }
            requireNotNull(serviceClassName) {
                "Need to specify a class name for the Remote Service."
            }
            componentName = ComponentName(servicePackageName, serviceClassName)
            val response =
                client
                    .execute(componentName!!) { iListenableWorkerImpl, callback ->
                        block(iListenableWorkerImpl, callback)
                    }
                    .awaitWithin(this@RemoteListenableDelegatingWorker)
            val result = transformation(response)
            Logger.get().debug(TAG, "Cleaning up")
            client.unbindService()
            result
        }
    }

    public companion object {
        private const val TAG = "RemoteListenableDelegatingWorker"

        // The RemoteListenableWorker class to delegate to.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME: String =
            "androidx.work.multiprocess.RemoteListenableDelegatingWorker.ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME"
    }
}
