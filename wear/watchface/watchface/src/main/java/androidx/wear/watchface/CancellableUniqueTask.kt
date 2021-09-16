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

package androidx.wear.watchface

import android.os.Handler

/**
 * Task posting helper which allows only one pending task at a time.
 */
internal class CancellableUniqueTask(private val handler: Handler) {
    private var pendingTask: Runnable? = null

    fun cancel() {
        pendingTask?.let { handler.removeCallbacks(it) }
        pendingTask = null
    }

    fun isPending() = (pendingTask != null)

    fun postUnique(task: () -> Unit) {
        postDelayedUnique(0, task)
    }

    fun postDelayedUnique(delayMillis: Long, task: () -> Unit) {
        cancel()
        val runnable = Runnable {
            task()
            pendingTask = null
        }
        handler.postDelayed(runnable, delayMillis)
        pendingTask = runnable
    }
}