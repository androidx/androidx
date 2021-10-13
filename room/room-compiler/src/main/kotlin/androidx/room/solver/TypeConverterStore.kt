/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.solver

import androidx.room.compiler.processing.XType
import androidx.room.processor.Context
import androidx.room.solver.types.CompositeTypeConverter
import androidx.room.solver.types.NoOpConverter
import androidx.room.solver.types.RequireNotNullTypeConverter
import androidx.room.solver.types.TypeConverter
import androidx.room.solver.types.UpCastTypeConverter

interface TypeConverterStore {
    val typeConverters: List<TypeConverter>

    /**
     * Finds a [TypeConverter] (might be composite) that can convert the given [input] type into
     * one of the given [columnTypes]. If [columnTypes] is not specified, targets all
     * `knownColumnTypes`.
     */
    fun findConverterIntoStatement(
        input: XType,
        columnTypes: List<XType>?
    ): TypeConverter?

    /**
     * Finds a [TypeConverter] (might be composite) that can convert the given [columnTypes] into
     * the [output] type. If [columnTypes] is not specified, uses all `knownColumnTypes`.
     */
    fun findConverterFromCursor(
        columnTypes: List<XType>?,
        output: XType
    ): TypeConverter?

    /**
     * Finds a [TypeConverter] from [input] to [output].
     */
    fun findTypeConverter(
        input: XType,
        output: XType
    ): TypeConverter?

    fun reverse(converter: TypeConverter): TypeConverter? {
        return when (converter) {
            is NoOpConverter -> converter
            is CompositeTypeConverter -> {
                val r1 = reverse(converter.conv1) ?: return null
                val r2 = reverse(converter.conv2) ?: return null
                CompositeTypeConverter(r2, r1)
            }
            // reverse of require not null is upcast since not null can be converted into nullable
            is RequireNotNullTypeConverter -> UpCastTypeConverter(
                upCastFrom = converter.to,
                upCastTo = converter.from
            )
            else -> {
                typeConverters.firstOrNull {
                    it.from.isSameType(converter.to) &&
                        it.to.isSameType(converter.from)
                }
            }
        }
    }

    companion object {
        /**
         * @param context Processing context
         * @param typeConverters Available TypeConverters, ordered by priority when they have the
         *        same cost.
         * @param knownColumnTypes List of types that can be saved into db/read from without a
         *        converter.
         */
        fun create(
            context: Context,
            typeConverters: List<TypeConverter>,
            knownColumnTypes: List<XType>
        ) = if (context.useNullAwareConverter) {
            NullAwareTypeConverterStore(
                context = context,
                typeConverters = typeConverters,
                knownColumnTypes = knownColumnTypes
            )
        } else {
            TypeConverterStoreImpl(
                typeConverters = typeConverters,
                knownColumnTypes = knownColumnTypes
            )
        }
    }
}