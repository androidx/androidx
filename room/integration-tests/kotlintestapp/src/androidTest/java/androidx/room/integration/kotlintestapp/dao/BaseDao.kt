/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery

interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(t: T)

    @Insert
    fun insertAll(t: List<T>)

    @Insert
    fun insertAllSet(t: Set<T>): List<Long>

    @Insert
    fun insertAllCollection(t: Collection<T>): Array<Long>

    @Insert
    fun insertAllArg(vararg t: T)

    @Update
    fun update(t: T)

    @Delete
    fun delete(t: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun suspendInsert(t: T)

    @Update
    suspend fun suspendUpdate(t: T)

    @Delete
    suspend fun suspendDelete(t: T)

    @RawQuery
    suspend fun rawQuery(query: SupportSQLiteQuery): List<T>
}
