/*
 * Copyright 2026 The Android Open Source Project
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

package andriodx.sqlite.driver.web

import androidx.driver.web.worker.createDefaultWebWorkerDriver
import androidx.kruth.assertThat
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import androidx.sqlite.executeSQL
import androidx.sqlite.open
import androidx.sqlite.prepare
import androidx.sqlite.step
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class WebWorkerSQLiteDriverTest {
    @Test
    fun openAndCloseConnection() = runTest {
        val driver: WebWorkerSQLiteDriver = createDefaultWebWorkerDriver()
        val connection = driver.open(":memory:")
        connection.executeSQL("PRAGMA user_version = 5")
        connection.prepare("PRAGMA user_version").use { statement ->
            assertThat(statement.step()).isTrue()
            assertThat(statement.getColumnName(0)).isEqualTo("user_version")
            assertThat(statement.getColumnType(0)).isEqualTo(SQLITE_DATA_INTEGER)
            assertThat(statement.getLong(0)).isEqualTo(5)
        }
        connection.close()
    }
}
