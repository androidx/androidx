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

package androidx.work.multiprocess

import androidx.arch.core.util.Function
import androidx.concurrent.futures.SuspendToFutureAdapter.launchFuture
import androidx.concurrent.futures.await
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.asCoroutineDispatcher

internal fun <I, O> ListenableFuture<I>.map(
    transformation: Function<I, O>,
    executor: Executor,
): ListenableFuture<O> =
    launchFuture(executor.asCoroutineDispatcher(), launchUndispatched = false) {
        transformation.apply(await())
    }
