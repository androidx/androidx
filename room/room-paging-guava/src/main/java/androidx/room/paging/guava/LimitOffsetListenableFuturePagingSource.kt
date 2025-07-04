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

package androidx.room.paging.guava

import android.database.Cursor
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.paging.ListenableFuturePagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.guava.createListenableFuture
import androidx.room.paging.CursorSQLiteStatement
import androidx.room.paging.util.INITIAL_ITEM_COUNT
import androidx.room.paging.util.INVALID
import androidx.room.paging.util.ThreadSafeInvalidationObserver
import androidx.room.paging.util.getClippedRefreshKey
import androidx.room.paging.util.queryDatabase
import androidx.room.paging.util.queryItemCount
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteQuery
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LimitOffsetListenableFuturePagingSource<Value : Any>(
    private val sourceQuery: RoomSQLiteQuery,
    private val db: RoomDatabase,
    vararg tables: String,
) : ListenableFuturePagingSource<Int, Value>() {

    public constructor(
        supportSQLiteQuery: SupportSQLiteQuery,
        db: RoomDatabase,
        vararg tables: String,
    ) : this(sourceQuery = RoomSQLiteQuery.copyFrom(supportSQLiteQuery), db = db, tables = tables)

    @VisibleForTesting internal val itemCount: AtomicInteger = AtomicInteger(INITIAL_ITEM_COUNT)
    private val observer = ThreadSafeInvalidationObserver(tables = tables, ::invalidate)

    /**
     * Returns a [ListenableFuture] immediately before loading from the database completes
     *
     * If PagingSource is invalidated while the [ListenableFuture] is still pending, the
     * invalidation will cancel the load() coroutine that calls await() on this future. The
     * cancellation of await() will transitively cancel this future as well.
     */
    override fun loadFuture(params: LoadParams<Int>): ListenableFuture<LoadResult<Int, Value>> {
        return Futures.transformAsync(
            @Suppress("DEPRECATION") // Due to createListenableFuture() with Callable
            createListenableFuture(db, false) { observer.registerIfNecessary(db) },
            {
                val tempCount = itemCount.get()
                if (tempCount == INITIAL_ITEM_COUNT) {
                    initialLoad(params)
                } else {
                    nonInitialLoad(params, tempCount)
                }
            },
            db.queryExecutor,
        )
    }

    /**
     * For refresh loads
     *
     * To guarantee a valid initial load, it is run in transaction so that db writes cannot happen
     * in between [queryItemCount] and [queryDatabase] to ensure a valid [itemCount]. [itemCount]
     * must be correct in order to calculate correct LIMIT and OFFSET for the query.
     *
     * However, the database load will be canceled via the cancellation signal if the future it
     * returned has been canceled before it has completed.
     */
    private fun initialLoad(params: LoadParams<Int>): ListenableFuture<LoadResult<Int, Value>> {
        val cancellationSignal = CancellationSignal()
        val loadCallable =
            Callable<LoadResult<Int, Value>> {
                db.runInTransaction(
                    Callable {
                        val tempCount = queryItemCount(sourceQuery, db)
                        itemCount.set(tempCount)
                        queryDatabase(
                            params,
                            sourceQuery,
                            db,
                            tempCount,
                            cancellationSignal,
                            ::convertRows,
                        )
                    }
                )
            }

        @Suppress("DEPRECATION") // Due to createListenableFuture() with Callable
        return createListenableFuture(
            db,
            true,
            loadCallable,
            sourceQuery,
            false,
            cancellationSignal,
        )
    }

    /**
     * For append and prepend loads
     *
     * The cancellation signal cancels room database operation if its running, or cancels it the
     * moment it starts. The signal is triggered when the future is canceled.
     */
    private fun nonInitialLoad(
        params: LoadParams<Int>,
        tempCount: Int,
    ): ListenableFuture<LoadResult<Int, Value>> {
        val cancellationSignal = CancellationSignal()
        val loadCallable =
            Callable<LoadResult<Int, Value>> {
                val result =
                    queryDatabase(
                        params,
                        sourceQuery,
                        db,
                        tempCount,
                        cancellationSignal,
                        ::convertRows,
                    )
                db.invalidationTracker.refreshVersionsSync()
                @Suppress("UNCHECKED_CAST")
                if (invalid) INVALID as LoadResult.Invalid<Int, Value> else result
            }

        @Suppress("DEPRECATION") // Due to createListenableFuture() with Callable
        return createListenableFuture(
            db,
            false,
            loadCallable,
            sourceQuery,
            false,
            cancellationSignal,
        )
    }

    protected open fun convertRows(cursor: Cursor): List<Value> {
        return convertRows(CursorSQLiteStatement(cursor))
    }

    protected open fun convertRows(statement: SQLiteStatement): List<Value> {
        throw NotImplementedError(
            "Unexpected call to a function with no implementation that Room is suppose to " +
                "generate. Please file a bug at: $BUG_LINK."
        )
    }

    override val jumpingSupported: Boolean
        get() = true

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return state.getClippedRefreshKey()
    }

    public companion object {
        public const val BUG_LINK: String =
            "https://issuetracker.google.com/issues/new?component=413107&template=1096568"
    }
}
