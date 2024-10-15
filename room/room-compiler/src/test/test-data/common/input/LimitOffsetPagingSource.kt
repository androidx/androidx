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

import androidx.paging.PagingState
import androidx.room.RoomDatabase
import androidx.room.RoomRawQuery
import androidx.sqlite.SQLiteConnection

@Suppress("UNUSED_PARAMETER")
abstract class LimitOffsetPagingSource<T : Any>(
    private val sourceQuery: RoomRawQuery,
    private val db: RoomDatabase,
    vararg tables: String
) : androidx.paging.PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return 0
    }

    override public suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return LoadResult.Invalid()
    }
    protected abstract suspend fun convertRows(limitOffsetQuery: RoomRawQuery, itemCount: Int): List<T>
}