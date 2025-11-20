/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room3.integration.testapp.test

import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TypeConverterPriorityTest {

    private lateinit var db: TestDatabase

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder<TestDatabase>(
                    InstrumentationRegistry.getInstrumentation().targetContext
                )
                .setDriver(AndroidSQLiteDriver())
                .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testConverterMultiParam() {
        db.getDao().insert(TestEntity("1", listOf("a", "b", "c")))
        db.getDao().insert(TestEntity("2", listOf("d", "e", "f")))
        db.getDao().insert(TestEntity("3", listOf("g", "h", "i")))
        db.getDao().delete(listOf("2", "3"))
        assertThat(db.getDao().getAll()).hasSize(1)
    }

    @Test
    fun testConverterSingleParam() {
        db.getDao().insert(TestEntity("1", listOf("a", "b", "c")))
        db.getDao().update("1", listOf("d", "e", "f"))
        val all = db.getDao().getAll()
        assertThat(all).hasSize(1)
        assertThat(all[0].data).isEqualTo(listOf("d", "e", "f"))
    }

    @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
    @TypeConverters(Converters::class)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun getDao(): TestDao
    }

    @Dao
    interface TestDao {
        @Insert fun insert(entity: TestEntity)

        @Query("SELECT * FROM TestEntity") fun getAll(): List<TestEntity>

        @Query("DELETE FROM TestEntity WHERE id IN (:ids)") fun delete(ids: List<String>)

        @Query("UPDATE TestEntity SET data = :csv WHERE id = :id")
        fun update(id: String, csv: List<String>)
    }

    @Entity data class TestEntity(@PrimaryKey val id: String, val data: List<String>)

    object Converters {
        @JvmStatic
        @TypeConverter
        fun fromData(list: List<String>): String {
            return list.joinToString(",")
        }

        @JvmStatic
        @TypeConverter
        fun toData(string: String): List<String> {
            return string.split(",")
        }
    }
}
