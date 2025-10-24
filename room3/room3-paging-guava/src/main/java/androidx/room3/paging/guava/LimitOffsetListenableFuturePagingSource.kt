/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room3.paging.guava

import androidx.annotation.RestrictTo
import androidx.paging.ListenableFuturePagingSource
import androidx.paging.PagingState
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.Transactor.SQLiteTransactionType
import androidx.room3.concurrent.AtomicBoolean
import androidx.room3.concurrent.AtomicInt
import androidx.room3.paging.util.INITIAL_ITEM_COUNT
import androidx.room3.paging.util.getClippedRefreshKey
import androidx.room3.paging.util.queryDatabase
import androidx.room3.paging.util.queryItemCount
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LimitOffsetListenableFuturePagingSource<Value : Any>(
    protected val sourceQuery: RoomRawQuery,
    protected val db: RoomDatabase,
    protected vararg val tables: String,
) : ListenableFuturePagingSource<Int, Value>() {

    internal val itemCount = AtomicInt(INITIAL_ITEM_COUNT)
    internal val refreshComplete = AtomicBoolean(false)
    private var invalidationFlowJob: Job? = null

    init {
        invalidationFlowJob =
            db.getCoroutineScope().launch {
                db.invalidationTracker.createFlow(*tables, emitInitialState = false).collect {
                    if (invalid) {
                        throw CancellationException("PagingSource is invalid")
                    }
                    if (refreshComplete.get()) {
                        invalidate()
                    }
                }
            }
        registerInvalidatedCallback { invalidationFlowJob?.cancel() }
    }

    override fun loadFuture(params: LoadParams<Int>): ListenableFuture<LoadResult<Int, Value>> {
        return db.getCoroutineScope()
            .async(CoroutineName("LimitOffsetListenableFuturePagingSource.loadFuture")) {
                val tempCount = itemCount.get()
                // if itemCount is < 0, then it is initial load
                try {
                    if (tempCount == INITIAL_ITEM_COUNT) {
                        initialLoad(params).also { refreshComplete.compareAndSet(false, true) }
                    } else {
                        nonInitialLoad(params, tempCount)
                    }
                } catch (e: Exception) {
                    LoadResult.Error(e)
                }
            }
            .asListenableFuture()
    }

    private suspend fun initialLoad(params: LoadParams<Int>): LoadResult<Int, Value> {
        return db.useConnection(isReadOnly = true) { connection ->
            // Using a transaction to ensure initial load's data integrity.
            connection.withTransaction(SQLiteTransactionType.DEFERRED) {
                val tempCount = queryItemCount(sourceQuery, db)
                itemCount.set(tempCount)
                queryDatabase(
                    params = params,
                    sourceQuery = sourceQuery,
                    itemCount = tempCount,
                    convertRows = ::convertRows,
                )
            }
        }
    }

    private suspend fun nonInitialLoad(
        params: LoadParams<Int>,
        tempCount: Int,
    ): LoadResult<Int, Value> {
        val loadResult =
            queryDatabase(
                params = params,
                sourceQuery = sourceQuery,
                itemCount = tempCount,
                convertRows = ::convertRows,
            )
        // TODO(b/192269858): Create a better API to facilitate source invalidation.
        // Manually check if database has been updated. If so, invalidate the source and the result.
        if (db.invalidationTracker.refresh(*tables)) {
            invalidate()
        }

        @Suppress("UNCHECKED_CAST")
        return if (invalid) INVALID as LoadResult.Invalid<Int, Value> else loadResult
    }

    protected abstract suspend fun convertRows(
        limitOffsetQuery: RoomRawQuery,
        itemCount: Int,
    ): List<Value>

    override val jumpingSupported: Boolean
        get() = true

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return state.getClippedRefreshKey()
    }

    public companion object {
        private val INVALID = LoadResult.Invalid<Any, Any>()
    }
}
