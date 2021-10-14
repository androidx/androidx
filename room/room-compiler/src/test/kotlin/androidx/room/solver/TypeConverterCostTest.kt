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

import androidx.room.solver.types.TypeConverter.Cost
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TypeConverterCostTest {
    @Test
    fun sections() {
        val cost = Cost(
            upCasts = 7,
            nullSafeWrapper = 3,
            converters = 1,
            requireNotNull = 2
        )
        assertThat(
            cost.toString()
        ).isEqualTo(
            "Cost[upcast:7,nullsafe:3,converters:1,requireNotNull:2]"
        )
    }

    @Test
    fun sum() {
        assertThat(
            Cost(converters = 3) + Cost(converters = 3, nullSafeWrapper = 4)

        ).isEqualTo(Cost(converters = 6, nullSafeWrapper = 4))
    }

    @Test
    fun sum2() {
        val sum = Cost(
            converters = 1,
            upCasts = 60,
        ) + Cost(
            converters = 1,
            nullSafeWrapper = 2,
            upCasts = 20
        )
        assertThat(
            sum
        ).isEqualTo(
            Cost(
               converters = 2,
               nullSafeWrapper = 2,
               upCasts = 80
            )
        )
    }

    @Test
    fun compare() {
        assertThat(
            Cost(requireNotNull = 1, converters = 2, nullSafeWrapper = 3, upCasts = 4)
        ).isEquivalentAccordingToCompareTo(
            Cost(requireNotNull = 1, converters = 2, nullSafeWrapper = 3, upCasts = 4)
        )
        // require not null is the most expensive
        assertThat(
            Cost(requireNotNull = 2, converters = 1, nullSafeWrapper = 1, upCasts = 1)
        ).isGreaterThan(
            Cost(requireNotNull = 1, converters = 2, nullSafeWrapper = 2, upCasts = 2)
        )
        // converters are the second most expensive
        assertThat(
            Cost(requireNotNull = 1, converters = 2, nullSafeWrapper = 1, upCasts = 1)
        ).isGreaterThan(
            Cost(requireNotNull = 1, converters = 1, nullSafeWrapper = 2, upCasts = 2)
        )
        // null safe wrapper is the third most expensive
        assertThat(
            Cost(requireNotNull = 1, converters = 1, nullSafeWrapper = 2, upCasts = 1)
        ).isGreaterThan(
            Cost(requireNotNull = 1, converters = 1, nullSafeWrapper = 1, upCasts = 2)
        )
        // upcast is the least expensive
        assertThat(
            Cost(requireNotNull = 1, converters = 1, nullSafeWrapper = 1, upCasts = 2)
        ).isGreaterThan(
            Cost(requireNotNull = 1, converters = 1, nullSafeWrapper = 1, upCasts = 1)
        )
    }
}