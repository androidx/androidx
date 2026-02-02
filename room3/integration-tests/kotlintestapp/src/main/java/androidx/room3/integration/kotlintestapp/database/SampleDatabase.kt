/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp.database

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(entities = [RemoteEntity::class], version = 1, exportSchema = false)
abstract class RemoteSampleDatabase : RoomDatabase() {
    abstract fun dao(): RemoteDao
}

@Dao
interface RemoteDao {
    @Insert suspend fun insert(entity: RemoteEntity)

    @Query("SELECT COUNT(*) FROM RemoteEntity") fun getEntityCount(): Int

    @Query("SELECT * FROM RemoteEntity WHERE pk = :id")
    fun getEntityFlow(id: Long): Flow<RemoteEntity?>
}

@Entity data class RemoteEntity(@PrimaryKey val pk: Long)
