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
@file:JvmName("RoomPagingUtil")
@file:JvmMultifileClass

package androidx.room.paging.util

import android.database.Cursor
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery

/**
 * A [LoadResult] that can be returned to trigger a new generation of PagingSource
 *
 * Any loaded data or queued loads prior to returning INVALID will be discarded
 */
@get:Suppress("AcronymName")
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val INVALID: LoadResult<Any, Any> = LoadResult.Invalid<Any, Any>()

/**
 * calls RoomDatabase.query() to return a cursor and then calls convertRows() to extract and return
 * list of data
 *
 * throws [IllegalArgumentException] from CursorUtil if column does not exist
 *
 * @param params load params to calculate query limit and offset
 * @param sourceQuery user provided [RoomSQLiteQuery] for database query
 * @param db the [RoomDatabase] to query from
 * @param itemCount the db row count, triggers a new PagingSource generation if itemCount changes,
 *   i.e. items are added / removed
 * @param cancellationSignal the signal to cancel the query if the query hasn't yet completed
 * @param convertRows the function to iterate data with provided [Cursor] to return List<Value>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <Value : Any> queryDatabase(
    params: LoadParams<Int>,
    sourceQuery: RoomSQLiteQuery,
    db: RoomDatabase,
    itemCount: Int,
    cancellationSignal: CancellationSignal? = null,
    convertRows: (Cursor) -> List<Value>,
): LoadResult<Int, Value> {
    val key = params.key ?: 0
    val limit: Int = getLimit(params, key)
    val offset: Int = getOffset(params, key, itemCount)
    val limitOffsetQuery = "SELECT * FROM ( ${sourceQuery.sql} ) LIMIT $limit OFFSET $offset"
    val sqLiteQuery: RoomSQLiteQuery =
        RoomSQLiteQuery.acquire(limitOffsetQuery, sourceQuery.argCount)
    sqLiteQuery.copyArgumentsFrom(sourceQuery)
    val cursor = db.query(sqLiteQuery, cancellationSignal)
    val data: List<Value>
    try {
        data = convertRows(cursor)
    } finally {
        cursor.close()
        sqLiteQuery.release()
    }
    val nextPosToLoad = offset + data.size
    val nextKey =
        if (data.isEmpty() || data.size < limit || nextPosToLoad >= itemCount) {
            null
        } else {
            nextPosToLoad
        }
    val prevKey = if (offset <= 0 || data.isEmpty()) null else offset
    return LoadResult.Page(
        data = data,
        prevKey = prevKey,
        nextKey = nextKey,
        itemsBefore = offset,
        itemsAfter = maxOf(0, itemCount - nextPosToLoad)
    )
}

/**
 * returns count of requested items to calculate itemsAfter and itemsBefore for use in creating
 * LoadResult.Page<>
 *
 * throws error when the column value is null, the column type is not an integral type, or the
 * integer value is outside the range [Integer.MIN_VALUE, Integer.MAX_VALUE]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun queryItemCount(sourceQuery: RoomSQLiteQuery, db: RoomDatabase): Int {
    val countQuery = "SELECT COUNT(*) FROM ( ${sourceQuery.sql} )"
    val sqLiteQuery: RoomSQLiteQuery = RoomSQLiteQuery.acquire(countQuery, sourceQuery.argCount)
    sqLiteQuery.copyArgumentsFrom(sourceQuery)
    val cursor: Cursor = db.query(sqLiteQuery)
    try {
        if (cursor.moveToFirst()) {
            return cursor.getInt(0)
        }
        return 0
    } finally {
        cursor.close()
        sqLiteQuery.release()
    }
}
