/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.room.integration.multiplatformtestapp.test

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity
data class SampleEntity(
    @PrimaryKey
    val pk: Long
)

@Dao
interface SampleDao {
    @Insert
    fun insert(item: SampleEntity)
}

@Database(
    entities = [SampleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SampleDatabase : RoomDatabase() {
    abstract fun dao(): SampleDao
}
