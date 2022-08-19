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

package androidx.work.integration.testapp

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.await
import androidx.work.multiprocess.RemoteWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RemoteService : LifecycleService() {

    private var mStopped: Boolean = true
    private lateinit var mScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        initService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (mStopped) {
            initService()
        }
        handleIntent(intent)
        // If the service were to crash, we want all unacknowledged Intents to get redelivered.
        return Service.START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        mStopped = true
        mScope.cancel()
    }

    private fun initService() {
        mScope = CoroutineScope(Dispatchers.Default)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_ENQUEUE_WORK -> {
                mScope.launch {
                    enqueueWorkRequest()
                }
            }
            ACTION_ENQUEUE_CONTINUATION -> {
                mScope.launch {
                    enqueueContinuation()
                }
            }
            ACTION_CANCEL_WORK_BY_TAG -> {
                mScope.launch {
                    cancelAllWorkByTag()
                }
            }
            ACTION_CANCEL_ALL_WORK -> {
                mScope.launch {
                    cancelAllWork()
                }
            }
            ACTION_QUERY_WORK_INFO -> {
                mScope.launch {
                    queryWorkInfo()
                }
            }
            ACTION_ENQUEUE_UNIQUE_PERIODIC -> {
                mScope.launch {
                    enqueuePeriodicWorkRequestWithInitialDelay()
                }
            }

            ACTION_UPDATE_UNIQUE_PERIODIC -> {
                mScope.launch {
                    updateUniquePeriodicWork()
                }
            }
            else -> Log.d(TAG, "Unknown intent")
        }
    }

    private suspend fun enqueueWorkRequest() {
        val request = OneTimeWorkRequestBuilder<TestWorker>().build()
        Log.d(TAG, "Enqueue-ing TestWorker")
        val remoteWorkManager = RemoteWorkManager.getInstance(this)
        remoteWorkManager.enqueue(listOf(request)).await()
    }

    private suspend fun enqueuePeriodicWorkRequestWithInitialDelay() {
        val request = PeriodicWorkRequestBuilder<TestWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(15L, TimeUnit.MINUTES)
            .build()
        Log.d(TAG, "Enqueue-ing PeriodicWorker ${request.id}")
        val remoteWorkManager = RemoteWorkManager.getInstance(this)
        remoteWorkManager.enqueueUniquePeriodicWork(
            "unique-periodic",
            ExistingPeriodicWorkPolicy.KEEP, request
        ).await()
    }

    private suspend fun updateUniquePeriodicWork() {
        val request = PeriodicWorkRequestBuilder<TestWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .build()
        Log.d(TAG, "Enqueue-ing PeriodicWorker ${request.id}")
        val remoteWorkManager = RemoteWorkManager.getInstance(this)
        remoteWorkManager.enqueueUniquePeriodicWork(
            "unique-periodic", ExistingPeriodicWorkPolicy.UPDATE, request
        ).await()
    }

    @SuppressLint("EnqueueWork")
    private suspend fun enqueueContinuation() {
        val request = OneTimeWorkRequestBuilder<TestWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()

        Log.d(TAG, "Enqueue-ing a Continuation")
        RemoteWorkManager.getInstance(this)
            .beginWith(request)
            .enqueue()
            .await()
    }

    private suspend fun cancelAllWorkByTag() {
        Log.d(TAG, "Cancelling work by tag")
        val remoteWorkManager = RemoteWorkManager.getInstance(this)
        remoteWorkManager.cancelAllWorkByTag(WORK_TAG).await()
    }

    private suspend fun cancelAllWork() {
        Log.d(TAG, "Cancelling all work")
        val remoteWorkManager = RemoteWorkManager.getInstance(this)
        remoteWorkManager.cancelAllWork().await()
    }

    private suspend fun queryWorkInfo() {
        Log.d(TAG, "Querying work info")
        val query = WorkQuery.Builder.fromStates(listOf(WorkInfo.State.ENQUEUED))
            .build()

        val remoteWorkManager = RemoteWorkManager.getInstance(this)
        val workInfoList: List<WorkInfo> = remoteWorkManager.getWorkInfos(query).await()
        workInfoList.forEach {
            Log.d(TAG, "Found Worker with tags ${it.tags}")
        }
    }

    companion object {
        const val TAG: String = "WM-RemoteService"
        const val WORK_TAG = "RemoteWorkTag"
        const val ACTION_ENQUEUE_WORK = "ACTION_ENQUEUE_WORK"
        const val ACTION_ENQUEUE_CONTINUATION = "ACTION_ENQUEUE_CONTINUATION"
        const val ACTION_CANCEL_WORK_BY_TAG = "ACTION_CANCEL_WORK_BY_TAG"
        const val ACTION_CANCEL_ALL_WORK = "ACTION_CANCEL_ALL_WORK"
        const val ACTION_QUERY_WORK_INFO = "ACTION_QUERY_WORK_INFO"
        const val ACTION_ENQUEUE_UNIQUE_PERIODIC = "ACTION_ENQUEUE_UNIQUE_PERIODIC"
        const val ACTION_UPDATE_UNIQUE_PERIODIC = "ACTION_UPDATE_UNIQUE_PERIODIC"

        fun enqueueIntent(context: Context): Intent {
            val intent = Intent(context.applicationContext, RemoteService::class.java)
            intent.action = ACTION_ENQUEUE_WORK
            return intent
        }

        fun enqueueContinuationIntent(context: Context): Intent {
            val intent = Intent(context.applicationContext, RemoteService::class.java)
            intent.action = ACTION_ENQUEUE_CONTINUATION
            return intent
        }

        fun enqueueUniquePeriodicIntent(context: Context): Intent {
            val intent = Intent(context.applicationContext, RemoteService::class.java)
            intent.action = ACTION_ENQUEUE_UNIQUE_PERIODIC
            return intent
        }

        fun updateUniquePeriodicIntent(context: Context): Intent {
            val intent = Intent(context.applicationContext, RemoteService::class.java)
            intent.action = ACTION_UPDATE_UNIQUE_PERIODIC
            return intent
        }

        fun cancelWorkByTagIntent(context: Context): Intent {
            val intent = Intent(context.applicationContext, RemoteService::class.java)
            intent.action = ACTION_CANCEL_WORK_BY_TAG
            return intent
        }

        fun cancelAllWorkIntent(context: Context): Intent {
            val intent = Intent(context.applicationContext, RemoteService::class.java)
            intent.action = ACTION_CANCEL_ALL_WORK
            return intent
        }

        fun queryWorkInfoIntent(context: Context): Intent {
            val intent = Intent(context.applicationContext, RemoteService::class.java)
            intent.action = ACTION_QUERY_WORK_INFO
            return intent
        }
    }
}
