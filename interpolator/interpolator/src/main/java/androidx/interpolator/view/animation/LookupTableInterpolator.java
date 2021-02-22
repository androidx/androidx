/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.interpolator.view.animation;

import android.view.animation.Interpolator;

final class LookupTableInterpolator {
    private LookupTableInterpolator() {}

    /**
     * An {@link Interpolator} helper that uses a lookup table to compute an interpolation based
     * on a given input.
     */
    static float interpolate(float[] values, float stepSize, float input) {
        if (input >= 1.0f) {
            return 1.0f;
        }
        if (input <= 0f) {
            return 0f;
        }

        // Calculate index - We use min with length - 2 to avoid IndexOutOfBoundsException when
        // we lerp (linearly interpolate) in the return statement
        int position = Math.min((int) (input * (values.length - 1)), values.length - 2);

        // Calculate values to account for small offsets as the lookup table has discrete values
        float quantized = position * stepSize;
        float diff = input - quantized;
        float weight = diff / stepSize;

        // Linearly interpolate between the table values
        return values[position] + weight * (values[position + 1] - values[position]);
    }

}
