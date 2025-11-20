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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.CreationExtras.Empty
import androidx.lifecycle.viewmodel.testing.internal.createScenarioExtras
import androidx.lifecycle.viewmodel.testing.internal.saveScenarioExtras
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.reflect.KClass

/**
 * [ViewModelScenario] provides API to start and drive a [ViewModel]'s lifecycle state for testing.
 *
 * [ViewModelScenario.recreate] allows you to simulate a System Process Death and restoration.
 *
 * [ViewModelScenario] does not clean up the [ViewModel] automatically. Call [close] in your test to
 * clean up the state or use [AutoCloseable.use] to ensure [ViewModelStore.clear] and
 * [ViewModel.onCleared] is invoked.
 */
@OptIn(ExperimentalStdlibApi::class)
public class ViewModelScenario<VM : ViewModel>
@PublishedApi
internal constructor(
    private val modelClass: KClass<VM>,
    private val modelFactory: Factory,
    initialModelExtras: CreationExtras = Empty,
) : AutoCloseable {

    /**
     * The current [CreationExtras] associated with the [viewModel]. This instance is updated when
     * [recreate] is executed.
     */
    private var modelExtras = createScenarioExtras(initialModelExtras)

    /**
     * The current [ViewModelProvider] responsible for retrieve the [ViewModel]. This instance is
     * updated when [recreate] is executed.
     */
    private var modelProvider =
        ViewModelProvider.create(
            owner = modelExtras[VIEW_MODEL_STORE_OWNER_KEY]!!,
            factory = modelFactory,
            extras = modelExtras,
        )

    /**
     * The current [ViewModel] being managed by this scenario. This instance is change if the
     * [ViewModelStore] is cleared or if [recreate] is invoked.
     */
    public val viewModel: VM
        get() = modelProvider[modelClass]

    /** Finishes the managed [ViewModel] and clear the [ViewModelStore]. */
    override fun close() {
        modelExtras[VIEW_MODEL_STORE_OWNER_KEY]!!.viewModelStore.clear()
    }

    /**
     * Simulates a System Process Death recreating the [ViewModel] and all associated components.
     *
     * This method:
     * - Saves the state of the [ViewModel] with [SavedStateRegistryController.performSave].
     * - Recreates the [ViewModelStoreOwner], [LifecycleOwner] and [SavedStateRegistryOwner].
     * - Recreates the [ViewModelProvider] instance.
     *
     * Call this method to verify that the [ViewModel] correctly preserves and restores its state.
     */
    public fun recreate() {
        val savedState = saveScenarioExtras(modelExtras)
        modelExtras = createScenarioExtras(initialExtras = modelExtras, restoredState = savedState)
        modelProvider =
            ViewModelProvider.create(
                owner = modelExtras[VIEW_MODEL_STORE_OWNER_KEY]!!,
                factory = modelFactory,
                extras = modelExtras,
            )
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
        modelFactory = factory,
        initialModelExtras = creationExtras,
    )
}
