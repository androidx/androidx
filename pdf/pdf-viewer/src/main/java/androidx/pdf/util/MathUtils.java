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

package androidx.pdf.util;

import androidx.annotation.RestrictTo;

/**
 * Static utility methods (like Math.min but which aren't included in Math).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class MathUtils {

    private MathUtils() {
        // Static utility.
    }

    /** Clamps a value between a lower and an upper bound (inclusive). */
    public static int clamp(int value, int lower, int upper) {
        return Math.min(upper, Math.max(lower, value));
    }

    /** Clamps a value between a lower and an upper bound (inclusive). */
    public static float clamp(float value, float lower, float upper) {
        return Math.min(upper, Math.max(lower, value));
    }

    /**
     * Returns lower when value is less than or equal to 0, upper when value is greater than or
     * equal
     * to 1, and linearly interpolates between them when value is between 0 and 1.
     */
    public static float lerp(float value, float lower, float upper) {
        return clamp(value, 0.0f, 1.0f) * (upper - lower) + lower;
    }

    /** Returns the closer of two values a and b to the given value. */
    public static int nearestValue(int value, int a, int b) {
        return Math.abs(a - value) < Math.abs(b - value) ? a : b;
    }

    /** Returns the closer of two values a and b to the given value. */
    public static float nearestValue(float value, float a, float b) {
        return Math.abs(a - value) < Math.abs(b - value) ? a : b;
    }

    /**
     *
     */
    public static boolean almostEqual(float a, float b, float epsilon) {
        return Math.abs(a - b) < epsilon;
    }

    /**
     *
     */
    public static int roundUpToPower2(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
}
