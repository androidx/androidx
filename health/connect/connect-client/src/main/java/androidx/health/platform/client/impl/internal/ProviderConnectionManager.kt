/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.impl.internal

import android.content.Context
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import androidx.annotation.RestrictTo
import androidx.health.platform.client.impl.ipc.internal.ConnectionManager
import javax.annotation.concurrent.GuardedBy

/** Utility to return an instance of connection manager. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object ProviderConnectionManager {
    @GuardedBy("this") private var instance: ConnectionManager? = null

    @SuppressWarnings("RestrictedApi")
    fun getInstance(context: Context): ConnectionManager {
        synchronized(this) {
            if (instance == null) {
                instance = ConnectionManager(context.getApplicationContext(), startHandlerThread())
            }
            return instance!!
        }
    }

    private fun startHandlerThread(): Looper {
        val handlerThread =
            HandlerThread(
                "ProviderConnectionManager",
                Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE
            )
        handlerThread.start()
        return handlerThread.getLooper()
    }
}
