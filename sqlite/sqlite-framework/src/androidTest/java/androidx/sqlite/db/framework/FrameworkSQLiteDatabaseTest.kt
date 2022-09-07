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
import android.os.Build
import androidx.annotation.RequiresApi
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
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun testFrameWorkSQLiteDatabase_simpleDeleteWorks() {
        val db = openHelper.writableDatabase
        db.execSQL("create table user (idk int)")

        val statement = db
            .compileStatement("insert into user (idk) values (1)")
        statement.executeInsert() // This should succeed

        db.query("select * from user").use {
            assertThat(it.count).isEqualTo(1)
        }

        db.delete(
            "user",
            null,
            null
        )

        db.query("select * from user").use {
            assertThat(it.count).isEqualTo(0)
        }
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun testFrameWorkSQLiteDatabase_deleteWorksWithWhereClause() {
        val db = openHelper.writableDatabase
        db.execSQL("create table user (idk int)")

        val statement = db
            .compileStatement("insert into user (idk) values (1)")
        statement.executeInsert() // This should succeed

        db.query("select * from user where idk=1").use {
            assertThat(it.count).isEqualTo(1)
        }

        db.delete("user", "idk = ?", arrayOf(1))

        db.query("select * from user where idk=1").use {
            assertThat(it.count).isEqualTo(0)
        }
    }
}