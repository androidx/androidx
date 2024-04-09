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

import androidx.lifecycle.ViewModel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Key used to [ViewModel.addCloseable] the [CoroutineScope] associated with a [ViewModel].
 *
 * @see androidx.lifecycle.viewmodel.internal.ViewModelImpl
 * @see androidx.lifecycle.viewModelScope
 */
internal const val VIEW_MODEL_SCOPE_KEY =
    "androidx.lifecycle.viewmodel.internal.ViewModelCoroutineScope.JOB_KEY"

/**
 * Creates a [CloseableCoroutineScope] intended for [ViewModel] use.
 *
 * The [CoroutineScope.coroutineContext] is configured with:
 * - [SupervisorJob]: ensures children jobs can fail independently of each other.
 * - [MainCoroutineDispatcher.immediate]: executes jobs immediately on the main (UI) thread. If
 *  the [Dispatchers.Main] is not available on the current platform (e.g., Linux), we fallback to
 *  an [EmptyCoroutineContext].
 *
 * For background execution, use [kotlinx.coroutines.withContext] to switch to appropriate
 * dispatchers (e.g., [kotlinx.coroutines.IO]).
 */
internal fun createViewModelScope(): CloseableCoroutineScope {
    val dispatcher = try {
        Dispatchers.Main.immediate
    } catch (_: NotImplementedError) {
        // In platforms where `Dispatchers.Main` is not available, Kotlin Multiplatform will throw
        // a `NotImplementedError`. Since there's no direct functional alternative, we use
        // `EmptyCoroutineContext` to ensure a `launch` will run in the same context as the caller.
        EmptyCoroutineContext
    }
    return CloseableCoroutineScope(coroutineContext = dispatcher + SupervisorJob())
}

/** Represents this [CoroutineScope] as a [AutoCloseable]. */
internal fun CoroutineScope.asCloseable() = CloseableCoroutineScope(coroutineScope = this)

/**
 * [CoroutineScope] that provides a method to [close] it, causing the rejection of any new tasks and
 * cleanup of all underlying resources associated with the scope.
 */
internal class CloseableCoroutineScope(
    override val coroutineContext: CoroutineContext,
) : AutoCloseable, CoroutineScope {

    constructor(coroutineScope: CoroutineScope) : this(coroutineScope.coroutineContext)

    override fun close() = coroutineContext.cancel()
}
