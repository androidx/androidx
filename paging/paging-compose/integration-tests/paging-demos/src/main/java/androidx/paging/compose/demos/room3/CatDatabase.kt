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

package androidx.paging.compose.demos.room3

import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transaction

@Database(entities = [Cat::class], version = 1, exportSchema = false)
internal abstract class CatDatabase : RoomDatabase() {
    abstract fun getDao(): CatDao
}

@Dao
@DaoReturnTypeConverters(KeyBasedPagingSourceDaoReturnTypeConverter::class)
internal interface CatDao {
    @Query("SELECT * FROM Cat") fun getCatsPagingSource(): KeyBasedPagingSource<Cat>

    @Query("SELECT count(*) FROM Cat WHERE name LIKE :name")
    suspend fun getCatsCountWithName(name: String): Int

    @Query("SELECT COUNT(*) FROM Cat") suspend fun getCatsCount(): Int

    @Insert suspend fun insertCat(cat: Cat)

    @Transaction
    suspend fun insertNewRandomCat(amount: Int = 1) =
        repeat(amount) {
            val newPossibleName = getCatName()
            val count = getCatsCountWithName("$newPossibleName%")
            val newName = getCatNameWithSuffix(newPossibleName, count)
            insertCat(Cat(newName))
        }
}

@Entity
internal data class Cat(@PrimaryKey val name: String) : KeyedValue {
    @Ignore
    override val key: String
        get() = name
}
