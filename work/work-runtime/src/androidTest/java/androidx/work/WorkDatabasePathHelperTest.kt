/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkDatabasePathHelper
import androidx.work.impl.WorkDatabaseVersions.VERSION_9
import java.io.File
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class WorkDatabasePathHelperTest {
    @get:Rule
    val migrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            WorkDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testMigration_toNoBackupDirectory() {
        // Create a database
        migrationTestHelper.createDatabase(
            WorkDatabasePathHelper.getDefaultDatabasePath(context).path,
            VERSION_9,
        )
        val paths = WorkDatabasePathHelper.migrationPaths(context)

        val asserts: Map<File, Boolean> =
            paths.map { (source, _) -> Pair(source, source.exists()) }.toMap()

        // Migrate
        WorkDatabasePathHelper.migrateDatabase(context)

        // Verify paths
        paths.forEach { (source, destination) ->
            val exists = asserts[source] ?: false
            if (exists) {
                assertThat(destination.exists(), `is`(true))
            }
        }
    }
}
