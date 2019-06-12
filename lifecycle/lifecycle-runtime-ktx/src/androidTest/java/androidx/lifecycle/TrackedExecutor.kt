/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * An executor wrapper that tracks active tasks and reports back to a Callback when it changes.
 */
class TrackedExecutor(
    private val callback: Callback,
    private val delegate: ExecutorService
) : Executor {
    override fun execute(runnable: Runnable) {
        callback.inc()
        delegate.execute {
            try {
                runnable.run()
            } finally {
                callback.dec()
            }
        }
    }

    fun shutdown(time: Long, unit: TimeUnit): Boolean {
        delegate.shutdown()
        return delegate.awaitTermination(time, unit)
    }

    interface Callback {
        fun inc()
        fun dec()
    }
}