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
        // AGP has some desugaring issues associated with compileOnly dependencies so we need to
        // fall back to the other create method to keep from crashing.
        return try {
            factory.create(modelClass, extras)
        } catch (e: Error) { // There is no `AbstractMethodError` on KMP, using common ancestor.
            factory.create(modelClass, CreationExtras.Empty)
        }.also { newViewModel -> store.put(key, newViewModel) }
    }
}
