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

@file:JvmName("FlowUtil")

package androidx.room.coroutines

import androidx.annotation.RestrictTo
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.util.getCoroutineContext
import androidx.room.util.internalPerform
import androidx.sqlite.SQLiteConnection
import kotlin.jvm.JvmName
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

// TODO(b/329315924): Migrate to Flow based machinery.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <R> createFlow(
    db: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    block: (SQLiteConnection) -> R
): Flow<R> = flow {
    coroutineScope {
        // Observer channel receives signals from the invalidation tracker to emit queries.
        val observerChannel = Channel<Unit>(Channel.CONFLATED)
        val observer =
            object : InvalidationTracker.Observer(tableNames) {
                override fun onInvalidated(tables: Set<String>) {
                    observerChannel.trySend(Unit)
                }
            }
        observerChannel.trySend(Unit) // Initial signal to perform first query.
        val resultChannel = Channel<R>()
        launch(db.getCoroutineContext(inTransaction).minusKey(Job)) {
            db.invalidationTracker.subscribe(observer)
            try {
                // Iterate until cancelled, transforming observer signals to query results
                // to be emitted to the flow.
                for (signal in observerChannel) {
                    val result =
                        db.internalPerform(true, inTransaction) { connection ->
                            val rawConnection = (connection as RawConnectionAccessor).rawConnection
                            block.invoke(rawConnection)
                        }
                    resultChannel.send(result)
                }
            } finally {
                db.invalidationTracker.unsubscribe(observer)
            }
        }

        emitAll(resultChannel)
    }
}
