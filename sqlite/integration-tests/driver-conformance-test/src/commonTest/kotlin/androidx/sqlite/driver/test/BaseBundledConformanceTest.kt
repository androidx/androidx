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
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_FULLMUTEX
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

abstract class BaseBundledConformanceTest : BaseConformanceTest() {

    abstract fun getDatabaseFileName(): String

    abstract override fun getDriver(): BundledSQLiteDriver

    @Test
    fun readSQLiteVersion() {
        val connection = getDriver().open(":memory:")
        try {
            val version =
                connection.prepare("SELECT sqlite_version()").use {
                    it.step()
                    it.getText(0)
                }
            // The bundled androidx SQLite version compiled and statically included
            assertThat(version).isEqualTo(EXPECTED_SQLITE_VERSION)
        } finally {
            connection.close()
        }
    }

    @Test
    fun openWithFullMutexFlag() = runTest {
        val connection =
            getDriver()
                .open(
                    fileName = getDatabaseFileName(),
                    flags = SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE or SQLITE_OPEN_FULLMUTEX
                )
        connection.execSQL("CREATE TABLE Test (col)")
        // Concurrently use the connection, many threads inserting and two threads reading, due to
        // being opened with the full mutex flag, it should be safe.
        coroutineScope {
            repeat(20) { i ->
                launch(Dispatchers.IO) {
                    connection.prepare("INSERT INTO Test (col) VALUES (?)").use {
                        it.bindInt(1, i)
                        it.step()
                    }
                }
            }
            repeat(2) {
                launch(Dispatchers.IO) {
                    val count = mutableListOf<Int>()
                    do {
                        count.clear()
                        connection.prepare("SELECT * FROM Test").use {
                            while (it.step()) {
                                count.add(it.getInt(0))
                            }
                        }
                    } while (count.size != 20)
                }
            }
        }
        connection.close()
    }

    @Test
    fun threadSafeMode() {
        // Validate bundled SQLite is compiled with SQLITE_THREADSAFE = 2
        val driver = BundledSQLiteDriver()
        assertThat(driver.threadingMode).isEqualTo(2)
    }

    companion object {
        const val EXPECTED_SQLITE_VERSION = "3.46.0"
    }
}
