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

package androidx.work.integration.testapp

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.multiprocess.RemoteListenableWorker
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RemoteWorker(private val context: Context, private val parameters: WorkerParameters) :
    RemoteListenableWorker(context, parameters) {
    private var job: Job? = null
    override fun startRemoteWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer ->
            Log.d(TAG, "Starting Remote Worker.")
            val scope = CoroutineScope(Dispatchers.Default)
            job = scope.launch {
                for (i in 1..30) {
                    delay(1000)
                    val progressData = workDataOf(PROGRESS_INFO to i)
                    setProgressAsync(progressData).await()
                }
            }

            job?.invokeOnCompletion {
                Log.d(TAG, "Done.")
                completer.set(Result.success())
            }
        }
    }

    override fun onStopped() {
        job?.cancel()
    }

    companion object {
        private const val TAG = "WM-RemoteWorker"
        private const val PROGRESS_INFO = "ProgressInformation"
    }
}
