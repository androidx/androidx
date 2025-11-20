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

package androidx.datastore.core

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Public factory for creating DataStore instances. */
public actual object DataStoreFactory {
    /**
     * Create an instance of SingleProcessDataStore. Never create more than one instance of
     * DataStore for a given file; doing so can break all DataStore functionality. You should
     * consider managing your DataStore instance as a singleton. If there are multiple DataStores
     * active, DataStore will throw IllegalStateException when reading or updating data. A DataStore
     * is considered active as long as its scope is active.
     *
     * T is the type DataStore acts on. The type T must be immutable. Mutating a type used in
     * DataStore invalidates any guarantees that DataStore provides and will result in potentially
     * serious, hard-to-catch bugs. We strongly recommend using protocol buffers:
     * https://developers.google.com/protocol-buffers/docs/javatutorial - which provides
     * immutability guarantees, a simple API and efficient serialization.
     *
     * It is important to note that if a [produceFile] lambda to produce a file in the User
     * Encrypted (UE) storage via a [Context] is used, this file will not be available during direct
     * boot, and may result in a [DirectBootUsageException] via a [java.io.FileNotFoundException] or
     * a silent failure. To create an instance of a DataStore to be used safely during direct boot
     * mode, please use [createInDeviceProtectedStorage].
     *
     * @param serializer Serializer for the type T used with DataStore. The type T must be
     *   immutable.
     * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
     *   [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
     *   serializers when data can not be de-serialized.
     * @param migrations Migrations are run before any access to data can occur. Migrations must be
     *   idempotent.
     * @param scope The scope in which IO operations and transform functions will execute.
     * @param produceFile Function which returns the file that the new DataStore will act on. The
     *   function must return the same path every time. No two instances of DataStore should act on
     *   the same file at the same time.
     * @return a new DataStore instance with the provided configuration
     */
    @JvmOverloads // Generate constructors for default params for java users.
    public fun <T> create(
        serializer: Serializer<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
        migrations: List<DataMigration<T>> = listOf(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile: () -> File,
    ): DataStore<T> =
        create(
            storage = FileStorage(serializer = serializer, produceFile = produceFile),
            corruptionHandler = corruptionHandler,
            migrations = migrations,
            scope = scope,
        )

    /**
     * Create an instance of SingleProcessDataStore. Never create more than one instance of
     * DataStore for a given file; doing so can break all DataStore functionality. You should
     * consider managing your DataStore instance as a singleton. If there are multiple DataStores
     * active, DataStore will throw IllegalStateException when reading or updating data. A DataStore
     * is considered active as long as its scope is active.
     *
     * T is the type DataStore acts on. The type T must be immutable. Mutating a type used in
     * DataStore invalidates any guarantees that DataStore provides and will result in potentially
     * serious, hard-to-catch bugs. We strongly recommend using protocol buffers:
     * https://developers.google.com/protocol-buffers/docs/javatutorial - which provides
     * immutability guarantees, a simple API and efficient serialization.
     *
     * It is important to note that if the [storage] used is located in the User Encrypted (UE)
     * storage, the corresponding storage file will not be available during direct boot, and may
     * result in a [DirectBootUsageException] via a [java.io.FileNotFoundException] or a silent
     * failure. To create an instance of a DataStore to be used safely during direct boot mode,
     * please use [createInDeviceProtectedStorage].
     *
     * @param storage Storage for the type T used with DataStore. The type T must be immutable.
     * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
     *   [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
     *   serializers when data can not be de-serialized.
     * @param migrations Migrations are run before any access to data can occur. Migrations must be
     *   idempotent.
     * @param scope The scope in which IO operations and transform functions will execute.
     * @return a new DataStore instance with the provided configuration
     */
    @JvmOverloads
    public actual fun <T> create(
        storage: Storage<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>?,
        migrations: List<DataMigration<T>>,
        scope: CoroutineScope,
    ): DataStore<T> =
        DataStoreImpl(
            storage = storage,
            corruptionHandler = corruptionHandler ?: NoOpCorruptionHandler(),
            initTasksList = listOf(DataMigrationInitializer.getInitializer(migrations)),
            scope = scope,
        )

    /**
     * Create an instance of SingleProcessDataStore that can be used during direct boot.
     *
     * This API always creates the DataStore in the Device Encrypted storage.
     *
     * @param context The DeviceProtectedStorageContext which will be used to create the file that
     *   the new DataStore will act on.
     * @param fileName the filename relative to
     *   Context.createDeviceProtectedStorageContext().filesDir that DataStore acts on. The File is
     *   obtained from [deviceProtectedDataStoreFile]. It is created in the "/datastore"
     *   subdirectory, in the device encrypted storage.
     * @param serializer Serializer for the type T used with DataStore. The type T must be
     *   immutable.
     * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
     *   [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
     *   serializers when data can not be de-serialized.
     * @param migrations Migrations are run before any access to data can occur. Migrations must be
     *   idempotent.
     * @param scope The scope in which IO operations and transform functions will execute.
     * @return a new DataStore instance with the provided configuration
     */
    @RequiresApi(Build.VERSION_CODES.N)
    @JvmOverloads // Generate constructors for default params for java users.
    public fun <T> createInDeviceProtectedStorage(
        context: Context,
        fileName: String,
        serializer: Serializer<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
        migrations: List<DataMigration<T>> = listOf(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    ): DataStore<T> {
        return DataStoreImpl(
            storage =
                FileStorage(
                    serializer = serializer,
                    produceFile = { context.deviceProtectedDataStoreFile(fileName) },
                ),
            corruptionHandler = corruptionHandler ?: NoOpCorruptionHandler(),
            initTasksList = listOf(DataMigrationInitializer.getInitializer(migrations)),
            scope = scope,
        )
    }
}
