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
package androidx.lifecycle

import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

/**
 * An implementation of [Lazy] to facilitate lazy retrieval of [ViewModel] instances.
 *
 * [storeProducer] is a lambda called during initialization; the [ViewModel] is created within the
 * returned [ViewModelStore].
 *
 * [factoryProducer] is a lambda called during initialization; the returned
 * [ViewModelProvider.Factory] is used to create the [ViewModel].
 *
 * [extrasProducer] is a lambda called during initialization; the returned [CreationExtras] are used
 * during the creation of the [ViewModel].
 */
public class ViewModelLazy<VM : ViewModel>
@JvmOverloads
constructor(
    private val viewModelClass: KClass<VM>,
    private val storeProducer: () -> ViewModelStore,
    private val factoryProducer: () -> ViewModelProvider.Factory,
    private val extrasProducer: () -> CreationExtras = { CreationExtras.Empty },
) : Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val store = storeProducer()
                val factory = factoryProducer()
                val extras = extrasProducer()
                val provider = ViewModelProvider.create(store, factory, extras)
                provider[viewModelClass].also { cached = it }
            } else {
                viewModel
            }
        }

    override fun isInitialized(): Boolean = cached != null
}
