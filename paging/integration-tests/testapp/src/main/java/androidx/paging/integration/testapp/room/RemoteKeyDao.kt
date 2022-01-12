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
package androidx.paging.integration.testapp.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query

/**
 * Simple Customer DAO for Room Customer list sample.
 */
@Dao
interface RemoteKeyDao {
    /**
     * Insert a RemoteKey
     *
     * @param remoteKey
     */
    @Insert(onConflict = REPLACE)
    suspend fun insert(remoteKey: RemoteKey)

    /**
     * Clears the RemoteKey
     */
    @Query("DELETE FROM remote_key")
    fun delete()

    /**
     * @return Latest persisted RemoteKey
     */
    @Query("SELECT * FROM remote_key LIMIT 1")
    suspend fun queryRemoteKey(): RemoteKey?
}
