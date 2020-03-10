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
import androidx.sqlite.inspection.test.MessageFactory.createTrackDatabasesCommand
import androidx.sqlite.inspection.test.MessageFactory.createTrackDatabasesResponse
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@ExperimentalCoroutinesApi
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
                assertThat(actual[ix].name).isEqualTo(expected[ix].path)
            }
        }

        // evaluate registered hooks
        val hookEntries = testEnvironment.consumeRegisteredHooks()
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
            val exitHook = (entry as Hook.ExitHook).exitHook as
                    ExitHook<SQLiteDatabase>
            val database = Database("db3").createInstance(temporaryFolder)
            assertThat(exitHook.onExit(database)).isSameInstanceAs(database)
            testEnvironment.receiveEvent().let { event ->
                assertThat(event.databaseOpened.name).isEqualTo(database.path)
            }
        }

        assertThat(testEnvironment.consumeRegisteredHooks()).isEmpty()
    }

    @Test
    fun test_track_databases_the_same_database_opened_twice() = runBlocking {
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        val hooks = testEnvironment.consumeRegisteredHooks()
        assertThat(hooks).hasSize(1)

        val onOpenHook = hooks.first()
        assertThat(onOpenHook.originMethod).isEqualTo(OPEN_DATABASE_COMMAND_SIGNATURE)
        val database = Database("db").createInstance(temporaryFolder)
        @Suppress("UNCHECKED_CAST")
        val onExit = ((onOpenHook as Hook.ExitHook).exitHook as ExitHook<SQLiteDatabase>)::onExit

        // open event on a database first time
        onExit(database)
        testEnvironment.receiveEvent()
            .let { event -> assertThat(event.hasDatabaseOpened()).isEqualTo(true) }

        // open event on the same database for the second time
        // TODO: track close database events or handle the below gracefully
        onExit(database)
        testEnvironment.receiveEvent().let { event ->
            assertThat(event.hasErrorOccurred()).isEqualTo(true)
            val error = event.errorOccurred.content
            assertThat(error.message).contains("Database is already tracked")
            assertThat(error.message).contains(database.path)
            assertThat(error.isRecoverable).isEqualTo(false)
        }
    }
}
