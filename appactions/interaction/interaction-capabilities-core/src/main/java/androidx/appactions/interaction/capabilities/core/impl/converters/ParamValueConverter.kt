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
    fun toParamValue(value: T): ParamValue

    companion object {
        fun <T> of(typeSpec: TypeSpec<T>) = object : ParamValueConverter<T> {

            override fun fromParamValue(paramValue: ParamValue): T {
                return typeSpec.fromStruct(paramValue.structValue)
            }

            override fun toParamValue(value: T): ParamValue {
                val builder = ParamValue.newBuilder()
                    .setStructValue(typeSpec.toStruct(value))
                typeSpec.getIdentifier(value)?.let { builder.setIdentifier(it) }
                return builder.build()
            }
        }
    }
}