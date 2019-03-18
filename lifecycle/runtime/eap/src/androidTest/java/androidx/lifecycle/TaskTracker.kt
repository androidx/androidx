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

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A simple counter on which we can await 0.
 */
class TaskTracker : TrackedExecutor.Callback {
    private val lock = ReentrantLock()
    private val idle = lock.newCondition()
    private var counter = 0

    override fun inc() {
        lock.withLock {
            counter++
        }
    }

    override fun dec() {
        lock.withLock {
            counter--
            if (counter == 0) {
                idle.signalAll()
            }
        }
    }

    fun awaitIdle(time: Long, timeUnit: TimeUnit): Boolean {
        lock.withLock {
            if (counter == 0) {
                return true
            }
            return idle.await(time, timeUnit)
        }
    }
}