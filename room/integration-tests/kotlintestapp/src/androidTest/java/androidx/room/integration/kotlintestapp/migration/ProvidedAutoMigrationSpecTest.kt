/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room.integration.kotlintestapp.migration

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ProvidedAutoMigrationSpec
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test custom database migrations.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProvidedAutoMigrationSpecTest {
    private val mProvidedSpec = ProvidedAutoMigrationDb.MyProvidedAutoMigration("Hi")

    @JvmField
    @Rule
    var helperWithoutSpec: MigrationTestHelper
    var helperWithSpec: MigrationTestHelper

    init {
        helperWithoutSpec = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            ProvidedAutoMigrationDb::class.java
        )
        val specs: MutableList<AutoMigrationSpec> = ArrayList()
        specs.add(mProvidedSpec)
        helperWithSpec = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            ProvidedAutoMigrationDb::class.java,
            specs,
            FrameworkSQLiteOpenHelperFactory()
        )
    }

    @Database(
        version = 2,
        entities = [ProvidedAutoMigrationDb.Entity1::class, ProvidedAutoMigrationDb.Entity2::class],
        autoMigrations = [AutoMigration(
            from = 1,
            to = 2,
            spec = ProvidedAutoMigrationDb.MyProvidedAutoMigration::class
        )],
        exportSchema = true
    )

    abstract class ProvidedAutoMigrationDb : RoomDatabase() {

        /**
         * No change between versions.
         */
        @Entity
        internal class Entity1 {
            @PrimaryKey
            var id = 0
            var name: String? = null

            @ColumnInfo(defaultValue = "1")
            var addedInV1 = 0

            companion object {
                const val TABLE_NAME = "Entity1"
            }
        }

        /**
         * A new table added.
         */
        @Entity
        internal class Entity2 {
            @PrimaryKey
            var id = 0
            var name: String? = null

            @ColumnInfo(defaultValue = "1")
            var addedInV1 = 0

            @ColumnInfo(defaultValue = "2")
            var addedInV2 = 0

            companion object {
                const val TABLE_NAME = "Entity2"
            }
        }

        @ProvidedAutoMigrationSpec
        internal class MyProvidedAutoMigration(private val mPrefString: String) :
            AutoMigrationSpec {
            var mOnPostMigrateCalled = false
            override fun onPostMigrate(db: SupportSQLiteDatabase) {
                mOnPostMigrateCalled = true
            }
        }
    }

    // Run this to create the very 1st version of the db.
    @Throws(IOException::class)
    fun createFirstVersion() {
        val db = helperWithoutSpec.createDatabase(TEST_DB, 1)
        db.execSQL("INSERT INTO Entity1 (id, name) VALUES (1, 'row1')")
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testOnPostMigrate() {
        createFirstVersion()
        helperWithSpec.runMigrationsAndValidate(
            TEST_DB,
            2,
            true
        )
        MatcherAssert.assertThat(mProvidedSpec.mOnPostMigrateCalled, CoreMatchers.`is`(true))
    }

    /**
     * Verifies that the user defined migration is selected over using an autoMigration.
     */
    @Test
    @Throws(IOException::class)
    fun testNoSpecProvidedInConfig() {
        createFirstVersion()
        try {
            helperWithoutSpec.runMigrationsAndValidate(
                TEST_DB,
                2,
                true
            )
        } catch (exception: IllegalArgumentException) {
            MatcherAssert.assertThat(
                exception.message,
                CoreMatchers.containsString(
                    "A required auto migration spec (androidx.room.integration." +
                        "kotlintestapp" +
                        ".migration.ProvidedAutoMigrationSpecTest" +
                        ".ProvidedAutoMigrationDb.MyProvidedAutoMigration) has not " +
                        "been provided."
                )
            )
        }
    }

    companion object {
        private const val TEST_DB = "auto-migration-test"
    }
}
