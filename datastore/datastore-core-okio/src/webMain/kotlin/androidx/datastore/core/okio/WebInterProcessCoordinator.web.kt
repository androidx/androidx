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

import androidx.datastore.core.InterProcessCoordinator
import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.w3c.dom.StorageEvent
import org.w3c.dom.events.Event

/**
 * An InterProcessCoordinator for web environments.
 *
 * For [sessionStorage], coordination is not needed as it's isolated to a single tab. For
 * [localStorage], it uses the 'storage' event to coordinate reads and writes across multiple
 * browser tabs.
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
            WebStorageType.LOCAL -> localStorage
            WebStorageType.SESSION -> sessionStorage
        }

    /**
     * Handles notifications within the current tab.
     *
     * In addition to being triggered by changes within the current tab, for [localStorage], this is
     * also triggered by the [StorageEvent] listener when another tab updates the data.
     */
    private val inTabNotifier = MutableStateFlow(domStorage.getItem(versionKey)?.toIntOrNull() ?: 0)

    /** Listen for [StorageEvent] which are fired when [localStorage] is modified in another tab. */
    private val storageEventListener: ((Event) -> Unit) = { event ->
        if (event is StorageEvent && event.key == versionKey) {
            // Update the notifier with the new value from storage to trigger flows.
            inTabNotifier.value = domStorage.getItem(versionKey)?.toIntOrNull() ?: 0
        }
    }

    init {
        // StorageEvent listener is only needed for localStorage.
        if (storageType == WebStorageType.LOCAL) {
            window.addEventListener("storage", storageEventListener)
        }
    }

    /** Used to notify listeners in a single tab. */
    override val updateNotifications: Flow<Unit> = inTabNotifier.map {}

    override suspend fun <T> lock(block: suspend () -> T): T {
        // Lock is always available since `sessionStorage` and `localStorage` storage are
        // single-threaded.
        return block()
    }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        // Lock is always available since `sessionStorage` and `localStorage` storage are
        // single-threaded.
        return block(true)
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

    /** Removes the [StorageEvent] listener from the window to prevent memory leaks. */
    fun removeStorageEventListener() {
        // StorageEvent listener is only needed for localStorage.
        if (storageType == WebStorageType.LOCAL) {
            window.removeEventListener("storage", storageEventListener)
        }
    }
}

/** Create a coordinator for web use cases. */
internal fun createWebProcessCoordinator(
    path: String,
    storageType: WebStorageType,
): WebInterProcessCoordinator {
    // TODO(b/441511612): Add support for OPFS.
    return WebInterProcessCoordinator(path, storageType)
}
