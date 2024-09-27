/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.sqlite.driver.test

import androidx.kruth.assertThat
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.test.Test
import kotlin.test.assertFailsWith

abstract class BaseConformanceTest {

    abstract val driverType: TestDriverType

    abstract fun getDriver(): SQLiteDriver

    enum class TestDriverType {
        ANDROID_FRAMEWORK, // :sqlite:sqlite-framework (Android)
        NATIVE_FRAMEWORK, // :sqlite:sqlite-framework (Native)
        BUNDLED, // :sqlite:sqlite-bundled (Android, Native, JVM)
    }

    @Test
    fun openAndCloseConnection() {
        val driver = getDriver()
        val connection = driver.open(":memory:")
        try {
            val version =
                connection.prepare("PRAGMA user_version").use { statement ->
                    statement.step()
                    statement.getLong(0)
                }
            assertThat(version).isEqualTo(0)
        } finally {
            connection.close()
        }
    }

    @Test
    fun bindAndReadColumns() = testWithConnection { connection ->
        connection.execSQL(
            """
            CREATE TABLE Test(
                integerCol_long INTEGER,
                integerCol_int INTEGER,
                integerCol_boolean INTEGER,
                realCol_double REAL,
                realCol_float REAL,
                textCol TEXT,
                blobCol BLOB
            )
            """
                .trimIndent()
        )
        connection
            .prepare(
                """
            INSERT INTO Test (
                integerCol_long,
                integerCol_int,
                integerCol_boolean,
                realCol_double,
                realCol_float,
                textCol,
                blobCol
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """
                    .trimIndent()
            )
            .use {
                it.bindLong(1, 3)
                it.bindInt(2, 22)
                it.bindBoolean(3, true)
                it.bindDouble(4, 7.87)
                it.bindFloat(5, 9.39f)
                it.bindText(6, "PR")
                it.bindBlob(7, byteArrayOf(0x0F, 0x12, 0x1B))
                assertThat(it.step()).isFalse() // SQLITE_DONE
            }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getColumnCount()).isEqualTo(7)
            assertThat(it.getColumnName(0)).isEqualTo("integerCol_long")
            assertThat(it.getColumnName(1)).isEqualTo("integerCol_int")
            assertThat(it.getColumnName(2)).isEqualTo("integerCol_boolean")
            assertThat(it.getColumnName(3)).isEqualTo("realCol_double")
            assertThat(it.getColumnName(4)).isEqualTo("realCol_float")
            assertThat(it.getColumnName(5)).isEqualTo("textCol")
            assertThat(it.getColumnName(6)).isEqualTo("blobCol")
            assertThat(it.getColumnNames())
                .containsExactly(
                    "integerCol_long",
                    "integerCol_int",
                    "integerCol_boolean",
                    "realCol_double",
                    "realCol_float",
                    "textCol",
                    "blobCol"
                )
                .inOrder()
            assertThat(it.getColumnType(0)).isEqualTo(SQLITE_DATA_INTEGER)
            assertThat(it.getColumnType(1)).isEqualTo(SQLITE_DATA_INTEGER)
            assertThat(it.getColumnType(2)).isEqualTo(SQLITE_DATA_INTEGER)
            assertThat(it.getColumnType(3)).isEqualTo(SQLITE_DATA_FLOAT)
            assertThat(it.getColumnType(4)).isEqualTo(SQLITE_DATA_FLOAT)
            assertThat(it.getColumnType(5)).isEqualTo(SQLITE_DATA_TEXT)
            assertThat(it.getColumnType(6)).isEqualTo(SQLITE_DATA_BLOB)
            assertThat(it.getLong(0)).isEqualTo(3)
            assertThat(it.getInt(1)).isEqualTo(22)
            assertThat(it.getBoolean(2)).isTrue()
            assertThat(it.getDouble(3)).isEqualTo(7.87)
            assertThat(it.getFloat(4)).isEqualTo(9.39f)
            assertThat(it.getText(5)).isEqualTo("PR")
            assertThat(it.getBlob(6)).isEqualTo(byteArrayOf(0x0F, 0x12, 0x1B))
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
    }

    @Test
    fun bindAndReadTextUtf8() = testWithConnection { connection ->
        val konnichiwa = "こんにちわ"
        val world = "κόσμε"
        connection.execSQL("CREATE TABLE Test (textCol TEXT)")
        connection.prepare("INSERT INTO Test (textCol) VALUES (?)").use {
            it.bindText(1, konnichiwa)
            assertThat(it.step()).isFalse() // SQLITE_DONE
            it.reset()
            it.bindText(1, "Hello $world")
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getText(0)).isEqualTo(konnichiwa)
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getText(0)).isEqualTo("Hello $world")
        }
    }

    @Test
    fun bindAndReadZeroLengthBlob() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (data BLOB)")
        connection.prepare("INSERT INTO Test (data) VALUES (?)").use {
            it.bindBlob(1, ByteArray(0))
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getBlob(0)).isEqualTo(ByteArray(0))
        }
    }

    @Test
    fun bindAndReadEmptyString() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (data TEXT)")
        connection.prepare("INSERT INTO Test (data) VALUES (?)").use {
            it.bindText(1, "")
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getText(0)).isEqualTo("")
        }
    }

    @Test
    fun bindTextInExpression() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (date TEXT)")
        connection.prepare("INSERT INTO Test (date) VALUES (?)").use {
            it.bindText(1, "1991-04-18")
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM Test WHERE strftime('%Y', date) = ?").use {
            it.bindText(1, "1991")
            assertThat(it.step()).isTrue() // SQLITE_ROW
        }
    }

    @Test
    fun bindAndReadNull() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("INSERT INTO Test (col) VALUES (?)").use {
            it.bindNull(1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getColumnType(0)).isEqualTo(SQLITE_DATA_NULL)
            assertThat(it.isNull(0)).isTrue()
        }
    }

    @Test
    open fun bindInvalidParam() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("SELECT 1 FROM Test").use {
            var message: String? = null
            val expectedMessage = "Error code: 25, message: column index out of range"

            fun checkExceptionMsg() {
                assertThat(message).isEqualTo(expectedMessage)
            }

            message = assertFailsWith<SQLiteException> { it.bindNull(1) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.bindBlob(1, byteArrayOf()) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.bindDouble(1, 0.0) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.bindLong(1, 0) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.bindText(1, "") }.message
            checkExceptionMsg()

            message = assertFailsWith<SQLiteException> { it.bindText(0, "") }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.bindText(-1, "") }.message
            checkExceptionMsg()
        }
    }

    @Test
    fun readInvalidColumn() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.execSQL("INSERT INTO Test (col) VALUES ('')")
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            var message: String? = null
            val expectedMessage = "Error code: 25, message: column index out of range"

            fun checkExceptionMsg() {
                assertThat(message).isEqualTo(expectedMessage)
            }

            message = assertFailsWith<SQLiteException> { it.isNull(3) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.getBlob(3) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.getDouble(3) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.getLong(3) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.getText(3) }.message
            checkExceptionMsg()
            message = assertFailsWith<SQLiteException> { it.getColumnName(3) }.message
            checkExceptionMsg()

            message = assertFailsWith<SQLiteException> { it.getColumnName(-1) }.message
            checkExceptionMsg()
        }
    }

    @Test
    fun readColumnWithoutStep() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.execSQL("INSERT INTO Test (col) VALUES ('')")
        connection.prepare("SELECT * FROM Test").use {
            val message = assertFailsWith<SQLiteException> { it.getText(1) }.message
            assertThat(message).isEqualTo("Error code: 21, message: no row")
        }
    }

    @Test
    fun readColumnNameWithoutStep() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("SELECT col FROM Test").use {
            assertThat(it.getColumnCount()).isEqualTo(1)
            assertThat(it.getColumnName(0)).isEqualTo("col")
        }
    }

    @Test
    fun readColumnOfInsertStatement() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("INSERT INTO Test (col) VALUES (?)").use {
            val message = assertFailsWith<SQLiteException> { it.getText(0) }.message
            assertThat(message).isEqualTo("Error code: 21, message: no row")
        }
    }

    @Test
    fun prepareInvalidReadStatement() = testWithConnection {
        assertThat(
                assertFailsWith<SQLiteException> {
                        it.prepare("SELECT * FROM Foo").use { it.step() }
                    }
                    .message
            )
            .contains("no such table: Foo")
    }

    @Test
    fun prepareInvalidWriteStatement() = testWithConnection {
        assertThat(
                assertFailsWith<SQLiteException> { it.execSQL("INSERT INTO Foo (id) VALUES (1)") }
                    .message
            )
            .contains("no such table: Foo")
    }

    @Test
    fun useClosedConnection() {
        val driver = getDriver()
        val connection = driver.open(":memory:")
        connection.close()
        assertFailsWith<SQLiteException> { connection.prepare("SELECT * FROM Foo") }
    }

    @Test
    fun useClosedSelectStatement() = testWithConnection {
        it.execSQL("CREATE TABLE Foo (id)")
        val statement = it.prepare("SELECT * FROM Foo")
        statement.close()
        assertFailsWith<SQLiteException> { statement.step() }
    }

    @Test
    fun useClosedInsertStatement() = testWithConnection {
        it.execSQL("CREATE TABLE Foo (id)")
        val statement = it.prepare("INSERT INTO Foo (id) VALUES (1)")
        statement.close()
        assertFailsWith<SQLiteException> { statement.step() }
    }

    @Test
    fun clearBindings() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Foo (id)")
        connection.execSQL("INSERT INTO Foo (id) VALUES (1)")
        connection.prepare("SELECT * FROM Foo WHERE id = ?").use {
            it.bindLong(1, 1)
            assertThat(it.step()).isTrue()
            it.reset()
            it.clearBindings()
            assertThat(it.step()).isFalse()
        }
    }

    @Test
    fun readLastRowId() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("INSERT INTO Test (col) VALUES (?)").use {
            it.bindNull(1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("INSERT INTO Test (col) VALUES (?)").use {
            it.bindNull(1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        val lastRowId =
            connection.prepare("SELECT last_insert_rowid()").use {
                it.step()
                it.getLong(0)
            }
        assertThat(lastRowId).isEqualTo(2)
    }

    @Test
    fun changes() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("INSERT INTO Test (col) VALUES (?),(?),(?)").use {
            it.bindNull(1)
            it.bindNull(2)
            it.bindNull(3)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        val changes =
            connection.prepare("SELECT changes()").use {
                it.step()
                it.getLong(0)
            }
        assertThat(changes).isEqualTo(3)
    }

    @Test
    fun withClause() = testWithConnection { connection ->
        var seriesSum = 0
        connection
            .prepare(
                """
                WITH RECURSIVE
                  cnt(x) AS (VALUES(1) UNION ALL SELECT x + 1 FROM cnt WHERE x < 10)
                SELECT x FROM cnt;
            """
                    .trimIndent()
            )
            .use {
                while (it.step()) {
                    seriesSum += it.getInt(0)
                }
            }
        assertThat(seriesSum).isEqualTo(55)
    }

    private inline fun testWithConnection(block: (SQLiteConnection) -> Unit) {
        val driver = getDriver()
        val connection = driver.open(":memory:")
        try {
            block.invoke(connection)
        } finally {
            connection.close()
        }
    }
}
