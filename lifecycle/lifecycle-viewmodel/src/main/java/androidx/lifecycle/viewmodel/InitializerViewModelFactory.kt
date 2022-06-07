/*
 * Copyright 2022 The Android Open Source Project
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
import kotlin.reflect.KClass

@DslMarker
public annotation class ViewModelFactoryDsl

/**
 * Creates an [InitializerViewModelFactory] with the initializers provided in the builder.
 */
public inline fun viewModelFactory(
    builder: InitializerViewModelFactoryBuilder.() -> Unit
): ViewModelProvider.Factory = InitializerViewModelFactoryBuilder().apply(builder).build()

/**
 * DSL for constructing a new [ViewModelProvider.Factory]
 */
@ViewModelFactoryDsl
public class InitializerViewModelFactoryBuilder {
    private val initializers = mutableListOf<ViewModelInitializer<*>>()

    /**
     * Add the initializer for the given ViewModel class.
     *
     * @param clazz the class the initializer is associated with.
     * @param initializer lambda used to create an instance of the ViewModel class
     */
    fun <T : ViewModel> addInitializer(clazz: KClass<T>, initializer: CreationExtras.() -> T) {
        initializers.add(ViewModelInitializer(clazz.java, initializer))
    }

    /**
     * Build the InitializerViewModelFactory.
     */
    fun build(): ViewModelProvider.Factory =
        InitializerViewModelFactory(*initializers.toTypedArray())
}

/**
 * Add an initializer to the [InitializerViewModelFactoryBuilder]
 */
inline fun <reified VM : ViewModel> InitializerViewModelFactoryBuilder.initializer(
    noinline initializer: CreationExtras.() -> VM
) {
    addInitializer(VM::class, initializer)
}

/**
 * Holds a [ViewModel] class and initializer for that class
 */
class ViewModelInitializer<T : ViewModel>(
    internal val clazz: Class<T>,
    internal val initializer: CreationExtras.() -> T,
)

/**
 * A [ViewModelProvider.Factory] that allows you to add lambda initializers for handling
 * particular ViewModel classes using [CreationExtras], while using the default behavior for any
 * other classes.
 *
 * ```
 * val factory = viewModelFactory {
 *   initializer { TestViewModel(this[key]) }
 * }
 * val viewModel: TestViewModel = factory.create(TestViewModel::class.java, extras)
 * ```
 */
internal class InitializerViewModelFactory(
    private vararg val initializers: ViewModelInitializer<*>
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the given `Class`.
     *
     * This will use the initializer if one has been set for the class, otherwise it throw an
     * [IllegalArgumentException].
     *
     * @param modelClass a `Class` whose instance is requested
     * @param extras an additional information for this creation request
     * @return a newly created ViewModel
     *
     * @throws IllegalArgumentException if no initializer has been set for the given class.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        var viewModel: T? = null
        @Suppress("UNCHECKED_CAST")
        initializers.forEach {
            if (it.clazz == modelClass) {
                viewModel = it.initializer.invoke(extras) as? T
            }
        }
        return viewModel ?: throw IllegalArgumentException(
            "No initializer set for given class ${modelClass.name}"
        )
    }
}
