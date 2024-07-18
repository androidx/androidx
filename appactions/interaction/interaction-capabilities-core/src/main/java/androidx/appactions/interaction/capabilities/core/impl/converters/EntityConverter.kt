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

import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.protobuf.Value

/**
 * Converter from any Type to the Entity proto. This converter is usually used in the direction from
 * app to Assistant. Examples where the converter is needed is the developer setting possible values
 * in Properties or returning "disambiguation entities" from an inventory listener.
 *
 * @param T The T instance is usually a value object provided by the app, e.g. a Timer object from
 *   the built-in-types library. </T>
 */
fun interface EntityConverter<T> {

    /** Converter to an Entity proto. */
    fun convert(obj: T): Entity

    companion object {
        /**
         * @param typeSpec the TypeSpec of the structured type.
         */
        @JvmStatic
        fun <T> of(typeSpec: TypeSpec<T>): EntityConverter<T> {
            return EntityConverter { obj ->
                val builder = valueToEntity(typeSpec.toValue(obj)).toBuilder()
                typeSpec.getIdentifier(obj)?.let { builder.setIdentifier(it) }
                builder.build()
            }
        }

        internal fun valueToEntity(value: Value): Entity {
            val builder = Entity.newBuilder()
            when {
                value.hasStringValue() -> builder.stringValue = value.stringValue
                value.hasBoolValue() -> builder.boolValue = value.boolValue
                value.hasNumberValue() -> builder.numberValue = value.numberValue
                value.hasStructValue() -> builder.structValue = value.structValue
                else -> throw IllegalStateException("cannot convert $value into Entity.")
            }
            return builder.build()
        }
    }
}
