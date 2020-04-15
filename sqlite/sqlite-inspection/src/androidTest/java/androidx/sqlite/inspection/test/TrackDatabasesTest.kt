/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection.test

import android.database.sqlite.SQLiteDatabase
import androidx.inspection.InspectorEnvironment.ExitHook
import androidx.sqlite.inspection.SqliteInspectorProtocol.Event
import androidx.sqlite.inspection.test.MessageFactory.createTrackDatabasesCommand
import androidx.sqlite.inspection.test.MessageFactory.createTrackDatabasesResponse
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

private const val OPEN_DATABASE_COMMAND_SIGNATURE: String = "openDatabase" +
        "(" +
        "Ljava/io/File;" +
        "Landroid/database/sqlite/SQLiteDatabase\$OpenParams;" +
        ")" +
        "Landroid/database/sqlite/SQLiteDatabase;"

@MediumTest
@RunWith(AndroidJUnit4::class)
class TrackDatabasesTest {
    @get:Rule
    val testEnvironment = SqliteInspectorTestEnvironment()

    @get:Rule
    val temporaryFolder = TemporaryFolder(getInstrumentation().context.cacheDir)

    @Test
    fun test_track_databases() = runBlocking {
        val alreadyOpenDatabases = listOf(
            Database("db1").createInstance(temporaryFolder),
            Database("db2").createInstance(temporaryFolder)
        )

        testEnvironment.registerAlreadyOpenDatabases(alreadyOpenDatabases)

        testEnvironment.sendCommand(createTrackDatabasesCommand()).let { response ->
            assertThat(response).isEqualTo(createTrackDatabasesResponse())
        }

        // evaluate 'already-open' instances are found
        alreadyOpenDatabases.let { expected ->
            val actual = expected.indices.map { testEnvironment.receiveEvent().databaseOpened }
            testEnvironment.assertNoQueuedEvents()
            assertThat(actual.map { it.databaseId }.distinct()).hasSize(expected.size)
            expected.forEachIndexed { ix, _ ->
                assertThat(actual[ix].path).isEqualTo(expected[ix].path)
            }
        }

        // evaluate registered hooks
        val hookEntries = testEnvironment.consumeRegisteredHooks()
            .filter { it.originMethod == OPEN_DATABASE_COMMAND_SIGNATURE }
        assertThat(hookEntries).hasSize(1)
        hookEntries.first().let { entry ->
            // expect one exit hook tracking database open events
            assertThat(entry).isInstanceOf(Hook.ExitHook::class.java)
            assertThat(entry.originClass.name).isEqualTo(SQLiteDatabase::class.java.name)
            assertThat(entry.originMethod)
                .isEqualTo(OPEN_DATABASE_COMMAND_SIGNATURE)

            // verify that executing the registered hook will result in tracking events
            testEnvironment.assertNoQueuedEvents()
            @Suppress("UNCHECKED_CAST")
            val exitHook = entry.asExitHook as ExitHook<SQLiteDatabase>
            val database = Database("db3").createInstance(temporaryFolder)
            assertThat(exitHook.onExit(database)).isSameInstanceAs(database)
            testEnvironment.receiveEvent().let { event ->
                assertThat(event.databaseOpened.path).isEqualTo(database.path)
            }
        }

        assertThat(testEnvironment.consumeRegisteredHooks()).isEmpty()
    }

    @Test
    fun test_track_databases_the_same_database_opened_multiple_times() = runBlocking {
        // given
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        val onOpenHook = testEnvironment.consumeRegisteredHooks()
            .first { it.originMethod == OPEN_DATABASE_COMMAND_SIGNATURE }
        @Suppress("UNCHECKED_CAST")
        val onOpen = (onOpenHook.asExitHook as ExitHook<SQLiteDatabase>)::onExit

        val seenDbIds = mutableSetOf<Int>()

        fun checkDbOpenedEvent(event: Event, database: SQLiteDatabase) {
            assertThat(event.hasDatabaseOpened()).isEqualTo(true)
            val isNewId = seenDbIds.add(event.databaseOpened.databaseId)
            assertThat(isNewId).isEqualTo(true)
            assertThat(event.databaseOpened.path).isEqualTo(database.path)
        }

        // file based db: first open
        val fileDbPath = "db1"
        val fileDb = Database(fileDbPath).createInstance(temporaryFolder)
        onOpen(fileDb)
        checkDbOpenedEvent(testEnvironment.receiveEvent(), fileDb)

        // file based db: same instance
        onOpen(fileDb)
        testEnvironment.assertNoQueuedEvents()

        // file based db: same path
        onOpen(Database(fileDbPath).createInstance(temporaryFolder))
        testEnvironment.assertNoQueuedEvents()

        // in-memory database: first open
        val inMemDb = Database(null).createInstance(temporaryFolder)
        onOpen(inMemDb)
        checkDbOpenedEvent(testEnvironment.receiveEvent(), inMemDb)

        // in-memory database: same instance
        onOpen(inMemDb)
        testEnvironment.assertNoQueuedEvents()

        // in-memory database: new instances (same path = :memory:)
        repeat(3) {
            val db = Database(null).createInstance(temporaryFolder)
            assertThat(db.path).isEqualTo(":memory:")
            onOpen(db)
            checkDbOpenedEvent(testEnvironment.receiveEvent(), db)
        }
    }

    @Test
    fun test_track_databases_keeps_db_open() = runBlocking {
        // given
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        val onOpenHook = testEnvironment.consumeRegisteredHooks()
            .first { it.originMethod == OPEN_DATABASE_COMMAND_SIGNATURE }

        @Suppress("UNCHECKED_CAST")
        val onOpen = (onOpenHook.asExitHook as ExitHook<SQLiteDatabase>)::onExit

        // without inspecting
        val dbNoInspecting = Database("dbNoInspecting").createInstance(temporaryFolder)
        dbNoInspecting.close()
        assertThat(dbNoInspecting.isOpen).isFalse()

        // with inspecting
        listOf("db2", null).forEach { name ->
            val dbWithInspecting = Database(name).createInstance(temporaryFolder)
            onOpen(dbWithInspecting) // start inspecting
            dbWithInspecting.close()
            assertThat(dbWithInspecting.isOpen).isTrue()
        }
    }

    @Test
    fun test_temporary_databases_same_path_different_database() {
        // given
        val db1 = Database(null).createInstance(temporaryFolder)
        val db2 = Database(null).createInstance(temporaryFolder)
        fun queryTableCount(db: SQLiteDatabase): Long =
            db.compileStatement("select count(*) from sqlite_master").simpleQueryForLong()
        assertThat(queryTableCount(db1)).isEqualTo(1) // android_metadata sole table
        assertThat(queryTableCount(db2)).isEqualTo(1) // android_metadata sole table
        assertThat(db1.path).isEqualTo(db2.path)
        assertThat(db1.path).isEqualTo(":memory:")

        // when
        db1.execSQL("create table t1 (c1 int)")

        // then
        assertThat(queryTableCount(db1)).isEqualTo(2)
        assertThat(queryTableCount(db2)).isEqualTo(1)
    }
}
