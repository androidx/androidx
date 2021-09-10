/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class EnumColumnTypeAdapterTest {
    private lateinit var db: EnumColumnTypeAdapterDatabase

    @Entity
    data class EntityWithEnum(
        @PrimaryKey
        val id: Long,
        val fruit: Fruit
    )

    @Entity
    data class EntityWithOneWayEnum(
        @PrimaryKey
        val id: Long,
        val color: Color
    )

    @Entity
    data class ComplexEntityWithEnum(
        @PrimaryKey
        val id: Long,
        val season: Season
    )

    enum class Color {
        RED, GREEN
    }

    enum class Fruit {
        BANANA, STRAWBERRY, WILDBERRY
    }

    enum class Season(private val text: String) {
        SUMMER("Sunny"), SPRING("Warm"), WINTER("Cold"), AUTUMN("Rainy");
    }

    @Dao
    interface SampleDao {
        @Query("INSERT INTO EntityWithEnum (id, fruit) VALUES (:id, :fruit)")
        fun insert(id: Long, fruit: Fruit?): Long

        @Query("SELECT * FROM EntityWithEnum WHERE id = :id")
        fun getValueWithId(id: Long): EntityWithEnum
    }

    @Dao
    interface SampleDaoWithOneWayConverter {
        @Query("INSERT INTO EntityWithOneWayEnum (id, color) VALUES (:id, :colorInt)")
        fun insert(id: Long, colorInt: Int): Long

        @Query("SELECT * FROM EntityWithOneWayEnum WHERE id = :id")
        fun getValueWithId(id: Long): EntityWithOneWayEnum
    }

    class ColorTypeConverter {
        @TypeConverter
        fun fromIntToColorEnum(colorInt: Int): Color {
            return if (colorInt == 1) {
                Color.RED
            } else {
                Color.GREEN
            }
        }
    }

    @Dao
    interface SampleDaoWithComplexEnum {
        @Query("INSERT INTO ComplexEntityWithEnum (id, season) VALUES (:id, :season)")
        fun insertComplex(id: Long, season: Season?): Long

        @Query("SELECT * FROM ComplexEntityWithEnum WHERE id = :id")
        fun getComplexValueWithId(id: Long): ComplexEntityWithEnum
    }

    @Database(
        entities = [
            EntityWithEnum::class,
            ComplexEntityWithEnum::class,
            EntityWithOneWayEnum::class
        ],
        version = 1,
        exportSchema = false
    )
    @TypeConverters(
        ColorTypeConverter::class
    )
    abstract class EnumColumnTypeAdapterDatabase : RoomDatabase() {
        abstract fun dao(): SampleDao
        abstract fun oneWayDao(): SampleDaoWithOneWayConverter
        abstract fun complexDao(): SampleDaoWithComplexEnum
    }

    @Before
    fun initDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            EnumColumnTypeAdapterDatabase::class.java
        ).build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun readAndWriteEnumToDatabase() {
        db.dao().insert(1, Fruit.BANANA)
        db.dao().insert(2, Fruit.STRAWBERRY)
        assertThat(
            db.dao().getValueWithId(1).fruit
        ).isEqualTo(Fruit.BANANA)
        assertThat(
            db.dao().getValueWithId(2).fruit
        ).isEqualTo(Fruit.STRAWBERRY)
    }

    @Test
    fun writeOneWayEnumToDatabase() {
        db.oneWayDao().insert(1, 1)
        assertThat(
            db.oneWayDao().getValueWithId(1).color
        ).isEqualTo(
            Color.RED
        )
    }

    @Test
    fun filterOutComplexEnumTest() {
        db.complexDao().insertComplex(1, Season.AUTUMN)
        assertThat(
            db.complexDao().getComplexValueWithId(1).season
        ).isEqualTo(
            Season.AUTUMN
        )
    }
}
