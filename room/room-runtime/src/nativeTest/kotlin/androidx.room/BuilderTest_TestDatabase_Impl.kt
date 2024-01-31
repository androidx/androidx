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

package androidx.room

import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import kotlin.reflect.KClass

internal class BuilderTest_TestDatabase_Impl : BuilderTest.TestDatabase() {
    override fun createOpenDelegate(): RoomOpenDelegateMarker {
        return object : RoomOpenDelegate(0, "") {
            override fun onCreate(connection: SQLiteConnection) { }
            override fun onPreMigrate(connection: SQLiteConnection) { }
            override fun onValidateSchema(connection: SQLiteConnection): ValidationResult {
                return ValidationResult(true, null)
            }
            override fun onPostMigrate(connection: SQLiteConnection) { }
            override fun onOpen(connection: SQLiteConnection) { }
            override fun createAllTables(connection: SQLiteConnection) { }
            override fun dropAllTables(connection: SQLiteConnection) { }
        }
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        return InvalidationTracker(this, emptyMap(), emptyMap())
    }

    override fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        return emptyList()
    }

    override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
        return emptySet()
    }

    override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
        return emptyMap()
    }
}
