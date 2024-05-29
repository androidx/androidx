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

package androidx.lifecycle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.internal.ViewModelProviders
import kotlin.reflect.KClass

/**
 * Internal implementation of the multiplatform [ViewModelProvider].
 *
 * Kotlin Multiplatform does not support expect class with default implementation yet, so we
 * extracted the common logic used by all platforms to this internal class.
 *
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-20427">KT-20427</a>
 */
internal class ViewModelProviderImpl(
    private val store: ViewModelStore,
    private val factory: ViewModelProvider.Factory,
    private val extras: CreationExtras,
) {

    constructor(
        owner: ViewModelStoreOwner,
        factory: ViewModelProvider.Factory,
        extras: CreationExtras,
    ) : this(owner.viewModelStore, factory, extras)

    @Suppress("UNCHECKED_CAST")
    internal fun <T : ViewModel> getViewModel(
        modelClass: KClass<T>,
        key: String = ViewModelProviders.getDefaultKey(modelClass),
    ): T {
        val viewModel = store[key]
        if (modelClass.isInstance(viewModel)) {
            if (factory is ViewModelProvider.OnRequeryFactory) {
                factory.onRequery(viewModel!!)
            }
            return viewModel as T
        } else {
            @Suppress("ControlFlowWithEmptyBody") if (viewModel != null) {
                // TODO: log a warning.
            }
        }
        val extras = MutableCreationExtras(extras)
        extras[ViewModelProviders.ViewModelKey] = key

        return createViewModel(factory, modelClass, extras).also { vm -> store.put(key, vm) }
    }
}

/**
 * Returns a new instance of a [ViewModel].
 *
 * **Important:** Android targets using `compileOnly` dependencies may encounter AGP desugaring
 * issues where `Factory.create` throws an `AbstractMethodError`. This is resolved by an
 * Android-specific implementation that first attempts all `ViewModelProvider.Factory.create`
 * method overloads before allowing the exception to propagate.
 *
 * @see <a href="https://b.corp.google.com/issues/230454566">b/230454566</a>
 * @see <a href="https://b.corp.google.com/issues/341792251">b/341792251</a>
 * @see <a href="https://github.com/square/leakcanary/issues/2314">leakcanary/issues/2314</a>
 * @see <a href="https://github.com/square/leakcanary/issues/2677">leakcanary/issues/2677</a>
 */
internal expect fun <VM : ViewModel> createViewModel(
    factory: ViewModelProvider.Factory,
    modelClass: KClass<VM>,
    extras: CreationExtras,
): VM
