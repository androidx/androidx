/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.datastore.snippets

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.datastore.snippets.multiprocess.MultiProcessDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// [START android_datastore_multiprocess_service]
class TimestampUpdateService : Service() {
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val multiProcessDataStore by lazy { MultiProcessDataStore(applicationContext) }

    // [START_EXCLUDE silent]
    override fun onBind(intent: Intent): IBinder? = null

    // [END_EXCLUDE]

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            while (true) {
                multiProcessDataStore.updateLastUpdateTime()
                delay(1000)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
// [END android_datastore_multiprocess_service]
