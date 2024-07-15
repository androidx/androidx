/*
 * Copyright 2024 The Android Open Source Project
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

package room.testapp

import androidx.room.*

@Database(entities = [NativeEntity::class], version = 1)
@ConstructedBy(MyDatabaseCtor::class)
abstract class MyDatabase : RoomDatabase() {
    abstract fun getMyDao(): MyDao
}

expect object MyDatabaseCtor : RoomDatabaseConstructor<MyDatabase>

@Entity
data class NativeEntity(
    @PrimaryKey val id: Long
)

@Dao
interface MyDao {
    @Query("SELECT * FROM NativeEntity")
    suspend fun getEntity(): NativeEntity

    // Insert-change
}