/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.integration.multiplatformtestapp.library

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query

@Entity(tableName = "library_table")
data class LibraryEntity(@PrimaryKey val id: Long, @ColumnInfo(name = "data") val text: String)

@Dao
interface LibraryDao {
    @Insert suspend fun insert(entity: LibraryEntity)

    @Query("SELECT * FROM library_table") suspend fun getAll(): List<LibraryEntity>
}
