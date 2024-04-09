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
import androidx.lifecycle.viewmodel.internal.ViewModelProviders
import kotlin.reflect.KClass

public actual class ViewModelInitializer<T : ViewModel>
actual constructor(
    internal actual val clazz: KClass<T>,
    internal actual val initializer: CreationExtras.() -> T,
)

internal actual class InitializerViewModelFactory
actual constructor(
    private vararg val initializers: ViewModelInitializer<*>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
        ViewModelProviders.createViewModelFromInitializers(modelClass, extras, *initializers)
}
