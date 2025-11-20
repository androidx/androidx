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
package androidx.room3.paging.rxjava3

import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.sqlite.SQLiteStatement
import io.reactivex.rxjava3.core.Single

abstract class LimitOffsetRxPagingSource<T : Any>(
    protected val sourceQuery: RoomRawQuery,
    protected val db: RoomDatabase,
    protected vararg val tables: String
) : androidx.paging.rxjava3.RxPagingSource<Int, T>() {

    override fun loadSingle(params: LoadParams<Int>): Single<LoadResult<Int, T>> {
        TODO()
    }
    protected abstract suspend fun convertRows(
        limitOffsetQuery: RoomRawQuery,
        itemCount: Int
    ): List<T>

    override fun getRefreshKey(state: androidx.paging.PagingState<Int, T>): Int? {
        TODO()
    }

    override val jumpingSupported: Boolean
        get() = true
}