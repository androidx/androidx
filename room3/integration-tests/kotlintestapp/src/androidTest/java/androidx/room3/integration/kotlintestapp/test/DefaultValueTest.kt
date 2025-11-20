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
import androidx.room3.ColumnInfo
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
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DefaultValueTest {

    object TimestampConverter {
        private val FORMAT =
            SimpleDateFormat("yyyy-MM-dd kk:mm:ss").apply { timeZone = TimeZone.getTimeZone("UTC") }

        @TypeConverter
        fun toTimestamp(date: Date): String {
            return FORMAT.format(date)
        }

        @TypeConverter
        fun fromTimestamp(timestamp: String): Date {
            return try {
                FORMAT.parse(timestamp)
            } catch (e: ParseException) {
                throw RuntimeException(e)
            }
        }
    }

    @Entity
    @TypeConverters(TimestampConverter::class)
    data class Sample(
        @PrimaryKey val id: Long,
        val name: String,
        @ColumnInfo(defaultValue = "No description") val description: String,
        @ColumnInfo(defaultValue = "1") val available: Boolean,
        @ColumnInfo(defaultValue = "0") val serial: Int,
        @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP") val timestamp: Date,
    )

    data class NameAndId(val id: Long, val name: String)

    @Dao
    interface SampleDao {
        @Query("INSERT INTO Sample (name) VALUES (:name)") fun insert(name: String): Long

        @Insert(entity = Sample::class) fun insertName(nameAndId: NameAndId)

        @Query("SELECT * FROM Sample WHERE id = :id") fun byId(id: Long): Sample
    }

    @Database(entities = [Sample::class], version = 1, exportSchema = false)
    abstract class DefaultValueDatabase : RoomDatabase() {
        abstract fun dao(): SampleDao
    }

    private lateinit var db: DefaultValueDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db =
            Room.inMemoryDatabaseBuilder<DefaultValueDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun defaultValues() {
        val id = db.dao().insert("A")
        val now = System.currentTimeMillis()
        val sample = db.dao().byId(id)
        assertThat(sample.name).isEqualTo("A")
        assertThat(sample.description).isEqualTo("No description")
        assertThat(sample.available).isTrue()
        assertThat(sample.serial).isEqualTo(0)
        assertThat(sample.timestamp.time.toDouble()).isWithin(3000.0).of(now.toDouble())
    }

    @Test
    fun defaultValues_partialEntity() {
        val nameAndId = NameAndId(1, "A")
        db.dao().insertName(nameAndId)
        val now = System.currentTimeMillis()
        val sample = db.dao().byId(1)
        assertThat(sample.name).isEqualTo("A")
        assertThat(sample.description).isEqualTo("No description")
        assertThat(sample.available).isTrue()
        assertThat(sample.serial).isEqualTo(0)
        assertThat(sample.timestamp.time.toDouble()).isWithin(3000.0).of(now.toDouble())
    }
}
