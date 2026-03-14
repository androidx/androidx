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

package androidx.room3

import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import kotlin.reflect.KClass
import org.mockito.kotlin.mock

internal class BuilderTest_TestDatabase_Impl : BuilderTest.TestDatabase() {
    var mAutoMigrations = listOf<Migration>(BuilderTest.EmptyMigration(1, 2))

    override fun createOpenDelegate(): RoomOpenDelegate {
        return mock()
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        return mock()
    }

    override fun clearAllTables() {}

    override fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        return mAutoMigrations
    }

    override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
        return emptySet()
    }

    override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
        return emptyMap()
    }
}
