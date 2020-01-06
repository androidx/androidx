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

import androidx.sqlite.inspection.test.MessageFactory.createGetSchemaCommand
import androidx.sqlite.inspection.test.MessageFactory.createTrackDatabasesCommand
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
class GetSchemaTest {
    @get:Rule
    val testEnvironment = SqliteInspectorTestEnvironment()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun test_get_schema_complex_tables() {
        test_get_schema(
            listOf(
                Database(
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
            )
        )
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
        assertThat(alreadyOpenDatabases).isNotEmpty() // sanity check

        testEnvironment.registerAlreadyOpenDatabases(alreadyOpenDatabases.map {
            it.createInstance(temporaryFolder)
        })
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        val databaseConnections =
            alreadyOpenDatabases.indices.map { testEnvironment.receiveEvent().databaseOpened }

        val schemas =
            databaseConnections
                .sortedBy { it.name }
                .map {
                    testEnvironment.sendCommand(createGetSchemaCommand(it.databaseId)).getSchema
                }

        alreadyOpenDatabases
            .sortedBy { it.name }
            .zipSameSize(schemas)
            .forEach { (expectedSchema, actualSchema) ->
                val expectedTables = expectedSchema.tables.sortedBy { it.name }
                val actualTables = actualSchema.tablesList.sortedBy { it.name }

                expectedTables
                    .zipSameSize(actualTables)
                    .forEach { (expectedTable, actualTable) ->
                        assertThat(actualTable.name).isEqualTo(expectedTable.name)

                        val expectedColumns = expectedTable.columns.sortedBy { it.name }
                        val actualColumns = actualTable.columnsList.sortedBy { it.name }

                        expectedColumns
                            .zipSameSize(actualColumns)
                            .forEach { (expectedColumn, actualColumn) ->
                                assertThat(actualColumn.name).isEqualTo(expectedColumn.name)
                                assertThat(actualColumn.type).isEqualTo(expectedColumn.type)
                            }
                    }
            }
    }

    /** Same as [List.zip] but ensures both lists are the same size. */
    private fun <A, B> List<A>.zipSameSize(other: List<B>): List<Pair<A, B>> {
        assertThat(this.size).isEqualTo(other.size)
        return this.zip(other)
    }
}
