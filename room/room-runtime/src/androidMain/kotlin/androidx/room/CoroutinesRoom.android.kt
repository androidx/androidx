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

package androidx.room

import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.room.coroutines.createFlow as createFlowCommon
import androidx.room.util.getCoroutineContext
import java.util.concurrent.Callable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** A helper class for supporting Kotlin Coroutines in Room. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CoroutinesRoom private constructor() {

    public companion object {

        @JvmStatic
        @Deprecated("No longer called by generated implementation")
        public suspend fun <R> execute(
            db: RoomDatabase,
            inTransaction: Boolean,
            callable: Callable<R>
        ): R {
            if (db.isOpenInternal && db.inTransaction()) {
                return callable.call()
            }

            val context = db.getCoroutineContext(inTransaction)
            return withContext(context) { callable.call() }
        }

        @JvmStatic
        @Deprecated("No longer called by generated implementation")
        public suspend fun <R> execute(
            db: RoomDatabase,
            inTransaction: Boolean,
            cancellationSignal: CancellationSignal?,
            callable: Callable<R>
        ): R {
            if (db.isOpenInternal && db.inTransaction()) {
                return callable.call()
            }

            val context = db.getCoroutineContext(inTransaction)
            return suspendCancellableCoroutine<R> { continuation ->
                val job =
                    db.getCoroutineScope().launch(context) {
                        try {
                            val result = callable.call()
                            continuation.resume(result)
                        } catch (exception: Throwable) {
                            continuation.resumeWithException(exception)
                        }
                    }
                continuation.invokeOnCancellation {
                    cancellationSignal?.cancel()
                    job.cancel()
                }
            }
        }

        @JvmStatic
        @Deprecated("No longer called by generated implementation")
        public fun <R> createFlow(
            db: RoomDatabase,
            inTransaction: Boolean,
            tableNames: Array<String>,
            callable: Callable<R>
        ): Flow<@JvmSuppressWildcards R> =
            createFlowCommon(db, inTransaction, tableNames) { callable.call() }
    }
}
