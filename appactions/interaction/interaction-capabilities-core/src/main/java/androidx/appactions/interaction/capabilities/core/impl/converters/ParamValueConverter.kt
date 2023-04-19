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

import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Value

/**
 * Converts to and from ParamValue (Assistant protocol for a slot).
 *
 * @param T the type of slot. This will usually be a type or property from the BIT library.
 */
interface ParamValueConverter<T> {

    /** Convert ParamValue sent from Assistant (e.g. BII Argument). */
    @Throws(StructConversionException::class)
    fun fromParamValue(paramValue: ParamValue): T

    /** Convert to ParamValue to send to Assistant (e.g. BIO). */
    fun toParamValue(obj: T): ParamValue

    companion object {
        /**
         * @param typeSpec the TypeSpec of the structured type, which can
         * read/write objects to/from Struct.
         */
        @JvmStatic
        fun <T> of(typeSpec: TypeSpec<T>) = object : ParamValueConverter<T> {
            override fun fromParamValue(paramValue: ParamValue): T {
                return typeSpec.fromValue(paramValueToValue(paramValue))
            }

            override fun toParamValue(obj: T): ParamValue {
                val builder = valueToParamValue(typeSpec.toValue(obj)).toBuilder()
                typeSpec.getIdentifier(obj)?.let { builder.setIdentifier(it) }
                return builder.build()
            }
        }

        internal fun paramValueToValue(paramValue: ParamValue): Value {
            val builder = Value.newBuilder()
            when {
                paramValue.hasStringValue() -> builder.setStringValue(paramValue.getStringValue())
                paramValue.hasBoolValue() -> builder.setBoolValue(paramValue.getBoolValue())
                paramValue.hasNumberValue() -> builder.setNumberValue(paramValue.getNumberValue())
                paramValue.hasStructValue() -> builder.setStructValue(paramValue.getStructValue())
                else -> throw StructConversionException("cannot convert $paramValue into Value.")
            }
            return builder.build()
        }

        internal fun valueToParamValue(value: Value): ParamValue {
            val builder = ParamValue.newBuilder()
            when {
                value.hasStringValue() -> builder.setStringValue(value.getStringValue())
                value.hasBoolValue() -> builder.setBoolValue(value.getBoolValue())
                value.hasNumberValue() -> builder.setNumberValue(value.getNumberValue())
                value.hasStructValue() -> builder.setStructValue(value.getStructValue())
                else -> throw IllegalStateException("cannot convert $value to ParamValue.")
            }
            return builder.build()
        }
    }
}
