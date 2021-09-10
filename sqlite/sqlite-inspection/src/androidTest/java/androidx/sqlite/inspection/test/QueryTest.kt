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

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.inspection.SqliteInspectorProtocol.CellValue
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_ISSUE_WITH_PROCESSING_QUERY_VALUE
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_NO_OPEN_DATABASE_WITH_REQUESTED_ID_VALUE
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryResponse
import androidx.sqlite.inspection.SqliteInspectorProtocol.Row
import androidx.sqlite.inspection.test.MessageFactory.createGetSchemaCommand
import androidx.sqlite.inspection.test.MessageFactory.createQueryCommand
import androidx.sqlite.inspection.test.MessageFactory.createTrackDatabasesCommand
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.ComparisonFailure
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
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
            listOf("500.0", 500, 500, 500.0, "500.0"), // text|integer|integer|float|text
            listOf("500.0", 500, 500, 500.0, 500.0), // text|integer|integer|float|float
            listOf("500", 500, 500, 500.0, 500), // text|integer|integer|float|integer
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
    fun test_error_wrong_database_id() = runBlocking {
        val databaseId = 123456789
        val command = "select * from sqlite_master"
        val queryParams = null
        testEnvironment.sendCommand(createQueryCommand(databaseId, command, queryParams))
            .let { response ->
                assertThat(response.hasErrorOccurred()).isEqualTo(true)
                val error = response.errorOccurred.content
                assertThat(error.message).contains(
                    "Unable to perform an operation on database (id=$databaseId)."
                )
                assertThat(error.message).contains("The database may have already been closed.")
                assertThat(error.stackTrace).isEqualTo("")
                assertThat(error.recoverability.isRecoverable).isEqualTo(true)
                assertThat(error.errorCodeValue).isEqualTo(
                    ERROR_NO_OPEN_DATABASE_WITH_REQUESTED_ID_VALUE
                )
            }
    }

    @Test
    fun test_error_invalid_query() = runBlocking {
        val databaseId = inspectDatabase(Database("db", table2).createInstance(temporaryFolder))
        val mistypedSelect = "selecttt"
        val command = "$mistypedSelect * from sqlite_master"
        val queryParams = null
        val response =
            testEnvironment.sendCommand(createQueryCommand(databaseId, command, queryParams))
        assertThat(response.hasErrorOccurred()).isEqualTo(true)
        val error = response.errorOccurred.content
        assertThat(error.message).contains("syntax error")
        assertThat(error.message).contains("near \"$mistypedSelect\"")
        assertThat(error.message).contains("while compiling: $command")
        assertThat(error.stackTrace).contains("SQLiteConnection.nativePrepareStatement")
        assertThat(error.stackTrace).contains("SQLiteDatabase.rawQueryWithFactory")
        assertThat(error.recoverability.isRecoverable).isEqualTo(true)
        assertThat(error.errorCodeValue).isEqualTo(ERROR_ISSUE_WITH_PROCESSING_QUERY_VALUE)
    }

    @Test
    fun test_error_wrong_param_count() = runBlocking {
        val databaseId = inspectDatabase(Database("db", table2).createInstance(temporaryFolder))
        val command = "select * from sqlite_master where name=?"
        val queryParams = listOf("'a'", "'b'") // one too many param
        val response =
            testEnvironment.sendCommand(createQueryCommand(databaseId, command, queryParams))
        assertThat(response.hasErrorOccurred()).isEqualTo(true)
        val error = response.errorOccurred.content
        assertThat(error.message).contains("Cannot bind argument")
        assertThat(error.message).contains("index is out of range")
        assertThat(error.message).contains("The statement has 1 parameters")
        assertThat(error.stackTrace).contains("SQLiteDatabase.rawQueryWithFactory")
        assertThat(error.stackTrace).contains("SQLiteDirectCursorDriver.query")
        assertThat(error.stackTrace).contains("SQLiteProgram.bind")
        assertThat(error.recoverability.isRecoverable).isEqualTo(true)
        assertThat(error.errorCodeValue).isEqualTo(ERROR_ISSUE_WITH_PROCESSING_QUERY_VALUE)
    }

    @Test
    fun test_valid_query_nested_query_with_a_comment() {
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

    @Test
    fun test_valid_query_with_params_syntax1() {
        test_valid_query_with_params(paramNameLeft = "?", paramNameRight = "?")
    }

    @Test
    fun test_valid_query_with_params_syntax2a() {
        test_valid_query_with_params(paramNameLeft = "?1", paramNameRight = "?2")
    }

    /**
     * Same as [test_valid_query_with_params_syntax2a] but with reversed argument "names".
     * Showcases that this syntax respects numerals after the "?" unlike all other cases (also
     * tested here).
     */
    @Test
    fun test_valid_query_with_params_syntax2b() {
        try {
            test_valid_query_with_params(paramNameLeft = "?2", paramNameRight = "?1")
        } catch (comparisonFailure: ComparisonFailure) {
            assertThat(comparisonFailure.actual).isEqualTo("0")
            return
        }
        fail()
    }

    @Test
    fun test_valid_query_with_params_syntax3() {
        test_valid_query_with_params(paramNameLeft = ":aa", paramNameRight = ":bb")
    }

    @Test
    fun test_valid_query_with_params_syntax4a() {
        test_valid_query_with_params(paramNameLeft = "@bb", paramNameRight = "@aa")
    }

    @Test
    fun test_valid_query_with_params_syntax4b() {
        test_valid_query_with_params(paramNameLeft = "@2", paramNameRight = "@1")
    }

    @Test
    fun test_valid_query_with_params_syntax5a() {
        test_valid_query_with_params(paramNameLeft = "\$B200", paramNameRight = "\$A1")
    }

    @Test
    fun test_valid_query_with_params_syntax5b() {
        test_valid_query_with_params(paramNameLeft = "\$A2", paramNameRight = "\$A1")
    }

    @Test
    fun test_valid_query_with_params_syntax5c() {
        test_valid_query_with_params(paramNameLeft = "\$1", paramNameRight = "\$2")
    }

    @Test
    fun test_valid_query_with_params_syntax5d() {
        test_valid_query_with_params(paramNameLeft = "\$2", paramNameRight = "\$1")
    }

    private fun test_valid_query_with_params(paramNameLeft: String, paramNameRight: String) {
        test_valid_query(
            Database("db", table2),
            values = listOf(
                table2 to arrayOf("1", "'A'"),
                table2 to arrayOf("2", "'B'"),
                table2 to arrayOf("3", "'C'")
            ),
            query = "select * from " +
                "(select * from ${table2.name} where id > $paramNameLeft) " +
                "where id < $paramNameRight",
            queryParams = listOf("1", "3"),
            expectedValues = listOf(listOf(2, "B")),
            expectedTypes = listOf(listOf("integer", "text")),
            expectedColumnNames = table2.columns.map { it.name }
        )
    }

    @Test
    fun test_valid_query_with_params_column_name_limitation() {
        test_valid_query(
            Database("db", table2),
            values = listOf(
                table2 to arrayOf("1", "'A'"),
                table2 to arrayOf("2", "'B'"),
                table2 to arrayOf("3", "'C'")
            ),
            query = "select ? as col from ${table2.name}",
            queryParams = listOf("id"),
            // Note: instead of expected 1, 2, 3, we get "id", "id", "id". This is a result of
            // binding ? as Strings.
            expectedValues = listOf(listOf("id"), listOf("id"), listOf("id")),
            expectedTypes = listOf(listOf("text"), listOf("text"), listOf("text")),
            expectedColumnNames = listOf("col")
        )
    }

    @Test
    fun test_valid_query_with_params_null_params() = runBlocking {
        // given
        val insertCommand = "insert into ${table2.name} values (?, ?)"

        val insertValues = listOf(
            listOf(null, null),
            listOf("0.5", null),
            listOf("'2'", null),
            listOf(null, "A")
        )

        val expectedValues = listOf(
            listOf(null, null),
            listOf(0.5, null),
            listOf("'2'", null),
            listOf(null, "A")
        )

        val expectedTypes = listOf(
            listOf("null", "null"),
            listOf("float", "null"),
            listOf("text", "null"),
            listOf("null", "text")
        )

        // when
        val databaseId = inspectDatabase(Database("db", table2).createInstance(temporaryFolder))
        insertValues.forEach { params -> issueQuery(databaseId, insertCommand, params) }

        // then
        issueQuery(databaseId, "select * from ${table2.name}").let { response ->
            assertThat(response.rowsCount).isEqualTo(4)
            response.rowsList.let { actualRows: List<Row> ->
                actualRows.forEachIndexed { rowIx, row ->
                    row.valuesList.forEachIndexed { colIx, cell ->
                        assertThat(cell.value).isEqualTo(expectedValues[rowIx][colIx])
                        assertThat(cell.type).isEqualTo(expectedTypes[rowIx][colIx])
                    }
                }
            }
        }
    }

    @Test
    fun test_valid_query_empty_result_column_names_present() {
        test_valid_query(
            Database("db", table2),
            values = listOf(
                table2 to arrayOf("1", "'A'"),
                table2 to arrayOf("2", "'B'")
            ),
            query = "select * from ${table2.name} where 1=0", // impossible condition
            expectedValues = emptyList(),
            expectedTypes = emptyList(),
            expectedColumnNames = table2.columns.map { it.name }
        )
    }

    @Test
    fun test_valid_query_missing_column_values() {
        test_valid_query(
            Database("db", table2),
            values = listOf(
                table2 to arrayOf("1", "'A'"),
                table2 to arrayOf("null", "null"),
                table2 to arrayOf("null", "'C'")
            ),
            query = "select * from ${table2.name}",
            expectedValues = listOf(
                listOf(1, "A"),
                listOf(null, null),
                listOf(null, "C")
            ),
            expectedTypes = listOf(
                listOf("integer", "text"),
                listOf("null", "null"),
                listOf("null", "text")
            ),
            expectedColumnNames = table2.columns.map { it.name }
        )
    }

    @Test
    fun test_large_number_of_values_with_response_size_limit() = runBlocking {
        // test config
        val expectedRecordCount = 4096
        val recordSize = 512
        val idealBatchCount = 256
        val responseSizeLimitHint = expectedRecordCount.toLong() * recordSize / idealBatchCount

        // create a database
        val db = Database(
            "db_large_val",
            Table("table1", Column("c1", "blob"))
        ).createInstance(temporaryFolder, writeAheadLoggingEnabled = true)

        // populate the database
        val records = mutableListOf<ByteArray>()
        val statement = db.compileStatement("insert into table1 values (?)")
        repeat(expectedRecordCount) { ix ->
            val value = ByteArray(recordSize) { ix.toByte() }
            records.add(value)
            statement.bindBlob(1, value)
            statement.executeInsert()
        }

        // query the data through inspection
        val dbId = inspectDatabase(db)
        var recordCount = 0
        var batchCount = 0
        while (true) { // break close inside of the loop
            val response = testEnvironment.sendCommand(
                createQueryCommand(
                    dbId,
                    "select * from table1 LIMIT 999999 OFFSET $recordCount",
                    responseSizeLimitHint = responseSizeLimitHint
                )
            )
            assertThat(response.hasErrorOccurred()).isFalse()
            val rows = response.query.rowsList
            if (rows.isEmpty()) break // no more rows to process

            batchCount++
            rows.forEach { row ->
                val actual = row.valuesList.single().blobValue.toByteArray()
                val expected = records[recordCount++]
                assertThat(actual).isEqualTo(expected)
            }
        }

        // verify the response
        assertThat(recordCount).isEqualTo(expectedRecordCount)
        assertThat(batchCount.toDouble()).isGreaterThan(idealBatchCount * 0.7) // 30% tolerance
        assertThat(batchCount.toDouble()).isLessThan(idealBatchCount * 1.3) // 30% tolerance
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
        expectedColumnNames: List<String>,
        queryParams: List<String>? = null
    ) = runBlocking {
        // given
        val databaseInstance = database.createInstance(temporaryFolder)
        values.forEach { (table, values) -> databaseInstance.insertValues(table, *values) }
        val databaseId = inspectDatabase(databaseInstance)

        // when
        issueQuery(databaseId, query, queryParams).let { response ->
            // then
            assertThat(response.rowsCount).isEqualTo(expectedValues.size)
            assertThat(response.columnNamesList).isEqualTo(expectedColumnNames)
            response.rowsList.let { actualRows: List<Row> ->
                actualRows.forEachIndexed { rowIx, row ->
                    row.valuesList.forEachIndexed { colIx, cell ->
                        assertThat(cell.value).isEqualTo(expectedValues[rowIx][colIx])
                        assertThat(cell.type).isEqualTo(expectedTypes[rowIx][colIx])
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
        assertThat(querySchema(databaseId)).isEqualTo(
            (database.tables + newTable).sortedBy { it.name }
        )
        assertThat(queryTotalChanges(databaseId)).isEqualTo(initialTotalChanges) // note no diff
    }

    @Test
    fun test_drop_table() = runBlocking {
        // given
        val database = Database("db1", table1, table2)
        val databaseId = inspectDatabase(
            database.createInstance(temporaryFolder).also {
                it.insertValues(table2, "1", "'1'")
                it.insertValues(table2, "2", "'2'")
            }
        )
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

    @Test
    fun test_int64() {
        test_value64(Long.MAX_VALUE, { s -> s.getLong(0) }, { c -> c.longValue })
    }

    @Test
    fun test_float64() {
        test_value64(Float.MAX_VALUE * 2.0, { s -> s.getDouble(0) }, { c -> c.doubleValue })
    }

    private fun <T> test_value64(
        value: T,
        fromCursor: (Cursor) -> T,
        fromCellValue: (CellValue) -> T
    ) = runBlocking {
        val db = Database("db1", Table("t1", Column("c1", "INT"))).createInstance(temporaryFolder)
        testEnvironment.registerAlreadyOpenDatabases(listOf(db))
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        val id = testEnvironment.receiveEvent().databaseOpened.databaseId

        db.execSQL("insert into t1 values ($value)")
        val query = "select * from t1"
        val dbValue: T = db.rawQuery(query, emptyArray()).also { it.moveToNext() }.let {
            val result = fromCursor(it)
            it.close()
            result
        }
        assertThat(dbValue).isEqualTo(value)

        testEnvironment.issueQuery(id, query, null).let { response ->
            assertThat(response.rowsList).hasSize(1)
            assertThat(response.rowsList[0].valuesList).hasSize(1)
            assertThat(fromCellValue(response.rowsList[0].valuesList[0])).isEqualTo(value)
        }
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
        return testEnvironment.awaitDatabaseOpenedEvent(databaseInstance.displayName).databaseId
    }

    private suspend fun issueQuery(
        databaseId: Int,
        command: String,
        queryParams: List<String?>? = null
    ): QueryResponse = testEnvironment.issueQuery(databaseId, command, queryParams)

    private suspend fun querySchema(databaseId: Int): List<Table> =
        testEnvironment.sendCommand(createGetSchemaCommand(databaseId)).getSchema.toTableList()

    private suspend fun queryChanges(databaseId: Int, useTotal: Boolean = false): Long {
        val criterion = if (useTotal) "total_changes" else "changes"
        return issueQuery(databaseId, "select $criterion()").rowsList.let { response ->
            assertThat(response).hasSize(1)
            assertThat(response.first().valuesList).hasSize(1)
            val cell = response.first().getValues(0)
            assertThat(cell.type).isEqualTo("integer")
            cell.value as Long
        }
    }

    private suspend fun queryTotalChanges(databaseId: Int): Long = queryChanges(databaseId, true)

    private inline fun <reified T> repeatN(value: T, n: Int): Array<T> =
        (0 until n).map { value }.toTypedArray()

    private inline fun <reified T> repeat5(v: T) = repeatN(v, 5)
}
