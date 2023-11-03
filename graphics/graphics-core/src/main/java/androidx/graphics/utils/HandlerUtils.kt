/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import java.util.concurrent.Executor

/**
 * Handler does not expose a post method that takes a token and a runnable.
 * We need the token to be able to cancel pending requests so just call
 * postAtTime with the default of SystemClock.uptimeMillis
 */
internal fun Handler.post(token: Any?, runnable: Runnable) {
    postAtTime(runnable, token, SystemClock.uptimeMillis())
}

/**
 * Helper class that wraps a Handler/HandlerThread combination and implements the [Executor]
 * interface
 */
internal class HandlerThreadExecutor(name: String) : Executor {

    private val mHandlerThread = HandlerThread(name).apply { start() }
    private val mHandler = Handler(mHandlerThread.looper)

    fun post(token: Any, runnable: Runnable) {
        mHandler.post(token, runnable)
    }

    fun post(runnable: Runnable) {
        mHandler.post(runnable)
    }

    fun removeCallbacksAndMessages(token: Any) {
        mHandler.removeCallbacksAndMessages(token)
    }

    override fun execute(runnable: Runnable?) {
        runnable?.let { mHandler.post(it) }
    }

    private var mIsQuit = false

    val isRunning: Boolean
        get() = !mIsQuit

    fun quit() {
        mHandlerThread.quit()
        mIsQuit = true
    }
}
