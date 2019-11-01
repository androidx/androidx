/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.sqlite.inspection

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorEnvironment.EntryHook
import androidx.inspection.InspectorEnvironment.ExitHook
import androidx.inspection.testing.InspectorTester
import androidx.sqlite.inspection.SqliteInspectorProtocol.Command
import androidx.sqlite.inspection.SqliteInspectorProtocol.Response
import androidx.sqlite.inspection.SqliteInspectorProtocol.TrackDatabasesCommand
import androidx.sqlite.inspection.SqliteInspectorProtocol.TrackDatabasesResponse
import androidx.sqlite.inspection.SqliteInspectorTest.FakeInspectorEnvironment.RegisterHookEntry.EntryHookEntry
import androidx.sqlite.inspection.SqliteInspectorTest.FakeInspectorEnvironment.RegisterHookEntry.ExitHookEntry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class SqliteInspectorTest {
    @get:Rule
    val tempDirectory = TemporaryFolder()

    @Test
    fun test_basic_proto() {
        val command = createTrackDatabasesCommand()

        val commandBytes = command.toByteArray()
        assertThat(commandBytes).isNotEmpty()

        val commandBack = Command.parseFrom(commandBytes)
        assertThat(commandBack).isEqualTo(command)
    }

    @Test
    fun test_basic_inject() = runBlocking {
        val inspectorTester = InspectorTester(SqliteInspectorFactory.SQLITE_INSPECTOR_ID)
        // no crash means the inspector was successfully injected
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        inspectorTester.dispose()
    }

    @Test
    fun test_track_databases() = runBlocking {
        val environment = FakeInspectorEnvironment()
        val inspectorTester =
            InspectorTester(SqliteInspectorFactory.SQLITE_INSPECTOR_ID, environment)
        assertThat(environment.consumeRegisteredHooks()).isEmpty()

        // evaluate response
        inspectorTester.sendCommand(createTrackDatabasesCommand().toByteArray())
            .let { responseBytes ->
                assertThat(responseBytes).isNotEmpty()
                val response = Response.parseFrom(responseBytes)
                assertThat(response).isEqualTo(createTrackDatabasesResponse())
            }

        // evaluate registered hooks
        val hookEntries = environment.consumeRegisteredHooks()
        assertThat(hookEntries).hasSize(1)
        hookEntries.first().let { entry ->
            // expect one exit hook tracking database open events
            assertThat(entry).isInstanceOf(ExitHookEntry::class.java)
            assertThat(entry.originClass.name).isEqualTo(SQLiteDatabase::class.java.name)
            assertThat(entry.originMethod).isEqualTo(SqliteInspector.sOpenDatabaseCommandSignature)

            // verify that executing the registered hook will result in tracking events
            assertThat(inspectorTester.channel.isEmpty).isTrue()
            @Suppress("UNCHECKED_CAST")
            val exitHook = (entry as ExitHookEntry).exitHook as ExitHook<SQLiteDatabase>
            val database = createDatabase("db")
            assertThat(exitHook.onExit(database)).isSameInstanceAs(database)
            inspectorTester.channel.receive().let { responseBytes ->
                assertThat(responseBytes).isNotEmpty()
                val response = SqliteInspectorProtocol.Event.parseFrom(responseBytes)
                assertThat(response.hasDatabaseOpened()).isTrue()
                assertThat(response.databaseOpened.name).isEqualTo(database.path)
            }
        }

        // TODO: verify already-open-database events sent
        assertThat(environment.consumeRegisteredHooks()).isEmpty()
        inspectorTester.dispose()
    }

    private fun createTrackDatabasesCommand(): Command =
        Command.newBuilder().setTrackDatabases(TrackDatabasesCommand.getDefaultInstance()).build()

    private fun createTrackDatabasesResponse(): Response =
        Response.newBuilder().setTrackDatabases(TrackDatabasesResponse.getDefaultInstance()).build()

    private fun createDatabase(databaseName: String): SQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext() as android.content.Context
        val path = tempDirectory.newFile(databaseName).absolutePath
        val openHelper = object : SQLiteOpenHelper(context, path, null, 1) {
            override fun onCreate(db: SQLiteDatabase?) = Unit
            override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit
        }
        return openHelper.readableDatabase
    }

    /**
     * Fake inspector environment with the following behaviour:
     * - [findInstances] returns pre-registered values from [registerInstancesToFind].
     * - [registerEntryHook] and [registerExitHook] record the calls which can later be
     * retrieved in [consumeRegisteredHooks].
     */
    private class FakeInspectorEnvironment : InspectorEnvironment {
        private val instancesToFind = mutableListOf<Any>()
        private val registeredHooks = mutableListOf<RegisterHookEntry>()

        fun registerInstancesToFind(instances: List<Any>) {
            instancesToFind.addAll(instances)
        }

        fun unregisterInstancesToFind(instances: List<Any>) {
            instancesToFind.removeAll(instances)
        }

        /**
         *  Returns instances pre-registered in [registerInstancesToFind].
         *  By design crashes in case of the wrong setup - indicating an issue with test code.
         */
        @Suppress("UNCHECKED_CAST")
        // TODO: implement actual findInstances behaviour
        override fun <T : Any?> findInstances(clazz: Class<T>): MutableList<T> =
            instancesToFind.map { it as T }.toMutableList()

        override fun registerEntryHook(
            originClass: Class<*>,
            originMethod: String,
            entryHook: EntryHook
        ) {
            // TODO: implement actual registerEntryHook behaviour
            registeredHooks.add(EntryHookEntry(originClass, originMethod, entryHook))
        }

        override fun <T : Any?> registerExitHook(
            originClass: Class<*>,
            originMethod: String,
            exitHook: ExitHook<T>
        ) {
            // TODO: implement actual registerExitHook behaviour
            registeredHooks.add(ExitHookEntry(originClass, originMethod, exitHook))
        }

        fun consumeRegisteredHooks(): List<RegisterHookEntry> =
            registeredHooks.toList().also {
                registeredHooks.clear()
            }

        sealed class RegisterHookEntry(val originClass: Class<*>, val originMethod: String) {
            class ExitHookEntry(
                originClass: Class<*>,
                originMethod: String,
                val exitHook: ExitHook<*>
            ) : RegisterHookEntry(originClass, originMethod)

            class EntryHookEntry(
                originClass: Class<*>,
                originMethod: String,
                val entryHook: EntryHook
            ) : RegisterHookEntry(originClass, originMethod)
        }
    }
}
