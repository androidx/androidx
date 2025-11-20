/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.room3.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room.inMemoryDatabaseBuilder
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DaoNameConflictTest {
    private lateinit var db: ConflictDatabase

    @Before
    fun init() {
        db =
            inMemoryDatabaseBuilder<ConflictDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun readFromItem1() {
        val item1 = Item1(1, "a")
        db.item1Dao().insert(item1)
        val item2 = Item2(2, "b")
        db.item2Dao().insert(item2)
        assertThat(db.item1Dao().get()).isEqualTo(item1)
        assertThat(db.item2Dao().get()).isEqualTo(item2)
    }

    @Entity
    data class Item1(@field:PrimaryKey var id: Int, var name: String?) {
        @Dao
        interface Store {
            @Query("SELECT * FROM Item1 LIMIT 1") fun get(): Item1

            @Insert fun insert(vararg items: Item1)
        }
    }

    @Entity
    data class Item2(@field:PrimaryKey var id: Int, var name: String?) {
        @Dao
        interface Store {
            @Query("SELECT * FROM Item2 LIMIT 1") fun get(): Item2

            @Insert fun insert(vararg items: Item2)
        }
    }

    @Database(version = 1, exportSchema = false, entities = [Item1::class, Item2::class])
    abstract class ConflictDatabase : RoomDatabase() {
        abstract fun item1Dao(): Item1.Store

        abstract fun item2Dao(): Item2.Store
    }
}
