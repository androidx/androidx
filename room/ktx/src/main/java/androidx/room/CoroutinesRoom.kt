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

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import kotlin.coroutines.coroutineContext

/**
 * A helper class for supporting Kotlin Coroutines in Room.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class CoroutinesRoom private constructor() {

    companion object {

        @JvmStatic
        suspend fun <R> execute(
            db: RoomDatabase,
            inTransaction: Boolean,
            callable: Callable<R>
        ): R {
            if (db.isOpen && db.inTransaction()) {
                return callable.call()
            }

            // Use the transaction dispatcher if we are on a transaction coroutine, otherwise
            // use the database dispatchers.
            val context = coroutineContext[TransactionElement]?.transactionDispatcher
                ?: if (inTransaction) db.transactionDispatcher else db.queryDispatcher
            return withContext(context) {
                callable.call()
            }
        }
    }
}

/**
 * Gets the query coroutine dispatcher.
 *
 * @hide
 */
internal val RoomDatabase.queryDispatcher: CoroutineDispatcher
    get() = backingFieldMap.getOrPut("QueryDispatcher") {
        queryExecutor.asCoroutineDispatcher()
    } as CoroutineDispatcher

/**
 * Gets the transaction coroutine dispatcher.
 *
 * @hide
 */
internal val RoomDatabase.transactionDispatcher: CoroutineDispatcher
    get() = backingFieldMap.getOrPut("TransactionDispatcher") {
        queryExecutor.asCoroutineDispatcher()
    } as CoroutineDispatcher
