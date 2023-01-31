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

package androidx.camera.core.impl.utils;

import android.opengl.Matrix;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Utility class to extend the {@link Matrix}.
 */
public final class MatrixExt {
    private static final float[] sTemp = new float[16];

    // Prevent instantiation.
    private MatrixExt() {
    }

    /**
     * Set the matrix to rotate by the specified number of degrees, with a pivot point at
     * (px, py).
     *
     * <p>The pivot point is the coordinate that should remain unchanged by the specified
     * transformation.
     *
     * @param matrix the matrix to rotate
     * @param degrees the rotation degrees
     * @param px px of pivot point at (px, py)
     * @param py py of pivot point at (px, py)
     */
    public static void setRotate(@NonNull float[] matrix, float degrees, float px, float py) {
        Matrix.setIdentityM(matrix, 0);
        preRotate(matrix, degrees, px, py);
    }

    /**
     * Preconcats the matrix with the specified rotation. M' = M * R(degrees, px, py)
     *
     * <p>The pivot point is the coordinate that should remain unchanged by the specified
     * transformation.
     *
     * @param matrix the matrix to rotate
     * @param degrees the rotation degrees
     * @param px px of pivot point at (px, py)
     * @param py py of pivot point at (px, py)
     */
    public static void preRotate(@NonNull float[] matrix, float degrees, float px, float py) {
        normalize(matrix, px, py);
        Matrix.rotateM(matrix, 0, degrees, 0, 0, 1);
        denormalize(matrix, px, py);
    }

    /**
     * Postconcats the matrix with the specified rotation. M' = R(degrees, px, py) * M
     *
     * <p>The pivot point is the coordinate that should remain unchanged by the specified
     * transformation.
     *
     * @param matrix the matrix to rotate
     * @param degrees the rotation degrees
     * @param px px of pivot point at (px, py)
     * @param py py of pivot point at (px, py)
     */
    public static void postRotate(@NonNull float[] matrix, float degrees, float px, float py) {
        synchronized (sTemp) {
            Matrix.setIdentityM(sTemp, 0);
            normalize(sTemp, px, py);
            Matrix.rotateM(sTemp, 0, degrees, 0, 0, 1);
            denormalize(sTemp, px, py);
            Matrix.multiplyMM(matrix, 0, sTemp, 0, matrix, 0);
        }
    }

    /**
     * Converts to a well-formed matrix for debugging.
     *
     * <p>Get the first 16 floats from the offset.
     *
     * @param matrix the matrix to convert
     * @param offset the offset of the matrix
     */
    @NonNull
    public static String toString(@NonNull float[] matrix, int offset) {
        return String.format(Locale.US, "Matrix:\n"
                        + "%2.1f %2.1f %2.1f %2.1f\n"
                        + "%2.1f %2.1f %2.1f %2.1f\n"
                        + "%2.1f %2.1f %2.1f %2.1f\n"
                        + "%2.1f %2.1f %2.1f %2.1f",
                matrix[offset], matrix[offset + 4], matrix[offset + 8], matrix[offset + 12],
                matrix[offset + 1], matrix[offset + 5], matrix[offset + 9], matrix[offset + 13],
                matrix[offset + 2], matrix[offset + 6], matrix[offset + 10], matrix[offset + 14],
                matrix[offset + 3], matrix[offset + 7], matrix[offset + 11], matrix[offset + 15]);
    }

    private static void normalize(float[] matrix, float px, float py) {
        Matrix.translateM(matrix, 0, px, py, 0f);
    }

    private static void denormalize(float[] matrix, float px, float py) {
        Matrix.translateM(matrix, 0, -px, -py, 0f);
    }
}
