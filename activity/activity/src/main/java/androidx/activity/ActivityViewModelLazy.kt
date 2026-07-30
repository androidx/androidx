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

package androidx.activity

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Returns a [Lazy] delegate to access the ComponentActivity's ViewModel, if [factoryProducer] is
 * specified then [ViewModelProvider.Factory] returned by it will be used to create [ViewModel]
 * first time.
 *
 * ```
 * class MyComponentActivity : ComponentActivity() {
 *     val viewmodel: MyViewModel by viewModels()
 * }
 * ```
 *
 * This property can be accessed only after the Activity is attached to the Application, and access
 * prior to that will result in IllegalArgumentException.
 */
@Deprecated("Superseded by viewModels that takes a CreationExtras", level = DeprecationLevel.HIDDEN)
@MainThread
public inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    noinline factoryProducer: (() -> Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: { defaultViewModelProviderFactory }

    return ViewModelLazy(
        VM::class,
        { viewModelStore },
        factoryPromise,
        { this.defaultViewModelCreationExtras },
    )
}

/**
 * Returns a [Lazy] delegate to access the ComponentActivity's ViewModel, if [factoryProducer] is
 * specified then [ViewModelProvider.Factory] returned by it will be used to create [ViewModel]
 * first time.
 *
 * ```
 * class MyComponentActivity : ComponentActivity() {
 *     val viewmodel: MyViewModel by viewModels()
 * }
 * ```
 *
 * This property can be accessed only after the Activity is attached to the Application, and access
 * prior to that will result in IllegalArgumentException.
 */
@MainThread
public inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline factoryProducer: (() -> Factory)? = null,
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: { defaultViewModelProviderFactory }

    return ViewModelLazy(
        VM::class,
        { viewModelStore },
        factoryPromise,
        { extrasProducer?.invoke() ?: this.defaultViewModelCreationExtras },
    )
}

/**
 * Returns a [Lazy] delegate for this [ComponentActivity]'s [ViewModel] identified by [key].
 *
 * Delegates with the same [key] and [ViewModel] type share the same instance. Different keys allow
 * multiple instances of the same [ViewModel] type in this activity.
 *
 * ```
 * class MyComponentActivity : ComponentActivity() {
 *     val primaryViewModel: MyViewModel by viewModels(key = "primary")
 *     val secondaryViewModel: MyViewModel by viewModels(key = "secondary")
 * }
 * ```
 *
 * This property can be accessed only after the Activity is attached to the Application. Access
 * before that results in an [IllegalArgumentException].
 *
 * @param key identifier used to store and retrieve the [ViewModel]
 * @param extrasProducer producer of the [CreationExtras] used to create the [ViewModel]
 * @param factoryProducer producer of the [Factory] used to create the [ViewModel]
 */
@MainThread
public inline fun <reified T : ViewModel> ComponentActivity.viewModels(
    key: String,
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline factoryProducer: (() -> Factory)? = null,
): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) {
        val factory = factoryProducer?.invoke() ?: defaultViewModelProviderFactory
        val extras = extrasProducer?.invoke() ?: defaultViewModelCreationExtras
        ViewModelProvider(viewModelStore, factory, extras)[key, T::class.java]
    }
