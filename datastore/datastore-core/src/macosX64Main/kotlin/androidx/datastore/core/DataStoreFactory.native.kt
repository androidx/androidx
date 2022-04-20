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

package androidx.datastore.core

import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path

actual object DataStoreFactory {
    /**
     * Create an instance of SingleProcessDataStore. Never create more than one instance of
     * DataStore for a given file; doing so can break all DataStore functionality. You should
     * consider managing your DataStore instance as a singleton. If there are multiple DataStores
     * active, DataStore will throw IllegalStateException when reading or updating data. A
     * DataStore is considered active as long as its scope is active.
     *
     * T is the type DataStore acts on. The type T must be immutable. Mutating a type used in
     * DataStore invalidates any guarantees that DataStore provides and will result in
     * potentially serious, hard-to-catch bugs. We strongly recommend using protocol buffers:
     * https://developers.google.com/protocol-buffers/docs/javatutorial - which provides
     * immutability guarantees, a simple API and efficient serialization.
     *
     * @param storage Storage to persist T
     * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
     * [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
     * serializers when data can not be de-serialized.
     * @param migrations Migrations are run before any access to data can occur. Migrations must
     * be idempotent.
     * @param scope The scope in which IO operations and transform functions will execute.
     *
     * @return a new DataStore instance with the provided configuration
     */
    actual fun <T> create(
        storage: Storage<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>?,
        migrations: List<DataMigration<T>>,
        scope: CoroutineScope
    ): DataStore<T> = SingleProcessDataStore(
        storage = storage,
        initTasksList = listOf(DataMigrationInitializer.getInitializer(migrations)),
        corruptionHandler = corruptionHandler ?: NoOpCorruptionHandler(),
        scope = scope
    )

    fun <T> create(
        fileSystem: FileSystem,
        producePath: () -> Path,
        serializer: Serializer<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>?,
        migrations: List<DataMigration<T>>,
        scope: CoroutineScope
    ): DataStore<T> = create(
        storage = OkioStorage(fileSystem, producePath, serializer),
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope
    )
}