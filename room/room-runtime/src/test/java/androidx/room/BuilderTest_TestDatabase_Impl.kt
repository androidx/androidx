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

package androidx.room

import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.mockito.kotlin.mock

internal class BuilderTest_TestDatabase_Impl : BuilderTest.TestDatabase() {
    lateinit var mConfig: DatabaseConfiguration
    var mAutoMigrations = listOf<Migration>(BuilderTest.EmptyMigration(1, 2))
    override fun init(configuration: DatabaseConfiguration) {
        super.init(configuration)
        mConfig = configuration
    }

    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        return mock()
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        return mock()
    }

    override fun clearAllTables() {}
    override fun getAutoMigrations(
        autoMigrationSpecs: Map<Class<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        return mAutoMigrations
    }
}
