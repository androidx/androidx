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
import kotlin.jvm.JvmField

/**
 * Dimensions and units for measuring distance/time along the length/duration of a stroke.
 *
 * This is used for [DampingNode]s, [IntegralNode]s, and [NoiseNode]s to specify the domain over
 * which they operate.
 */
public class ProgressDomain
internal constructor(@JvmField internal val value: Int, private val name: String) {
    init {
        check(value !in VALUE_TO_INSTANCE) { "Duplicate ProgressDomain value: $value." }
        VALUE_TO_INSTANCE[value] = this
    }

    internal fun toSimpleString(): String = name

    override fun toString(): String = "ProgressDomain.$name"

    public companion object {

        private val VALUE_TO_INSTANCE = MutableIntObjectMap<ProgressDomain>()

        internal fun fromInt(value: Int): ProgressDomain =
            checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid ProgressDomain value: $value" }

        /**
         * Progress in input distance traveled since the start of the stroke, measured in
         * centimeters. If the stroke input data does not indicate the relationship between stroke
         * units and physical units (e.g. as may be the case for programmatically-generated inputs),
         * then any node relying on this domain will emit null.
         */
        @JvmField
        public val DISTANCE_IN_CENTIMETERS: ProgressDomain =
            ProgressDomain(0, "DISTANCE_IN_CENTIMETERS")
        /**
         * Progress in input distance traveled since the start of the stroke, measured in multiples
         * of the brush size.
         */
        @JvmField
        public val DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE: ProgressDomain =
            ProgressDomain(1, "DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE")
        /** Progress in input time since the start of the stroke, measured in seconds. */
        @JvmField public val TIME_IN_SECONDS: ProgressDomain = ProgressDomain(2, "TIME_IN_SECONDS")
    }
}
