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

package androidx.datastore.core.okio

import androidx.annotation.RestrictTo
import androidx.datastore.core.InterProcessCoordinator
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * An InterProcessCoordinator for web environments.
 *
 * For [sessionStorage], it provides in-tab notifications. An InterProcessCoordinator for web
 * environments. For `sessionStorage`, no coordination is needed as its tab-isolated. For
 * `localStorage`, it uses BroadcastChannel to coordinate work across multiple browser tabs.
 */
internal class WebInterProcessCoordinator(
    name: String,
    // TODO(b/441511612): Implement WebInterProcessCoordinator for LocalStorage and OPFS.
    //  LocalStorage should handle notifications both within a single tab as well as between
    //  multiple tabs.
    private val storageType: WebStorageType,
) : InterProcessCoordinator {

    private val versionKey = "datastore_${storageType}_${name}_version"
    private val domStorage =
        when (storageType) {
            WebStorageType.SESSION -> sessionStorage
        }

    /* Handles notifications within the current tab. */
    private val inTabNotifier = MutableStateFlow(domStorage.getItem(versionKey)?.toIntOrNull() ?: 0)

    /**
     * Used to notify all tabs.
     *
     * For `sessionStorage` use the [inTabNotifier] to notify listeners in a single tab.
     */
    override val updateNotifications: Flow<Unit> =
        when (storageType) {
            WebStorageType.SESSION -> inTabNotifier.map { Unit }
        }

    override suspend fun <T> lock(block: suspend () -> T): T {
        return when (storageType) {
            // Don't need a lock since local storage is single-threaded.
            WebStorageType.SESSION -> block()
        }
    }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        return when (storageType) {
            // Lock is always available since `sessionStorage` is single-threaded.
            WebStorageType.SESSION -> block(true)
        }
    }

    override suspend fun getVersion(): Int {
        return domStorage.getItem(versionKey)?.toIntOrNull() ?: 0
    }

    /** Atomically increments the version in the web storage and notifies other tabs. */
    override suspend fun incrementAndGetVersion(): Int {
        val currentVersion = getVersion()
        val newVersion = currentVersion + 1
        domStorage.setItem(versionKey, newVersion.toString())

        // Set the notifier value to the new version to notify listeners within the current tab.
        inTabNotifier.value = newVersion
        return newVersion
    }
}

/** Create a coordinator for web use cases. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createWebProcessCoordinator(
    path: String,
    storageType: WebStorageType,
): InterProcessCoordinator {
    // TODO(b/441511612): Add support for LocalStorage and OPFS.
    return when (storageType) {
        WebStorageType.SESSION -> WebInterProcessCoordinator(path, storageType)
    }
}
