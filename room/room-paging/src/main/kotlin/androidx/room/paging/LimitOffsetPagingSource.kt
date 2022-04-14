/*
 * Copyright 2021 The Android Open Source Project
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

import android.database.Cursor
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.getQueryDispatcher
import androidx.room.paging.util.ThreadSafeInvalidationObserver
import androidx.room.paging.util.getClippedRefreshKey
import androidx.room.paging.util.queryDatabase
import androidx.room.paging.util.queryItemCount
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * An implementation of [PagingSource] to perform a LIMIT OFFSET query
 *
 * This class is used for Paging3 to perform Query and RawQuery in Room to return a PagingSource
 * for Pager's consumption. Registers observers on tables lazily and automatically invalidates
 * itself when data changes.
 */

private val INVALID = PagingSource.LoadResult.Invalid<Any, Any>()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class LimitOffsetPagingSource<Value : Any>(
    private val sourceQuery: RoomSQLiteQuery,
    private val db: RoomDatabase,
    vararg tables: String,
) : PagingSource<Int, Value>() {

    constructor(
        supportSQLiteQuery: SupportSQLiteQuery,
        db: RoomDatabase,
        vararg tables: String,
    ) : this(
        sourceQuery = RoomSQLiteQuery.copyFrom(supportSQLiteQuery),
        db = db,
        tables = tables,
    )

    internal val itemCount: AtomicInteger = AtomicInteger(-1)

    private val observer = ThreadSafeInvalidationObserver(
        tables = tables,
        onInvalidated = ::invalidate
    )

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        return withContext(db.getQueryDispatcher()) {
            observer.registerIfNecessary(db)
            val tempCount = itemCount.get()
            // if itemCount is < 0, then it is initial load
            if (tempCount < 0) {
                initialLoad(params)
            } else {
                nonInitialLoad(params, tempCount)
            }
        }
    }

    /**
     *  For the very first time that this PagingSource's [load] is called. Executes the count
     *  query (initializes [itemCount]) and db query within a transaction to ensure initial load's
     *  data integrity.
     *
     *  For example, if the database gets updated after the count query but before the db query
     *  completes, the paging source may not invalidate in time, but this method will return
     *  data based on the original database that the count was performed on to ensure a valid
     *  initial load.
     */
    private suspend fun initialLoad(params: LoadParams<Int>): LoadResult<Int, Value> {
        return db.withTransaction {
            val tempCount = queryItemCount(sourceQuery, db)
            itemCount.set(tempCount)
            queryDatabase(params, sourceQuery, db, tempCount, ::convertRows)
        }
    }

    private suspend fun nonInitialLoad(
        params: LoadParams<Int>,
        tempCount: Int,
    ): LoadResult<Int, Value> {
        val loadResult = queryDatabase(params, sourceQuery, db, tempCount, ::convertRows)
        // manually check if database has been updated. If so, the observers's
        // invalidation callback will invalidate this paging source
        db.invalidationTracker.refreshVersionsSync()
        @Suppress("UNCHECKED_CAST")
        return if (invalid) INVALID as LoadResult.Invalid<Int, Value> else loadResult
    }

    @NonNull
    protected abstract fun convertRows(cursor: Cursor): List<Value>

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return state.getClippedRefreshKey()
    }

    override val jumpingSupported: Boolean
        get() = true
}
