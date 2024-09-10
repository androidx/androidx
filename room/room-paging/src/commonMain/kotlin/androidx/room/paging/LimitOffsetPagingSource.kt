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

package androidx.room.paging

import androidx.annotation.RestrictTo
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingSource.LoadResult.Invalid
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomRawQuery
import androidx.room.immediateTransaction
import androidx.room.paging.util.INITIAL_ITEM_COUNT
import androidx.room.paging.util.queryDatabase
import androidx.room.paging.util.queryItemCount
import androidx.room.useReaderConnection
import androidx.sqlite.SQLiteStatement
import kotlinx.atomicfu.atomic
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
expect abstract class LimitOffsetPagingSource<Value : Any>(
    sourceQuery: RoomRawQuery,
    db: RoomDatabase,
    vararg tables: String
) : PagingSource<Int, Value> {
    val sourceQuery: RoomRawQuery
    val db: RoomDatabase

    val itemCount: Int

    protected open fun convertRows(statement: SQLiteStatement, itemCount: Int): List<Value>
}

internal class CommonLimitOffsetImpl<Value : Any>(
    tables: Array<out String>,
    val pagingSource: LimitOffsetPagingSource<Value>,
    private val convertRows: (SQLiteStatement, Int) -> List<Value>
) {
    private val db = pagingSource.db
    private val sourceQuery = pagingSource.sourceQuery
    internal val itemCount = atomic(INITIAL_ITEM_COUNT)
    private val registered = atomic(false)
    private val observer =
        object : InvalidationTracker.Observer(tables) {
            override fun onInvalidated(tables: Set<String>) {
                pagingSource.invalidate()
            }
        }

    init {
        pagingSource.registerInvalidatedCallback {
            db.getCoroutineScope().launch { db.invalidationTracker.unsubscribe(observer) }
        }
    }

    suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        return withContext(db.getCoroutineScope().coroutineContext) {
            if (!pagingSource.invalid && registered.compareAndSet(expect = false, update = true)) {
                db.invalidationTracker.subscribe(observer)
            }
            val tempCount = itemCount.value
            // if itemCount is < 0, then it is initial load
            try {
                if (tempCount == INITIAL_ITEM_COUNT) {
                    initialLoad(params)
                } else {
                    nonInitialLoad(params, tempCount)
                }
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
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
        return db.useReaderConnection { connection ->
            connection.immediateTransaction {
                val tempCount = queryItemCount(sourceQuery, db)
                itemCount.value = tempCount
                queryDatabase(
                    params = params,
                    sourceQuery = sourceQuery,
                    db = db,
                    itemCount = tempCount,
                    convertRows = convertRows,
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
                db = db,
                itemCount = tempCount,
                convertRows = convertRows
            )
        // manually check if database has been updated. If so, the observer's
        // invalidation callback will invalidate this paging source
        db.invalidationTracker.refreshInvalidation()

        @Suppress("UNCHECKED_CAST")
        return if (pagingSource.invalid) INVALID as Invalid<Int, Value> else loadResult
    }

    companion object {
        /**
         * A [LoadResult] that can be returned to trigger a new generation of PagingSource
         *
         * Any loaded data or queued loads prior to returning INVALID will be discarded
         */
        val INVALID = Invalid<Any, Any>()

        const val BUG_LINK =
            "https://issuetracker.google.com/issues/new?component=413107&template=1096568"
    }
}
