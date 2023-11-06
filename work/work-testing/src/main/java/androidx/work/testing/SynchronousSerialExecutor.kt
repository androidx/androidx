/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.work.testing

import androidx.annotation.GuardedBy
import androidx.work.impl.utils.taskexecutor.SerialExecutor

internal class SynchronousSerialExecutor : SerialExecutor {
    private val lock = Any()

    @GuardedBy("lock")
    private val tasks = ArrayDeque<Runnable>()

    @GuardedBy("lock")
    private var isRunning = false

    override fun execute(command: Runnable) {
        synchronized(lock) {
            tasks.add(command)
            if (isRunning) return
            isRunning = true
        }
        do {
            // running potentially long task without the lock
            // so other threads can grab the lock to add runnables to the queue
            synchronized(lock) { tasks.removeFirstOrNull() }?.run()
            // check if new tasks were added while the previous was ran.
            synchronized(lock) { isRunning = tasks.isNotEmpty() }
        } while (isRunning)
    }

    override fun hasPendingTasks(): Boolean {
        return synchronized(lock) { tasks.isNotEmpty() }
    }
}
