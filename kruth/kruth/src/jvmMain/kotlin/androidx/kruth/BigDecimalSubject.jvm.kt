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

package androidx.kruth

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact
import java.math.BigDecimal

/**
 * Propositions for [BigDecimal] typed subjects.
 */
class BigDecimalSubject internal constructor(
    actual: BigDecimal?,
    metadata: FailureMetadata = FailureMetadata(),
) : ComparableSubject<BigDecimal>(actual, metadata = metadata) {

    /**
     * Fails if the subject's value is not equal to the value of the given [BigDecimal]. (i.e.,
     * fails if `actual.compareTo(expected) != 0`).
     *
     * **Note:** The scale of the BigDecimal is ignored. If you want to compare the values and
     * the scales, use [isEqualTo].
     */
    fun isEqualToIgnoringScale(expected: BigDecimal) {
        compareValues(expected)
    }

    /**
     * Fails if the subject's value is not equal to the value of the [BigDecimal] created from
     * the expected string (i.e., fails if `actual.compareTo(BigDecimal(expected)) != 0`).
     *
     * **Note:** The scale of the BigDecimal is ignored. If you want to compare the values and
     * the scales, use [isEqualTo].
     */
    fun isEqualToIgnoringScale(expected: String) {
        isEqualToIgnoringScale(BigDecimal(expected))
    }

    /**
     * Fails if the subject's value is not equal to the value of the [BigDecimal] created from
     * the expected `Long` (i.e., fails if `actual.compareTo(BigDecimal(expected)) != 0`).
     *
     * **Note:** The scale of the BigDecimal is ignored. If you want to compare the values and
     * the scales, use [isEqualTo].
     */
    fun isEqualToIgnoringScale(expected: Long) {
        isEqualToIgnoringScale(BigDecimal(expected))
    }

    /**
     * Fails if the subject's value and scale is not equal to the given [BigDecimal].
     *
     * **Note:** If you only want to compare the values of the BigDecimals and not their scales,
     * use [isEqualToIgnoringScale] instead.
     */
    @Suppress("RedundantOverride") // To express more specific KDoc
    override fun isEqualTo(expected: Any?) {
        super.isEqualTo(expected)
    }

    /**
     * Fails if the subject is not equivalent to the given value according to
     * [Comparable.compareTo], (i.e., fails if `a.compareTo(b) != 0`). This method behaves
     * identically to (the more clearly named) [isEqualToIgnoringScale].
     *
     * **Note:** Do not use this method for checking object equality. Instead, use [isEqualTo].
     */
    override fun isEquivalentAccordingToCompareTo(other: BigDecimal?) {
        compareValues(other)
    }

    private fun compareValues(expected: BigDecimal?) {
        if (requireNonNull(actual).compareTo(requireNonNull(expected)) != 0) {
            failWithoutActual(
                fact("expected", expected),
                fact("but was", actual.toString()),
                simpleFact("(scale is ignored)"),
            )
        }
    }
}
