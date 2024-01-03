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
package androidx.room.integration.kotlintestapp.test

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DaoNameConflictTest {
    private lateinit var mDb: ConflictDatabase
    @Before
    fun init() {
        mDb = inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ConflictDatabase::class.java
        ).build()
    }

    @After
    fun close() {
        mDb.close()
    }

    @Test
    fun readFromItem1() {
        val item1 = Item1(1, "a")
        mDb.item1Dao().insert(item1)
        val item2 = Item2(2, "b")
        mDb.item2Dao().insert(item2)
        MatcherAssert.assertThat(
            mDb.item1Dao().get(), CoreMatchers.`is`(item1)
        )
        MatcherAssert.assertThat(
            mDb.item2Dao().get(), CoreMatchers.`is`(item2)
        )
    }

    @Entity
    class Item1(@field:PrimaryKey var id: Int, var name: String?) {
        @Dao
        interface Store {
            @Query("SELECT * FROM Item1 LIMIT 1")
            fun get(): Item1

            @Insert
            fun insert(vararg items: Item1)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val item1 = other as Item1
            if (id != item1.id) return false
            return if (name != null) name == item1.name else item1.name == null
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + if (name != null) name.hashCode() else 0
            return result
        }
    }

    @Entity
    class Item2(@field:PrimaryKey var id: Int, var name: String?) {
        @Dao
        interface Store {
            @Query("SELECT * FROM Item2 LIMIT 1")
            fun get(): Item2

            @Insert
            fun insert(vararg items: Item2)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val item2 = other as Item2
            if (id != item2.id) return false
            return if (name != null) name == item2.name else item2.name == null
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + if (name != null) name.hashCode() else 0
            return result
        }
    }

    @Database(version = 1, exportSchema = false, entities = [Item1::class, Item2::class])
    abstract class ConflictDatabase : RoomDatabase() {
        abstract fun item1Dao(): Item1.Store
        abstract fun item2Dao(): Item2.Store
    }
}
