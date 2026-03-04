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

/** Public factory for creating DataStore instances. */
actual object DataStoreFactory {
    actual fun <T> create(
        storage: Storage<T>,
        corruptionHandler: androidx.datastore.core.handlers.ReplaceFileCorruptionHandler<T>?,
        migrations: List<DataMigration<T>>,
        scope: kotlinx.coroutines.CoroutineScope,
    ): DataStore<T> {
        return DataStore.Builder(storage = storage, context = scope.coroutineContext)
            .apply { corruptionHandler?.let { setCorruptionHandler(it) } }
            .addMigrations(migrations)
            .build()
    }
}
