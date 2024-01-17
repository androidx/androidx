/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore.preferences.core

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

/**
 * Public factory for creating PreferenceDataStore instances.
 */
public actual object PreferenceDataStoreFactory {
    /**
     * Create an instance of [DataStore]. Never create more than one instance of
     * DataStore for a given file; doing so can break all DataStore functionality. You should
     * consider managing your DataStore instance as a singleton.
     *
     * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
     * [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
     * serializers when data cannot be de-serialized.
     * @param migrations are run before any access to data can occur. Each producer and migration
     * may be run more than once whether or not it already succeeded (potentially because another
     * migration failed or a write to disk failed.)
     * @param scope The scope in which IO operations and transform functions will execute.
     * @param produceFile Function which returns the file that the new DataStore will act on.
     * The function must return the same path every time. No two instances of PreferenceDataStore
     * should act on the same file at the same time. The file must have the extension
     * preferences_pb. File will be created if it doesn't exist.
     *
     * @return a new DataStore instance with the provided configuration
     */
    @JvmOverloads
    public fun create(
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
        migrations: List<DataMigration<Preferences>> = listOf(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile: () -> File
    ): DataStore<Preferences> {
        val delegate = create(
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                val file = produceFile()
                check(file.extension == PreferencesSerializer.fileExtension) {
                    "File extension for file: $file does not match required extension for" +
                        " Preferences file: ${PreferencesSerializer.fileExtension}"
                }
                file.absoluteFile.toOkioPath()
            },
            corruptionHandler = corruptionHandler,
            migrations = migrations,
            scope = scope
        )
        return PreferenceDataStore(delegate)
    }

    /**
     * Create an instance of [DataStore]. Never create more than one instance of
     * DataStore for a given file; doing so can break all DataStore functionality. You should
     * consider managing your DataStore instance as a singleton.
     *
     * @param storage The storage object defines where and how the preferences will be stored.
     * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
     * [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
     * serializers when data cannot be de-serialized.
     * @param migrations are run before any access to data can occur. Each producer and migration
     * may be run more than once whether or not it already succeeded (potentially because another
     * migration failed or a write to disk failed.)
     * @param scope The scope in which IO operations and transform functions will execute.
     *
     * @return a new DataStore instance with the provided configuration
     */
    @JvmOverloads
    public actual fun create(
        storage: Storage<Preferences>,
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
        migrations: List<DataMigration<Preferences>>,
        scope: CoroutineScope,
    ): DataStore<Preferences> {
        return PreferenceDataStore(DataStoreFactory.create(
            storage = storage,
            corruptionHandler = corruptionHandler,
            migrations = migrations,
            scope = scope
        ))
    }

    /**
     * Create an instance of [DataStore] using an okio Path. Never create more than one
     * instance of DataStore for a given file; doing so can break all DataStore functionality. You
     * should consider managing your DataStore instance as a singleton.
     *
     * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
     * [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
     * serializers when data cannot be de-serialized.
     * @param migrations are run before any access to data can occur. Each producer and migration
     * may be run more than once whether or not it already succeeded (potentially because another
     * migration failed or a write to disk failed.)
     * @param scope The scope in which IO operations and transform functions will execute.
     * @param produceFile Function which returns the file that the new DataStore will act on.
     * The function must return the same path every time. No two instances of PreferenceDataStore
     * should act on the same file at the same time. The file must have the extension
     * preferences_pb. File will be created if it doesn't exist.
     *
     * @return a new DataStore instance with the provided configuration
     */
    @JvmOverloads
    public actual fun createWithPath(
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
        migrations: List<DataMigration<Preferences>>,
        scope: CoroutineScope,
        produceFile: () -> Path
    ): DataStore<Preferences> {
        return create(
            corruptionHandler,
            migrations,
            scope,
            produceFile = { produceFile().toFile() }
        )
    }
}
