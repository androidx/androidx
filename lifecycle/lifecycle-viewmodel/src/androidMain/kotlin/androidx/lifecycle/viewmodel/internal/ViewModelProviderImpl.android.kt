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
import kotlin.reflect.KClass

internal actual fun <VM : ViewModel> createViewModel(
    factory: ViewModelProvider.Factory,
    modelClass: KClass<VM>,
    extras: CreationExtras
): VM {
    // Android targets using `compileOnly` dependencies may encounter AGP desugaring
    // issues where `Factory.create` throws an `AbstractMethodError`. This is resolved by an
    // Android-specific implementation that first attempts all `ViewModelProvider.Factory.create`
    // method overloads before allowing the exception to propagate.
    // (See b/230454566 and b/341792251 for more details).
    return try {
        factory.create(modelClass, extras)
    } catch (e: AbstractMethodError) {
        try {
            factory.create(modelClass.java, extras)
        } catch (e: AbstractMethodError) {
            factory.create(modelClass.java)
        }
    }
}
