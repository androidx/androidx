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

package androidx.hilt.lifecycle.viewmodel.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.HiltViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.defaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.lifecycle.withCreationCallback

/**
 * Returns an existing
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] or creates a new one scoped to the current [ViewModelStoreOwner].
 */
@Composable
public inline fun <reified VM : ViewModel> hiltViewModel(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
    key: String? = null,
): VM {
    val factory = rememberHiltViewModelFactory(viewModelStoreOwner.defaultViewModelProviderFactory)
    return viewModel(viewModelStoreOwner, key, factory = factory)
}

/**
 * Returns an existing
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] with an [@AssistedInject]-annotated constructor or creates a new one
 * scoped to the current [ViewModelStoreOwner].
 */
@Composable
public inline fun <reified VM : ViewModel, reified VMF> hiltViewModel(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
    key: String? = null,
    noinline creationCallback: (VMF) -> VM,
): VM {
    return viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
        factory = rememberHiltViewModelFactory(viewModelStoreOwner.defaultViewModelProviderFactory),
        extras =
            viewModelStoreOwner.defaultViewModelCreationExtras.withCreationCallback(
                creationCallback
            ),
    )
}

/**
 * Remembers a [ViewModelProvider.Factory] that allows the creation of
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated `ViewModel` instances within Compose.
 *
 * This factory is bound to the [LocalContext], which should normally be an
 * `@AndroidEntryPoint`-annotated component (like a `ComponentActivity`).
 *
 * You can pass this factory to `viewModel(factory)`, `rememberViewModelStoreOwner(defaultFactory)`
 * or other state-holders that require a factory to properly inject Hilt dependencies into your
 * ViewModels.
 *
 * @param delegateFactory A fallback [ViewModelProvider.Factory] used to instantiate ViewModels that
 *   are not annotated with [HiltViewModel]. Defaults to the default factory of the current
 *   [LocalViewModelStoreOwner].
 * @return A remembered [ViewModelProvider.Factory] for Hilt-injected ViewModels.
 */
@Composable
public fun rememberHiltViewModelFactory(
    delegateFactory: ViewModelProvider.Factory =
        LocalViewModelStoreOwner.current.defaultViewModelProviderFactory
): ViewModelProvider.Factory {
    val context = LocalContext.current
    return remember(context, delegateFactory) { HiltViewModelFactory(context, delegateFactory) }
}

/** @deprecated This function is kept purely to preserve binary compatibility. */
@Deprecated("Replaced by `rememberHiltViewModelFactory`.")
@Suppress("RedundantNullableReturnType")
@Composable
@PublishedApi
internal fun createHiltViewModelFactory(
    viewModelStoreOwner: ViewModelStoreOwner
): ViewModelProvider.Factory? =
    rememberHiltViewModelFactory(viewModelStoreOwner.defaultViewModelProviderFactory)
