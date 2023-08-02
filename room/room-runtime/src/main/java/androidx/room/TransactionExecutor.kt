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
package androidx.room

import java.util.ArrayDeque
import java.util.concurrent.Executor

/**
 * Executor wrapper for performing database transactions serially.
 *
 * Since database transactions are exclusive, this executor ensures that transactions are performed
 * in-order and one at a time, preventing threads from blocking each other when multiple concurrent
 * transactions are attempted.
 */
internal class TransactionExecutor(private val executor: Executor) : Executor {
    private val tasks = ArrayDeque<Runnable>()
    private var active: Runnable? = null
    private val syncLock = Any()
    override fun execute(command: Runnable) {
        synchronized(syncLock) {
            tasks.offer(Runnable {
                try {
                    command.run()
                } finally {
                    scheduleNext()
                }
            })
            if (active == null) {
                scheduleNext()
            }
        }
    }

    fun scheduleNext() {
        synchronized(syncLock) {
            if (tasks.poll().also { active = it } != null) {
                executor.execute(active)
            }
        }
    }
}
