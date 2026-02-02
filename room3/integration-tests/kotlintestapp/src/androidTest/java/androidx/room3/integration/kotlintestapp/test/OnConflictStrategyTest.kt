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

import android.content.Context
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@MediumTest
@RunWith(Parameterized::class)
class OnConflictStrategyTest(private val useDriver: UseDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    @Database(version = 1, entities = [Animal::class], exportSchema = false)
    abstract class OnConflictStrategyDatabase : RoomDatabase() {
        abstract fun animal(): AnimalDao
    }

    @Entity data class Animal(@PrimaryKey val id: Long, val name: String)

    @Dao
    interface AnimalDao {
        @Insert fun insertOrAbort(animals: Iterable<Animal>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertOrReplace(animals: Iterable<Animal>)

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insertOrIgnore(animals: Iterable<Animal>)

        @Query("SELECT name FROM Animal") fun allNames(): List<String>
    }

    private lateinit var db: OnConflictStrategyDatabase

    @Before
    fun setup() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        db =
            Room.inMemoryDatabaseBuilder<OnConflictStrategyDatabase>(context)
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertOrAbort() {
        assertThrows<SQLiteException> {
                db.animal()
                    .insertOrAbort(
                        listOf(
                            Animal(1, "Dog"),
                            Animal(2, "Cat"),
                            Animal(2, "Bird"),
                            Animal(3, "Monkey"),
                        )
                    )
            }
            .hasMessageThat()
            .contains("UNIQUE constraint failed")
        assertThat(db.animal().allNames()).isEmpty()
    }

    @Test
    fun insertOrReplace() {
        db.animal()
            .insertOrReplace(
                listOf(Animal(1, "Dog"), Animal(2, "Cat"), Animal(2, "Bird"), Animal(3, "Monkey"))
            )
        assertThat(db.animal().allNames()).containsExactly("Dog", "Bird", "Monkey")
    }

    @Test
    fun insertOrIgnore() {
        db.animal()
            .insertOrIgnore(
                listOf(Animal(1, "Dog"), Animal(2, "Cat"), Animal(2, "Bird"), Animal(3, "Monkey"))
            )
        assertThat(db.animal().allNames()).containsExactly("Dog", "Cat", "Monkey")
    }
}
