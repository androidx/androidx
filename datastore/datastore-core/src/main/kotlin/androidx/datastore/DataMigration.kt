/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore

/**
 * Interface for migrations to DataStore.
 */
interface DataMigration<T> {

    /**
     * Return whether this migration needs to be performed. If this returns false, no migration or
     * cleanup will occur. Apps should do the cheapest possible check to determine if this migration
     * should run, since this will be called every time the DataStore is initialized.
     */
    suspend fun shouldMigrate(): Boolean

    /**
     * Perform the migration. Implementations should be idempotent since this may be called
     * multiple times. If migrate fails, DataStore will not commit any data to disk, cleanUp will
     * not be called, and the exception will be propagated back to the DataStore call that
     * triggered the migration. Future calls to DataStore will result in DataMigrations being
     * attempted again.
     *
     * @param currentData the current data (it might be populated from other migrations or from
     * manual changes before this migration was added to the app)
     * @return The migrated data.
     */
    suspend fun migrate(currentData: T): T

    /**
     * Clean up any old state/data that was migrated into the DataStore. This will not be called
     * if the migration fails. If cleanUp throws an exception, the exception will be propagated
     * back to the DataStore call that triggered the migration and future calls to DataStore will
     * result in DataMigrations being attempted again.
     */
    suspend fun cleanUp()
}