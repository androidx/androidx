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

package androidx.lifecycle.viewmodel.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.CreationExtras.Empty
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.reflect.KClass

/**
 * [ViewModelScenario] provides API to start and drive a [ViewModel]'s lifecycle state for testing.
 */
@OptIn(ExperimentalStdlibApi::class)
public class ViewModelScenario<VM : ViewModel>
@PublishedApi
internal constructor(
    private val modelClass: KClass<VM>,
    private val modelStore: ViewModelStore,
    modelFactory: Factory,
    modelExtras: CreationExtras = Empty,
) : AutoCloseable {

    private val modelProvider = ViewModelProvider.create(modelStore, modelFactory, modelExtras)

    /** The current [ViewModel]. The instance might update if the [ViewModelStore] is cleared. */
    public val viewModel: VM
        get() = modelProvider[modelClass]

    /** Finishes the managed [ViewModel] and clear the [ViewModelStore]. */
    override fun close() {
        modelStore.clear()
    }
}

/**
 * Creates a [ViewModelScenario] using a given [VM] class as key, an [initializer] as a
 * [ViewModelProvider.Factory] and a [creationExtras] as default extras.
 *
 * You should access the [ViewModel] instance using [ViewModelScenario.viewModel], and clear the
 * [ViewModelStore] using [ViewModelScenario.close].
 *
 * Example usage:
 * ```
 * viewModelScenario { MyViewModel(parameters) }
 *   .use { scenario ->
 *     val vm = scenario.viewModel
 *     // Use the ViewModel
 *   }
 * ```
 *
 * @param VM The reified [ViewModel] class to be created.
 * @param creationExtras Additional data passed to the [Factory] during a [ViewModel] creation.
 * @param initializer A [Factory] function to create a [ViewModel].
 */
public inline fun <reified VM : ViewModel> viewModelScenario(
    creationExtras: CreationExtras = DefaultCreationExtras(),
    noinline initializer: CreationExtras.() -> VM,
): ViewModelScenario<VM> {
    return viewModelScenario(
        creationExtras = creationExtras,
        factory = viewModelFactory { addInitializer(VM::class, initializer) },
    )
}

/**
 * Creates a [ViewModelScenario] using a given [VM] class as key, an [factory] and a
 * [creationExtras] as default extras.
 *
 * You should access the [ViewModel] instance using [ViewModelScenario.viewModel], and clear the
 * [ViewModelStore] using [ViewModelScenario.close].
 *
 * Example usage:
 * ```
 * val myFactory: ViewModelProvider.Factory = MyViewModelFactory()
 * viewModelScenario<MyViewModel>(myFactory)
 *   .use { scenario ->
 *     val vm = scenario.viewModel
 *     // Use the ViewModel
 *   }
 * ```
 *
 * @param VM The reified [ViewModel] class to be created.
 * @param creationExtras Additional data passed to the [Factory] during a [ViewModel] creation.
 * @param factory A [Factory] to create a [ViewModel].
 */
public inline fun <reified VM : ViewModel> viewModelScenario(
    factory: Factory,
    creationExtras: CreationExtras = DefaultCreationExtras(),
): ViewModelScenario<VM> {
    return ViewModelScenario(
        modelClass = VM::class,
        modelStore = ViewModelStore(),
        modelFactory = factory,
        modelExtras = creationExtras,
    )
}
