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

package androidx.lifecycle.viewmodel.internal

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.InitializerViewModelFactory
import androidx.lifecycle.viewmodel.ViewModelInitializer
import kotlin.reflect.KClass

/**
 * [ViewModelProviders] provides common helper functionalities.
 *
 * Kotlin Multiplatform does not support expect class with default implementation yet, so we
 * extracted the common logic used by all platforms to this internal class.
 *
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-20427">KT-20427</a>
 */
internal object ViewModelProviders {

    private const val VIEW_MODEL_PROVIDER_DEFAULT_KEY: String =
        "androidx.lifecycle.ViewModelProvider.DefaultKey"

    internal fun <T : ViewModel> getDefaultKey(modelClass: KClass<T>): String {
        val canonicalName = requireNotNull(modelClass.qualifiedName) {
            "Local and anonymous classes can not be ViewModels"
        }
        return "$VIEW_MODEL_PROVIDER_DEFAULT_KEY:$canonicalName"
    }

    internal object ViewModelKey : CreationExtras.Key<String>

    internal fun <VM : ViewModel> unsupportedCreateViewModel(): VM =
        throw UnsupportedOperationException(
            "`Factory.create(String, CreationExtras)` is not implemented. You may need to " +
                "override the method and provide a custom implementation. Note that using " +
                "`Factory.create(String)` is not supported and considered an error."
        )

    internal fun createInitializerFactory(
        initializers: Collection<ViewModelInitializer<*>>,
    ): ViewModelProvider.Factory = InitializerViewModelFactory(*initializers.toTypedArray())

    internal fun createInitializerFactory(
        vararg initializers: ViewModelInitializer<*>,
    ): ViewModelProvider.Factory = InitializerViewModelFactory(*initializers)

    internal fun getDefaultFactory(owner: ViewModelStoreOwner): ViewModelProvider.Factory =
        if (owner is HasDefaultViewModelProviderFactory) {
            owner.defaultViewModelProviderFactory
        } else {
            DefaultViewModelProviderFactory
        }

    internal fun getDefaultCreationExtras(owner: ViewModelStoreOwner): CreationExtras =
        if (owner is HasDefaultViewModelProviderFactory) {
            owner.defaultViewModelCreationExtras
        } else {
            CreationExtras.Empty
        }

    internal fun <VM : ViewModel> createViewModelFromInitializers(
        modelClass: KClass<VM>,
        extras: CreationExtras,
        vararg initializers: ViewModelInitializer<*>,
    ): VM {
        @Suppress("UNCHECKED_CAST")
        val viewModel = initializers.firstOrNull { it.clazz == modelClass }
            ?.initializer
            ?.invoke(extras) as VM?
        return requireNotNull(viewModel) {
            "No initializer set for given class ${modelClass.qualifiedName}"
        }
    }
}
