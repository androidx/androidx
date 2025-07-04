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

package androidx.room.benchmark

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.RoomWarnings

@Database(entities = [User::class, Item::class], version = 1, exportSchema = false)
abstract class TestDatabase : RoomDatabase() {
    abstract fun getUserDao(): UserDao
}

@Entity data class User(@PrimaryKey val id: Int, val name: String)

@Entity data class Item(@PrimaryKey val id: Int, val ownerId: Int)

@Dao
interface UserDao {
    @Insert fun insert(user: User)

    @Insert fun insertUsers(user: List<User>)

    @Insert fun insertItems(item: List<Item>)

    @SuppressWarnings(RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION)
    @Query("SELECT * FROM User")
    fun getUserWithItems(): List<UserWithItems>

    @Query("DELETE FROM User") fun deleteAll(): Int
}

data class UserWithItems(
    @Embedded val user: User,
    @Relation(parentColumn = "id", entityColumn = "ownerId") val items: List<Item>,
)
