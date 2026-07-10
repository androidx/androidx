/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp.test

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.kruth.assertWithMessage
import androidx.room3.Room
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class NoBackupDirectoryTest(private val useDriver: UseDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testNoBackupDirectoryFileName() = runTest {
        val databaseFile = File(context.noBackupFilesDir, "temporal.db")

        withContext(Dispatchers.Main) {
            // This is an easy way to use the no backup dir, but the API getNoBackupFilesDir() does
            // IO and depending where the Room builder is created, it can be in the main thread.
            val database =
                Room.databaseBuilder<TestDatabase>(context, databaseFile.path)
                    .setDriver(
                        when (useDriver) {
                            UseDriver.ANDROID -> AndroidSQLiteDriver()
                            UseDriver.BUNDLED -> BundledSQLiteDriver()
                        }
                    )
                    .build()
            database.booksDao().insertPublisherSuspend("p1", "pub1")
            database.close()
        }

        assertWithMessage("Expected the database file in the no backup dir to be created.")
            .that(databaseFile.exists())
            .isTrue()
        databaseFile.delete()
    }

    @Test
    fun testNoBackupDirectoryDriver() = runTest {
        // Enable StrictMode to validate no IO is done in the main thread
        val currentPolicy = StrictMode.getThreadPolicy()
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath().build()
        )

        // This is a more appropriate way to use the no backup dir, as the driver open() call is
        // done off the main thread and will be OK with the getNoBackupFilesDir() IO.
        val actualDriver =
            when (useDriver) {
                UseDriver.ANDROID -> AndroidSQLiteDriver()
                UseDriver.BUNDLED -> BundledSQLiteDriver()
            }
        val noBackupDirDriver =
            object : SQLiteDriver by actualDriver {
                override fun open(fileName: String): SQLiteConnection {
                    val noBackupFileName =
                        File(context.noBackupFilesDir, fileName.substringAfterLast("/"))
                    return actualDriver.open(noBackupFileName.path)
                }
            }
        withContext(Dispatchers.Main) {
            val database =
                Room.databaseBuilder<TestDatabase>(context, "temporal.db")
                    .setDriver(noBackupDirDriver)
                    .build()
            database.booksDao().insertPublisherSuspend("p1", "pub1")
            database.close()
        }

        StrictMode.setThreadPolicy(currentPolicy)

        val databaseFile = File(context.noBackupFilesDir, "temporal.db")
        assertWithMessage("Expected the database file in the no backup dir to be created.")
            .that(databaseFile.exists())
            .isTrue()
        databaseFile.delete()
    }
}
