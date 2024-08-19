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

package androidx.hilt.navigation.fragment

import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.lifecycle.withCreationCallback

/**
 * Returns a property delegate to access a
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] scoped to a navigation graph present on the [NavController] back stack:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by hiltNavGraphViewModels(R.navigation.main)
 * }
 * ```
 *
 * This property can be accessed only after this NavGraph is on the NavController back stack, and an
 * attempt access prior to that will result in an IllegalArgumentException.
 *
 * @param navGraphId ID of a NavGraph that exists on the [NavController] back stack
 */
@MainThread
public inline fun <reified VM : ViewModel> Fragment.hiltNavGraphViewModels(
    @IdRes navGraphId: Int
): Lazy<VM> {
    val backStackEntry by lazy { findNavController().getBackStackEntry(navGraphId) }
    val storeProducer: () -> ViewModelStore = { backStackEntry.viewModelStore }
    return createViewModelLazy(
        VM::class,
        storeProducer,
        factoryProducer = {
            HiltViewModelFactory(requireActivity(), backStackEntry.defaultViewModelProviderFactory)
        },
        extrasProducer = { backStackEntry.defaultViewModelCreationExtras }
    )
}

/**
 * Returns a property delegate to access a
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] with an [@AssistedInject]-annotated constructor that is scoped to a
 * navigation graph present on the [NavController] back stack:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by hiltNavGraphViewModels(R.navigation.main) { factory: MainViewModelFactory ->
 *         factory.create(...)
 *     }
 * }
 * ```
 *
 * This property can be accessed only after this NavGraph is on the NavController back stack, and an
 * attempt access prior to that will result in an IllegalArgumentException.
 *
 * @param navGraphId ID of a NavGraph that exists on the [NavController] back stack
 * @param creationCallback callback that takes an @AssistedFactory-annotated factory and creates a
 *   HiltViewModel using @AssistedInject-annotated constructor.
 */
@MainThread
public inline fun <reified VM : ViewModel, reified VMF : Any> Fragment.hiltNavGraphViewModels(
    @IdRes navGraphId: Int,
    noinline creationCallback: (VMF) -> VM
): Lazy<VM> {
    val backStackEntry by lazy { findNavController().getBackStackEntry(navGraphId) }
    val storeProducer: () -> ViewModelStore = { backStackEntry.viewModelStore }
    return createViewModelLazy(
        viewModelClass = VM::class,
        storeProducer = storeProducer,
        factoryProducer = {
            HiltViewModelFactory(requireActivity(), backStackEntry.defaultViewModelProviderFactory)
        },
        extrasProducer = {
            backStackEntry.defaultViewModelCreationExtras.withCreationCallback(creationCallback)
        }
    )
}
