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
@file:OptIn(ExperimentalStdlibApi::class)

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.lifecycle.viewmodel.internal.ViewModelImpl
import kotlinx.coroutines.CoroutineScope

public actual abstract class ViewModel {

    private val impl: ViewModelImpl

    public actual constructor() {
        impl = ViewModelImpl()
    }

    public actual constructor(viewModelScope: CoroutineScope) {
        impl = ViewModelImpl(viewModelScope)
    }

    public actual constructor(vararg closeables: AutoCloseable) {
        impl = ViewModelImpl(*closeables)
    }

    public actual constructor(viewModelScope: CoroutineScope, vararg closeables: AutoCloseable) {
        impl = ViewModelImpl(viewModelScope, *closeables)
    }

    protected actual open fun onCleared() {}

    @MainThread
    internal actual fun clear() {
        impl.clear()
        onCleared()
    }

    public actual fun addCloseable(key: String, closeable: AutoCloseable) {
        impl.addCloseable(key, closeable)
    }

    public actual open fun addCloseable(closeable: AutoCloseable) {
        impl.addCloseable(closeable)
    }

    public actual fun <T : AutoCloseable> getCloseable(key: String): T? = impl.getCloseable(key)
}
