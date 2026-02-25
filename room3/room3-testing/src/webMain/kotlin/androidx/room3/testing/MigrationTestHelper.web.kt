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

package androidx.room3.testing

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

// TODO(b/336758416): Implement helper for web, figuring out reading / loading schema files.
public actual class MigrationTestHelper {
    public actual suspend fun createDatabase(version: Int): SQLiteConnection {
        TODO("Not yet implemented")
    }

    public actual suspend fun runMigrationsAndValidate(
        version: Int,
        migrations: List<Migration>,
    ): androidx.sqlite.SQLiteConnection {
        TODO("Not yet implemented")
    }
}
