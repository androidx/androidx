/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;

/** Support access to floats in matrix */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MatrixAccess {

    /**
     * Get a the matrix
     *
     * @return the matrix
     */
    float @NonNull [] get();

    /**
     * Convert a 4x4 matrix to a 3x3 matrix
     *
     * @param matrix 4x4 matrix
     * @return 3x3 matrix
     */
    static float @Nullable [] to3x3(float @NonNull [] matrix) {

        if (matrix.length == 16) {
            float[] matrix3x3 = new float[9];
            float[] matrix4x4;
            matrix4x4 = matrix;

            matrix3x3[0] = matrix4x4[0];
            matrix3x3[1] = matrix4x4[1];
            matrix3x3[2] = matrix4x4[3];

            // Column 1
            matrix3x3[3] = matrix4x4[4];
            matrix3x3[4] = matrix4x4[5];
            matrix3x3[5] = matrix4x4[7];

            // Column 2
            matrix3x3[6] = matrix4x4[8];
            matrix3x3[7] = matrix4x4[9];
            matrix3x3[8] = matrix4x4[15]; // should be 1. Not sure about this

            return matrix3x3;
        } else if (matrix.length == 9) {
            return matrix;
        }
        return null;
    }

    /**
     * Dump a matrix to the console
     *
     * @param m matrix
     */
    static void dump(float @NonNull [] m) {
        String str = "";
        int step = m.length == 16 ? 4 : 3;
        DecimalFormat df = new DecimalFormat("0.00");
        for (int i = 0; i < m.length; i++) {
            if (i % step == 0) {
                str += "\n";
            }
            str += df.format(m[i]) + "  ";
        }
        System.out.println(str);
    }
}
