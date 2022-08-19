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
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifies that Room mapping code works with both newly created tables and altered tables whose
 * star projections results in a different order of columns.
 *
 * For example, start with entity with fields: A, B, C. Then on the entity a new field and column
 * is added with the fields order being A, B, X, C and the migration is ALTER TABLE _ ADD COLUMN X.
 * The column result order for a star projection query will be different between these two, but Room
 * should be able to do the right mapping anyway.
 */
class AlteredTableColumnOrderTest {
    private lateinit var cleanDb: TestDatabase
    private lateinit var migratedDb: TestDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        context.deleteDatabase("foo.db")
        cleanDb =
            Room.databaseBuilder(context, TestDatabase::class.java, "foo.db").build()

        context.deleteDatabase("migrated_foo.db")
        migratedDb =
            Room.databaseBuilder(context, TestDatabase::class.java, "migrated_foo.db")
                .createFromAsset("databases/foo_v1.db")
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE Foo ADD COLUMN X TEXT NOT NULL DEFAULT 'X';")
                    }
                })
                .build()
    }

    @After
    fun teardown() {
        migratedDb.close()
        cleanDb.close()
    }

    @Test
    fun verifyPojoResult() {
        val expectedFoo = Foo(1, "A", "B", "X", "C")
        cleanDb.getDao().insertFoo(expectedFoo)
        migratedDb.getDao().insertFoo(expectedFoo)
        assertThat(cleanDb.getDao().getOneFoo()).isEqualTo(expectedFoo)
        assertThat(migratedDb.getDao().getOneFoo()).isEqualTo(expectedFoo)
    }

    @Test
    fun verifyDifferentColumnOrder() {
        val c1 = cleanDb.openHelper.writableDatabase.query("SELECT * FROM Foo")
        val c2 = migratedDb.openHelper.writableDatabase.query("SELECT * FROM Foo")
        try {
            val columnNames1 = c1.columnNames
            val columnNames2 = c2.columnNames
            // Result order matches field order
            assertThat(columnNames1).isEqualTo(arrayOf("id", "A", "B", "X", "C"))
            // Result order is field order in v1 plus new column appended
            assertThat(columnNames2).isEqualTo(arrayOf("id", "A", "B", "C", "X"))
        } finally {
            c1.close()
            c2.close()
        }
    }

    @Database(entities = [Foo::class], version = 2, exportSchema = false)
    internal abstract class TestDatabase : RoomDatabase() {
        abstract fun getDao(): FooDao
    }

    @Entity
    internal data class Foo(
        @PrimaryKey val id: Int,
        val A: String,
        val B: String,
        @ColumnInfo(defaultValue = "X") val X: String,
        val C: String
    )

    @Dao
    internal interface FooDao {
        @Insert
        fun insertFoo(f: Foo?)

        @Query("SELECT * FROM Foo LIMIT 1")
        fun getOneFoo(): Foo
    }
}