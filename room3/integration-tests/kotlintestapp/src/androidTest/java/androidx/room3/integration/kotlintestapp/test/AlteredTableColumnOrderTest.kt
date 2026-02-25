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

package androidx.room3.integration.kotlintestapp.test

import android.content.Context
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
import androidx.room3.migration.Migration
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifies that Room mapping code works with both newly created tables and altered tables whose
 * star projections results in a different order of columns.
 *
 * For example, start with entity with properties: A, B, C. Then on the entity a new property and
 * column is added with the property order being A, B, X, C and the migration is ALTER TABLE _ ADD
 * COLUMN X. The column result order for a star projection query will be different between these
 * two, but Room should be able to do the right mapping anyway.
 */
class AlteredTableColumnOrderTest {
    private lateinit var cleanDb: TestDatabase
    private lateinit var migratedDb: TestDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        context.deleteDatabase("foo.db")
        cleanDb =
            Room.databaseBuilder<TestDatabase>(context, "foo.db")
                .setDriver(AndroidSQLiteDriver())
                .build()

        context.deleteDatabase("migrated_foo.db")
        context.assets.open("databases/foo_v1.db").use { input ->
            context.getDatabasePath("migrated_foo.db").outputStream().use { output ->
                input.copyTo(output)
            }
        }
        migratedDb =
            Room.databaseBuilder<TestDatabase>(context, "migrated_foo.db")
                .setDriver(AndroidSQLiteDriver())
                .addMigrations(
                    object : Migration(1, 2) {
                        override suspend fun migrate(connection: SQLiteConnection) {
                            connection.execSQL(
                                "ALTER TABLE Foo ADD COLUMN X TEXT NOT NULL DEFAULT 'X';"
                            )
                        }
                    }
                )
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
    fun verifyDifferentColumnOrder() = runTest {
        val columnNames1 =
            cleanDb.useReaderConnection { connection ->
                connection.usePrepared("SELECT * FROM Foo") { it.getColumnNames() }
            }
        val columnNames2 =
            migratedDb.useReaderConnection { connection ->
                connection.usePrepared("SELECT * FROM Foo") { it.getColumnNames() }
            }
        // Result order matches field order
        assertThat(columnNames1).containsExactly("id", "A", "B", "X", "C").inOrder()
        // Result order is field order in v1 plus new column appended
        assertThat(columnNames2).containsExactly("id", "A", "B", "C", "X").inOrder()
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
        val C: String,
    )

    @Dao
    internal interface FooDao {
        @Insert fun insertFoo(f: Foo)

        @Query("SELECT * FROM Foo LIMIT 1") fun getOneFoo(): Foo
    }
}
