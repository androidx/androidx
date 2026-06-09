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

package androidx.room3.solver

import androidx.room3.compiler.processing.XType
import androidx.room3.processor.Context
import androidx.room3.solver.types.ColumnTypeConverter
import androidx.room3.solver.types.CompositeTypeConverter
import androidx.room3.solver.types.NoOpConverter
import androidx.room3.solver.types.RequireNotNullTypeConverter
import androidx.room3.solver.types.UpCastTypeConverter

interface ColumnTypeConverterStore {
    val columnTypeConverters: List<ColumnTypeConverter>

    /**
     * Finds a [ColumnTypeConverter] (might be composite) that can convert the given [input] type
     * into one of the given [columnTypes]. If [columnTypes] is not specified, targets all
     * `knownColumnTypes`.
     */
    fun findConverterIntoStatement(input: XType, columnTypes: List<XType>?): ColumnTypeConverter?

    /**
     * Finds a [ColumnTypeConverter] (might be composite) that can convert the given [columnTypes]
     * into the [output] type. If [columnTypes] is not specified, uses all `knownColumnTypes`.
     */
    fun findConverterFromStatement(columnTypes: List<XType>?, output: XType): ColumnTypeConverter?

    /** Finds a [ColumnTypeConverter] from [input] to [output]. */
    fun findColumnTypeConverter(input: XType, output: XType): ColumnTypeConverter?

    fun reverse(converter: ColumnTypeConverter): ColumnTypeConverter? {
        return when (converter) {
            is NoOpConverter -> converter
            is CompositeTypeConverter -> {
                val r1 = reverse(converter.conv1) ?: return null
                val r2 = reverse(converter.conv2) ?: return null
                CompositeTypeConverter(r2, r1)
            }
            // reverse of require not null is upcast since not null can be converted into nullable
            is RequireNotNullTypeConverter ->
                UpCastTypeConverter(upCastFrom = converter.to, upCastTo = converter.from)
            else -> {
                columnTypeConverters.firstOrNull {
                    it.from.isSameType(converter.to) && it.to.isSameType(converter.from)
                }
            }
        }
    }

    companion object {
        /**
         * @param context Processing context
         * @param columnTypeConverters Available ColumnTypeConverters, ordered by priority when they
         *   have the same cost.
         * @param knownColumnTypes List of types that can be saved into db/read from without a
         *   converter.
         */
        fun create(
            context: Context,
            columnTypeConverters: List<ColumnTypeConverter>,
            knownColumnTypes: List<XType>,
        ) =
            NullAwareColumnTypeConverterStore(
                context = context,
                columnTypeConverters = columnTypeConverters,
                knownColumnTypes = knownColumnTypes,
            )
    }
}
