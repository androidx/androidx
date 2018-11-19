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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.ViewModelProvider.Factory
import kotlin.reflect.KClass

/**
 * Returns a [Lazy] delegate to access the ComponentActivity's ViewModel, if [factoryProducer]
 * is specified then [ViewModelProvider.Factory] returned by it will be used
 * to create [ViewModel] first time.
 *
 * ```
 * class MyComponentActivity : ComponentActivity() {
 *     val viewmodel: MyViewModel by viewmodels()
 * }
 * ```
 *
 * This property can be accessed only after the Activity is attached to the Application,
 * and access prior to that will result in IllegalArgumentException.
 */
@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    noinline factoryProducer: (() -> Factory)? = null
): Lazy<VM> = ActivityViewModelLazy(this, VM::class, factoryProducer)

/**
 * An implementation of [Lazy] used by [ComponentActivity.viewModels] tied to the given [activity],
 * [viewModelClass], [factoryProducer]
 */
class ActivityViewModelLazy<VM : ViewModel>(
    private val activity: ComponentActivity,
    private val viewModelClass: KClass<VM>,
    private val factoryProducer: (() -> Factory)?
) : Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        get() {
            var viewModel = cached
            if (viewModel == null) {
                val application = activity.application
                        ?: throw IllegalArgumentException("ViewModel can be accessed " +
                                "only when Activity is attached")
                val resolvedFactory = factoryProducer?.invoke()
                    ?: AndroidViewModelFactory.getInstance(application)
                viewModel = ViewModelProvider(activity, resolvedFactory).get(viewModelClass.java)
                cached = viewModel
            }
            return viewModel
        }

    override fun isInitialized() = cached != null
}
