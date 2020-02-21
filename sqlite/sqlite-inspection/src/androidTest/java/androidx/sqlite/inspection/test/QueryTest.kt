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
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryResponse
import androidx.sqlite.inspection.SqliteInspectorProtocol.Row
import androidx.sqlite.inspection.test.MessageFactory.createGetSchemaCommand
import androidx.sqlite.inspection.test.MessageFactory.createQueryCommand
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
            Database("db1", table1, table2),
            values,
            query,
            expectedValues,
            expectedTypes,
            expectedColumnNames
        )
    }

    @Test
    fun test_nested_query_with_a_comment() {
        test_valid_query(
            Database("db", table2),
            values = listOf(
                table2 to arrayOf("1", "'A'"),
                table2 to arrayOf("2", "'B'"),
                table2 to arrayOf("3", "'C'")
            ),
            query = "select count(*) from (select * from table2 /* comment */)",
            expectedValues = listOf(listOf(3)),
            expectedTypes = listOf(listOf("integer")),
            expectedColumnNames = listOf("count(*)")
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
            Database("db1", table1, table2),
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
        val databaseId = inspectDatabase(databaseInstance)

        // when
        issueQuery(databaseId, query).let { response ->
            // then
            response.rowsList.let { actualRows: List<Row> ->
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

    @Test
    fun test_create_table() = runBlocking {
        // given
        val database = Database("db1", table1, table2)
        val databaseId = inspectDatabase(database.createInstance(temporaryFolder))
        val initialTotalChanges = queryTotalChanges(databaseId)

        // when
        val newTable = Table("t", Column("c", "NUMBER"))
        issueQuery(databaseId, newTable.toCreateString())

        // then
        assertThat(querySchema(databaseId)).isEqualTo(database.tables + newTable)
        assertThat(queryTotalChanges(databaseId)).isEqualTo(initialTotalChanges) // note no diff
    }

    @Test
    fun test_drop_table() = runBlocking {
        // given
        val database = Database("db1", table1, table2)
        val databaseId = inspectDatabase(database.createInstance(temporaryFolder).also {
            it.insertValues(table2, "1", "'1'")
            it.insertValues(table2, "2", "'2'")
        })
        val initialTotalChanges = queryTotalChanges(databaseId)
        assertThat(querySchema(databaseId)).isNotEmpty()

        // when
        database.tables.forEach { t -> issueQuery(databaseId, "drop table ${t.name}") }

        // then
        assertThat(querySchema(databaseId)).isEmpty()
        assertThat(queryTotalChanges(databaseId)).isEqualTo(initialTotalChanges) // note no diff
    }

    @Test
    fun test_alter_table() = runBlocking {
        // given
        val table = table2
        val databaseId = inspectDatabase(Database("db", table).createInstance(temporaryFolder))
        val initialTotalChanges = queryTotalChanges(databaseId)
        val newColumn = Column("num", "NUM")

        // when
        issueQuery(databaseId, "alter table ${table.name} add ${newColumn.name} ${newColumn.type}")

        // then
        assertThat(querySchema(databaseId)).isEqualTo(
            listOf(Table(table.name, table.columns + newColumn))
        )
        assertThat(queryTotalChanges(databaseId)).isEqualTo(initialTotalChanges) // note no diff
    }

    @Test
    fun test_insert_update_delete_changes() = runBlocking {
        // given
        val table = table2
        val databaseId = inspectDatabase(Database("db", table).createInstance(temporaryFolder))
        var expectedTotalChanges = 1 // TODO: investigate why 1 and not 0
        assertThat(queryChanges(databaseId)).isEqualTo(1) // TODO: investigate why 1 and not 0
        assertThat(queryChanges(databaseId)).isEqualTo(1) // note querying doesn't reset it
        assertThat(queryTotalChanges(databaseId)).isEqualTo(expectedTotalChanges)

        val newValue = listOf("1", "a")
        val insertQuery = "insert into ${table.name} values (${newValue.joinToString { "'$it'" }})"
        val insertCount = 5

        // when (insert)
        repeat(insertCount) {
            issueQuery(databaseId, insertQuery)
            expectedTotalChanges++
            assertThat(queryChanges(databaseId)).isEqualTo(1)
            assertThat(queryTotalChanges(databaseId)).isEqualTo(expectedTotalChanges)
        }

        // then (insert)
        issueQuery(databaseId, "select * from ${table.name}").let { response ->
            assertThat(response.rowsList).hasSize(insertCount)
            response.rowsList.forEach { row ->
                assertThat(row.valuesList.map { it.value.toString() }).isEqualTo(newValue)
            }
        }
        assertThat(queryChanges(databaseId)).isEqualTo(1) // note select doesn't reset it
        assertThat(queryTotalChanges(databaseId)).isEqualTo(expectedTotalChanges)

        // when (update)
        issueQuery(databaseId, "update ${table.name} set id = 2 ")
        expectedTotalChanges += insertCount // all inserted rows are affected

        // then (update)
        assertThat(queryChanges(databaseId)).isEqualTo(insertCount)
        assertThat(queryTotalChanges(databaseId)).isEqualTo(expectedTotalChanges)

        // when (delete)
        issueQuery(databaseId, "delete from ${table.name}")
        expectedTotalChanges += insertCount // all inserted rows are affected

        // then (delete)
        assertThat(queryChanges(databaseId)).isEqualTo(insertCount)
        assertThat(queryTotalChanges(databaseId)).isEqualTo(expectedTotalChanges)
    }

    private suspend fun inspectDatabase(databaseInstance: SQLiteDatabase): Int {
        testEnvironment.registerAlreadyOpenDatabases(
            listOf(
                Database("ignored_1").createInstance(temporaryFolder), // extra testing value
                databaseInstance,
                Database("ignored_2").createInstance(temporaryFolder) // extra testing value
            )
        )
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        return testEnvironment.awaitDatabaseOpenedEvent(databaseInstance.path).databaseId
    }

    private suspend fun issueQuery(databaseId: Int, command: String): QueryResponse =
        testEnvironment.sendCommand(createQueryCommand(databaseId, command)).query

    private suspend fun querySchema(databaseId: Int): List<Table> =
        testEnvironment.sendCommand(createGetSchemaCommand(databaseId)).getSchema.toTableList()

    private suspend fun queryChanges(databaseId: Int, useTotal: Boolean = false): Int {
        val criterion = if (useTotal) "total_changes" else "changes"
        return issueQuery(databaseId, "select $criterion()").rowsList.let { response ->
            assertThat(response).hasSize(1)
            assertThat(response.first().valuesList).hasSize(1)
            val cell = response.first().getValues(0)
            assertThat(cell.type).isEqualTo("integer")
            cell.value as Int
        }
    }

    private suspend fun queryTotalChanges(databaseId: Int): Int = queryChanges(databaseId, true)

    private inline fun <reified T> repeatN(value: T, n: Int): Array<T> =
        (0 until n).map { value }.toTypedArray()

    private inline fun <reified T> repeat5(v: T) = repeatN(v, 5)
}
