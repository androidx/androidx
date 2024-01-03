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

package androidx.sqlite.db.framework

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

@SmallTest
class FrameworkSQLiteDatabaseTest {
    private val dbName = "test.db"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val openHelper = FrameworkSQLiteOpenHelper(
        context,
        dbName,
        OpenHelperRecoveryTest.EmptyCallback(),
        useNoBackupDirectory = false,
        allowDataLossOnRecovery = false
    )

    @Before
    fun setup() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun testFrameWorkSQLiteDatabase_simpleDeleteWorks() {
        val db = openHelper.writableDatabase
        db.execSQL("create table user (idk int)")

        val statement = db
            .compileStatement("insert into user (idk) values (1)")
        statement.executeInsert() // This should succeed

        val cursor1 = db.query("select * from user")
        assertThat(cursor1.count).isEqualTo(1)

        db.delete(
            "user",
            null,
            null
        )

        val cursor2 = db.query("select * from user")
        assertThat(cursor2.count).isEqualTo(0)
    }

    @Test
    fun testFrameWorkSQLiteDatabase_deleteWorksWithWhereClause() {
        val db = openHelper.writableDatabase
        db.execSQL("create table user (idk int)")

        val statement = db
            .compileStatement("insert into user (idk) values (1)")
        statement.executeInsert() // This should succeed

        val cursor1 = db.query("select * from user where idk=1")
        assertThat(cursor1.count).isEqualTo(1)

        db.delete("user", "idk = ?", arrayOf(1))

        val cursor2 = db.query("select * from user where idk=1")
        assertThat(cursor2.count).isEqualTo(0)
    }

    @Test
    fun testFrameWorkSQLiteDatabase_attachDbWorks() {
        val openHelper2 = FrameworkSQLiteOpenHelper(
            context,
            "test2.db",
            OpenHelperRecoveryTest.EmptyCallback(),
            useNoBackupDirectory = false,
            allowDataLossOnRecovery = false
        )
        val db1 = openHelper.writableDatabase
        val db2 = openHelper2.writableDatabase

        db1.execSQL(
            "ATTACH DATABASE '${db2.path}' AS database2"
        )

        val cursor = db1.query("pragma database_list")
        val expected = buildList<Pair<String, String>> {
            while (cursor.moveToNext()) {
                add(cursor.getString(1) to cursor.getString(2))
            }
        }
        cursor.close()
        openHelper2.close()

        val actual = db1.attachedDbs?.map { it.first to it.second }
        assertThat(expected).isEqualTo(actual)
    }

    // b/271083856 and b/183028015
    @Test
    fun testFrameWorkSQLiteDatabase_onUpgrade_maxSqlCache() {
        // Open and close DB at initial version.
        openHelper.writableDatabase.use { db ->
            db.execSQL("CREATE TABLE Foo (id INTEGER NOT NULL PRIMARY KEY, data TEXT)")
            db.execSQL("INSERT INTO Foo (id, data) VALUES (1, 'bar')")
        }

        FrameworkSQLiteOpenHelper(
            context,
            dbName,
            object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {}

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    // Do a query, this query will get cached, but we expect it to get evicted if
                    // androidx.sqlite workarounds this issue by reducing the cache size.
                    db.query("SELECT * FROM Foo").let { c ->
                        assertThat(c.moveToNext()).isTrue()
                        assertThat(c.getString(1)).isEqualTo("bar")
                        c.close()
                    }
                    // Alter table, specifically make it so that using a cached query will be
                    // troublesome.
                    db.execSQL("ALTER TABLE Foo RENAME TO Foo_old")
                    db.execSQL("CREATE TABLE Foo (id INTEGER NOT NULL PRIMARY KEY)")
                    // Do an irrelevant query to evict the last SELECT statement, sadly this is
                    // required because we can only reduce the cache size to 1, and only SELECT or
                    // UPDATE statement are cache.
                    // See frameworks/base/core/java/android/database/sqlite/SQLiteConnection.java;l=1209
                    db.query("SELECT * FROM Foo_old").close()
                    // Do earlier query, checking it is not cached
                    db.query("SELECT * FROM Foo").let { c ->
                        assertThat(c.columnNames.toList()).containsExactly("id")
                        assertThat(c.count).isEqualTo(0)
                        c.close()
                    }
                }
            },
            useNoBackupDirectory = false,
            allowDataLossOnRecovery = false
        ).writableDatabase.close()
    }
}
