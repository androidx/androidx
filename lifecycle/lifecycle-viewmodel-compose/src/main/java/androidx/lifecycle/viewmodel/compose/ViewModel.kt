/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Returns an existing [ViewModel] or creates a new one in the given owner (usually, a fragment or
 * an activity), defaulting to the owner provided by [LocalViewModelStoreOwner].
 *
 * The created [ViewModel] is associated with the given [viewModelStoreOwner] and will be retained
 * as long as the owner is alive (e.g. if it is an activity, until it is
 * finished or process is killed).
 *
 * @param viewModelStoreOwner The owner of the [ViewModel] that controls the scope and lifetime
 * of the returned [ViewModel]. Defaults to using [LocalViewModelStoreOwner].
 * @param key The key to use to identify the [ViewModel].
 * @param factory The [ViewModelProvider.Factory] that should be used to create the [ViewModel]
 * or null if you would like to use the default factory from the [LocalViewModelStoreOwner]
 * @return A [ViewModel] that is an instance of the given [VM] type.
 */
@Suppress("MissingJvmstatic")
@Composable
public inline fun <reified VM : ViewModel> viewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    factory: ViewModelProvider.Factory? = null
): VM = viewModel(VM::class.java, viewModelStoreOwner, key, factory)

/**
 * Returns an existing [ViewModel] or creates a new one in the scope (usually, a fragment or
 * an activity)
 *
 * The created [ViewModel] is associated with the given [viewModelStoreOwner] and will be retained
 * as long as the scope is alive (e.g. if it is an activity, until it is
 * finished or process is killed).
 *
 * @param modelClass The class of the [ViewModel] to create an instance of it if it is not
 * present.
 * @param viewModelStoreOwner The scope that the created [ViewModel] should be associated with.
 * @param key The key to use to identify the [ViewModel].
 * @return A [ViewModel] that is an instance of the given [VM] type.
 */
@Suppress("MissingJvmstatic")
@Composable
public fun <VM : ViewModel> viewModel(
    modelClass: Class<VM>,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    factory: ViewModelProvider.Factory? = null
): VM = viewModelStoreOwner.get(modelClass, key, factory)

private fun <VM : ViewModel> ViewModelStoreOwner.get(
    javaClass: Class<VM>,
    key: String? = null,
    factory: ViewModelProvider.Factory? = null
): VM {
    val provider = if (factory != null) {
        ViewModelProvider(this, factory)
    } else {
        ViewModelProvider(this)
    }
    return if (key != null) {
        provider.get(key, javaClass)
    } else {
        provider.get(javaClass)
    }
}
