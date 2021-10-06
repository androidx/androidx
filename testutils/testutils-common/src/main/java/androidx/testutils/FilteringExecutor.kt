/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.testutils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An executor that can block some known runnables. We use it to slow down database
 * invalidation events.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FilteringExecutor(
    private val delegate: ExecutorService = Executors.newSingleThreadExecutor()
) : Executor {
    private val deferred = mutableListOf<Runnable>()
    private val deferredSize = MutableStateFlow(0)
    private val lock = ReentrantLock()

    var filterFunction: (Runnable) -> Boolean = { true }
        set(value) {
            field = value
            reEnqueueDeferred()
        }

    suspend fun awaitDeferredSizeAtLeast(min: Int) = withTestTimeout {
        deferredSize.mapLatest {
            it >= min
        }.first()
    }

    private fun reEnqueueDeferred() {
        val copy = lock.withLock {
            val copy = deferred.toMutableList()
            deferred.clear()
            deferredSize.value = 0
            copy
        }
        copy.forEach(this::execute)
    }

    fun deferredSize(): Int {
        return deferred.size
    }

    fun executeAll() {
        while (deferred.isNotEmpty()) {
            deferred.removeFirst().run()
        }
    }

    fun executeLatestDeferred() {
        deferred.removeLast().run()
    }

    override fun execute(command: Runnable) {
        lock.withLock {
            if (filterFunction(command)) {
                delegate.execute(command)
            } else {
                deferred.add(command)
                deferredSize.value += 1
            }
        }
    }
}

suspend fun <T> withTestTimeout(block: suspend () -> T): T {
    try {
        return withTimeout(
            timeMillis = TimeUnit.SECONDS.toMillis(3)
        ) {
            block()
        }
    } catch (err: Throwable) {
        throw AssertionError("didn't complete in expected time", err)
    }
}
