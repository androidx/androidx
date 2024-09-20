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
import androidx.annotation.RestrictTo
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.RoomRawQuery
import androidx.room.RoomSQLiteQuery
import androidx.room.paging.CommonLimitOffsetImpl.Companion.BUG_LINK
import androidx.room.paging.util.getClippedRefreshKey
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteQuery

/**
 * An implementation of [PagingSource] to perform a LIMIT OFFSET query
 *
 * This class is used for Paging3 to perform Query and RawQuery in Room to return a PagingSource for
 * Pager's consumption. Registers observers on tables lazily and automatically invalidates itself
 * when data changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public actual abstract class LimitOffsetPagingSource<Value : Any>
actual constructor(
    public actual val sourceQuery: RoomRawQuery,
    public actual val db: RoomDatabase,
    vararg tables: String,
) : PagingSource<Int, Value>() {
    public constructor(
        sourceQuery: RoomSQLiteQuery,
        db: RoomDatabase,
        vararg tables: String,
    ) : this(sourceQuery = sourceQuery.toRoomRawQuery(), db = db, tables = tables)

    public constructor(
        supportSQLiteQuery: SupportSQLiteQuery,
        db: RoomDatabase,
        vararg tables: String,
    ) : this(
        sourceQuery = RoomSQLiteQuery.copyFrom(supportSQLiteQuery).toRoomRawQuery(),
        db = db,
        tables = tables,
    )

    private val implementation = CommonLimitOffsetImpl(tables, this, ::convertRows)

    public actual val itemCount: Int
        get() = implementation.itemCount.value

    override val jumpingSupported: Boolean
        get() = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> =
        implementation.load(params)

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? = state.getClippedRefreshKey()

    protected open fun convertRows(cursor: Cursor): List<Value> {
        throw NotImplementedError(
            "Unexpected call to a function with no implementation that Room is suppose to " +
                "generate. Please file a bug at: $BUG_LINK."
        )
    }

    protected actual open fun convertRows(statement: SQLiteStatement, itemCount: Int): List<Value> {
        return convertRows(SQLiteStatementCursor(statement, itemCount))
    }
}
