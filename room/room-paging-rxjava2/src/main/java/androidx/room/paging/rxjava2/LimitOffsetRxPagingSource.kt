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

package androidx.room.paging.rxjava2

import android.database.Cursor
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.paging.PagingState
import androidx.paging.rxjava2.RxPagingSource
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RxRoom
import androidx.room.paging.util.INITIAL_ITEM_COUNT
import androidx.room.paging.util.INVALID
import androidx.room.paging.util.ThreadSafeInvalidationObserver
import androidx.room.paging.util.getClippedRefreshKey
import androidx.room.paging.util.queryDatabase
import androidx.room.paging.util.queryItemCount
import androidx.sqlite.db.SupportSQLiteQuery
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class LimitOffsetRxPagingSource<Value : Any>(
    private val sourceQuery: RoomSQLiteQuery,
    private val db: RoomDatabase,
    vararg tables: String,
) : RxPagingSource<Int, Value>() {

    constructor(
        supportSQLiteQuery: SupportSQLiteQuery,
        db: RoomDatabase,
        vararg tables: String
    ) : this(sourceQuery = RoomSQLiteQuery.copyFrom(supportSQLiteQuery), db = db, tables = tables)

    @VisibleForTesting internal val itemCount: AtomicInteger = AtomicInteger(INITIAL_ITEM_COUNT)
    @VisibleForTesting
    internal val observer = ThreadSafeInvalidationObserver(tables = tables) { invalidate() }

    override fun loadSingle(params: LoadParams<Int>): Single<LoadResult<Int, Value>> {
        val scheduler = Schedulers.from(db.queryExecutor)
        return RxRoom.createSingle {
                observer.registerIfNecessary(db)
                val tempCount = itemCount.get()
                if (tempCount == INITIAL_ITEM_COUNT) {
                    initialLoad(params)
                } else {
                    nonInitialLoad(tempCount, params)
                }
            }
            .subscribeOn(scheduler)
    }

    private fun initialLoad(params: LoadParams<Int>): LoadResult<Int, Value> {
        return db.runInTransaction(
            Callable {
                val tempCount = queryItemCount(sourceQuery, db)
                itemCount.set(tempCount)
                queryDatabase(
                    params = params,
                    sourceQuery = sourceQuery,
                    db = db,
                    itemCount = tempCount,
                    convertRows = ::convertRows
                )
            }
        )
    }

    private fun nonInitialLoad(tempCount: Int, params: LoadParams<Int>): LoadResult<Int, Value> {
        val result =
            queryDatabase(
                params = params,
                sourceQuery = sourceQuery,
                db = db,
                itemCount = tempCount,
                convertRows = ::convertRows
            )
        // manually check if database has been updated. If so, the observer's
        // invalidation callback will invalidate this paging source
        db.invalidationTracker.refreshVersionsSync()
        @Suppress("UNCHECKED_CAST")
        return if (invalid) INVALID as LoadResult.Invalid<Int, Value> else result
    }

    @NonNull protected abstract fun convertRows(cursor: Cursor): List<Value>

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return state.getClippedRefreshKey()
    }

    override val jumpingSupported: Boolean
        get() = true
}
