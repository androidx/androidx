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

package androidx.room.integration.kotlintestapp.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification
import androidx.room.Upsert
import androidx.room.integration.kotlintestapp.vo.Counter

@Dao
interface CounterDao {
    @Upsert
    suspend fun upsert(c: Counter)

    @Query("SELECT * FROM Counter WHERE id = :id")
    suspend fun getCounter(id: Long): Counter

    @SkipQueryVerification // ModifiedCounter is not an entity
    @Query("INSERT OR REPLACE INTO Counter SELECT * FROM ModifiedCounter WHERE id = :id")
    fun conditionalInsert(id: Long): Long
}
