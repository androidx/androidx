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

package androidx.sqliteMultiplatform

import androidx.kruth.assertThat
import kotlin.test.Test

abstract class BaseConformanceTest {

    abstract fun getDriver(): SQLiteDriver

    @Test
    fun openAndCloseConnection() {
        val driver = getDriver()
        val connection = driver.open()
        try {
            val version = connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                statement.getLong(0)
            }
            assertThat(version).isEqualTo(0)
        } finally {
            connection.close()
        }
    }

    @Test
    fun bindAndReadColumns() {
        val driver = getDriver()
        val connection = driver.open()
        try {
            connection.execSQL(
                "CREATE TABLE Test(integerCol INTEGER, realCol REAL, textCol TEXT, blobCol BLOB)"
            )
            connection.prepare(
                "INSERT INTO Test (integerCol, realCol, textCol, blobCol) VALUES (?, ?, ?, ?)"
            ).use {
                it.bindLong(1, 3)
                it.bindDouble(2, 7.87)
                it.bindText(3, "PR")
                it.bindBlob(4, byteArrayOf(0x0F, 0x12, 0x1B))
                assertThat(it.step()).isFalse() // SQLITE_DONE
            }
            connection.prepare("SELECT * FROM Test").use {
                assertThat(it.step()).isTrue() // SQLITE_ROW
                assertThat(it.getColumnCount()).isEqualTo(4)
                assertThat(it.getColumnName(0)).isEqualTo("integerCol")
                assertThat(it.getColumnName(1)).isEqualTo("realCol")
                assertThat(it.getColumnName(2)).isEqualTo("textCol")
                assertThat(it.getColumnName(3)).isEqualTo("blobCol")
                assertThat(it.getLong(0)).isEqualTo(3)
                assertThat(it.getDouble(1)).isEqualTo(7.87)
                assertThat(it.getText(2)).isEqualTo("PR")
                assertThat(it.getBlob(3)).isEqualTo(byteArrayOf(0x0F, 0x12, 0x1B))
            }
        } finally {
            connection.close()
        }
    }

    @Test
    fun bindAndReadTextUtf8() {
        val driver = getDriver()
        val connection = driver.open()
        try {
            val konnichiwa = "こんにちわ"
            connection.execSQL("CREATE TABLE Test (textCol)")
            connection.prepare("INSERT INTO Test (textCol) VALUES (?)").use {
                it.bindText(1, konnichiwa)
                assertThat(it.step()).isFalse() // SQLITE_DONE
            }
            connection.prepare("SELECT * FROM Test").use {
                assertThat(it.step()).isTrue() // SQLITE_ROW
                assertThat(it.getText(0)).isEqualTo(konnichiwa)
            }
        } finally {
            connection.close()
        }
    }

    @Test
    fun bindAndReadNull() {
        val driver = getDriver()
        val connection = driver.open()
        try {
            connection.execSQL("CREATE TABLE Test (col)")
            connection.prepare("INSERT INTO Test (col) VALUES (?)").use {
                it.bindNull(1)
                assertThat(it.step()).isFalse() // SQLITE_DONE
            }
            connection.prepare("SELECT * FROM Test").use {
                assertThat(it.step()).isTrue() // SQLITE_ROW
                assertThat(it.isNull(0)).isTrue()
            }
        } finally {
            connection.close()
        }
    }
}
