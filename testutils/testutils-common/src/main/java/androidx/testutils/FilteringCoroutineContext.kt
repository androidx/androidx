/*
 * Copyright 2024 The Android Open Source Project
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

import java.util.concurrent.ExecutorService
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * A coroutine dispatcher that can block some known coroutine runnables. We use it to slow down
 * database invalidation events.
 */
class FilteringCoroutineContext(delegate: ExecutorService) : CoroutineDispatcher() {

    val executor = FilteringExecutor(delegate)

    var filterFunction: (CoroutineContext, Runnable) -> Boolean = { _, _ -> true }
        set(value) {
            field = value
            executor.filterFunction = this::filter
        }

    private fun filter(runnable: Runnable): Boolean {
        if (runnable is RunnableWithCoroutineContext) {
            return filterFunction.invoke(runnable.context, runnable)
        }
        return true
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        executor.execute(RunnableWithCoroutineContext(context, block))
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true

    class RunnableWithCoroutineContext(val context: CoroutineContext, val actual: Runnable) :
        Runnable by actual
}
