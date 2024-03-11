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

/**
 * A Subject for [Char] arrays.
 */
class PrimitiveFloatArraySubject internal constructor(
    actual: FloatArray?,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<FloatArray?>(actual, metadata = metadata, typeDescriptionOverride = "array") {

    private val helper =
        HelperArraySubject(
            actual = actual,
            size = FloatArray::size,
            metadata = metadata,
        )

    /**
     * A check that the actual array and [expected] are arrays of the same length and type,
     * containing elements such that each element in [expected] is equal to each element in the
     * actual array, and in the same position, with element equality defined the same way that
     * [Float.equals] define it (which is different to the way that the `==` operator on primitive
     * [Float] defines it). This method is *not* recommended when the code under test is doing any
     * kind of arithmetic: use `usingTolerance` with a suitable tolerance in that case, e.g.
     * `assertThat(actualArray).usingTolerance(1.0e-10).containsExactly(expectedArray).inOrder()`.
     * (Remember that the exact result of floating point arithmetic is sensitive to apparently
     * trivial changes such as replacing `(a + b) + c` with `a + (b + c)`, and that unless
     * `strictfp` is in force even the result of `(a + b) + c` is sensitive to the JVM's choice of
     * precision for the intermediate result.) This method is recommended when the code under test
     * is specified as either copying values without modification from its input or returning
     * well-defined literal or constant values.
     *
     * - It considers [Float.POSITIVE_INFINITY], [Float.NEGATIVE_INFINITY], and
     * [Float.NaN] to be equal to themselves (contrast with `usingTolerance(0.0)` which does not).
     * - It does *not* consider `-0.0` to be equal to `0.0` (contrast with `usingTolerance(0.0)`
     * which does).
     */
    @Suppress("RedundantOverride") // Documented
    override fun isEqualTo(expected: Any?) {
        super.isEqualTo(expected)
    }

    /**
     * A check that the actual array and [unexpected] are not arrays of the same length and type,
     * containing elements such that each element in [unexpected] is equal to each element in the
     * actual array, and in the same position, with element equality defined the same way that
     * [Float.equals] define it (which is different to the way that the `==` operator on primitive
     * [Float] defines it). See [isEqualTo] for advice on when exact equality is recommended.
     *
     * - It considers [Float.POSITIVE_INFINITY], [Float.NEGATIVE_INFINITY], and [Float.NaN] to be
     * equal to themselves.
     * - It does *not* consider `-0.0` to be equal to `0.0`.
     */
    @Suppress("RedundantOverride") // Documented
    override fun isNotEqualTo(unexpected: Any?) {
        super.isNotEqualTo(unexpected)
    }

    /** Fails if the array is not empty (i.e. `array.size > 0`). */
    fun isEmpty() {
        helper.isEmpty()
    }

    /** Fails if the array is empty (i.e. `array.size == 0`). */
    fun isNotEmpty() {
        helper.isNotEmpty()
    }

    /**
     * Fails if the array does not have the given length.
     *
     * @throws IllegalArgumentException if [length] < 0
     */
    fun hasLength(length: Int) {
        helper.hasLength(length)
    }

    /** Converts this [PrimitiveBooleanArraySubject] to [IterableSubject].*/
    fun asList(): IterableSubject<Float> {
        requireNonNull(actual)
        return IterableSubject(actual = actual.asList(), metadata = metadata)
    }
}
