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
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
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
// TODO: add tests for invalid queries: union of unequal number of columns, syntax error, etc.
class QueryTest {
    @get:Rule
    val testEnvironment = SqliteInspectorTestEnvironment()

    @get:Rule
    val temporaryFolder = TemporaryFolder(getInstrumentation().context.cacheDir)

    private val table1: Table = Table(
        "table1",
        Column("t", "TEXT"),
        Column("nu", "NUMERIC"),
        Column("i", "INTEGER"),
        Column("r", "REAL"),
        Column("b", "BLOB")
    )

    private val table2: Table = Table(
        "table2",
        Column("id", "INTEGER"),
        Column("name", "TEXT")
    )

    private val database = Database("db1", table1, table2)

    /** Query verifying type affinity behaviour */
    @Test
    fun test_valid_query_type_affinity_cases() {
        val values = listOf(
            table1 to repeat5("'abc'"),
            table1 to repeat5("'500.0'"),
            table1 to repeat5("500.0"),
            table1 to repeat5("500"),
            table1 to repeat5("x'0500'"),
            table1 to repeat5("NULL"),

            table2 to arrayOf("1", "'A'"),
            table2 to arrayOf("2", "'B'"),
            table2 to arrayOf("3", "'C'"),
            table2 to arrayOf("4", "'D'")
        )

        val query = "select * from ${table1.name}"

        val expectedValues = listOf(
            repeat5("abc").toList(),
            listOf("500.0", 500, 500, 500.0f, "500.0"), // text|integer|integer|float|text
            listOf("500.0", 500, 500, 500.0f, 500.0f), // text|integer|integer|float|float
            listOf("500", 500, 500, 500.0f, 500), // text|integer|integer|float|integer
            listOf(*repeat5(arrayOf<Any?>(5.toByte(), 0.toByte()))), // blob|blob|blob|blob|blob
            listOf(*repeat5<Any?>(null)) // null|null|null|null|null
        )

        val expectedTypes = listOf(
            repeat5("text").toList(),
            listOf("text", "integer", "integer", "float", "text"),
            listOf("text", "integer", "integer", "float", "float"),
            listOf("text", "integer", "integer", "float", "integer"),
            listOf("blob", "blob", "blob", "blob", "blob"),
            listOf("null", "null", "null", "null", "null")
        )

        val expectedColumnNames = table1.columns.map { it.name }

        test_valid_query(
            database,
            values,
            query,
            expectedValues,
            expectedTypes,
            expectedColumnNames
        )
    }

    /** Union of two queries (different column names) resulting in using first query columns. */
    @Test
    fun test_valid_query_two_table_union() {
        val values = listOf(
            table1 to repeat5("'abc'"),
            table1 to repeat5("'xyz'"),
            table2 to arrayOf("1", "'A'"),
            table2 to arrayOf("2", "'B'")
        )

        // query construction
        val columns1 = table1.columns.take(2).map { it.name }
        val columns2 = table2.columns.take(2).map { it.name }
        val column11 = columns1.first()
        val column12 = columns1.last()
        val column21 = columns2.first()
        val column22 = columns2.last()
        val query1 = "select $column11, $column12 from ${table1.name} where $column11 is 'abc'"
        val query2 = "select $column21, $column22 from ${table2.name} where $column22 is 'B'"
        val query = "$query1 union $query2"

        val expectedValues = listOf(
            listOf(2, "B"),
            listOf("abc", "abc")
        )

        val expectedTypes = listOf(
            listOf("integer", "text"),
            listOf("text", "text")
        )

        test_valid_query(
            database,
            values,
            query,
            expectedValues,
            expectedTypes,
            expectedColumnNames = columns1
        )
    }

    private fun test_valid_query(
        database: Database,
        values: List<Pair<Table, Array<String>>>,
        query: String,
        expectedValues: List<List<Any?>>,
        expectedTypes: List<List<String>>,
        expectedColumnNames: List<String>
    ) = runBlocking {
        // given
        val databaseInstance = database.createInstance(temporaryFolder)
        values.forEach { (table, values) -> databaseInstance.insertValues(table, *values) }

        testEnvironment.registerAlreadyOpenDatabases(
            listOf(
                Database("ignored_1").createInstance(temporaryFolder),
                databaseInstance,
                Database("ignored_2").createInstance(temporaryFolder)
            )
        )
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        val databaseId = testEnvironment.awaitDatabaseOpenedEvent(database.name).databaseId

        // when
        testEnvironment.sendCommand(createQueryTableCommand(databaseId, query)).let { response ->
            // then
            response.query.rowsList.let { actualRows: List<SqliteInspectorProtocol.Row> ->
                actualRows.forEachIndexed { rowIx, row ->
                    row.valuesList.forEachIndexed { colIx, cell ->
                        assertThat(cell.value).isEqualTo(expectedValues[rowIx][colIx])
                        assertThat(cell.type).isEqualTo(expectedTypes[rowIx][colIx])
                        assertThat(cell.columnName).isEqualTo(expectedColumnNames[colIx])
                    }
                }
            }
        }
    }

    private inline fun <reified T> repeatN(value: T, n: Int): Array<T> =
        (0 until n).map { value }.toTypedArray()

    private inline fun <reified T> repeat5(v: T) = repeatN(v, 5)
}
