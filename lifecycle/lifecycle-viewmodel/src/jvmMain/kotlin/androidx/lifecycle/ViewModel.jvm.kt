/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:JvmName("ViewModel")

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.lifecycle.viewmodel.internal.ViewModelImpl
import java.io.Closeable

public actual abstract class ViewModel {

    /**
     * Internal implementation of the multiplatform [ViewModel].
     *
     * **Why is it nullable?** Since [clear] is final, this method is still called on mock
     * objects. In those cases, [impl] is `null`. It'll always be empty though because
     * [addCloseable] and [getCloseable] are open so we can skip clearing it.
     */
    private val impl: ViewModelImpl?

    public actual constructor() {
        impl = ViewModelImpl()
    }

    public actual constructor(vararg closeables: AutoCloseable) {
        impl = ViewModelImpl(*closeables)
    }

    /**
     * Construct a new ViewModel instance. Any [AutoCloseable] objects provided here
     * will be closed directly before [ViewModel.onCleared] is called.
     *
     * You should **never** manually construct a ViewModel outside of a
     * [ViewModelProvider.Factory].
     */
    @Deprecated(message = "Replaced by `AutoCloseable` overload.", level = DeprecationLevel.HIDDEN)
    public constructor(vararg closeables: Closeable) {
        impl = ViewModelImpl(*closeables)
    }

    protected actual open fun onCleared() {}

    @MainThread
    internal actual fun clear() {
        impl?.clear()
        onCleared()
    }

    public actual fun addCloseable(key: String, closeable: AutoCloseable) {
        impl?.addCloseable(key, closeable)
    }

    public actual open fun addCloseable(closeable: AutoCloseable) {
        impl?.addCloseable(closeable)
    }

    /**
     * Add a new [Closeable] object that will be closed directly before
     * [ViewModel.onCleared] is called.
     *
     * If `onCleared()` has already been called, the closeable will not be added,
     * and will instead be closed immediately.
     *
     * @param closeable The object that should be [closed][Closeable.close] directly before
     *                  [ViewModel.onCleared] is called.
     */
    @Deprecated(message = "Replaced by `AutoCloseable` overload.", level = DeprecationLevel.HIDDEN)
    public open fun addCloseable(closeable: Closeable) {
        impl?.addCloseable(closeable)
    }

    public actual fun <T : AutoCloseable> getCloseable(key: String): T? = impl?.getCloseable(key)
}
