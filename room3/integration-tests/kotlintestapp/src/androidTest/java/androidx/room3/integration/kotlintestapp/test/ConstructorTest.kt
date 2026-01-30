/*
 * Copyright (C) 2017 The Android Open Source Project
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
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
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
class ConstructorTest {
    @Database(
        version = 1,
        entities = [FullConstructor::class, PartialConstructor::class],
        exportSchema = false,
    )
    abstract class MyDb : RoomDatabase() {
        abstract fun dao(): MyDao
    }

    @Dao
    interface MyDao {
        @Insert fun insertFull(vararg full: FullConstructor)

        @Query("SELECT * FROM fc WHERE a = :a") fun loadFull(a: Int): FullConstructor

        @Insert fun insertPartial(vararg partial: PartialConstructor)

        @Query("SELECT * FROM pc WHERE a = :a") fun loadPartial(a: Int): PartialConstructor
    }

    @Entity(tableName = "fc")
    data class FullConstructor(
        @PrimaryKey val a: Int,
        val b: Int,
        @Embedded val embedded: MyEmbedded?,
    )

    @Entity(tableName = "pc")
    data class PartialConstructor(@PrimaryKey val a: Int) {
        var b: Int = 0
        @Embedded var embedded: MyEmbedded? = null
    }

    data class MyEmbedded(val text: String?)

    private lateinit var db: MyDb
    private lateinit var dao: MyDao

    @Before
    fun init() {
        db =
            Room.inMemoryDatabaseBuilder<MyDb>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
        dao = db.dao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndReadFullConstructor() {
        val inserted = FullConstructor(1, 2, null)
        dao.insertFull(inserted)
        val load = dao.loadFull(1)
        assertThat(load).isEqualTo(inserted)
    }

    @Test
    fun insertAndReadPartial() {
        val item = PartialConstructor(3).apply { b = 7 }
        dao.insertPartial(item)
        val load = dao.loadPartial(3)
        assertThat(load).isEqualTo(item)
    }
}
