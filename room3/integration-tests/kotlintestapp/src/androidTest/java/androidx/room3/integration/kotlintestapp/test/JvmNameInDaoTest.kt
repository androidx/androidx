/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import org.junit.Test

/**
 * This test has [JvmName] annotation on @Dao declarations to ensure Room properly handles them.
 *
 * If this this test fails because of the `INAPPLICABLE_JVM_NAME` suppression, we can disable the
 * test if needed. We should not try too hard to support cases when developer disables a compiler
 * error.
 */
class JvmNameInDaoTest {
    @Test
    fun test() {
        val entity =
            JvmNameEntity(id = 1, name = "value1", convertedClass = MyConvertedClass("value2"))
        val db =
            Room.inMemoryDatabaseBuilder<JvmNameDb>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
        try {
            db.getDao().insert(entity)
            assertThat(db.getDao().query()).containsExactly(entity)
            db.getDao().delete(entity)
            assertThat(db.getDao().query()).isEmpty()
        } finally {
            db.close()
        }
    }

    data class MyConvertedClass(val value: String)

    object MyConverterConverter {
        @TypeConverter @JvmName("jvmToDb") fun toDb(value: MyConvertedClass): String = value.value

        @TypeConverter
        @JvmName("jvmFromDb")
        fun fromDb(value: String): MyConvertedClass = MyConvertedClass(value)
    }

    @Entity
    data class JvmNameEntity(
        @PrimaryKey(autoGenerate = true) val id: Int,
        val name: String,
        val convertedClass: MyConvertedClass,
    )

    @Dao
    interface JvmBaseDao<T> {
        @Delete @Suppress("INAPPLICABLE_JVM_NAME") @JvmName("jvmDelete") fun delete(t: T)

        @Delete fun overriddenJvmDelete(t: T)
    }

    @Dao
    interface JvmNameDao : JvmBaseDao<JvmNameEntity> {
        @Insert
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("jvmInsert")
        fun insert(entity: JvmNameEntity)

        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("jvmQuery")
        @Query("SELECT * FROM JvmNameEntity")
        fun query(): List<JvmNameEntity>

        // This will generate two methods for `overriddenJvmDelete`, which is not
        // supported in KSP. Keeping this code here as an ack that this is WAI.
        // @Suppress("INAPPLICABLE_JVM_NAME")
        // @JvmName("jvmDeleteChild")
        @Delete override fun overriddenJvmDelete(t: JvmNameEntity)
    }

    @Database(entities = [JvmNameEntity::class], exportSchema = false, version = 1)
    @TypeConverters(MyConverterConverter::class)
    abstract class JvmNameDb : RoomDatabase() {
        @Suppress("INAPPLICABLE_JVM_NAME") @JvmName("jvmDao") abstract fun getDao(): JvmNameDao
    }
}
