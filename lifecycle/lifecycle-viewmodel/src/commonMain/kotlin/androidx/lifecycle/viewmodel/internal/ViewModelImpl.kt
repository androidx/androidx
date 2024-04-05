/*
 * Copyright 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalStdlibApi::class)

package androidx.lifecycle.viewmodel.internal

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope

/**
 * Internal implementation of the multiplatform [ViewModel].
 *
 * Kotlin Multiplatform does not support expect class with default implementation yet, so we
 * extracted the common logic used by all platforms to this internal class.
 *
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-20427">KT-20427</a>
 */
internal class ViewModelImpl {

    private val lock = Lock()

    /**
     * Holds a mapping between [String] keys and [AutoCloseable] resources that have been associated
     * with this [ViewModel].
     *
     * The associated resources will be [AutoCloseable.close] right before the [ViewModel.onCleared]
     * is called. This provides automatic resource cleanup upon [ViewModel] release.
     *
     * The clearing order is:
     * 1. [keyToCloseables][AutoCloseable.close]
     * 2. [closeables][AutoCloseable.close]
     * 3. [ViewModel.onCleared]
     *
     * **Note:** Manually [Lock] is necessary to prevent issues on Android API 21 and 22.
     * This avoids potential problems found in older versions of `ConcurrentHashMap`.
     *
     * @see <a href="https://issuetracker.google.com/37042460">b/37042460</a>
     */
    private val keyToCloseables = mutableMapOf<String, AutoCloseable>()

    /**
     * @see [keyToCloseables]
     */
    private val closeables = mutableSetOf<AutoCloseable>()

    @Volatile
    private var isCleared = false

    constructor()

    constructor(viewModelScope: CoroutineScope) {
        addCloseable(VIEW_MODEL_SCOPE_KEY, viewModelScope.asCloseable())
    }

    constructor(vararg closeables: AutoCloseable) {
        this.closeables += closeables
    }

    constructor(viewModelScope: CoroutineScope, vararg closeables: AutoCloseable) {
        addCloseable(VIEW_MODEL_SCOPE_KEY, viewModelScope.asCloseable())
        this.closeables += closeables
    }

    /** @see [ViewModel.clear] */
    @MainThread
    fun clear() {
        if (isCleared) return

        isCleared = true
        lock.withLock {
            // 1. Closes resources added without a key.
            // 2. Closes resources added with a key.
            for (closeable in closeables + keyToCloseables.values) {
                closeWithRuntimeException(closeable)
            }
            // Clear only resources without keys to prevent accidental recreation of resources.
            // For example, `viewModelScope` would be recreated leading to unexpected behaviour.
            closeables.clear()
        }
    }

    /** @see [ViewModel.addCloseable] */
    fun addCloseable(key: String, closeable: AutoCloseable) {
        // Although no logic should be done after user calls onCleared(), we will
        // ensure that if it has already been called, the closeable attempting to
        // be added will be closed immediately to ensure there will be no leaks.
        if (isCleared) {
            closeWithRuntimeException(closeable)
            return
        }

        val oldCloseable = lock.withLock { keyToCloseables.put(key, closeable) }
        closeWithRuntimeException(oldCloseable)
    }

    /** @see [ViewModel.addCloseable] */
    fun addCloseable(closeable: AutoCloseable) {
        // Although no logic should be done after user calls onCleared(), we will
        // ensure that if it has already been called, the closeable attempting to
        // be added will be closed immediately to ensure there will be no leaks.
        if (isCleared) {
            closeWithRuntimeException(closeable)
            return
        }

        lock.withLock { closeables += closeable }
    }

    /** @see [ViewModel.getCloseable] */
    fun <T : AutoCloseable> getCloseable(key: String): T? =
        @Suppress("UNCHECKED_CAST")
        lock.withLock { keyToCloseables[key] as T? }

    private fun closeWithRuntimeException(closeable: AutoCloseable?) {
        try {
            closeable?.close()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
