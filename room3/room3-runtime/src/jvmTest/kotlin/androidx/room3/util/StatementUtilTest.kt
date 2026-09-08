/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room3.util

import androidx.kruth.assertThat
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class BufferedSQLiteStatementTest {

    private val driver = BundledSQLiteDriver()
    private val connection = driver.open(":memory:")

    @BeforeTest
    fun setUp() {
        connection.execSQL(
            "CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT, val REAL, data BLOB)"
        )
        connection.execSQL("INSERT INTO test_table VALUES (1, 'alice', 3.14, x'0102')")
        connection.execSQL("INSERT INTO test_table VALUES (2, 'bob', 2.71, x'0304')")
        connection.execSQL("INSERT INTO test_table VALUES (3, 'charlie', 1.41, null)")
    }

    @AfterTest
    fun tearDown() {
        connection.close()
    }

    @Test
    fun bufferStatementRewindsWithoutReexecuting() {
        val rawStmt = connection.prepare("SELECT id FROM test_table ORDER BY RANDOM() LIMIT 2")
        val bufferedStmt = bufferStatement(rawStmt)

        // Pass 1
        val pass1 = buildList {
            while (bufferedStmt.step()) {
                add(bufferedStmt.getLong(0))
            }
        }
        assertThat(pass1).hasSize(2)

        // Reset
        bufferedStmt.reset()

        // Pass 2 - must return exact same rows in exact same order
        val pass2 = buildList {
            while (bufferedStmt.step()) {
                add(bufferedStmt.getLong(0))
            }
        }
        assertThat(pass2).isEqualTo(pass1)

        bufferedStmt.close()
    }

    @Test
    fun bufferStatementTypeCoercions() {
        val rawStmt = connection.prepare("SELECT id, name, val, data FROM test_table WHERE id = 1")
        val bufferedStmt = bufferStatement(rawStmt)

        assertThat(bufferedStmt.step()).isTrue()

        // Column 0: INTEGER (1)
        assertThat(bufferedStmt.getColumnType(0)).isEqualTo(SQLITE_DATA_INTEGER)
        assertThat(bufferedStmt.getLong(0)).isEqualTo(1L)
        assertThat(bufferedStmt.getInt(0)).isEqualTo(1)
        assertThat(bufferedStmt.getDouble(0)).isEqualTo(1.0)
        assertThat(bufferedStmt.getFloat(0)).isEqualTo(1.0f)
        assertThat(bufferedStmt.getText(0)).isEqualTo("1")
        assertThat(bufferedStmt.getBoolean(0)).isTrue()
        assertThat(bufferedStmt.isNull(0)).isFalse()

        // Column 1: TEXT ('alice')
        assertThat(bufferedStmt.getColumnType(1)).isEqualTo(SQLITE_DATA_TEXT)
        assertThat(bufferedStmt.getText(1)).isEqualTo("alice")
        assertThat(bufferedStmt.isNull(1)).isFalse()

        // Column 2: FLOAT (3.14)
        assertThat(bufferedStmt.getColumnType(2)).isEqualTo(SQLITE_DATA_FLOAT)
        assertThat(bufferedStmt.getDouble(2)).isEqualTo(3.14)
        assertThat(bufferedStmt.getFloat(2)).isEqualTo(3.14f)
        assertThat(bufferedStmt.isNull(2)).isFalse()

        // Column 3: BLOB (x'0102')
        assertThat(bufferedStmt.getColumnType(3)).isEqualTo(SQLITE_DATA_BLOB)
        assertThat(bufferedStmt.getBlob(3)).isEqualTo(byteArrayOf(1, 2))
        assertThat(bufferedStmt.isNull(3)).isFalse()

        bufferedStmt.close()
    }

    @Test
    fun bufferStatementNullValues() {
        val rawStmt = connection.prepare("SELECT id, data FROM test_table WHERE id = 3")
        val bufferedStmt = bufferStatement(rawStmt)

        assertThat(bufferedStmt.step()).isTrue()
        assertThat(bufferedStmt.isNull(1)).isTrue()
        assertThat(bufferedStmt.getColumnType(1)).isEqualTo(SQLITE_DATA_NULL)

        bufferedStmt.close()
    }

    @Test
    fun bufferStatementIdempotent() {
        val rawStmt = connection.prepare("SELECT id FROM test_table")
        val bufferedStmt1 = bufferStatement(rawStmt)
        val bufferedStmt2 = bufferStatement(bufferedStmt1)

        assertThat(bufferedStmt2).isSameInstanceAs(bufferedStmt1)

        bufferedStmt2.close()
    }

    @Test
    fun bufferStatementPartialIteration() {
        val rawStmt = connection.prepare("SELECT id FROM test_table ORDER BY id ASC")
        val bufferedStmt = bufferStatement(rawStmt)

        // Read only 1st row
        assertThat(bufferedStmt.step()).isTrue()
        assertThat(bufferedStmt.getLong(0)).isEqualTo(1L)

        // Reset early
        bufferedStmt.reset()

        // Read all rows
        val allIds = buildList {
            while (bufferedStmt.step()) {
                add(bufferedStmt.getLong(0))
            }
        }
        assertThat(allIds).isEqualTo(listOf(1L, 2L, 3L))

        bufferedStmt.close()
    }
}
