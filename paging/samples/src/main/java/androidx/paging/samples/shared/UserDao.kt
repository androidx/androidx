/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging.samples.shared

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Single

@Dao
interface UserDao {
    // Normally suspend when using Kotlin Coroutines, but sync version allows this Dao to be used
    // in both Java and Kotlin samples.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<User>)

    fun pagingSource(): PagingSource<Int, User>

    // Normally suspend when using Kotlin Coroutines, but sync version allows this Dao to be used
    // in both Java and Kotlin samples.
    @Query("DELETE FROM users WHERE label = :query")
    fun deleteByQuery(query: String)

    suspend fun lastUpdated(): Long

    fun lastUpdatedFuture(): ListenableFuture<Long>

    fun lastUpdatedSingle(): Single<Long>
}