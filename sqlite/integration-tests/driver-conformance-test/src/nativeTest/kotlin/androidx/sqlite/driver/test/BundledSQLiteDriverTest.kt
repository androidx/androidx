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

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import platform.posix.remove

class BundledSQLiteDriverTest : BaseBundledConformanceTest() {

    private val filename = "/tmp/test-${Random.nextInt()}.db"

    override val driverType = TestDriverType.BUNDLED

    override fun getDatabaseFileName(): String = filename

    override fun getDriver(): BundledSQLiteDriver {
        return BundledSQLiteDriver()
    }

    override fun getTestExtensionPath(): String = getExtensionFileName()

    @BeforeTest
    fun before() {
        deleteDatabaseFile()
    }

    @AfterTest
    fun after() {
        deleteDatabaseFile()
    }

    private fun deleteDatabaseFile() {
        remove(filename)
        remove("$filename-wal")
        remove("$filename-shm")
    }
}
