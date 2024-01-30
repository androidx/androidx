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

package androidx.sqliteMultiplatform.driver

import androidx.kruth.assertThat
import kotlin.test.AfterTest
import kotlin.test.Test
import platform.posix.remove

class NativeSQLiteDriverTest {

    @AfterTest
    fun after() {
        remove("test.db")
    }

    @Test
    fun smokeTest() {
        val driver = NativeSQLiteDriver("test.db")
        val connection = driver.open()
        connection.prepare("PRAGMA journal_mode").let { statement ->
            statement.step()
            // Default journal mode is 'delete'
            assertThat(statement.getText(0)).isEqualTo("delete")
            statement.close()
        }
        connection.close()
    }
}
