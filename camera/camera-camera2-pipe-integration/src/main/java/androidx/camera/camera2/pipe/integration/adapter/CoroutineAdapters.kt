/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * This allows a java method that returns a ListenableFuture<Void> to be implemented by calling a
 * suspend function.
 *
 * Exceptions thrown from the coroutine scope are propagated to the returned future.
 * Canceling the future will attempt to cancel the coroutine.
 */
fun CoroutineScope.launchAsVoidFuture(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): ListenableFuture<Void> {
    // TODO: This method currently uses guava.asListenableFuture. This may be an expensive
    //  dependency to take on. We may need to evaluate this.
    @Suppress("UNCHECKED_CAST")
    return this.async(context = context, start = start, block = block)
        .asListenableFuture() as ListenableFuture<Void>
}
