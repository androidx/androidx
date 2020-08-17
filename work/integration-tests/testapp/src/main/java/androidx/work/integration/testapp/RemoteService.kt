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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.await
import androidx.work.multiprocess.RemoteWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
            else -> Log.d(TAG, "Unknown intent")
        }
    }

    private suspend fun enqueueWorkRequest() {
        val request = OneTimeWorkRequestBuilder<TestWorker>().build()
        Log.d(TAG, "Enqueue-ing TestWorker")
        val remoteWorkManager = RemoteWorkManager.getInstance(this)
        remoteWorkManager.enqueue(listOf(request)).await()
    }

    companion object {
        const val TAG: String = "WM-RemoteService"
        const val ACTION_ENQUEUE_WORK = "ACTION_ENQUEUE_WORK"

        fun enqueueIntent(context: Context): Intent {
            val intent = Intent(context.applicationContext, RemoteService::class.java)
            intent.action = ACTION_ENQUEUE_WORK
            return intent
        }
    }
}
