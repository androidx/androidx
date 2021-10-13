/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.solver.types

import androidx.annotation.VisibleForTesting
import androidx.room.compiler.processing.XType
import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope

/**
 * A code generator that can convert from 1 type to another
 */
abstract class TypeConverter(
    val from: XType,
    val to: XType,
    val cost: Cost = Cost.CONVERTER
) {
    /**
     * Should generate the code that will covert [inputVarName] of type [from] to [outputVarName]
     * of type [to]. This method *should not* declare the [outputVarName] as it is already
     * declared by the caller.
     */
    protected abstract fun doConvert(
        inputVarName: String,
        outputVarName: String,
        scope: CodeGenScope
    )

    /**
     * A type converter can optionally override this method if they can handle the case where
     * they don't need a temporary output variable (e.g. no op conversion or null checks).
     *
     * @return The variable name where the result is saved.
     */
    protected open fun doConvert(
        inputVarName: String,
        scope: CodeGenScope
    ): String {
        val outVarName = scope.getTmpVar()
        scope.builder().apply {
            addStatement("final $T $L", to.typeName, outVarName)
        }
        doConvert(
            inputVarName = inputVarName,
            outputVarName = outVarName,
            scope = scope
        )
        return outVarName
    }

    fun convert(
        inputVarName: String,
        scope: CodeGenScope
    ): String = doConvert(inputVarName, scope)

    fun convert(
        inputVarName: String,
        outputVarName: String,
        scope: CodeGenScope
    ) {
        doConvert(inputVarName, outputVarName, scope)
    }

    /**
     * Represents the cost of a type converter.
     *
     * When calculating cost, we consider multiple types of conversions in ascending order, from
     * cheapest to expensive:
     * * `upcast`: Converts from a subtype to super type (e.g. Int to Number)
     * * `nullSafeWrapper`: Adds a null check before calling the delegated converter or else
     *    returns null.
     * * `converter`: Unit converter
     * * `requireNotNull`: Adds a null check before returning the delegated converter's value
     *    or throws if the value is null.
     *
     * The comparison happens in buckets such that having 10 upcasts is still cheaper than having
     * 1 nullSafeWrapper.
     *
     * Internally, this class uses an IntArray to keep its fields to optimize for readability in
     * operators.
     */
    class Cost private constructor(
        /**
         * Values for each bucket, ordered from most expensive to least expensive.
         */
        private val values: IntArray
    ) : Comparable<Cost> {
        init {
            require(values.size == Buckets.SIZE)
        }

        constructor(
            converters: Int,
            nullSafeWrapper: Int = 0,
            upCasts: Int = 0,
            requireNotNull: Int = 0
        ) : this(
            // NOTE: construction order here MUST match the [Buckets]
            intArrayOf(
                requireNotNull,
                converters,
                nullSafeWrapper,
                upCasts
            )
        )

        @VisibleForTesting
        val upCasts: Int
            get() = values[Buckets.UP_CAST]

        @VisibleForTesting
        val nullSafeWrapper: Int
            get() = values[Buckets.NULL_SAFE]

        @VisibleForTesting
        val requireNotNull: Int
            get() = values[Buckets.REQUIRE_NOT_NULL]

        @VisibleForTesting
        val converters: Int
            get() = values[Buckets.CONVERTER]

        operator fun plus(other: Cost) = Cost(
            values = IntArray(Buckets.SIZE) { index ->
                values[index] + other.values[index]
            }
        )

        override operator fun compareTo(other: Cost): Int {
            for (index in 0 until Buckets.SIZE) {
                val cmp = values[index].compareTo(other.values[index])
                if (cmp != 0) {
                    return cmp
                }
            }
            return 0
        }

        override fun toString() = buildString {
            append("Cost[")
            append("upcast:")
            append(upCasts)
            append(",nullsafe:")
            append(nullSafeWrapper)
            append(",converters:")
            append(converters)
            append(",requireNotNull:")
            append(requireNotNull)
            append("]")
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Cost) {
                return false
            }
            return compareTo(other) == 0
        }

        override fun hashCode(): Int {
            // we don't really use hash functions so this is good enough as a hash function.
            return values[Buckets.CONVERTER]
        }

        companion object {
            val UP_CAST = Cost(converters = 0, upCasts = 1)
            val NULL_SAFE = Cost(converters = 0, nullSafeWrapper = 1)
            val CONVERTER = Cost(converters = 1)
            val REQUIRE_NOT_NULL = Cost(converters = 0, requireNotNull = 1)
        }

        /**
         * Comparison buckets, ordered from the MOST expensive to LEAST expensive
         */
        private object Buckets {
            const val REQUIRE_NOT_NULL = 0
            const val CONVERTER = 1
            const val NULL_SAFE = 2
            const val UP_CAST = 3
            const val SIZE = 4
        }
    }
}
