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

package androidx.room.solver.shortcut.binderprovider

import androidx.room.solver.shortcut.binder.InsertMethodBinder
import androidx.room.vo.ShortcutQueryParameter
import javax.lang.model.type.DeclaredType

/**
 * Provider for insert method binders.
 */
interface InsertMethodBinderProvider {

    /**
     * Check whether the [DeclaredType] can be handled by the [InsertMethodBinder]
     */
    fun matches(declared: DeclaredType): Boolean

    /**
     * Provider of [InsertMethodBinder], based on the [DeclaredType] and the list of parameters
     */
    fun provide(declared: DeclaredType, params: List<ShortcutQueryParameter>): InsertMethodBinder
}