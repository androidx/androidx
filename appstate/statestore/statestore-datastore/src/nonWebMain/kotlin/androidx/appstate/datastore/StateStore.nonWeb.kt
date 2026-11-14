/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("StateStoreKt")
@file:JvmMultifileClass

package androidx.appstate.datastore

import androidx.appstate.statestore.StateStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

/**
 * Tracks changes to [StateStore] and persists keys implementing [PersistToDataStore] to a
 * [androidx.datastore.core.DataStore] backed by a file.
 *
 * Keys must implement [PersistToDataStore] to be saved.
 *
 * @sample androidx.appstate.datastore.samples.SyncStateStoreDataStorePathSample
 * @param path the file path to save and restore state from
 * @param coroutineScope the [CoroutineScope] used for the [androidx.datastore.core.DataStore]
 */
@Suppress("PairedRegistration")
public suspend fun StateStore.syncToDataStore(path: String, coroutineScope: CoroutineScope) {
    val dataStore =
        DataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, StateStoreSerializer) { path.toPath() },
            scope = coroutineScope,
        )
    syncToDataStore(dataStore)
}
