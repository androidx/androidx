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

package androidx.appactions.interaction.capabilities.core.impl.spec

import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter
import androidx.appactions.interaction.proto.ParamValue
import java.util.function.Function

data class ParamBinding<ArgumentsT, ArgumentsBuilderT : BuilderOf<ArgumentsT>>
internal constructor(
    val name: String,
    val propertyConverter: Function<Map<String, Property<*>>, IntentParameter?>,
    val argumentSetter: ArgumentSetter<ArgumentsBuilderT>
) {
    /**
     * Given a `List<ParamValue>`, convert it to user-visible type and set it into
     * ArgumentBuilder.
     */
    fun interface ArgumentSetter<ArgumentsBuilderT> {
        /** Conversion from protos to user-visible type.  */
        @Throws(StructConversionException::class)
        fun setArguments(builder: ArgumentsBuilderT, paramValues: List<ParamValue>)
    }

    companion object {
        @JvmStatic
        fun <ArgumentsT, ArgumentsBuilderT : BuilderOf<ArgumentsT>> create(
            name: String,
            paramGetter: Function<Map<String, Property<*>>, IntentParameter?>,
            argumentSetter: ArgumentSetter<ArgumentsBuilderT>
        ): ParamBinding<ArgumentsT, ArgumentsBuilderT> {
            return ParamBinding(name, paramGetter, argumentSetter)
        }
    }
}
