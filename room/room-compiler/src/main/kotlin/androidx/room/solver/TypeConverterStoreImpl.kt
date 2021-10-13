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
import androidx.room.solver.types.CompositeTypeConverter
import androidx.room.solver.types.NoOpConverter
import androidx.room.solver.types.TypeConverter
import java.util.LinkedList

/**
 * Legacy [TypeConverterStore] implementation that does not assume we have type nullability
 * information. It is kept around for backwards compatibility.
 */
class TypeConverterStoreImpl(
    /**
     * Available TypeConverters
     */
    override val typeConverters: List<TypeConverter>,
    /**
     * List of types that can be saved into db/read from without a converter.
     */
    private val knownColumnTypes: List<XType>
) : TypeConverterStore {
    override fun findConverterIntoStatement(
        input: XType,
        columnTypes: List<XType>?
    ) = findTypeConverter(
        inputs = listOf(input),
        outputs = columnTypes ?: knownColumnTypes
    )

    override fun findConverterFromCursor(
        columnTypes: List<XType>?,
        output: XType
    ) = findTypeConverter(
        inputs = columnTypes ?: knownColumnTypes,
        outputs = listOf(output)
    )

    override fun findTypeConverter(
        input: XType,
        output: XType
    ) = findTypeConverter(
        inputs = listOf(input),
        outputs = listOf(output)
    )

    /**
     * Finds a type converter that can convert one of the input values to one of the output values.
     *
     * When multiple conversion paths are possible, shortest path (least amount of conversion) is
     * preferred.
     */
    private fun findTypeConverter(
        inputs: List<XType>,
        outputs: List<XType>
    ): TypeConverter? {
        if (inputs.isEmpty()) {
            return null
        }
        inputs.forEach { input ->
            if (outputs.any { output -> input.isSameType(output) }) {
                return NoOpConverter(input)
            }
        }

        val excludes = arrayListOf<XType>()

        val queue = LinkedList<TypeConverter>()
        fun List<TypeConverter>.findMatchingConverter(): TypeConverter? {
            // We prioritize exact match over assignable. To do that, this variable keeps any
            // assignable match and if we cannot find exactly same type match, we'll return the
            // assignable match.
            var assignableMatchFallback: TypeConverter? = null
            this.forEach { converter ->
                outputs.forEach { output ->
                    if (output.isSameType(converter.to)) {
                        return converter
                    } else if (assignableMatchFallback == null &&
                        output.isAssignableFrom(converter.to)
                    ) {
                        // if we don't find exact match, we'll return this.
                        assignableMatchFallback = converter
                    }
                }
            }
            return assignableMatchFallback
        }
        inputs.forEach { input ->
            val candidates = getAllTypeConverters(input, excludes)
            val match = candidates.findMatchingConverter()
            if (match != null) {
                return match
            }
            candidates.forEach {
                excludes.add(it.to)
                queue.add(it)
            }
        }
        excludes.addAll(inputs)
        while (queue.isNotEmpty()) {
            val prev = queue.pop()
            val from = prev.to
            val candidates = getAllTypeConverters(from, excludes)
            val match = candidates.findMatchingConverter()
            if (match != null) {
                return CompositeTypeConverter(prev, match)
            }
            candidates.forEach {
                excludes.add(it.to)
                queue.add(CompositeTypeConverter(prev, it))
            }
        }
        return null
    }

    /**
     * Returns all type converters that can receive input type and return into another type.
     * The returned list is ordered by priority such that if we have an exact match, it is
     * prioritized.
     */
    private fun getAllTypeConverters(input: XType, excludes: List<XType>): List<TypeConverter> {
        // for input, check assignability because it defines whether we can use the method or not.
        // for excludes, use exact match
        return typeConverters.filter { converter ->
            converter.from.isAssignableFrom(input) &&
                !excludes.any { it.isSameType(converter.to) }
        }.sortedByDescending {
            // if it is the same, prioritize
            if (it.from.isSameType(input)) {
                2
            } else {
                1
            }
        }
    }
}