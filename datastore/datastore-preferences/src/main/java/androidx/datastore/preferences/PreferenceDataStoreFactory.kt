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

package androidx.datastore.preferences

import androidx.datastore.DataMigration
import androidx.datastore.DataMigrationInitializer
import androidx.datastore.DataStore
import androidx.datastore.SingleProcessDataStore
import androidx.datastore.handlers.NoOpCorruptionHandler
import androidx.datastore.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Public factory for creating PreferenceDataStore instances.
 */
class PreferenceDataStoreFactory {
    /**
     * Create an instance of SingleProcessDataStore. The user is responsible for ensuring that
     * there is never more than one instance of SingleProcessDataStore acting on a file at a time.
     */
    @JvmOverloads
    fun create(
        /**
         * Function which returns the file that the new DataStore will act on. The function
         * must return the same path every time. No two instances of PreferenceDataStore
         * should act on the same file at the same time.
         */
        produceFile: () -> File,
        /**
         * The corruptionHandler is invoked if PreferenceDataStore encounters a
         * {@link Serializer.CorruptionException} when attempting to read data. CorruptionExceptions
         * are thrown when the data can not be de-serialized.
         */
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
        /**
         * Migrations are run before any access to data can occur. Each producer and migration
         * may be run more than once whether or not it already succeeded (potentially because
         * another migration failed or a write to disk failed.)
         */
        migrationProducers: List<() -> DataMigration<Preferences>> = listOf(),
        /**
         * The scope in which IO operations and transform functions will execute.
         */
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    ): DataStore<Preferences> =
        SingleProcessDataStore(
            produceFile = produceFile,
            serializer = PreferencesSerializer,
            corruptionHandler = corruptionHandler ?: NoOpCorruptionHandler(),
            initTasksList = listOf(DataMigrationInitializer.getInitializer(migrationProducers)),
            scope = scope
        )
}