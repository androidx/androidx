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
    fun simpleReadAndWrite() {
        val driver = getDriver()
        val connection = driver.open()
        try {
            connection.execSQL("CREATE TABLE Pet (id INTEGER NOT NULL PRIMARY KEY)")
            connection.execSQL("INSERT INTO Pet (id) VALUES (3)")
            connection.prepare("SELECT * FROM Pet WHERE id = ?").use { statement ->
                statement.bindLong(0, 3)
                assertThat(statement.step()).isTrue()
                assertThat(statement.getColumnCount()).isEqualTo(1)
                assertThat(statement.getLong(0)).isEqualTo(3)
            }
        } finally {
            connection.close()
        }
    }
}
