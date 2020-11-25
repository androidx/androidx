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

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Create an instance of SingleProcessDataStore. Never create more than one instance of
 * DataStore for a given file; doing so can break all DataStore functionality. You should
 * consider managing your DataStore instance as a singleton. Note: this function is *not*
 * comparable to [Context.getSharedPreferences]; this function creates and returns a
 * new instance of DataStore each time it is called.
 *
 * @param fileName the filename relative to Context.filesDir that DataStore acts on. The File is
 * obtained by calling File(context.filesDir, fileName). No two instances of DataStore should
 * act on the same file at the same time.
 * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
 * [androidx.datastore.CorruptionException] when attempting to read data. CorruptionExceptions are
 * thrown by
 * serializers when data can not be de-serialized.
 * @param migrations are run before any access to data can occur. Each producer and migration
 * may be run more than once whether or not it already succeeded (potentially because another
 * migration failed or a write to disk failed.)
 * @param scope The scope in which IO operations and transform functions will execute.
 *
 * @return a new DataStore instance with the provided configuration
 */
public fun <T> Context.createDataStore(
    fileName: String,
    serializer: Serializer<T>,
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    migrations: List<DataMigration<T>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
): DataStore<T> =
    DataStoreFactory.create(
        serializer = serializer,
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope
    ) { File(this.filesDir, "datastore/$fileName") }
