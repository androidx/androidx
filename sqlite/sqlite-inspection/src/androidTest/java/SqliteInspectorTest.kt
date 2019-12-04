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
import androidx.sqlite.inspection.SqliteInspectorProtocol.Event
import androidx.sqlite.inspection.SqliteInspectorProtocol.GetSchemaCommand
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
import java.util.Collections.singletonList

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
        // prepare test environment
        val environment = FakeInspectorEnvironment()
        val inspectorTester =
            InspectorTester(SqliteInspectorFactory.SQLITE_INSPECTOR_ID, environment)
        // prepare test environment: registered hooks
        assertThat(environment.consumeRegisteredHooks()).isEmpty()
        // prepare test environment: register 'already open' instances
        val alreadyOpenInstances = listOf(createDatabase("db1"), createDatabase("db2"))
        environment.registerInstancesToFind(alreadyOpenInstances)

        // send request and evaluate response
        inspectorTester.sendCommand(createTrackDatabasesCommand().toByteArray())
            .let { responseBytes ->
                assertThat(responseBytes).isNotEmpty()
                val response = Response.parseFrom(responseBytes)
                assertThat(response).isEqualTo(createTrackDatabasesResponse())
            }

        // evaluate 'already-open' instances are found
        alreadyOpenInstances.let { expected ->
            val actual = expected.indices.map {
                Event.parseFrom(inspectorTester.channel.receive()).databaseOpened
            }
            assertThat(inspectorTester.channel.isEmpty).isTrue()
            assertThat(actual.map { it.id }.distinct()).hasSize(expected.size)
            expected.forEachIndexed { ix, _ ->
                assertThat(actual[ix].name).isEqualTo(expected[ix].path)
            }
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
            val database = createDatabase("db3")
            assertThat(exitHook.onExit(database)).isSameInstanceAs(database)
            inspectorTester.channel.receive().let { responseBytes ->
                assertThat(responseBytes).isNotEmpty()
                val response = Event.parseFrom(responseBytes)
                assertThat(response.hasDatabaseOpened()).isTrue()
                assertThat(response.databaseOpened.name).isEqualTo(database.path)
            }
        }

        assertThat(environment.consumeRegisteredHooks()).isEmpty()
        inspectorTester.dispose()
    }

    @Test
    fun test_get_schema() = runBlocking {
        // prepare test environment
        val expectedColumns1 = listOf(
            "t" to "TEXT",
            "nu" to "NUMERIC",
            "i" to "INTEGER",
            "r" to "REAL",
            "b" to "BLOB"
        )
        val expectedColumns2 = listOf(
            "id" to "INTEGER",
            "name" to "TEXT"
        )
        val alreadyOpenInstances = singletonList(
            createDatabase(
                "db1",
                "CREATE TABLE t1(${expectedColumns1.foldCommaSeparated()});",
                "CREATE TABLE t2 (${expectedColumns2.foldCommaSeparated()}, " +
                        "PRIMARY KEY(${expectedColumns2.map { it.first }.joinToString { it }}));"
            )
        )
        val environment = FakeInspectorEnvironment()
        environment.registerInstancesToFind(alreadyOpenInstances)
        val inspectorTester =
            InspectorTester(SqliteInspectorFactory.SQLITE_INSPECTOR_ID, environment)

        // get id of the database from track databases event
        inspectorTester.sendCommand(createTrackDatabasesCommand().toByteArray())
        val databaseOpenedEvent = Event.parseFrom(inspectorTester.channel.receive()).databaseOpened

        // query schema and validate the response
        inspectorTester.sendCommand(
            createGetSchemaCommand(databaseOpenedEvent.id).toByteArray()
        ).let { response ->
            val tables =
                Response.parseFrom(response).getSchema.tablesList.sortedBy { it.name }
            assertThat(tables).hasSize(2)
            val table1 = tables[0]
            val table2 = tables[1]
            val actualColumns1 = table1.columnsList.sortedBy { it.name }
            val actualColumns2 = table2.columnsList.sortedBy { it.name }

            assertThat(table1.name).isEqualTo("t1")
            assertThat(table2.name).isEqualTo("t2")

            assertThat(actualColumns1).hasSize(expectedColumns1.size)
            assertThat(actualColumns2).hasSize(expectedColumns2.size)

            expectedColumns1.sortedBy { it.first }.forEachIndexed { ix, (name, type) ->
                assertThat(actualColumns1[ix].name).isEqualTo(name)
                assertThat(actualColumns1[ix].type).isEqualTo(type)
            }

            expectedColumns2.sortedBy { it.first }.forEachIndexed { ix, (name, type) ->
                assertThat(actualColumns2[ix].name).isEqualTo(name)
                assertThat(actualColumns2[ix].type).isEqualTo(type)
            }
        }

        inspectorTester.dispose()
    }

    private fun List<Pair<String, String>>.foldCommaSeparated(): String =
        joinToString { (a, b) -> "$a $b" }

    private fun createTrackDatabasesCommand(): Command =
        Command.newBuilder().setTrackDatabases(TrackDatabasesCommand.getDefaultInstance()).build()

    private fun createTrackDatabasesResponse(): Response =
        Response.newBuilder().setTrackDatabases(TrackDatabasesResponse.getDefaultInstance()).build()

    private fun createGetSchemaCommand(databaseId: Int): Command {
        return Command.newBuilder().setGetSchema(
            GetSchemaCommand.newBuilder().setId(databaseId).build()
        ).build()
    }

    private fun createDatabase(
        databaseName: String,
        vararg queries: String
    ): SQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext() as android.content.Context
        val path = tempDirectory.newFile(databaseName).absolutePath
        val openHelper = object : SQLiteOpenHelper(context, path, null, 1) {
            override fun onCreate(db: SQLiteDatabase?) = queries.forEach { db!!.execSQL(it) }
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
