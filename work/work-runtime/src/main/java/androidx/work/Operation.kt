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

// Always inline ktx extension methods unless we have additional call site costs.
@file:Suppress("NOTHING_TO_INLINE")

package androidx.work

import androidx.work.impl.OperationImpl
import java.util.concurrent.Executor

/**
 * Awaits an [Operation] without blocking a thread.
 *
 * <p>
 * This method returns the terminal state of the [Operation] which is [Operation.State.SUCCESS]
 * or throws a [Throwable] that represents why the [Operation] failed.
 */
public suspend inline fun Operation.await(): Operation.State.SUCCESS = result.await()

internal fun launchOperation(executor: Executor, block: () -> Unit): Operation =
    OperationImpl().also { operation ->
        executor.execute {
            try {
                block()
                operation.markState(Operation.SUCCESS)
            } catch (t: Throwable) {
                operation.markState(Operation.State.FAILURE(t))
            }
        }
    }
