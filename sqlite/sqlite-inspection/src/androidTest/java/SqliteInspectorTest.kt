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
import androidx.sqlite.inspection.SqliteInspectorTest.MessageFactory.createTrackDatabasesCommand
import androidx.sqlite.inspection.SqliteInspectorTest.MessageFactory.createTrackDatabasesResponse
import androidx.sqlite.inspection.SqliteInspectorTest.MessageFactory.createGetSchemaCommand
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
        TestEnvironment.setUp().run {
            // no crash means the inspector was successfully injected
            assertNoQueuedEvents()
            dispose()
        }
    }

    @Test
    fun test_track_databases() = runBlocking {
        val alreadyOpenDatabases = listOf(Database("db1"), Database("db2"))

        TestEnvironment.setUp(alreadyOpenDatabases).run {
            sendCommand(createTrackDatabasesCommand()).let { response ->
                assertThat(response).isEqualTo(createTrackDatabasesResponse())
            }

            // evaluate 'already-open' instances are found
            alreadyOpenDatabases.let { expected ->
                val actual = expected.indices.map { receiveEvent().databaseOpened }
                assertNoQueuedEvents()
                assertThat(actual.map { it.id }.distinct()).hasSize(expected.size)
                expected.forEachIndexed { ix, _ ->
                    assertThat(actual[ix].name).isEqualTo(expected[ix].path)
                }
            }

            // evaluate registered hooks
            val hookEntries = consumeRegisteredHooks()
            assertThat(hookEntries).hasSize(1)
            hookEntries.first().let { entry ->
                // expect one exit hook tracking database open events
                assertThat(entry).isInstanceOf(ExitHookEntry::class.java)
                assertThat(entry.originClass.name).isEqualTo(SQLiteDatabase::class.java.name)
                assertThat(entry.originMethod)
                    .isEqualTo(SqliteInspector.sOpenDatabaseCommandSignature)

                // verify that executing the registered hook will result in tracking events
                assertNoQueuedEvents()
                @Suppress("UNCHECKED_CAST")
                val exitHook = (entry as ExitHookEntry).exitHook as ExitHook<SQLiteDatabase>
                val database = Database("db3").createInstance()
                assertThat(exitHook.onExit(database)).isSameInstanceAs(database)
                receiveEvent().let { event ->
                    assertThat(event.databaseOpened.name).isEqualTo(database.path)
                }
            }

            assertThat(consumeRegisteredHooks()).isEmpty()
            dispose()
        }
    }

    @Test
    fun test_get_schema_complex_tables() {
        // prepare test environment
        val database = Database(
            "db1",
            Table(
                "table1",
                Column("t", "TEXT"),
                Column("nu", "NUMERIC"),
                Column("i", "INTEGER"),
                Column("r", "REAL"),
                Column("b", "BLOB")
            ),
            Table(
                "table2",
                Column("id", "INTEGER"),
                Column("name", "TEXT")

            )
        )

        test_get_schema(listOf(database))
    }

    @Test
    fun test_get_schema_multiple_databases() {
        test_get_schema(
            listOf(
                Database("db3", Table("t3", Column("c3", "BLOB"))),
                Database("db2", Table("t2", Column("c2", "TEXT"))),
                Database("db1", Table("t1", Column("c1", "TEXT")))
            )
        )
    }

    private fun test_get_schema(alreadyOpenDatabases: List<Database>) = runBlocking {
        assertThat(alreadyOpenDatabases).isNotEmpty()

        // prepare test environment
        TestEnvironment.setUp(alreadyOpenDatabases).run {
            sendCommand(createTrackDatabasesCommand())
            val databaseConnections =
                alreadyOpenDatabases.indices.map { receiveEvent().databaseOpened }

            val schemas =
                databaseConnections
                    .sortedBy { it.name }
                    .map { sendCommand(createGetSchemaCommand(it.id)).getSchema }

            alreadyOpenDatabases
                .sortedBy { it.path }
                .forEach2(schemas) { expectedSchema, actualSchema ->
                    val expectedTables = expectedSchema.tables.sortedBy { it.name }
                    val actualTables = actualSchema.tablesList.sortedBy { it.name }

                    expectedTables.forEach2(actualTables) { expectedTable, actualTable ->
                        assertThat(actualTable.name).isEqualTo(expectedTable.name)

                        val expectedColumns = expectedTable.columns.sortedBy { it.name }
                        val actualColumns = actualTable.columnsList.sortedBy { it.name }

                        expectedColumns.forEach2(actualColumns) { expectedColumn, actualColumn ->
                            assertThat(actualColumn.name).isEqualTo(expectedColumn.name)
                            assertThat(actualColumn.type).isEqualTo(expectedColumn.type)
                        }
                }
            }

            dispose()
        }
    }

    private class TestEnvironment private constructor(
        private val inspectorTester: InspectorTester,
        private val environment: FakeInspectorEnvironment
    ) {
        suspend fun sendCommand(command: Command): Response =
            inspectorTester.sendCommand(command.toByteArray())
                .let { responseBytes ->
                    assertThat(responseBytes).isNotEmpty()
                    Response.parseFrom(responseBytes)
                }

        suspend fun receiveEvent(): Event =
            inspectorTester.channel.receive().let { responseBytes ->
                assertThat(responseBytes).isNotEmpty()
                Event.parseFrom(responseBytes)
            }

        fun consumeRegisteredHooks(): List<FakeInspectorEnvironment.RegisterHookEntry> =
            environment.consumeRegisteredHooks()

        fun dispose() {
            assertNoQueuedEvents() // remove if doesn't match future test design
            inspectorTester.dispose()
        }

        fun assertNoQueuedEvents() {
            assertThat(inspectorTester.channel.isEmpty).isTrue()
        }

        /** Iterates over two lists of the same size */
        fun <A, B> List<A>.forEach2(other: List<B>, action: (A, B) -> Unit) {
            assertThat(this.size).isEqualTo(other.size)
            zip(other, action)
        }

        companion object {
            suspend fun setUp(alreadyOpenDatabases: List<Database> = emptyList()): TestEnvironment {
                // prepare test environment
                val environment = FakeInspectorEnvironment()
                val inspectorTester =
                    InspectorTester(SqliteInspectorFactory.SQLITE_INSPECTOR_ID, environment)
                // prepare test environment: registered hooks
                assertThat(environment.consumeRegisteredHooks()).isEmpty()
                // prepare test environment: register 'already open' instances
                environment.registerInstancesToFind(alreadyOpenDatabases.map {
                    it.createInstance()
                })
                return TestEnvironment(inspectorTester, environment)
            }
        }
    }

    private inner class Database(name: String, val tables: List<Table>) {
        constructor(name: String, vararg tables: Table) : this(name, tables.toList())

        val path: String = tempDirectory.newFile(name).absolutePath

        fun createInstance(): SQLiteDatabase =
            createDatabase().also { db -> tables.forEach { t -> db.addTable(t) } }

        private fun createDatabase(): SQLiteDatabase {
            val context = ApplicationProvider.getApplicationContext() as android.content.Context
            val openHelper = object : SQLiteOpenHelper(context, path, null, 1) {
                override fun onCreate(db: SQLiteDatabase?) = Unit
                override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit
            }
            return openHelper.readableDatabase
        }

        private fun SQLiteDatabase.addTable(table: Table) = this.execSQL(table.toCreateString())
    }

    private data class Table(val name: String, val columns: List<Column>) {
        constructor(name: String, vararg columns: Column) : this(name, columns.toList())

        init {
            assertThat(columns).isNotEmpty()
        }

        fun toCreateString(): String =
            columns.joinToString(
                prefix = "CREATE TABLE $name (",
                postfix = ");"
            ) { "${it.name} ${it.type}" }
    }

    private data class Column(val name: String, val type: String)

    private object MessageFactory {
        fun createTrackDatabasesCommand(): Command =
            Command.newBuilder().setTrackDatabases(
                TrackDatabasesCommand.getDefaultInstance()
            ).build()

        fun createTrackDatabasesResponse(): Response =
            Response.newBuilder().setTrackDatabases(
                TrackDatabasesResponse.getDefaultInstance()
            ).build()

        fun createGetSchemaCommand(databaseId: Int): Command {
            return Command.newBuilder().setGetSchema(
                GetSchemaCommand.newBuilder().setId(databaseId).build()
            ).build()
        }
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
                @Suppress("unused") val entryHook: EntryHook
            ) : RegisterHookEntry(originClass, originMethod)
        }
    }
}
