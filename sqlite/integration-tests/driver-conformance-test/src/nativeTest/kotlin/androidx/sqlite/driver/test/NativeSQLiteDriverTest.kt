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
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlin.test.Test

class NativeSQLiteDriverTest : BaseConformanceTest() {

    override val driverType = TestDriverType.NATIVE_FRAMEWORK

    override fun getDriver(): SQLiteDriver {
        return NativeSQLiteDriver()
    }

    @Test
    fun loadExtension() {
        val extensionPath = getExtensionFileName()

        val driver =
            NativeSQLiteDriver().apply {
                addExtension(extensionPath, "sqlite3_test_extension_init")
            }

        driver.open(":memory:").use { connection ->
            connection.prepare("SELECT hello_world()").use { stmt ->
                assertThat(stmt.step()).isTrue()
                assertThat(stmt.getText(0)).isEqualTo("Hello from sqlite_extension.cpp!")
            }
        }
    }
}
