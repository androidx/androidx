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
import androidx.navigation.fragment.findNavController

/**
 * Returns a property delegate to access a [ViewModel] scoped to a navigation graph present on the
 * {@link NavController} back stack:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by navGraphViewModels(R.navigation.main)
 * }
 * ```
 *
 * Custom [ViewModelProvider.Factory] can be defined via [factoryProducer] parameter,
 * factory returned by it will be used to create [ViewModel]:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by navGraphViewModels(R.navigation.main) { myFactory }
 * }
 * ```
 *
 * This property can be accessed only after this NavGraph is on the NavController back stack,
 * and an attempt access prior to that will result in an IllegalArgumentException.
 *
 * @param navGraphId ID of a NavGraph that exists on the {@link NavController} back stack
 */
@MainThread
inline fun <reified VM : ViewModel> Fragment.navGraphViewModels(
    @IdRes navGraphId: Int,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val storeProducer: () -> ViewModelStore = {
        findNavController().getViewModelStoreOwner(navGraphId).viewModelStore
    }
    return createViewModelLazy(VM::class, storeProducer, factoryProducer)
}