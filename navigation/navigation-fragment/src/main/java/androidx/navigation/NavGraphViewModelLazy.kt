/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation

import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController

/**
 * Returns a property delegate to access a [ViewModel] scoped to a navigation graph present on the
 * [NavController] back stack:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by navGraphViewModels(R.id.main)
 * }
 * ```
 *
 * Custom [ViewModelProvider.Factory] can be defined via [factoryProducer] parameter,
 * factory returned by it will be used to create [ViewModel]:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by navGraphViewModels(R.id.main) { myFactory }
 * }
 * ```
 *
 * This property can be accessed only after this NavGraph is on the NavController back stack,
 * and an attempt access prior to that will result in an IllegalArgumentException.
 *
 * @param navGraphId ID of a NavGraph that exists on the [NavController] back stack
 * @param factoryProducer lambda that will be called during initialization to return
 * [ViewModelProvider.Factory]. If none is provided, this will use the factory from the
 * [NavBackStackEntry] referenced by the [navGraphId].
 */
@Deprecated(
    "Superseded by navGraphViewModels that takes a CreationExtras producer",
    level = DeprecationLevel.HIDDEN
)
@MainThread
public inline fun <reified VM : ViewModel> Fragment.navGraphViewModels(
    @IdRes navGraphId: Int,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val backStackEntry by lazy {
        findNavController().getBackStackEntry(navGraphId)
    }
    val storeProducer: () -> ViewModelStore = {
        backStackEntry.viewModelStore
    }
    return createViewModelLazy(
        VM::class, storeProducer,
        { backStackEntry.defaultViewModelCreationExtras },
        factoryProducer ?: { backStackEntry.defaultViewModelProviderFactory }
    )
}

/**
 * Returns a property delegate to access a [ViewModel] scoped to a navigation graph present on the
 * [NavController] back stack:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by navGraphViewModels(R.id.main)
 * }
 * ```
 *
 * Custom [ViewModelProvider.Factory] can be defined via [factoryProducer] parameter,
 * factory returned by it will be used to create [ViewModel]:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by navGraphViewModels(R.id.main) { myFactory }
 * }
 * ```
 *
 * This property can be accessed only after this NavGraph is on the NavController back stack,
 * and an attempt access prior to that will result in an IllegalArgumentException.
 *
 * @param navGraphId ID of a NavGraph that exists on the [NavController] back stack
 * @param extrasProducer lambda that will be called during initialization to return
 * [CreationExtras]. If none is provided, this will use the extras from the [NavBackStackEntry]
 * referenced by the [navGraphId].
 * @param factoryProducer lambda that will be called during initialization to return
 * [ViewModelProvider.Factory]. If none is provided, this will use the factory from the
 * [NavBackStackEntry] referenced by the [navGraphId].
 */
@MainThread
public inline fun <reified VM : ViewModel> Fragment.navGraphViewModels(
    @IdRes navGraphId: Int,
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val backStackEntry by lazy {
        findNavController().getBackStackEntry(navGraphId)
    }
    val storeProducer: () -> ViewModelStore = {
        backStackEntry.viewModelStore
    }
    return createViewModelLazy(
        VM::class, storeProducer,
        { extrasProducer?.invoke() ?: backStackEntry.defaultViewModelCreationExtras },
        factoryProducer ?: { backStackEntry.defaultViewModelProviderFactory }
    )
}

/**
 * Returns a property delegate to access a [ViewModel] scoped to a navigation graph present on the
 * [NavController] back stack:
 * ```
 * class MyFragment : Fragment() {
 *     val viewModel: MainViewModel by navGraphViewModels("main")
 * }
 * ```
 *
 * Custom [ViewModelProvider.Factory] can be defined via [factoryProducer] parameter,
 * factory returned by it will be used to create [ViewModel]:
 * ```
 * class MyFragment : Fragment() {
 *     val viewModel: MainViewModel by navGraphViewModels("main") { myFactory }
 * }
 * ```
 *
 * This property can be accessed only after this NavGraph is on the NavController back stack,
 * and an attempt access prior to that will result in an IllegalArgumentException.
 *
 * @param navGraphRoute [NavDestination.route] of a NavGraph that exists on the [NavController]
 * back stack. If a [NavDestination] with the given route does not exist on the back stack, an
 * [IllegalArgumentException] will be thrown.
 * @param factoryProducer lambda that will be called during initialization to return
 * [ViewModelProvider.Factory]. If none is provided, this will use the factory from the
 * [NavBackStackEntry] referenced by the [navGraphRoute].
 */
@Deprecated(
    "Superseded by navGraphViewModels that takes a CreationExtras producer",
    level = DeprecationLevel.HIDDEN
)
@MainThread
public inline fun <reified VM : ViewModel> Fragment.navGraphViewModels(
    navGraphRoute: String,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val backStackEntry by lazy {
        findNavController().getBackStackEntry(navGraphRoute)
    }
    val storeProducer: () -> ViewModelStore = {
        backStackEntry.viewModelStore
    }
    return createViewModelLazy(
        VM::class, storeProducer,
        { backStackEntry.defaultViewModelCreationExtras },
        factoryProducer ?: { backStackEntry.defaultViewModelProviderFactory }
    )
}

/**
 * Returns a property delegate to access a [ViewModel] scoped to a navigation graph present on the
 * [NavController] back stack:
 * ```
 * class MyFragment : Fragment() {
 *     val viewModel: MainViewModel by navGraphViewModels("main")
 * }
 * ```
 *
 * Custom [ViewModelProvider.Factory] can be defined via [factoryProducer] parameter,
 * factory returned by it will be used to create [ViewModel]:
 * ```
 * class MyFragment : Fragment() {
 *     val viewModel: MainViewModel by navGraphViewModels("main") { myFactory }
 * }
 * ```
 *
 * This property can be accessed only after this NavGraph is on the NavController back stack,
 * and an attempt access prior to that will result in an IllegalArgumentException.
 *
 * @param navGraphRoute [NavDestination.route] of a NavGraph that exists on the [NavController]
 * back stack. If a [NavDestination] with the given route does not exist on the back stack, an
 * [IllegalArgumentException] will be thrown.
 * @param extrasProducer lambda that will be called during initialization to return
 * [CreationExtras]. If none is provided, this will use the extras from the [NavBackStackEntry]
 * referenced by the [navGraphRoute].
 * @param factoryProducer lambda that will be called during initialization to return
 * [ViewModelProvider.Factory]. If none is provided, this will use the factory from the
 * [NavBackStackEntry] referenced by the [navGraphRoute].
 */
@MainThread
public inline fun <reified VM : ViewModel> Fragment.navGraphViewModels(
    navGraphRoute: String,
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val backStackEntry by lazy {
        findNavController().getBackStackEntry(navGraphRoute)
    }
    val storeProducer: () -> ViewModelStore = {
        backStackEntry.viewModelStore
    }
    return createViewModelLazy(
        VM::class, storeProducer,
        { extrasProducer?.invoke() ?: backStackEntry.defaultViewModelCreationExtras },
        factoryProducer ?: { backStackEntry.defaultViewModelProviderFactory }
    )
}
