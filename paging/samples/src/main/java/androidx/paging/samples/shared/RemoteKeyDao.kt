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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Single

@Dao
interface RemoteKeyDao {
    // Normally suspend when using Kotlin Coroutines, but sync version allows this Dao to be used
    // in both Java and Kotlin samples.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(remoteKey: RemoteKey)

    @Query("SELECT * FROM remote_keys WHERE label = :query")
    fun remoteKeyByQuery(query: String): RemoteKey

    @Query("SELECT * FROM remote_keys WHERE label = :query")
    fun remoteKeyByQuerySingle(query: String): Single<RemoteKey>

    @Query("SELECT * FROM remote_keys WHERE label = :query")
    fun remoteKeyByQueryFuture(query: String): ListenableFuture<RemoteKey>

    // Normally suspend when using Kotlin Coroutines, but sync version allows this Dao to be used
    // in both Java and Kotlin samples.
    @Query("DELETE FROM remote_keys WHERE label = :query")
    fun deleteByQuery(query: String)
}
