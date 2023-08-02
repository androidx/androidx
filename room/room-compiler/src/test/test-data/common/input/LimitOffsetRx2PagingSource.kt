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
package androidx.room.paging.rxjava2

import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery

abstract class LimitOffsetRxPagingSource<T : Any>(
    private val sourceQuery: RoomSQLiteQuery,
    private val db: RoomDatabase,
    vararg tables: String
) : androidx.paging.rxjava2.RxPagingSource<Int, T>(
    sourceQuery,
    db,
    *tables
) {
    protected abstract override fun convertRows(cursor: Cursor): List<T>
}