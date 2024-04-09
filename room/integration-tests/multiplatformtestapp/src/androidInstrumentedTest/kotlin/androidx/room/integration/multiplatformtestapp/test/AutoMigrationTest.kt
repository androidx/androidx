/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class AutoMigrationTest : BaseAutoMigrationTest() {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("test.db")
    private val driver: SQLiteDriver = BundledSQLiteDriver()

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        instrumentation = instrumentation,
        driver = driver,
        databaseClass = AutoMigrationDatabase::class,
        fileName = file.path
    )

    override fun getTestHelper() = migrationTestHelper

    override fun getRoomDatabase(): AutoMigrationDatabase {
        return Room.databaseBuilder<AutoMigrationDatabase>(
            context = instrumentation.targetContext,
            name = file.path
        ).setDriver(driver).build()
    }

    @Test
    fun migrationWithWrongOverride() = runTest {
        // Create database in V1
        val connection = migrationTestHelper.createDatabase(1)
        connection.close()

        // Auto migrate to V2
        val v2Db = Room.databaseBuilder<AutoMigrationDatabase>(
            context = instrumentation.targetContext,
            name = file.path
        ).setDriver(driver).addMigrations(object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {} }
        ).build()
        assertThrows<NotImplementedError> {
            v2Db.dao().insert(AutoMigrationEntity(1, 1))
        }
        v2Db.close()
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
}
