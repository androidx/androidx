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

import androidx.sqlite.inspection.SqliteInspectorProtocol
import androidx.sqlite.inspection.test.MessageFactory.createQueryTableCommand
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
class QueryTest {
    @get:Rule
    val testEnvironment = SqliteInspectorTestEnvironment()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun test_query() = runBlocking {
        // TODO: add tests for invalid queries
        // TODO: add tests spanning multiple tables (e.g. union of two queries)
        // TODO: split into smaller tests

        // given
        val table1 = Table(
            "table1",
            Column("t", "TEXT"),
            Column("nu", "NUMERIC"),
            Column("i", "INTEGER"),
            Column("r", "REAL"),
            Column("b", "BLOB")
        )

        val table2 = Table(
            "table2",
            Column("id", "INTEGER"),
            Column("name", "TEXT")
        )

        val database = Database("db1", table1, table2)
        val databaseInstance = database.createInstance(temporaryFolder).apply {
            insertValues(table1, *repeat5("'500.0'"))
            insertValues(table1, *repeat5("500.0"))
            insertValues(table1, *repeat5("500"))
            insertValues(table1, *repeat5("x'0500'"))
            insertValues(table1, *repeat5("NULL"))

            insertValues(table2, "1", "'A'")
            insertValues(table2, "2", "'B'")
            insertValues(table2, "3", "'C'")
            insertValues(table2, "4", "'D'")
        }

        testEnvironment.registerAlreadyOpenDatabases(
            listOf(
                Database("ignored_1").createInstance(temporaryFolder),
                databaseInstance,
                Database("ignored_2").createInstance(temporaryFolder)
            )
        )
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        val databaseId = testEnvironment.awaitDatabaseOpenedEvent(database.name).databaseId

        val query = "select * from ${table1.name}"

        val expectedValues = listOf(
            listOf("500.0", 500, 500, 500.0f, "500.0"), // text|integer|integer|float|text
            listOf("500.0", 500, 500, 500.0f, 500.0f), // text|integer|integer|float|float
            listOf("500", 500, 500, 500.0f, 500), // text|integer|integer|float|integer
            listOf(*repeat5(arrayOf<Any?>(5.toByte(), 0.toByte()))), // blob|blob|blob|blob|blob
            listOf(*repeat5<Any?>(null)) // null|null|null|null|null
        )

        val expectedTypes = listOf(
            listOf("text", "integer", "integer", "float", "text"),
            listOf("text", "integer", "integer", "float", "float"),
            listOf("text", "integer", "integer", "float", "integer"),
            listOf("blob", "blob", "blob", "blob", "blob"),
            listOf("null", "null", "null", "null", "null")
        )

        // when
        testEnvironment.sendCommand(createQueryTableCommand(databaseId, query)).let { response ->
            // then
            response.query.rowsList.let { actualRows: List<SqliteInspectorProtocol.Row> ->
                actualRows.forEachIndexed { rowIx, row ->
                    row.valuesList.forEachIndexed { colIx, cell ->
                        assertThat(cell.value).isEqualTo(expectedValues[rowIx][colIx])
                        assertThat(cell.type).isEqualTo(expectedTypes[rowIx][colIx])
                        assertThat(cell.columnName).isEqualTo(table1.columns[colIx].name)
                    }
                }
            }
        }
    }

    private inline fun <reified T> repeatN(value: T, n: Int): Array<T> =
        (0 until n).map { value }.toTypedArray()

    private inline fun <reified T> repeat5(v: T) = repeatN(v, 5)
}
