/*
 * Copyright (C) 2018 The Android Open Source Project
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
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.RoomWarnings
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
class ItemWithNullableConstructor {
    lateinit var db: Db

    @Before
    fun initDb() {
        db =
            Room.inMemoryDatabaseBuilder<Db>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertWithNull() {
        db.getDao().insert(TestItem(null, null))
        assertThat(db.getDao().get()).isEqualTo(TestItem(1, null))
    }

    @Entity
    data class TestItem(
        @PrimaryKey(autoGenerate = true) val id: Long? = null,
        val nullable: Boolean?,
    )

    @Dao
    interface TestDao {
        @Insert fun insert(testItem: TestItem)

        @Query("SELECT * FROM TestItem LIMIT 1") fun get(): TestItem?
    }

    @Database(version = 1, entities = [TestItem::class], exportSchema = false)
    @SuppressWarnings(RoomWarnings.MISSING_SCHEMA_LOCATION)
    abstract class Db : RoomDatabase() {
        abstract fun getDao(): TestDao
    }
}
