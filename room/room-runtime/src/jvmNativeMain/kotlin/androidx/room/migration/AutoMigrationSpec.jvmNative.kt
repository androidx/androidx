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

package androidx.room.migration

import androidx.sqlite.SQLiteConnection

/**
 * Interface for defining an automatic migration specification for Room databases.
 *
 * The methods defined in this interface will be called on a background thread from the executor
 * set in Room's builder. It is important to note that the methods are all in a transaction when
 * it is called.
 *
 * @see [androidx.room.AutoMigration]
 */
actual interface AutoMigrationSpec {
    /**
     * Invoked after the migration is completed.
     *
     * @param connection The database connection.
     */
    actual fun onPostMigrate(connection: SQLiteConnection) { }
}
