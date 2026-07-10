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

@file:OptIn(ExperimentalWasmJsInterop::class)

package androidx.datastore.core.okio

import androidx.datastore.core.InterProcessCoordinator
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import org.w3c.dom.StorageEvent
import org.w3c.dom.events.Event

internal interface WebInterProcessCoordinator : InterProcessCoordinator {
    fun removeStorageEventListener()
}

internal class SyncWebCoordinator(
    private val name: String,
    private val storageType: WebStorageType,
) : WebInterProcessCoordinator {

    private val versionKey = "datastore_${storageType}_${name}_version"

    private val domStorage by lazy {
        when (storageType) {
            WebStorageType.LOCAL -> localStorage
            WebStorageType.SESSION -> sessionStorage
            WebStorageType.OPFS -> error("SyncWebCoordinator should not be used for OPFS")
        }
    }

    private val updateNotificationsMutable = MutableStateFlow(1)
    override val updateNotifications: Flow<Unit> = updateNotificationsMutable.drop(1).map {}

    private val storageEventListener: ((Event) -> Unit) = { event ->
        if (event is StorageEvent && event.key == versionKey) {
            updateNotificationsMutable.value += 1
        }
    }

    init {
        if (storageType == WebStorageType.LOCAL) {
            window.addEventListener("storage", storageEventListener)
        }
    }

    override suspend fun <T> lock(block: suspend () -> T): T {
        return block()
    }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        return block(true)
    }

    override suspend fun getVersion(): Int {
        return domStorage.getItem(versionKey)?.toIntOrNull() ?: 0
    }

    override suspend fun incrementAndGetVersion(): Int {
        val newVersion = getVersion() + 1
        domStorage.setItem(versionKey, newVersion.toString())
        updateNotificationsMutable.value += 1
        return newVersion
    }

    override fun removeStorageEventListener() {
        if (storageType == WebStorageType.LOCAL) {
            window.removeEventListener("storage", storageEventListener)
        }
    }
}

internal expect class AsyncWebCoordinator(name: String) : BaseAsyncWebCoordinator {
    override suspend fun <T> lock(block: suspend () -> T): T

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T
}

internal fun createWebProcessCoordinator(
    path: String,
    storageType: WebStorageType,
): WebInterProcessCoordinator {
    return when (storageType) {
        WebStorageType.LOCAL,
        WebStorageType.SESSION -> SyncWebCoordinator(path, storageType)
        WebStorageType.OPFS -> AsyncWebCoordinator(path)
    }
}

/**
 * Base coordinator class for asynchronous web storage operations for OPFS.
 *
 * This base class is designed to avoid code duplication by sharing common state management logic
 * (like update notifications, coroutine scopes, and versioning) between the `wasmJs` and `js`
 * actual implementations.
 *
 * The actual implementations of `AsyncWebCoordinator` had to be separated per platform because the
 * `kotlin.js.Promise` API lacks a common implementation across the `wasmJs` and `js` target
 * platforms. Because the underlying Web Locks API heavily relies on Promises to manage exclusive
 * access, the specific [lock] and [tryLock] implementations are delegated to the individual
 * platform-specific actuals.
 */
internal abstract class BaseAsyncWebCoordinator(name: String) : WebInterProcessCoordinator {
    internal val updateNotificationsMutable = MutableStateFlow(0)
    override val updateNotifications: Flow<Unit> = updateNotificationsMutable.drop(1).map {}
    internal val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    internal val broadcastChannel =
        BroadcastChannel("datastore-$name").also {
            it.onmessage = { updateNotificationsMutable.value += 1 }
        }

    internal var isExclusiveLockHeld = false

    override suspend fun getVersion(): Int {
        return updateNotificationsMutable.value
    }

    override suspend fun incrementAndGetVersion(): Int {
        val newVersion = getVersion() + 1
        broadcastChannel.postMessage(null)
        updateNotificationsMutable.value += 1
        return newVersion
    }

    override fun removeStorageEventListener() {
        broadcastChannel.close()
    }
}

internal fun tryLockOptions(): LockManagerOptions = js("({mode: 'exclusive', ifAvailable: true})")

internal enum class WebStorageType {
    SESSION,
    LOCAL,
    OPFS,
}
