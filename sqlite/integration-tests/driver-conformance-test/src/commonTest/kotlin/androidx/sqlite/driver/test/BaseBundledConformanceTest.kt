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
import androidx.sqlite.use
import kotlin.test.Test

abstract class BaseBundledConformanceTest : BaseConformanceTest() {
    @Test
    fun readSQLiteVersion() {
        val connection = getDriver().open(":memory:")
        try {
            val version = connection.prepare("SELECT sqlite_version()").use {
                it.step()
                it.getText(0)
            }
            // The bundled androidx SQLite version compiled and statically included
            assertThat(version).isEqualTo("3.42.0")
        } finally {
            connection.close()
        }
    }
}
