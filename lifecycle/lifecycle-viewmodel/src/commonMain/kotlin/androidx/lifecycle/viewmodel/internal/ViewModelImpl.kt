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
import kotlin.jvm.Volatile

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
    private val bagOfTags = mutableMapOf<String, AutoCloseable>()

    /**
     * @see [bagOfTags]
     */
    private val closeables = mutableSetOf<AutoCloseable>()

    @Volatile
    private var isCleared = false

    /**
     * Construct a new [ViewModel] instance.
     *
     * You should **never** manually construct a [ViewModel] outside of a
     * [androidx.lifecycle.ViewModelProvider.Factory].
     */
    constructor()

    /**
     * Construct a new [ViewModel] instance. Any [AutoCloseable] objects provided here
     * will be closed directly before [ViewModel.onCleared] is called.
     *
     * You should **never** manually construct a [ViewModel] outside of a
     * [androidx.lifecycle.ViewModelProvider.Factory].
     */
    constructor(vararg closeables: AutoCloseable) {
        this.closeables += closeables
    }

    @MainThread
    fun clear() {
        isCleared = true
        lock.withLock {
            for (value in bagOfTags.values) {
                // see comment for the similar call in `setTagIfAbsent`
                closeWithRuntimeException(value)
            }
            for (closeable in closeables) {
                closeWithRuntimeException(closeable)
            }
        }
        closeables.clear()
    }

    /**
     * Add a new [AutoCloseable] object that will be closed directly before
     * [ViewModel.onCleared] is called.
     *
     * If `onCleared()` has already been called, the closeable will not be added,
     * and will instead be closed immediately.
     *
     * @param key A key that allows you to retrieve the closeable passed in by using the same
     *            key with [ViewModel.getCloseable]
     * @param closeable The object that should be [AutoCloseable.close] directly before
     *                  [ViewModel.onCleared] is called.
     */
    fun addCloseable(key: String, closeable: AutoCloseable) {
        // Although no logic should be done after user calls onCleared(), we will
        // ensure that if it has already been called, the closeable attempting to
        // be added will be closed immediately to ensure there will be no leaks.
        if (isCleared) {
            closeWithRuntimeException(closeable)
            return
        }

        lock.withLock { bagOfTags.put(key, closeable) }
    }

    /**
     * Add a new [AutoCloseable] object that will be closed directly before
     * [ViewModel.onCleared] is called.
     *
     * If `onCleared()` has already been called, the closeable will not be added,
     * and will instead be closed immediately.
     *
     * @param closeable The object that should be [closed][AutoCloseable.close] directly before
     *                  [ViewModel.onCleared] is called.
     */
    fun addCloseable(closeable: AutoCloseable) {
        // Although no logic should be done after user calls onCleared(), we will
        // ensure that if it has already been called, the closeable attempting to
        // be added will be closed immediately to ensure there will be no leaks.
        if (isCleared) {
            closeWithRuntimeException(closeable)
            return
        }

        lock.withLock { this.closeables += closeable }
    }

    /**
     * Returns the closeable previously added with [ViewModel.addCloseable] with the given [key].
     *
     * @param key The key that was used to add the Closeable.
     */
    fun <T : AutoCloseable> getCloseable(key: String): T? =
        @Suppress("UNCHECKED_CAST")
        lock.withLock { bagOfTags[key] as T? }

    private fun closeWithRuntimeException(instance: Any) {
        if (instance is AutoCloseable) {
            try {
                instance.close()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
