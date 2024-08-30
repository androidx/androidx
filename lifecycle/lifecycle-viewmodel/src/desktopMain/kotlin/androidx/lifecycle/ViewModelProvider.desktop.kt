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

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.CreationExtras.Key
import androidx.lifecycle.viewmodel.ViewModelProviderImpl
import androidx.lifecycle.viewmodel.internal.JvmViewModelProviders
import androidx.lifecycle.viewmodel.internal.ViewModelProviders
import kotlin.reflect.KClass

public actual class ViewModelProvider private constructor(
    private val impl: ViewModelProviderImpl,
) {

    @MainThread
    public actual operator fun <T : ViewModel> get(modelClass: KClass<T>): T =
        impl.getViewModel(modelClass)

    @MainThread
    public actual operator fun <T : ViewModel> get(
        key: String,
        modelClass: KClass<T>,
    ): T = impl.getViewModel(modelClass, key)

    public actual interface Factory {
        public actual fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T = ViewModelProviders.unsupportedCreateViewModel()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open class OnRequeryFactory {
        public actual open fun onRequery(viewModel: ViewModel) {}
    }

    /** Simple factory, which calls empty constructor on the give class. */
    public open class NewInstanceFactory
    /**
     * Construct a new [NewInstanceFactory] instance.
     *
     * Use [NewInstanceFactory.instance] to get a default instance of [NewInstanceFactory].
     */
    @Suppress("SingletonConstructor")
    constructor() : Factory {

        public override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T = JvmViewModelProviders.createViewModel(modelClass.java)

        public companion object {
            private var _instance: NewInstanceFactory? = null

            /**
             * Retrieve a singleton instance of NewInstanceFactory.
             *
             * @return A valid [NewInstanceFactory]
             */
            @JvmStatic
            public val instance: NewInstanceFactory
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                get() {
                    if (_instance == null) {
                        _instance = NewInstanceFactory()
                    }
                    return _instance!!
                }
        }
    }

    public actual companion object {
        @JvmStatic
        public actual fun create(
            owner: ViewModelStoreOwner,
            factory: Factory,
            extras: CreationExtras,
        ): ViewModelProvider =
            ViewModelProvider(ViewModelProviderImpl(owner.viewModelStore, factory, extras))

        @JvmStatic
        public actual fun create(
            store: ViewModelStore,
            factory: Factory,
            extras: CreationExtras
        ): ViewModelProvider = ViewModelProvider(ViewModelProviderImpl(store, factory, extras))

        @JvmField
        public actual val VIEW_MODEL_KEY: CreationExtras.Key<String> =
            ViewModelProviders.ViewModelKey
    }
}
