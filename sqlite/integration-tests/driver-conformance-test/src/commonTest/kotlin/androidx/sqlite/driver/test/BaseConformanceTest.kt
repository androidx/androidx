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
import androidx.kruth.assertThrows
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.executeSQL
import androidx.sqlite.open
import androidx.sqlite.prepare
import androidx.sqlite.step
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BaseConformanceTest {

    abstract val driverType: TestDriverType

    abstract fun getDriver(): SQLiteDriver

    enum class TestDriverType {
        ANDROID_FRAMEWORK, // :sqlite:sqlite-framework (Android)
        NATIVE_FRAMEWORK, // :sqlite:sqlite-framework (Native)
        BUNDLED, // :sqlite:sqlite-bundled (Android, Native, JVM)
        WEB, // :sqlite-sqlite-web (JS)
    }

    @Test
    fun openAndCloseConnection() = runTest {
        if (driverType == TestDriverType.WEB) {
            // No 'encoding' for web driver
            return@runTest
        }
        val driver = getDriver()
        val connection = driver.open(":memory:")
        try {
            val encoding =
                connection.prepare("PRAGMA encoding").use { statement ->
                    statement.step()
                    statement.getText(0)
                }
            assertThat(encoding).isEqualTo("UTF-8")
        } finally {
            connection.close()
        }
    }

    @Test
    fun bindAndReadColumns() = testWithConnection { connection ->
        connection.executeSQL(
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
                    "blobCol",
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
    fun bindAndReadNumberLimits() = testWithConnection { connection ->
        // TODO(b/485611476): Support big integers for web
        // Due to conversion to JsNumber to transfer values to and from the web worker and because
        // kotlin.Long is an emulated object, values outside the range of Number.MAX_SAFE_INTEGER
        // and Number.MIN_SAFE_INTEGER are currently not possible.
        if (driverType == TestDriverType.WEB) {
            return@testWithConnection
        }

        connection.executeSQL(
            """
            CREATE TABLE Test(
                integerCol_long INTEGER,
                integerCol_int INTEGER,
                realCol_double REAL,
                realCol_float REAL
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
                    realCol_double,
                    realCol_float
                ) VALUES (?, ?, ?, ?)
                """
                    .trimIndent()
            )
            .use {
                it.bindLong(1, Long.MIN_VALUE)
                it.bindInt(2, Int.MIN_VALUE)
                it.bindDouble(3, Double.MIN_VALUE)
                it.bindFloat(4, Float.MIN_VALUE)
                it.step()

                it.reset()

                it.bindLong(1, Long.MAX_VALUE)
                it.bindInt(2, Int.MAX_VALUE)
                it.bindDouble(3, Double.MAX_VALUE)
                it.bindFloat(4, Float.MAX_VALUE)
                it.step()
            }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW

            assertThat(it.getLong(0)).isEqualTo(Long.MIN_VALUE)
            assertThat(it.getInt(1)).isEqualTo(Int.MIN_VALUE)
            assertThat(it.getDouble(2)).isEqualTo(Double.MIN_VALUE)
            assertThat(it.getFloat(3)).isEqualTo(Float.MIN_VALUE)

            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getLong(0)).isEqualTo(Long.MAX_VALUE)
            assertThat(it.getInt(1)).isEqualTo(Int.MAX_VALUE)
            assertThat(it.getDouble(2)).isEqualTo(Double.MAX_VALUE)
            assertThat(it.getFloat(3)).isEqualTo(Float.MAX_VALUE)

            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
    }

    @Test
    fun bindAndReadTextUtf8() = testWithConnection { connection ->
        val konnichiwa = "こんにちわ"
        val world = "κόσμε"
        connection.executeSQL("CREATE TABLE Test (textCol TEXT)")
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
        connection.executeSQL("CREATE TABLE Test (data BLOB)")
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
        connection.executeSQL("CREATE TABLE Test (data TEXT)")
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
        connection.executeSQL("CREATE TABLE Test (date TEXT)")
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
        connection.executeSQL("CREATE TABLE Test (col)")
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
        connection.executeSQL("CREATE TABLE Test (col)")
        connection.prepare("SELECT 1 FROM Test").use {
            val expectedMessage = "Error code: 25, message: column index out of range"
            assertThrows<SQLiteException> { it.bindNull(1) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.bindBlob(1, byteArrayOf()) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.bindDouble(1, 0.0) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.bindLong(1, 0) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.bindText(1, "") }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.bindText(0, "") }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.bindText(-1, "") }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
        }
    }

    @Test
    fun readInvalidColumn() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Test (col)")
        connection.executeSQL("INSERT INTO Test (col) VALUES ('')")
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            val expectedMessage = "Error code: 25, message: column index out of range"
            assertThrows<SQLiteException> { it.isNull(3) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.getBlob(3) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.getDouble(3) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.getLong(3) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.getText(3) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.getColumnName(3) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
            assertThrows<SQLiteException> { it.getColumnName(-1) }
                .hasMessageThat()
                .isEqualTo(expectedMessage)
        }
    }

    @Test
    fun readColumnWithoutStep() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Test (col)")
        connection.executeSQL("INSERT INTO Test (col) VALUES ('')")
        connection.prepare("SELECT * FROM Test").use {
            assertThrows<SQLiteException> { it.getText(1) }
                .hasMessageThat()
                .isEqualTo("Error code: 21, message: no row")
        }
    }

    @Test
    fun readColumnNameWithoutStep() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Test (col)")
        connection.prepare("SELECT col FROM Test").use {
            assertThat(it.getColumnCount()).isEqualTo(1)
            assertThat(it.getColumnName(0)).isEqualTo("col")
        }
    }

    @Test
    fun readColumnOfInsertStatement() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Test (col)")
        connection.prepare("INSERT INTO Test (col) VALUES (?)").use {
            assertThrows<SQLiteException> { it.getText(0) }
                .hasMessageThat()
                .isEqualTo("Error code: 21, message: no row")
        }
    }

    @Test
    fun prepareInvalidReadStatement() = testWithConnection { connection ->
        assertThrows<SQLiteException> { connection.prepare("SELECT * FROM Foo").use { it.step() } }
            .hasMessageThat()
            .contains("no such table: Foo")
    }

    @Test
    fun prepareInvalidWriteStatement() = testWithConnection {
        assertThrows<SQLiteException> { it.executeSQL("INSERT INTO Foo (id) VALUES (1)") }
            .hasMessageThat()
            .contains("no such table: Foo")
    }

    @Test
    fun useClosedConnection() = runTest {
        val driver = getDriver()
        val connection = driver.open(":memory:")
        connection.close()
        assertThrows<SQLiteException> { connection.prepare("SELECT * FROM Foo") }
    }

    @Test
    fun useClosedSelectStatement() = testWithConnection {
        it.executeSQL("CREATE TABLE Foo (id)")
        val statement = it.prepare("SELECT * FROM Foo")
        statement.close()
        assertThrows<SQLiteException> { statement.step() }
    }

    @Test
    fun useClosedInsertStatement() = testWithConnection {
        it.executeSQL("CREATE TABLE Foo (id)")
        val statement = it.prepare("INSERT INTO Foo (id) VALUES (1)")
        statement.close()
        assertThrows<SQLiteException> { statement.step() }
    }

    @Test
    fun clearBindings() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Foo (id)")
        connection.executeSQL("INSERT INTO Foo (id) VALUES (1)")
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
        connection.executeSQL("CREATE TABLE Test (col)")
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
        connection.executeSQL("CREATE TABLE Test (col)")
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

    @Test
    fun inTransaction() = testWithConnection { connection ->
        assertThat(connection.inTransaction()).isFalse()
        connection.executeSQL("BEGIN TRANSACTION")
        assertThat(connection.inTransaction()).isTrue()
        connection.executeSQL("END TRANSACTION")
        assertThat(connection.inTransaction()).isFalse()
        connection.executeSQL("BEGIN DEFERRED TRANSACTION")
        assertThat(connection.inTransaction()).isTrue()
        connection.executeSQL("END TRANSACTION")
        assertThat(connection.inTransaction()).isFalse()
        connection.executeSQL("BEGIN IMMEDIATE TRANSACTION")
        assertThat(connection.inTransaction()).isTrue()
        connection.executeSQL("END TRANSACTION")
        assertThat(connection.inTransaction()).isFalse()
        connection.executeSQL("BEGIN EXCLUSIVE TRANSACTION")
        assertThat(connection.inTransaction()).isTrue()
        connection.executeSQL("END TRANSACTION")
        assertThat(connection.inTransaction()).isFalse()
    }

    @Test
    fun commitImmediateTransaction() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Test (col)")
        connection.executeSQL("BEGIN IMMEDIATE TRANSACTION")
        connection.executeSQL("INSERT INTO Test (col) VALUES (1)")
        connection.executeSQL("END TRANSACTION")

        val count =
            connection.prepare("SELECT COUNT(*) FROM Test").use {
                it.step()
                it.getInt(0)
            }
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun commitExclusiveTransaction() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Test (col)")
        connection.executeSQL("BEGIN EXCLUSIVE TRANSACTION")
        connection.executeSQL("INSERT INTO Test (col) VALUES (1)")
        connection.executeSQL("END TRANSACTION")

        val count =
            connection.prepare("SELECT COUNT(*) FROM Test").use {
                it.step()
                it.getInt(0)
            }
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun rollbackTransaction() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Test (col)")
        connection.executeSQL("BEGIN IMMEDIATE TRANSACTION")
        connection.executeSQL("INSERT INTO Test (col) VALUES (1)")
        connection.executeSQL("ROLLBACK TRANSACTION")

        val count =
            connection.prepare("SELECT COUNT(*) FROM Test").use {
                it.step()
                it.getInt(0)
            }
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun foreignKeysCheck() = testWithConnection { connection ->
        connection.executeSQL("CREATE TABLE Parent (pid PRIMARY KEY)")
        connection.executeSQL(
            """
            CREATE TABLE Child (
                cid PRIMARY KEY,
                pid,
                FOREIGN KEY(pid) REFERENCES Parent(pid)
            )
            """
                .trimIndent()
        )
        connection.executeSQL("INSERT INTO Parent (pid) VALUES ('p1')")
        connection.executeSQL("INSERT INTO Child (cid, pid) VALUES ('c1', 'p1')")

        // Disable FKs checks (a new connection should be off by default, but making sure)
        connection.executeSQL("PRAGMA foreign_keys = OFF")

        connection.executeSQL("BEGIN IMMEDIATE TRANSACTION")
        connection.executeSQL("DELETE FROM Parent WHERE pid = 'p1'") // OK, FKs not enabled
        connection.executeSQL("ROLLBACK TRANSACTION")

        connection.executeSQL("BEGIN IMMEDIATE TRANSACTION")
        connection.executeSQL("DELETE FROM Parent WHERE pid = 'p1'") // OK, FKs not enabled
        connection.prepare("PRAGMA foreign_key_check").use { stmt ->
            assertThat(stmt.step()).isTrue()
            assertThat(stmt.getText(0)).isEqualTo("Child")
            assertThat(stmt.getLong(1)).isEqualTo(1)
            assertThat(stmt.getText(2)).isEqualTo("Parent")
            assertThat(stmt.getLong(3)).isEqualTo(0)
        }
        connection.executeSQL("ROLLBACK TRANSACTION")

        // Enable foreign keys checks
        connection.executeSQL("PRAGMA foreign_keys = ON")

        connection.executeSQL("BEGIN IMMEDIATE TRANSACTION")
        assertThrows<SQLiteException> {
                connection.executeSQL("DELETE FROM Parent WHERE pid = 'p1'") // Fail, FKs enabled
            }
            .hasMessageThat()
            .let {
                if (driverType == TestDriverType.ANDROID_FRAMEWORK) {
                    // Not all versions of Android use extended error codes
                    it.contains("FOREIGN KEY constraint failed")
                } else {
                    it.isEqualTo("Error code: 787, message: FOREIGN KEY constraint failed")
                }
            }
        connection.executeSQL("ROLLBACK TRANSACTION")
    }

    private inline fun testWithConnection(crossinline block: suspend (SQLiteConnection) -> Unit) =
        runTest {
            val driver = getDriver()
            val connection = driver.open(":memory:")
            try {
                block.invoke(connection)
            } finally {
                connection.close()
            }
        }
}
