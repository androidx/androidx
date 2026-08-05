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

package androidx.appstate.datastore.samples

import androidx.annotation.Sampled
import androidx.appstate.AppState
import androidx.appstate.AppStateKey
import androidx.appstate.datastore.AppStatePreferences
import androidx.appstate.datastore.PersistToDataStore
import androidx.appstate.datastore.addAppStateToDataStoreListener
import androidx.datastore.core.DataStore
import kotlinx.serialization.Serializable

@Serializable @PersistToDataStore private object SampleKey : AppStateKey<String>()

@Sampled
suspend fun AppStateDataStoreListenerSample(dataStore: DataStore<AppStatePreferences>) {
    val appState = AppState()

    // Register the key by getting the state before attaching the listener
    val state = appState.getState(SampleKey, "default")

    appState.addAppStateToDataStoreListener(dataStore)
}
