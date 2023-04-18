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
import androidx.appactions.interaction.protobuf.Value

/**
 * TypeSpec is used to convert between native objects in capabilities/values and Value proto.
 */
interface TypeSpec<T> {
    /* Given the object, returns its identifier, which can be null. */
    fun getIdentifier(obj: T): String?

    /** Converts a object into a Value proto. */
    fun toValue(obj: T): Value

    /**
     * Converts a Value into an object.
     *
     * @throws StructConversionException if the Struct is malformed.
     */
    @Throws(StructConversionException::class)
    fun fromValue(value: Value): T

    companion object {
        @JvmField
        val STRING_TYPE_SPEC = object : TypeSpec<String> {
            override fun getIdentifier(obj: String): String? = null

            override fun toValue(
                obj: String,
            ): Value = Value.newBuilder().setStringValue(obj).build()

            override fun fromValue(value: Value): String = when {
                value.hasStringValue() -> value.getStringValue()
                else -> throw StructConversionException("STRING_TYPE_SPEC cannot convert $value")
            }
        }

        @JvmField
        val BOOL_TYPE_SPEC = object : TypeSpec<Boolean> {
            override fun getIdentifier(obj: Boolean): String? = null

            override fun toValue(
                obj: Boolean,
            ): Value = Value.newBuilder().setBoolValue(obj).build()

            override fun fromValue(value: Value): Boolean = when {
                value.hasBoolValue() -> value.getBoolValue()
                else -> throw StructConversionException("BOOL_TYPE_SPEC cannot convert $value")
            }
        }

        @JvmField
        val NUMBER_TYPE_SPEC = object : TypeSpec<Double> {
            override fun getIdentifier(obj: Double): String? = null

            override fun toValue(
                obj: Double,
            ): Value = Value.newBuilder().setNumberValue(obj).build()

            override fun fromValue(value: Value): Double = when {
                value.hasNumberValue() -> value.getNumberValue()
                else -> throw StructConversionException("NUMBER_TYPE_SPEC cannot convert $value")
            }
        }

        @JvmField
        val INTEGER_TYPE_SPEC = object : TypeSpec<Int> {
            override fun getIdentifier(obj: Int): String? = null

            override fun toValue(
                obj: Int,
            ): Value = Value.newBuilder().setNumberValue(obj.toDouble()).build()

            override fun fromValue(value: Value): Int = when {
                value.hasNumberValue() -> value.getNumberValue().toInt()
                else -> throw StructConversionException("INTEGER_TYPE_SPEC cannot convert $value")
            }
        }
    }
}
