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

package androidx.room3.integration.kotlintestapp.test

import android.content.Context
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverter
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OperationType
import androidx.room3.PrimaryKey
import androidx.room3.ProvidedDaoReturnTypeConverter
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ProvidedDaoReturnTypeConverterTest {

    @Test
    fun testProvidedDaoReturnTypeConverter() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db =
            Room.inMemoryDatabaseBuilder<TestDatabaseWithDaoConverter>(context)
                .addDaoReturnTypeConverter(FooReturnTypeConverter())
                .setDriver(AndroidSQLiteDriver())
                .build()
        val entity = TestEntity(1, "test")
        db.dao().insert(entity)
        val result: Foo<TestEntity?> = db.dao().getEntity(1)
        assertThat(result.value).isNotNull()
        assertThat(result.value!!.name).isEqualTo("test")
        db.close()
    }

    @Test
    fun testMissingProvidedDaoReturnTypeConverterInstance() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        try {
            val db =
                Room.inMemoryDatabaseBuilder<TestDatabaseWithDaoConverter>(context)
                    .setDriver(AndroidSQLiteDriver())
                    .build()
            db.dao().getEntity(1)
            assertWithMessage("Should have thrown an IllegalArgumentException").fail()
        } catch (throwable: Throwable) {
            assertThat(throwable).isInstanceOf<IllegalArgumentException>()
        }
    }

    @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
    internal abstract class TestDatabaseWithDaoConverter : RoomDatabase() {
        abstract fun dao(): TestDao
    }

    @Dao
    @DaoReturnTypeConverters(FooReturnTypeConverter::class)
    interface TestDao {
        @Insert fun insert(entity: TestEntity)

        @Query("SELECT * FROM TestEntity WHERE id = :id")
        suspend fun getEntity(id: Int): Foo<TestEntity?>
    }

    @Entity data class TestEntity(@PrimaryKey val id: Int, val name: String)

    class Foo<T>(val value: T)

    @ProvidedDaoReturnTypeConverter
    class FooReturnTypeConverter {
        @DaoReturnTypeConverter([OperationType.READ])
        suspend fun <T> convert(executeAndConvert: suspend () -> T): Foo<T> {
            return Foo(executeAndConvert())
        }
    }
}
