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

package androidx.compose.runtime

import androidx.compose.runtime.internal.Trace
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

internal actual abstract class TracingContext actual constructor(private val name: String) :
    CoroutineContext.Element, ThreadContextElement<Unit> {
    actual override val key: CoroutineContext.Key<*>
        get() = Key

    /**
     * This function is invoked after the coroutine has suspended on the current thread. When a
     * multi-threaded dispatcher is used, calls to `restoreThreadContext` may happen in parallel to
     * the subsequent `updateThreadContext` and `restoreThreadContext` operations.
     *
     * ```
     * Thread #1 | [updateThreadContext].x..^              [restoreThreadContext]
     * --------------------------------------------------------------------------------------------
     * Thread #2 |                           [updateThreadContext]..x..x.....^[restoreThreadContext]
     * ```
     *
     * OR
     *
     * ```
     * Thread #1 |  [update].x..^  [   ...    restore    ...   ]              [update].x..^[restore]
     * --------------------------------------------------------------------------------------------
     * Thread #2 |                 [update]...x....x..^[restore]
     * --------------------------------------------------------------------------------------------
     * Thread #3 |                                     [ ... update ... ] ...^  [restore]
     * ```
     *
     * (`...` indicate coroutine body is running; whitespace indicates the thread is not scheduled;
     * `^` is a suspension point; `x` are calls to modify the thread-local trace data)
     *
     * ```
     */
    override fun updateThreadContext(context: CoroutineContext) {
        Trace.beginSection(name)
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) {
        Trace.endSection(Unit)
    }

    actual companion object Key : CoroutineContext.Key<TracingContext>
}
