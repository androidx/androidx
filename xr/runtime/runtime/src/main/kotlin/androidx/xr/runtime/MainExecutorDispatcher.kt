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

package androidx.xr.runtime

import android.os.Looper
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * A [MainCoroutineDispatcher] backed by a main-thread [Executor] that supports both standard
 * (dispatched) and [immediate] execution semantics based on the Android main Looper.
 *
 * Note: The provided [executor] MUST execute tasks on the Android Main Looper thread (for example,
 * obtained via `ContextCompat.getMainExecutor(context)`). The [immediate] variant relies on
 * [Looper.getMainLooper] to determine whether execution can proceed synchronously.
 *
 * @param executor An [Executor] that executes tasks on the Android Main Looper thread.
 * @param isImmediate Whether this dispatcher executes tasks immediately without redispatching when
 *   already on the main thread.
 */
internal class MainExecutorDispatcher(
    private val executor: Executor,
    private val isImmediate: Boolean = false,
) : MainCoroutineDispatcher() {
    private val delegate = executor.asCoroutineDispatcher()

    override val immediate: MainExecutorDispatcher by lazy {
        if (isImmediate) this else MainExecutorDispatcher(executor, isImmediate = true)
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        if (!isImmediate) return true
        val mainLooper = Looper.getMainLooper() ?: return true
        return Looper.myLooper() != mainLooper
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        delegate.dispatch(context, block)
    }

    override fun toString(): String = "MainExecutorDispatcher(isImmediate=$isImmediate)"
}
