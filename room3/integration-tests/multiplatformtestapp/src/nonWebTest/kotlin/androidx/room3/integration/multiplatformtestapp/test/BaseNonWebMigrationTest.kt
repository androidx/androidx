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

package androidx.room3.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.executeSQL
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BaseNonWebMigrationTest : BaseMigrationTest() {
    // Validates that the type of connection created by the given driver are used in migrations.
    @Test
    fun customConnectionsOnMigrate() = runTest {
        val migrationTestHelper = getTestHelper()
        val connection = migrationTestHelper.createDatabase(1)
        connection.close()

        class MyConnection(private val delegate: SQLiteConnection) : SQLiteConnection by delegate

        val bundledDriver = BundledSQLiteDriver()
        val dbVersion2 =
            getDatabaseBuilder()
                .addMigrations(
                    object : Migration(1, 2) {
                        override suspend fun migrate(connection: SQLiteConnection) {
                            assertThat(connection).isInstanceOf<MyConnection>()
                            connection.executeSQL(
                                "ALTER TABLE MigrationEntity ADD COLUMN addedInV2 TEXT"
                            )
                        }
                    }
                )
                .setDriver(
                    object : SQLiteDriver by bundledDriver {
                        override fun open(fileName: String): SQLiteConnection {
                            return MyConnection(bundledDriver.open(fileName))
                        }
                    }
                )
                .build()
        dbVersion2.dao().getSingleItem(1)
        dbVersion2.close()
    }
}
