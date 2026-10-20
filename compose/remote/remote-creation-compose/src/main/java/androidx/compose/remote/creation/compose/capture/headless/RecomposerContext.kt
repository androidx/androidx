/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.capture.headless

import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher

/**
 * Returns a [CoroutineContext] (without a [Job]) whose dispatcher executes at most one task at a
 * time so that recomposition, frame clock callbacks, effect continuations, and rendering are
 * serialized:
 * - When no [ContinuationInterceptor] is present (e.g. a raw background or Binder thread), falls
 *   back to a single-threaded slice of [Dispatchers.Default].
 * - When running with a [MainCoroutineDispatcher], preserves the context as-is since execution is
 *   already confined to the main thread.
 * - When running on a multi-threaded [CoroutineDispatcher] (where
 *   [CoroutineDispatcher.isDispatchNeeded] is true), constrains it via
 *   `limitedParallelism(parallelism = 1, name = name)`.
 * - For unconfined dispatchers (e.g. [Dispatchers.Unconfined], where `isDispatchNeeded` is false
 *   and `limitedParallelism` throws [UnsupportedOperationException]) or custom non-dispatcher
 *   [ContinuationInterceptor]s (such as Compose test interceptors), preserves the context as-is.
 */
internal fun CoroutineContext.toSingleThreadedRecomposerContext(name: String): CoroutineContext {
    val baseContext = this.minusKey(Job)
    val interceptor = baseContext[ContinuationInterceptor]
    return when {
        interceptor == null ->
            baseContext +
                countingSerialDispatcher(
                    Dispatchers.Default.limitedParallelism(parallelism = 1, name = name)
                )
        interceptor is MainCoroutineDispatcher -> baseContext
        interceptor is CoroutineDispatcher && interceptor.isDispatchNeeded(EmptyCoroutineContext) ->
            baseContext +
                countingSerialDispatcher(
                    interceptor.limitedParallelism(parallelism = 1, name = name)
                )
        else -> baseContext
    }
}

/**
 * Wraps [delegate] in a [CountingSerialDispatcher], keeping its [Delay] implementation if it has
 * one so that `delay()` on the returned dispatcher still runs on the delegate's clock (a
 * virtual-time test dispatcher in particular; without this, coroutines fall back to real time).
 */
// TODO(b/263369561): Delay is @InternalCoroutinesApi; this mirrors what compose:ui-test does to
// propagate delay through its continuation interceptor, see
// DelayPropagatingContinuationInterceptorWrapper.
@OptIn(InternalCoroutinesApi::class)
internal fun countingSerialDispatcher(delegate: CoroutineDispatcher): CountingSerialDispatcher =
    if (delegate is Delay) CountingSerialDelayDispatcher(delegate)
    else CountingSerialDispatcher(delegate)

@OptIn(InternalCoroutinesApi::class)
private class CountingSerialDelayDispatcher(delegate: CoroutineDispatcher) :
    CountingSerialDispatcher(delegate), Delay by (delegate as Delay)

/**
 * Runs tasks on [delegate], which executes at most one task at a time, and counts the tasks that
 * were dispatched but have not started yet. Since tasks never run concurrently, a task that
 * observes [queuedTasks] `== 0` knows nothing is waiting to run behind it; see
 * `drainRecomposerDispatcher` in RecomposerQuiescence.kt. Create instances with
 * [countingSerialDispatcher].
 */
internal open class CountingSerialDispatcher(private val delegate: CoroutineDispatcher) :
    CoroutineDispatcher() {
    private val queued = AtomicInteger()

    /** Number of dispatched tasks that have not started running yet. */
    val queuedTasks: Int
        get() = queued.get()

    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        delegate.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queued.incrementAndGet()
        try {
            delegate.dispatch(context) {
                queued.decrementAndGet()
                block.run()
            }
        } catch (e: Throwable) {
            queued.decrementAndGet()
            throw e
        }
    }
}
