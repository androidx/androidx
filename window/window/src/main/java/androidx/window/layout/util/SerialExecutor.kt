/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.window.layout.util

import java.util.ArrayDeque
import java.util.concurrent.Executor

/** An [Executor] that executes tasks sequentially. */
internal class SerialExecutor(private val executor: Executor) : Executor {
    private val lock = Any()
    private val tasks = ArrayDeque<Runnable>()
    private var active: Runnable? = null

    override fun execute(r: Runnable) {
        synchronized(lock) {
            tasks.add(
                Runnable {
                    try {
                        r.run()
                    } finally {
                        scheduleNext()
                    }
                }
            )
            if (active == null) {
                scheduleNext()
            }
        }
    }

    private fun scheduleNext() {
        synchronized(lock) {
            active = tasks.poll()
            if (active != null) {
                executor.execute(active)
            }
        }
    }
}
