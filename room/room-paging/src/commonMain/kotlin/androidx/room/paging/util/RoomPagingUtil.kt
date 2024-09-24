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
@file:JvmName("RoomPagingUtil")
@file:JvmMultifileClass

package androidx.room.paging.util

import androidx.annotation.RestrictTo
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadParams.Append
import androidx.paging.PagingSource.LoadParams.Prepend
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.RoomRawQuery
import androidx.room.useReaderConnection
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/** The default itemCount value */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val INITIAL_ITEM_COUNT: Int = -1

/**
 * Calculates query limit based on LoadType.
 *
 * Prepend: If requested loadSize is larger than available number of items to prepend, it will query
 * with OFFSET = 0, LIMIT = prevKey.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun getLimit(params: LoadParams<Int>, key: Int): Int {
    return when (params) {
        is Prepend ->
            if (key < params.loadSize) {
                key
            } else {
                params.loadSize
            }
        else -> params.loadSize
    }
}

/**
 * Calculates query offset amount based on load type.
 *
 * Prepend: OFFSET is calculated by counting backwards the number of items that needs to be loaded
 * before [key]. For example, if key = 30 and loadSize = 5, then offset = 25 and items in db
 * position 26-30 are loaded. If requested loadSize is larger than the number of available items to
 * prepend, OFFSET clips to 0 to prevent negative OFFSET.
 *
 * Refresh: If initialKey is supplied through Pager, Paging 3 will now start loading from initialKey
 * with initialKey being the first item. If key is supplied by [getClippedRefreshKey], the key has
 * already been adjusted to load half of the requested items before anchorPosition and the other
 * half after anchorPosition. See comments on [getClippedRefreshKey] for more details. If key
 * (regardless if from initialKey or [getClippedRefreshKey]) is larger than available items, the
 * last page will be loaded by counting backwards the loadSize before last item in database. For
 * example, this can happen if invalidation came from a large number of items dropped. i.e. in items
 * 0 - 100, items 41-80 are dropped. Depending on last viewed item, hypothetically
 * [getClippedRefreshKey] may return key = 60. If loadSize = 10, then items 31-40 will be loaded.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun getOffset(params: LoadParams<Int>, key: Int, itemCount: Int): Int {
    return when (params) {
        is Prepend ->
            if (key < params.loadSize) {
                0
            } else {
                key - params.loadSize
            }
        is Append -> key
        is Refresh ->
            if (key >= itemCount) {
                maxOf(0, itemCount - params.loadSize)
            } else {
                key
            }
    }
}

/**
 * Calls RoomDatabase.query() to return a cursor and then calls convertRows() to extract and return
 * list of data.
 *
 * Throws [IllegalArgumentException] from CursorUtil if column does not exist.
 *
 * @param params load params to calculate query limit and offset
 * @param sourceQuery user provided database query
 * @param db the [RoomDatabase] to query from
 * @param itemCount the db row count, triggers a new PagingSource generation if itemCount changes,
 *   i.e. items are added / removed
 * @param convertRows the function to iterate data with provided [androidx.sqlite.SQLiteStatement]
 *   to return List<Value>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun <Value : Any> queryDatabase(
    params: LoadParams<Int>,
    sourceQuery: RoomRawQuery,
    itemCount: Int,
    convertRows: suspend (RoomRawQuery, Int) -> List<Value>
): LoadResult<Int, Value> {
    val key = params.key ?: 0
    val limit = getLimit(params, key)
    val offset = getOffset(params, key, itemCount)
    val rowsCount =
        if (limit + offset > itemCount) {
            itemCount - offset
        } else {
            limit
        }
    val limitOffsetQuery =
        RoomRawQuery(
            sql = "SELECT * FROM ( ${sourceQuery.sql} ) LIMIT $limit OFFSET $offset",
            onBindStatement = sourceQuery.getBindingFunction()
        )

    val data: List<Value> = convertRows(limitOffsetQuery, rowsCount)
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
 * Returns count of requested items to calculate itemsAfter and itemsBefore for use in creating
 * LoadResult.Page<>.
 *
 * Throws error when the column value is null, the column type is not an integral type, or the
 * integer value is outside the range [Integer.MIN_VALUE, Integer.MAX_VALUE].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun queryItemCount(sourceQuery: RoomRawQuery, db: RoomDatabase): Int {
    val countQuery = "SELECT COUNT(*) FROM ( ${sourceQuery.sql} )"
    return db.useReaderConnection { connection ->
        connection.usePrepared(countQuery) { stmt ->
            sourceQuery.getBindingFunction().invoke(stmt)
            if (stmt.step()) {
                stmt.getInt(0)
            } else {
                0
            }
        }
    }
}

/**
 * Returns the key for [PagingSource] for a non-initial REFRESH load.
 *
 * To prevent a negative key, key is clipped to 0 when the number of items available before
 * anchorPosition is less than the requested amount of initialLoadSize / 2.
 */
@Suppress("AutoBoxing")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <Value : Any> PagingState<Int, Value>.getClippedRefreshKey(): Int? {
    return when (val anchorPosition = anchorPosition) {
        null -> null
        /**
         * It is unknown whether anchorPosition represents the item at the top of the screen or item
         * at the bottom of the screen. To ensure the number of items loaded is enough to fill up
         * the screen, half of loadSize is loaded before the anchorPosition and the other half is
         * loaded after the anchorPosition -- anchorPosition becomes the middle item.
         */
        else -> maxOf(0, anchorPosition - (config.initialLoadSize / 2))
    }
}
