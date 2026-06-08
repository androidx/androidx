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
import androidx.room3.Ignore
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
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
        entities =
            [
                FullConstructor::class,
                PartialConstructor::class,
                DefaultValueConstructor::class,
                IgnoredDefaultValueConstructor::class,
            ],
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

        @Insert fun insertDefaultValue(vararg dvc: DefaultValueConstructor)

        @Query("SELECT a FROM dvc WHERE a = :a")
        fun loadDefaultValue(a: Int): DefaultValueConstructor

        @Insert fun insertIgnoredDefaultValue(vararg idvc: IgnoredDefaultValueConstructor)

        @Query("SELECT a FROM idvc WHERE a = :a")
        fun loadIgnoredDefaultValue(a: Int): IgnoredDefaultValueConstructor
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

    @Entity(tableName = "dvc")
    data class DefaultValueConstructor(@PrimaryKey val a: Int, val b: Int = 10)

    @Entity(tableName = "idvc")
    data class IgnoredDefaultValueConstructor(@PrimaryKey val a: Int, @Ignore val b: String? = null)

    data class MyEmbedded(val text: String?)

    private lateinit var db: MyDb
    private lateinit var dao: MyDao

    @Before
    fun init() {
        db = Room.inMemoryDatabaseBuilder<MyDb>().setDriver(AndroidSQLiteDriver()).build()
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

    @Test
    fun readWithDefaultValue() {
        val inserted = DefaultValueConstructor(1, 2)
        dao.insertDefaultValue(inserted)
        // Query only 'a', so 'b' should use default value 10
        val load = dao.loadDefaultValue(1)
        assertThat(load).isEqualTo(DefaultValueConstructor(1, 10))
    }

    @Test
    fun readWithIgnoredDefaultValue() {
        val inserted = IgnoredDefaultValueConstructor(1, "notNull")
        dao.insertIgnoredDefaultValue(inserted)
        // Query only 'a', so 'b' should be null (ignored and default null)
        val load = dao.loadIgnoredDefaultValue(1)
        assertThat(load).isEqualTo(IgnoredDefaultValueConstructor(1, null))
    }
}
