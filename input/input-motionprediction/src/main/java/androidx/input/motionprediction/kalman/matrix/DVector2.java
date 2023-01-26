/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.input.motionprediction.kalman.matrix;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * A 2 element fixed sized vector, where each element is a double. This class can represent a (2x1)
 * or (1x2) matrix.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class DVector2 {
    public double a1;
    public double a2;

    public DVector2() {}

    /** Returns the vector magnitude (abs, length). */
    public double magnitude() {
        return Math.hypot(a1, a2);
    }

    /** Sets the elements to the values from {@code newValue}. */
    public void set(@NonNull DVector2 newValue) {
        a1 = newValue.a1;
        a2 = newValue.a2;
    }
}
