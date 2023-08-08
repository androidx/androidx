/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.impl.converters

import androidx.appactions.interaction.protobuf.Value
import java.util.function.Function

internal data class FieldBinding<T, BuilderT> constructor(
    val name: String,
    val valueGetter: Function<T, Value?>,
    val valueSetter: CheckedInterfaces.BiConsumer<BuilderT, Value>
) {
    companion object {
        @JvmStatic
        fun <T, BuilderT> create(
            name: String,
            valueGetter: Function<T, Value?>,
            valueSetter: CheckedInterfaces.BiConsumer<BuilderT, Value>
        ): FieldBinding<T, BuilderT> {
            return FieldBinding(name, valueGetter, valueSetter)
        }
    }
}
