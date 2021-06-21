/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl.internal

import android.annotation.SuppressLint
import android.content.Context
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import androidx.annotation.GuardedBy
import androidx.health.services.client.impl.ipc.internal.ConnectionManager

/**
 * Utility to return an instance of connection manager.
 *
 * @hide
 */
public object HsConnectionManager {

    private val lock = Any()

    // Suppress StaticFieldLeak; we're only storing application Context.
    @SuppressLint("StaticFieldLeak")
    @GuardedBy("lock")
    private lateinit var instance: ConnectionManager

    @JvmStatic
    public fun getInstance(context: Context): ConnectionManager {
        synchronized(lock) {
            if (!::instance.isInitialized) {
                val looper = startHandlerThread()
                instance = ConnectionManager(context.applicationContext, looper)
            }

            return instance
        }
    }

    private fun startHandlerThread(): Looper {
        val handlerThread =
            HandlerThread(
                "HsConnectionManager",
                Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE
            )
        handlerThread.start()
        return handlerThread.looper
    }
}
