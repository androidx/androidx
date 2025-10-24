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

package androidx.room3.benchmark

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.RoomDatabase
import androidx.room3.Transaction

@Database(entities = [User::class, Item::class], version = 1, exportSchema = false)
abstract class TestDatabase : RoomDatabase() {
    abstract fun getUserDao(): UserDao
}

@Entity data class User(@PrimaryKey val id: Int, val name: String)

@Entity data class Item(@PrimaryKey val id: Int, val ownerId: Int)

@Dao
interface UserDao {
    @Insert suspend fun insert(user: User)

    @Insert suspend fun insertUsers(user: List<User>)

    @Insert suspend fun insertItems(item: List<Item>)

    @Transaction @Query("SELECT * FROM User") suspend fun getUserWithItems(): List<UserWithItems>

    @Query("DELETE FROM User") suspend fun deleteAll(): Int
}

data class UserWithItems(
    @Embedded val user: User,
    @Relation(parentColumn = "id", entityColumn = "ownerId") val items: List<Item>,
)
