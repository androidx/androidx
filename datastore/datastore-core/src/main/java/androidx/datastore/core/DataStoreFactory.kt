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

package androidx.datastore.core

import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Public factory for creating DataStore instances.
 */
public object DataStoreFactory {
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
     * @param serializer Serializer for the type T used with DataStore. The type T must be immutable.
     * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
     * [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
     * serializers when data can not be de-serialized.
     * @param migrations Migrations are run before any access to data can occur. Migrations must
     * be idempotent.
     * @param scope The scope in which IO operations and transform functions will execute.
     * @param produceFile Function which returns the file that the new DataStore will act on. The
     * function must return the same path every time. No two instances of DataStore should act on
     * the same file at the same time.
     *
     * @return a new DataStore instance with the provided configuration
     */
    @JvmOverloads // Generate constructors for default params for java users.
    public fun <T> create(
        serializer: Serializer<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
        migrations: List<DataMigration<T>> = listOf(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile: () -> File
    ): DataStore<T> =
        SingleProcessDataStore(
            produceFile = produceFile,
            serializer = serializer,
            corruptionHandler = corruptionHandler ?: NoOpCorruptionHandler(),
            initTasksList = listOf(DataMigrationInitializer.getInitializer(migrations)),
            scope = scope
        )
}
