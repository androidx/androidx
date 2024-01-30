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

package androidx.work.impl.background.greedy

import androidx.work.RunnableScheduler
import androidx.work.WorkInfo
import androidx.work.impl.StartStopToken
import androidx.work.impl.WorkLauncher
import java.util.concurrent.TimeUnit

internal class TimeLimiter @JvmOverloads constructor(
    private val runnableScheduler: RunnableScheduler,
    private val launcher: WorkLauncher,
    private val timeoutMs: Long = TimeUnit.MINUTES.toMillis(90),
) {
    private val lock = Any()
    private val tracked = mutableMapOf<StartStopToken, Runnable>()

    fun track(token: StartStopToken) {
        val stopRunnable = Runnable {
            launcher.stopWork(token, WorkInfo.STOP_REASON_TIMEOUT)
        }
        synchronized(lock) { tracked.put(token, stopRunnable) }
        runnableScheduler.scheduleWithDelay(timeoutMs, stopRunnable)
    }

    fun cancel(token: StartStopToken) {
        synchronized(lock) { tracked.remove(token) }?.let { runnableScheduler.cancel(it) }
    }
}
