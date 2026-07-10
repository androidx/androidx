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

import android.os.StrictMode
import androidx.kruth.assertThat
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class BundledSQLiteDriverTest : BaseBundledConformanceTest() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("test.db")

    override val driverType = TestDriverType.BUNDLED

    override fun getDatabaseFileName(): String = file.path

    override fun getDriver(): BundledSQLiteDriver {
        return BundledSQLiteDriver()
    }

    override fun getTestExtensionPath(): String {
        val file =
            File(
                instrumentation.targetContext.applicationInfo.nativeLibraryDir,
                "libsqliteExtension.so",
            )
        return file.absolutePath
    }

    @BeforeTest
    fun before() {
        assertThat(file).isNotNull()
        file.parentFile?.mkdirs()
        deleteDatabaseFile()
    }

    @AfterTest
    fun after() {
        deleteDatabaseFile()
    }

    private fun deleteDatabaseFile() {
        instrumentation.targetContext.deleteDatabase(file.name)
    }

    @Test
    fun strictMode() {
        val currentPolicy = StrictMode.getThreadPolicy()
        try {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyDeath()
                    .build()
            )

            // Validates that creating a driver does not cause a disk read violation due to the
            // native library being loaded lazily when a connection is opened.
            val driver = getDriver()

            runBlocking(Dispatchers.IO) {
                val connection = driver.open(":memory:")
                connection.close()
            }
        } finally {
            StrictMode.setThreadPolicy(currentPolicy)
        }
    }
}
