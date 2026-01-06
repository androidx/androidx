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

package androidx.room3.paging

import androidx.annotation.RestrictTo
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.Transactor.SQLiteTransactionType
import androidx.room3.concurrent.AtomicBoolean
import androidx.room3.concurrent.AtomicInt
import androidx.room3.paging.util.INITIAL_ITEM_COUNT
import androidx.room3.paging.util.queryContext
import androidx.room3.paging.util.queryDatabase
import androidx.room3.paging.util.queryItemCount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An implementation of [PagingSource] to perform a LIMIT OFFSET query
 *
 * This class is used for Paging3 to perform Query and RawQuery in Room to return a PagingSource for
 * Pager's consumption. Registers observers on tables lazily and automatically invalidates itself
 * when data changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public expect abstract class LimitOffsetPagingSource<Value : Any>(
    sourceQuery: RoomRawQuery,
    db: RoomDatabase,
    vararg tables: String,
) : PagingSource<Int, Value> {
    public val sourceQuery: RoomRawQuery
    public val db: RoomDatabase

    public val itemCount: Int

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value>

    override fun getRefreshKey(state: PagingState<Int, Value>): Int?

    protected abstract suspend fun convertRows(
        limitOffsetQuery: RoomRawQuery,
        itemCount: Int,
    ): List<Value>
}

internal class CommonLimitOffsetImpl<Value : Any>(
    private val tables: Array<out String>,
    private val pagingSource: LimitOffsetPagingSource<Value>,
    private val convertRows: suspend (RoomRawQuery, Int) -> List<Value>,
) {
    private val db = pagingSource.db
    private val sourceQuery = pagingSource.sourceQuery

    internal val itemCount = AtomicInt(INITIAL_ITEM_COUNT)

    private val refreshComplete = AtomicBoolean(false)
    private val collectorLatch = CompletableDeferred<Unit>()

    private var invalidationFlowJob: Job? = null

    init {
        // register db listeners right away
        invalidationFlowJob =
            db.getCoroutineScope().launch {
                db.invalidationTracker.createFlow(*tables, emitInitialState = true).collect {
                    // Signal even before checking if we're still valid, to ensure the load function
                    // doesn't block.
                    val initialState = collectorLatch.complete(Unit)
                    if (pagingSource.invalid) {
                        throw CancellationException("PagingSource is invalid")
                    }
                    if (initialState) {
                        // The first emit essentially just lets us know the flow has started.
                        return@collect
                    }
                    if (refreshComplete.get()) {
                        pagingSource.invalidate()
                    }
                }
            }

        pagingSource.registerInvalidatedCallback { invalidationFlowJob?.cancel() }
    }

    suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        val tempCount = itemCount.get()
        // if itemCount is == INITIAL_ITEM_COUNT, then it is initial load
        return try {
            // Wait until we know the collector has started, which means we're guaranteed to get an
            // invalidation for any updates that happen after this point.
            collectorLatch.await()
            if (tempCount == INITIAL_ITEM_COUNT) {
                // Mark the load started for invalidation purposes; once we pass this point, we
                // want to ensure we process any invalidations by the flow collector.
                refreshComplete.compareAndSet(false, true)
                initialLoad(params)
            } else {
                nonInitialLoad(params, tempCount)
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /**
     * For the very first time that this PagingSource's [load] is called. Executes the count query
     * (initializes [itemCount]) and db query within a transaction to ensure initial load's data
     * integrity.
     *
     * For example, if the database gets updated after the count query but before the db query
     * completes, the paging source may not invalidate in time, but this method will return data
     * based on the original database that the count was performed on to ensure a valid initial
     * load.
     */
    private suspend fun initialLoad(params: LoadParams<Int>): LoadResult<Int, Value> {
        // Load in the database's coroutine context since useConnection is unconfined.
        return withContext(db.queryContext) {
            db.useConnection(isReadOnly = true) { connection ->
                // Using a transaction to ensure initial load's data integrity.
                connection.withTransaction(SQLiteTransactionType.DEFERRED) {
                    val tempCount = queryItemCount(sourceQuery, db)
                    itemCount.set(tempCount)
                    queryDatabase(
                        params = params,
                        sourceQuery = sourceQuery,
                        itemCount = tempCount,
                        convertRows = convertRows,
                    )
                }
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
                convertRows = convertRows,
            )
        // TODO(b/192269858): Create a better API to facilitate source invalidation.
        // Manually check if database has been updated. If so, invalidate the source and the result.
        withContext(db.queryContext) {
            if (db.invalidationTracker.refresh(*tables)) {
                pagingSource.invalidate()
            }
        }

        @Suppress("UNCHECKED_CAST")
        return if (pagingSource.invalid) INVALID as LoadResult.Invalid<Int, Value> else loadResult
    }

    companion object {
        /**
         * A [LoadResult] that can be returned to trigger a new generation of PagingSource
         *
         * Any loaded data or queued loads prior to returning INVALID will be discarded
         */
        val INVALID = LoadResult.Invalid<Any, Any>()

        const val BUG_LINK =
            "https://issuetracker.google.com/issues/new?component=413107&template=1096568"
    }
}
