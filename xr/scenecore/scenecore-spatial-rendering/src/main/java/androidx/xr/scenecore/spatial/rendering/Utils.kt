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

package androidx.xr.scenecore.spatial.rendering

import androidx.xr.runtime.Log
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

/* Awaits the result of a ListenableFuture<T>. Returns the underlying <T>. */
internal suspend fun <T : Any> ListenableFuture<T>.awaitSuspending(): T {
    val deferred = CompletableDeferred<T>(coroutineContext[Job])
    val futureBeingAwaited = this

    this.addListener(
        Runnable {
            try {
                deferred.complete(this.get())
            } catch (e: Throwable) {
                Log.error(message = { "ListenableFuture failed: $futureBeingAwaited" })
                deferred.completeExceptionally(e)
            }
        },
        DirectExecutor,
    )

    return deferred.await()
}

internal object DirectExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}
