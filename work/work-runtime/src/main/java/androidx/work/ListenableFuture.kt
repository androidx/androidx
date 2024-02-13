/*
 * Copyright 2018 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.work

import androidx.concurrent.futures.CallbackToFutureAdapter.getFuture
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun <T> launchFuture(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): ListenableFuture<T> = getFuture { completer ->
    val job = context[Job]
    completer.addCancellationListener({ job?.cancel() }, DirectExecutor.INSTANCE)
    CoroutineScope(context).launch(start = start) {
        try {
            val result = block()
            completer.set(result)
        } catch (_: CancellationException) {
            completer.setCancelled()
        } catch (throwable: Throwable) {
            completer.setException(throwable)
        }
    }
}

internal fun <V> Executor.executeAsync(debugTag: String, block: () -> V): ListenableFuture<V> =
    getFuture { completer ->
        val cancelled = AtomicBoolean(false)
        completer.addCancellationListener({ cancelled.set(true) }, DirectExecutor.INSTANCE)
        execute {
            if (cancelled.get()) return@execute
            try {
                completer.set(block())
            } catch (throwable: Throwable) {
                completer.setException(throwable)
            }
        }
        debugTag
    }
