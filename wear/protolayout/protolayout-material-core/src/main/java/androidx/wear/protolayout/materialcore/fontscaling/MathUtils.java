/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.wear.protolayout.materialcore.fontscaling;

import static java.lang.Math.min;

/** A class that contains utility methods related to numbers. */
final class MathUtils {
    private MathUtils() {}

    /**
     * Returns the linear interpolation of {@code amount} between {@code start} and {@code stop}.
     */
    static float lerp(float start, float stop, float amount) {
        return start + (stop - start) * amount;
    }

    /**
     * Returns the interpolation scalar (s) that satisfies the equation: {@code value = }{@link
     * #lerp}{@code (a, b, s)}
     *
     * <p>If {@code a == b}, then this function will return 0.
     */
    static float lerpInv(float a, float b, float value) {
        return a != b ? ((value - a) / (b - a)) : 0.0f;
    }

    /** Returns the single argument constrained between [0.0, 1.0]. */
    static float saturate(float value) {
        return value < 0.0f ? 0.0f : min(1.0f, value);
    }

    /** Returns the saturated (constrained between [0, 1]) result of {@link #lerpInv}. */
    static float lerpInvSat(float a, float b, float value) {
        return saturate(lerpInv(a, b, value));
    }

    /**
     * Calculates a value in [rangeMin, rangeMax] that maps value in [valueMin, valueMax] to
     * returnVal in [rangeMin, rangeMax].
     *
     * <p>Always returns a constrained value in the range [rangeMin, rangeMax], even if value is
     * outside [valueMin, valueMax].
     *
     * <p>Eg: constrainedMap(0f, 100f, 0f, 1f, 0.5f) = 50f constrainedMap(20f, 200f, 10f, 20f, 20f)
     * = 200f constrainedMap(20f, 200f, 10f, 20f, 50f) = 200f constrainedMap(10f, 50f, 10f, 20f, 5f)
     * = 10f
     *
     * @param rangeMin minimum of the range that should be returned.
     * @param rangeMax maximum of the range that should be returned.
     * @param valueMin minimum of range to map {@code value} to.
     * @param valueMax maximum of range to map {@code value} to.
     * @param value to map to the range [{@code valueMin}, {@code valueMax}]. Note, can be outside
     *     this range, resulting in a clamped value.
     * @return the mapped value, constrained to [{@code rangeMin}, {@code rangeMax}.
     */
    static float constrainedMap(
            float rangeMin, float rangeMax, float valueMin, float valueMax, float value) {
        return lerp(rangeMin, rangeMax, lerpInvSat(valueMin, valueMax, value));
    }
}
