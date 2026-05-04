/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush.behavior

import androidx.collection.MutableIntObjectMap

/**
 * The desired behavior when an input value is outside the range defined by
 * [SourceNode.sourceValueRangeStart] and [SourceNode.sourceValueRangeEnd].
 */
public class OutOfRange
private constructor(@JvmField internal val value: Int, private val name: String) {
    init {
        check(value !in VALUE_TO_INSTANCE) { "Duplicate OutOfRange value: $value." }
        VALUE_TO_INSTANCE[value] = this
    }

    internal fun toSimpleString(): String = name

    override fun toString(): String = "OutOfRange." + name

    public companion object {
        private val VALUE_TO_INSTANCE = MutableIntObjectMap<OutOfRange>()

        internal fun fromInt(value: Int): OutOfRange =
            checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid OutOfRange value: $value" }

        /** Values outside the range will be clamped to not exceed the bounds. */
        @JvmField public val CLAMP: OutOfRange = OutOfRange(0, "CLAMP")

        /**
         * Values will be shifted by an integer multiple of the range size so that they fall within
         * the bounds.
         *
         * In this case, the range will be treated as a half-open interval, with a value exactly at
         * [SourceNode.sourceValueRangeEnd] being treated as though it was
         * [SourceNode.sourceValueRangeStart].
         */
        @JvmField public val REPEAT: OutOfRange = OutOfRange(1, "REPEAT")
        /**
         * Similar to [REPEAT], but every other repetition of the bounds will be mirrored, as though
         * [SourceNode.sourceValueRangeStart] and [SourceNode.sourceValueRangeEnd] were swapped.
         * This means the range does not need to be treated as a half-open interval like in the case
         * of [REPEAT].
         */
        @JvmField public val MIRROR: OutOfRange = OutOfRange(2, "MIRROR")
    }
}
