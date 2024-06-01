/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.Room
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.vo.Email
import androidx.room.integration.kotlintestapp.vo.User
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteStatement
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MissingMasterTableTest {

    @Before
    fun deleteDb() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase("existingDb.db")
    }

    @Test
    fun openDatabaseWithMissingRoomMasterTable() {
        val allowVersionUpdates = AtomicBoolean(true)
        val factory = MyOpenHelperFactory(allowVersionUpdates)
        val user = User("u1", Email("e1", "email address 1"), Email("e2", "email address 2"))

        fun openDb(): TestDatabase {
            return Room.databaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    klass = TestDatabase::class.java,
                    name = "existingDb.db"
                )
                .openHelperFactory(factory)
                .build()
        }
        // Open first version of the database, and remove the room_master_table. This will cause
        // a failure next time we open the database, due to a verification failure.
        val db1 = openDb()
        db1.usersDao().insertUser(user)
        // Delete the room_master_table
        db1.compileStatement("DROP TABLE room_master_table;").execute()
        db1.close()
        allowVersionUpdates.set(false)

        // Second version of the database fails at open, the failed transaction should be rolled
        // back, allowing the third version to open successfully.
        val db2 = openDb()
        assertThrows<Throwable> { assertThat(db2.usersDao().getUsers()).containsExactly(user) }
            .hasMessageThat()
            .contains("no-version-updates")
        db2.close()
        allowVersionUpdates.set(true)

        // Third version of the database is expected to open with no failures.
        val db3 = openDb()
        assertThat(db3.usersDao().getUsers()).containsExactly(user)
        db3.close()
    }

    private class MyOpenHelperFactory(private val allowVersionUpdates: AtomicBoolean) :
        SupportSQLiteOpenHelper.Factory {
        private val delegate = FrameworkSQLiteOpenHelperFactory()

        override fun create(
            configuration: SupportSQLiteOpenHelper.Configuration
        ): SupportSQLiteOpenHelper {
            val newConfig =
                SupportSQLiteOpenHelper.Configuration(
                    context = configuration.context,
                    name = configuration.name,
                    callback = MyCallback(allowVersionUpdates, configuration.callback),
                    useNoBackupDirectory = configuration.useNoBackupDirectory,
                    allowDataLossOnRecovery = configuration.allowDataLossOnRecovery
                )
            return MyOpenHelper(allowVersionUpdates, delegate.create(newConfig))
        }
    }

    private class MyOpenHelper(
        private val allowVersionUpdates: AtomicBoolean,
        private val delegate: SupportSQLiteOpenHelper,
    ) : SupportSQLiteOpenHelper {
        override val databaseName: String?
            get() = delegate.databaseName

        override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
            delegate.setWriteAheadLoggingEnabled(enabled)
        }

        override val writableDatabase: SupportSQLiteDatabase by lazy {
            MySupportSQLiteDatabase(allowVersionUpdates, delegate.writableDatabase)
        }
        override val readableDatabase: SupportSQLiteDatabase by lazy {
            MySupportSQLiteDatabase(allowVersionUpdates, delegate.writableDatabase)
        }

        override fun close() {
            delegate.close()
        }
    }

    private class MySupportSQLiteDatabase(
        private val allowVersionUpdates: AtomicBoolean,
        private val delegate: SupportSQLiteDatabase
    ) : SupportSQLiteDatabase by delegate {
        override fun compileStatement(sql: String): SupportSQLiteStatement {
            // The "insert" SQL should not go through if version updates are disabled.
            // This mimics app halt scenario, where a master table is created but not initialized.
            if (
                sql.startsWith("INSERT OR REPLACE INTO room_master_table") &&
                    !allowVersionUpdates.get()
            ) {
                throw RuntimeException("no-version-updates")
            }
            return delegate.compileStatement(sql)
        }
    }

    private class MyCallback(
        private val allowVersionUpdates: AtomicBoolean,
        private val delegate: SupportSQLiteOpenHelper.Callback
    ) : SupportSQLiteOpenHelper.Callback(delegate.version) {
        private val wrappers = IdentityHashMap<SupportSQLiteDatabase, MySupportSQLiteDatabase>()

        private fun SupportSQLiteDatabase.wrap(): MySupportSQLiteDatabase {
            if (this is MySupportSQLiteDatabase) return this
            return wrappers.getOrPut(this) { MySupportSQLiteDatabase(allowVersionUpdates, this) }
        }

        override fun onConfigure(db: SupportSQLiteDatabase) {
            delegate.onConfigure(db.wrap())
        }

        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            delegate.onDowngrade(db.wrap(), oldVersion, newVersion)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            delegate.onOpen(db.wrap())
        }

        override fun onCorruption(db: SupportSQLiteDatabase) {
            delegate.onCorruption(db.wrap())
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            delegate.onCreate(db.wrap())
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            delegate.onUpgrade(db.wrap(), oldVersion, newVersion)
        }
    }
}
