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

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.await
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * Awaits an [Operation] without blocking a thread.
 *
 * <p>
 * This method returns the terminal state of the [Operation] which is [Operation.State.SUCCESS] or
 * throws a [Throwable] that represents why the [Operation] failed.
 */
public suspend inline fun Operation.await(): Operation.State.SUCCESS = result.await()

internal fun launchOperation(
    tracer: Tracer,
    label: String,
    executor: Executor,
    block: () -> Unit
): Operation {
    val liveData = MutableLiveData<Operation.State>(Operation.IN_PROGRESS)
    val future =
        CallbackToFutureAdapter.getFuture { completer ->
            executor.execute {
                tracer.traced(label) {
                    try {
                        block()
                        liveData.postValue(Operation.SUCCESS)
                        completer.set(Operation.SUCCESS)
                    } catch (t: Throwable) {
                        liveData.postValue(Operation.State.FAILURE(t))
                        completer.setException(t)
                    }
                }
            }
        }
    return OperationImpl(liveData, future)
}

private class OperationImpl(
    private val state: LiveData<Operation.State>,
    private val future: ListenableFuture<Operation.State.SUCCESS>,
) : Operation {
    override fun getState(): LiveData<Operation.State> = state

    override fun getResult(): ListenableFuture<Operation.State.SUCCESS> = future
}
