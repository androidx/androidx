/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.fragment.app

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.reflect.KClass

/**
 * Returns a property delegate to access [ViewModel] by **default** scoped to this [Fragment]:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: NYViewModel by viewmodels()
 * }
 * ```
 *
 * Custom [ViewModelProvider.Factory] can be defined via [factoryProducer] parameter,
 * factory returned by it will be used to create [ViewModel]:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MYViewModel by viewmodels { myFactory }
 * }
 * ```
 *
 * Default scope may be overridden with parameter [ownerProducer]:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MYViewModel by viewmodels ({requireParentFragment()})
 * }
 * ```
 *
 * This property can be accessed only after this Fragment is attached i.e., after
 * [Fragment.onAttach()], and access prior to that will result in IllegalArgumentException.
 */
@MainThread
inline fun <reified VM : ViewModel> Fragment.viewModels(
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline factoryProducer: (() -> Factory)? = null
) = createViewModelLazy(VM::class, { ownerProducer().viewModelStore }, factoryProducer)

/**
 * Returns a property delegate to access parent activity's [ViewModel],
 * if [factoryProducer] is specified then [ViewModelProvider.Factory]
 * returned by it will be used to create [ViewModel] first time. Otherwise, the activity's
 * [ViewModelStoreOwner.getDefaultViewModelProviderFactory](default factory) will be used.
 *
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MyViewModel by activityViewModels()
 * }
 * ```
 *
 * This property can be accessed only after this Fragment is attached i.e., after
 * [Fragment.onAttach()], and access prior to that will result in IllegalArgumentException.
 */
@MainThread
inline fun <reified VM : ViewModel> Fragment.activityViewModels(
    noinline factoryProducer: (() -> Factory)? = null
) = createViewModelLazy(VM::class, { requireActivity().viewModelStore },
    factoryProducer ?: { requireActivity().defaultViewModelProviderFactory })

/**
 * Helper method for creation of [ViewModelLazy], that resolves `null` passed as [factoryProducer]
 * to default factory.
 */
@MainThread
fun <VM : ViewModel> Fragment.createViewModelLazy(
    viewModelClass: KClass<VM>,
    storeProducer: () -> ViewModelStore,
    factoryProducer: (() -> Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }
    return ViewModelLazy(viewModelClass, storeProducer, factoryPromise)
}