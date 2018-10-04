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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import kotlin.reflect.KClass

/**
 * Returns a [Lazy] delegate to access FragmentActivity's viewmodel, if [factory] is specified
 * it will be used to create [ViewModel] first time.
 *
 * ```
 * class MyFragmentActivity : FragmentActivity() {
 *     val viewmodel: MyViewModel by viewmodels()
 * }
 * ```
 *
 * This property can be accessed only after the Activity is attached to the Application,
 * and access prior to that will result in IllegalArgumentException.
 */
@MainThread
inline fun <reified VM : ViewModel> FragmentActivity.viewModels(
    factory: ViewModelProvider.Factory? = null
): Lazy<VM> = ActivityViewModelLazy(this, VM::class, factory)

/**
 * An implementation of [Lazy] used by [FragmentActivity.viewModels] tied to the given [activity],
 * [viewModelClass], [factory]
 */
class ActivityViewModelLazy<VM : ViewModel>(
    private val activity: FragmentActivity,
    private val viewModelClass: KClass<VM>,
    private val factory: ViewModelProvider.Factory?
) : Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        get() {
            var viewModel = cached
            if (viewModel == null) {
                val application = activity.application
                        ?: throw IllegalArgumentException("ViewModel can be accessed " +
                                "only when Activity is attached")
                val resolvedFactory = factory ?: AndroidViewModelFactory.getInstance(application)
                viewModel = ViewModelProvider(activity, resolvedFactory).get(viewModelClass.java)
                cached = viewModel
            }
            return viewModel
        }

    override fun isInitialized() = cached != null
}