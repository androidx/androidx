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
package androidx.compose.remote.core.operations.utilities;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.Utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Arrays;

/** This Matrix class is used to represent up to 4x4 matrix. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Matrix {
    int mDim0 = 4;
    int mDim1 = 4;
    float @NonNull [] mMatrix = new float[16]; // support up to 4x4 matrices
    public static final @NonNull Matrix sTmpMatrix1 = new Matrix();
    public static final @NonNull Matrix sTmpMatrix2 = new Matrix();
    public static float @Nullable [] sTempOutVec;
    public static float @Nullable [] sTempInVec;

    /** Creates a new identity matrix. */
    public Matrix() {
        setIdentity();
    }

    /**
     * Creates a new matrix with the given dimensions.
     *
     * @param dim0 The number of rows.
     * @param dim1 The number of columns.
     */
    public Matrix(int dim0, int dim1) {
        setDimensions(dim0, dim1);
    }

    /**
     * Sets the dimensions of the matrix.
     *
     * @param dim0 The number of rows.
     * @param dim1 The number of columns.
     */
    public void setDimensions(int dim0, int dim1) {
        this.mDim0 = dim0;
        this.mDim1 = dim1;
        this.mMatrix = new float[dim0 * dim1];
    }

    /**
     * Copies the values from one matrix to another.
     *
     * @param src The source matrix.
     * @param dest The destination matrix.
     */
    public static void copy(@NonNull Matrix src, @NonNull Matrix dest) {
        dest.setDimensions(src.mDim0, src.mDim1);
        for (int i = 0; i < src.mMatrix.length; i++) {
            dest.mMatrix[i] = src.mMatrix[i];
        }
    }

    /**
     * Copies the values from one matrix to another.
     *
     * @param src The source matrix.
     */
    public void copyFrom(@NonNull Matrix src) {
        setDimensions(src.mDim0, src.mDim1);
        for (int i = 0; i < mMatrix.length; i++) {
            mMatrix[i] = src.mMatrix[i];
        }
    }

    /** Sets the matrix to the identity matrix. */
    public void setIdentity() {
        Arrays.fill(mMatrix, 0.0f);
        for (int i = 0; i < Math.min(mDim0, mDim1); i++) {
            mMatrix[i * mDim1 + i] = 1.0f;
        }
    }

    /**
     * The matrix multiplication operation.
     *
     * @param a The first matrix.
     * @param b The second matrix.
     * @param dest The destination matrix.
     */
    public static void multiply(@NonNull Matrix a, @NonNull Matrix b, @NonNull Matrix dest) {
        dest.setDimensions(a.mDim0, b.mDim1);
        for (int i = 0; i < dest.mDim0; i++) {
            for (int j = 0; j < dest.mDim1; j++) {
                float sum = 0.0f;
                for (int k = 0; k < a.mDim1; k++) {
                    sum += a.mMatrix[i * a.mDim1 + k] * b.mMatrix[k * b.mDim1 + j];
                }
                dest.mMatrix[i * dest.mDim1 + j] = sum;
            }
        }
    }

    /**
     * Gets the index of the matrix element.
     *
     * @param row The row index.
     * @param col The column index.
     * @return The index of the matrix element.
     */
    private int getIndex(int row, int col) {
        if (row < 0 || row >= mDim0 || col < 0 || col >= mDim1) {
            throw new IndexOutOfBoundsException(
                    "Matrix index ("
                            + row
                            + ", "
                            + col
                            + ") out of bounds for ("
                            + mDim0
                            + "x"
                            + mDim1
                            + ")");
        }
        return row * mDim1 + col;
    }

    /**
     * Gets the value of the matrix element.
     *
     * @param row The row index.
     * @param col The column index.
     * @return The value of the matrix element.
     */
    public float get(int row, int col) {
        return mMatrix[getIndex(row, col)];
    }

    /**
     * Sets the value of the matrix element.
     *
     * @param row The row index.
     * @param col The column index.
     * @param value The value to set.
     */
    public void set(int row, int col, float value) {
        mMatrix[getIndex(row, col)] = value;
    }

    /**
     * Rotates the matrix around the X axis.
     *
     * @param degrees The rotation angle in degrees.
     */
    public void rotateX(float degrees) {
        float angleRadians = (float) Math.toRadians(degrees);

        float cosTheta = (float) Math.cos(angleRadians);
        float sinTheta = (float) Math.sin(angleRadians);
        sTmpMatrix1.setIdentity();
        sTmpMatrix1.set(1, 1, cosTheta);
        sTmpMatrix1.set(1, 2, -sinTheta);
        sTmpMatrix1.set(2, 1, sinTheta);
        sTmpMatrix1.set(2, 2, cosTheta);
        multiply(this, sTmpMatrix1, sTmpMatrix2);
        copyFrom(sTmpMatrix2);
    }

    /**
     * Rotates the matrix around the Y axis.
     *
     * @param degrees The rotation angle in degrees.
     */
    public void rotateY(float degrees) {
        float angleRadians = (float) Math.toRadians(degrees);

        float cosTheta = (float) Math.cos(angleRadians);
        float sinTheta = (float) Math.sin(angleRadians);
        sTmpMatrix1.setIdentity();
        sTmpMatrix1.set(0, 0, cosTheta);
        sTmpMatrix1.set(0, 2, sinTheta);
        sTmpMatrix1.set(2, 0, -sinTheta);
        sTmpMatrix1.set(2, 2, cosTheta);
        multiply(this, sTmpMatrix1, sTmpMatrix2);
        copyFrom(sTmpMatrix2);
    }

    /**
     * Rotates the matrix around the Z axis.
     *
     * @param degrees The rotation angle in degrees.
     */
    public void rotateZ(float degrees) {
        float angleRadians = (float) Math.toRadians(degrees);
        float cosTheta = (float) Math.cos(angleRadians);
        float sinTheta = (float) Math.sin(angleRadians);
        sTmpMatrix1.setIdentity();
        sTmpMatrix1.set(0, 0, cosTheta);
        sTmpMatrix1.set(0, 1, -sinTheta);
        sTmpMatrix1.set(1, 0, sinTheta);
        sTmpMatrix1.set(1, 1, cosTheta);
        multiply(this, sTmpMatrix1, sTmpMatrix2);
        copyFrom(sTmpMatrix2);
    }

    /**
     * Translates the matrix.
     *
     * @param x The translation amount in the X direction.
     * @param y The translation amount in the Y direction.
     * @param z The translation amount in the Z direction.
     */
    public void translate(float x, float y, float z) {
        sTmpMatrix1.setIdentity();
        sTmpMatrix1.set(0, 3, x);
        sTmpMatrix1.set(1, 3, y);
        sTmpMatrix1.set(2, 3, z);
        multiply(this, sTmpMatrix1, sTmpMatrix2);
        copyFrom(sTmpMatrix2);
    }

    /**
     * Formats an element into a fixed length string.
     *
     * @param s The element to format.
     * @return The formatted string.
     */
    private static String six(String s) {
        if (s.length() < 6) {
            return String.format("%6s", s);
        } else {
            return s;
        }
    }

    /**
     * Formats the matrix into a string.
     *
     * @return The formatted string.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        DecimalFormat df = new DecimalFormat("0.00");
        for (int i = 0; i < mDim0; i++) {
            for (int j = 0; j < mDim1; j++) {
                if (j != 0) {
                    str.append(" ");
                }
                str.append(six(df.format(mMatrix[i * mDim1 + j])));
            }
            str.append("\n");
        }
        return str.toString();
    }

    /**
     * Scales the matrix.
     *
     * @param x The scaling factor in the X direction.
     * @param y The scaling factor in the Y direction.
     * @param z The scaling factor in the Z direction.
     */
    public void setScale(float x, float y, float z) {
        mMatrix[getIndex(0, 0)] *= x;
        mMatrix[getIndex(1, 1)] *= y;
        mMatrix[getIndex(2, 2)] *= z;
    }

    /**
     * Rotates the matrix around the Z axis.
     *
     * @param pivotX The pivot point in the X direction.
     * @param pivotY The pivot point in the Y direction.
     * @param degrees The rotation angle in degrees.
     */
    public void rotateZ(float pivotX, float pivotY, float degrees) {
        float angleRadians = (float) Math.toRadians(degrees);
        float cosTheta = (float) Math.cos(angleRadians);
        float sinTheta = (float) Math.sin(angleRadians);
        float oneMinusCos = 1.0f - cosTheta;

        // Calculate the effective translation components of the combined T(P)*Rz*T(-P) matrix
        float tx = pivotX * oneMinusCos + pivotY * sinTheta;
        float ty = pivotY * oneMinusCos - pivotX * sinTheta;

        float[] resultData = new float[16]; // Temporary array for results

        for (int i = 0; i < 4; i++) { // row of result
            for (int j = 0; j < 4; j++) { // column of result / column of 'this'
                float sum = 0.0f;
                for (int k = 0; k < 4; k++) { // inner dimension / row of 'this'
                    // Determine the element M[i][k] from the conceptual pivot rotation matrix M
                    float m_ik;
                    if (i == 0) { // Row 0 of M
                        if (k == 0) {
                            m_ik = cosTheta;
                        } else if (k == 1) {
                            m_ik = -sinTheta;
                        } else if (k == 3) {
                            m_ik = tx;
                        } else /* k == 2 */ {
                            m_ik = 0.0f;
                        }
                    } else if (i == 1) { // Row 1 of M
                        if (k == 0) {
                            m_ik = sinTheta;
                        } else if (k == 1) {
                            m_ik = cosTheta;
                        } else if (k == 3) {
                            m_ik = ty;
                        } else /* k == 2 */ {
                            m_ik = 0.0f;
                        }
                    } else if (i == 2) { // Row 2 of M
                        m_ik = (k == 2) ? 1.0f : 0.0f;
                    } else { // i == 3, Row 3 of M
                        m_ik = (k == 3) ? 1.0f : 0.0f;
                    }

                    // Get element from original 'this' matrix: this[k][j]
                    float this_kj = this.mMatrix[k * 4 + j]; // k is row index for 'this'

                    sum += m_ik * this_kj;
                }
                resultData[i * 4 + j] = sum;
            }
        }
        System.arraycopy(resultData, 0, this.mMatrix, 0, 16);
    }

    /**
     * Copies the values from the matrix to an array.
     *
     * @param dest The destination array.
     */
    public void putValues(float @NonNull [] dest) {
        for (int i = 0; i < dest.length; i++) {
            dest[i] = mMatrix[i];
        }
    }

    /**
     * Sets up a projection matrix.
     *
     * @param fovDegrees The field of view in degrees.
     * @param aspectRatio The aspect ratio of the viewport.
     * @param near The near clipping plane distance.
     * @param far The far clipping plane distance.
     */
    public void projection(float fovDegrees, float aspectRatio, float near, float far) {
        float[] matrix = sTmpMatrix1.mMatrix;

        // Convert FOV from degrees to radians
        float fovRadians = (float) Math.toRadians(fovDegrees);
        float f = 1.0f / (float) Math.tan(fovRadians / 2.0f);

        float rangeInv = 1.0f / (near - far);

        // Column-major order
        matrix[0] = f / aspectRatio;
        matrix[1] = 0.0f;
        matrix[2] = 0.0f;
        matrix[3] = 0.0f;

        matrix[4] = 0.0f;
        matrix[5] = f;
        matrix[6] = 0.0f;
        matrix[7] = 0.0f;

        matrix[8] = 0.0f;
        matrix[9] = 0.0f;
        matrix[10] = (far + near) * rangeInv;
        matrix[11] = -1.0f;

        matrix[12] = 0.0f;
        matrix[13] = 0.0f;
        matrix[14] = (2.0f * far * near) * rangeInv;
        matrix[15] = 0.0f;
        //        System.out.println("projection\n"+sTmpMatrix1+"\n");
        multiply(this, sTmpMatrix1, sTmpMatrix2);

        copyFrom(sTmpMatrix2);
    }

    /**
     * Copies the values from an array to the matrix.
     *
     * @param values The source array.
     */
    public void copyFrom(float @NonNull [] values) {
        if (values.length == 16) {
            for (int i = 0; i < values.length; i++) {
                mMatrix[i] = values[i];
            }
        } else if (values.length == 9) {
            mMatrix[0] = values[0];
            mMatrix[1] = values[1];
            mMatrix[3] = values[2];
            mMatrix[4] = values[3];
            mMatrix[5] = values[4];
            mMatrix[6] = values[5];
            mMatrix[8] = values[6];
            mMatrix[9] = values[7];
            mMatrix[10] = values[8];
            mMatrix[11] = 0.0f;

            mMatrix[12] = 0.0f;
            mMatrix[13] = 0.0f;
            mMatrix[14] = 0.0f;
            mMatrix[15] = 1.0f;
        }
    }

    /**
     * Applies a rotation around an arbitrary axis vector (vx, vy, vz) passing through the origin
     * Assumes this is a 4x4 matrix. Performs: this = R_axis * this.
     *
     * @param vx The x-component of the rotation axis vector.
     * @param vy The y-component of the rotation axis vector.
     * @param vz The z-component of the rotation axis vector.
     * @param angleDegrees The rotation angle in degrees.
     * @throws IllegalStateException if this matrix is not 4x4.
     * @throws IllegalArgumentException if the axis vector is a zero vector.
     */
    public void rotateAroundAxis(float vx, float vy, float vz, float angleDegrees) {
        // Normalize the axis vector
        double angleRadians = angleDegrees * Math.PI / 180.0f;
        float lenSq = vx * vx + vy * vy + vz * vz;
        if (lenSq == 0.0f) { // Or very close to zero, e.g., < 1e-9f
            // If angle is also effectively zero, it's identity. Otherwise, axis is undefined.
            // For simplicity, if axis is zero, no rotation occurs.
            // A more robust solution might throw an IllegalArgumentException
            // if lenSq is too small and angle is non-zero.
            if (angleRadians != 0.0f) {
                System.err.println(
                        "Warning: Rotation axis vector is zero. No rotation applied for non-zero"
                                + " angle.");
                // Or throw new IllegalArgumentException("Rotation axis vector cannot be zero for a
                // non-zero angle.");
            }
            return; // No rotation if axis is zero or angle is zero
        }
        float len = (float) Math.sqrt(lenSq);
        float ux = vx / len;
        float uy = vy / len;
        float uz = vz / len;

        float cosTheta = (float) Math.cos(angleRadians);
        float sinTheta = (float) Math.sin(angleRadians);
        float oneMinusCos = 1.0f - cosTheta;

        // Pre-calculate terms for the conceptual rotation matrix R
        // R[row][col]
        float r00 = cosTheta + ux * ux * oneMinusCos;
        float r01 = ux * uy * oneMinusCos - uz * sinTheta;
        float r02 = ux * uz * oneMinusCos + uy * sinTheta;
        // r03 = 0

        float r10 = uy * ux * oneMinusCos + uz * sinTheta;
        float r11 = cosTheta + uy * uy * oneMinusCos;
        float r12 = uy * uz * oneMinusCos - ux * sinTheta;
        // r13 = 0

        float r20 = uz * ux * oneMinusCos - uy * sinTheta;
        float r21 = uz * uy * oneMinusCos + ux * sinTheta;
        float r22 = cosTheta + uz * uz * oneMinusCos;
        // r23 = 0

        // r30, r31, r32 = 0; r33 = 1;

        float[] resultData = sTmpMatrix1.mMatrix; // Temporary array for results

        for (int i = 0; i < 4; i++) { // row of result
            for (int j = 0; j < 4; j++) { // column of result / column of 'this'
                float sum = 0.0f;
                for (int k = 0; k < 4; k++) { // inner dimension / row of 'this'
                    float r_ik; // Element from conceptual rotation matrix R

                    if (i == 0) {
                        if (k == 0) {
                            r_ik = r00;
                        } else if (k == 1) {
                            r_ik = r01;
                        } else if (k == 2) {
                            r_ik = r02;
                        } else {
                            r_ik = 0.0f; // k==3
                        }
                    } else if (i == 1) {
                        if (k == 0) {
                            r_ik = r10;
                        } else if (k == 1) {
                            r_ik = r11;
                        } else if (k == 2) {
                            r_ik = r12;
                        } else {
                            r_ik = 0.0f; // k==3
                        }
                    } else if (i == 2) {
                        if (k == 0) {
                            r_ik = r20;
                        } else if (k == 1) {
                            r_ik = r21;
                        } else if (k == 2) {
                            r_ik = r22;
                        } else {
                            r_ik = 0.0f; // k==3
                        }
                    } else { // i == 3
                        r_ik = (k == 3) ? 1.0f : 0.0f;
                    }

                    float this_kj = this.mMatrix[k * 4 + j]; // k is row index for 'this'
                    sum += r_ik * this_kj;
                }
                resultData[i * 4 + j] = sum;
            }
        }
        multiply(this, sTmpMatrix1, sTmpMatrix2);
        copyFrom(sTmpMatrix2);
        // Copy result back
        System.arraycopy(resultData, 0, this.mMatrix, 0, 16);
    }

    /**
     * The matrix multiplication operation.
     *
     * @param input
     * @param out
     */
    public void multiply(float @NonNull [] input, float @NonNull [] out) {
        for (int j = 0; j < out.length; j++) {
            float tmp = 0;
            for (int i = 0; i < input.length; i++) {
                tmp += mMatrix[i + j * 4] * input[i];
            }
            out[j] = tmp + mMatrix[3 + j * 4];
        }
    }

    /**
     * The matrix multiplication operation. This can also used to perform perspective transform
     *
     * @param input input needs to be a 2,3,4 floats
     * @param out input needs to be a 2,3,4 floats
     */
    public void evalPerspective(float @NonNull [] input, float @NonNull [] out) {

        if (input.length < 4) {
            if (sTempInVec == null) {
                sTempInVec = new float[4];
                sTempOutVec = new float[4];
                sTempInVec[3] = 1;
                Utils.log("perspective transform ");
            }
            System.arraycopy(input, 0, sTempInVec, 0, input.length);
        }

        for (int j = 0; j < sTempOutVec.length; j++) {
            float tmp = 0;
            for (int i = 0; i < sTempInVec.length; i++) {
                tmp += mMatrix[i + j * 4] * sTempInVec[i];
            }
            sTempOutVec[j] = tmp;
        }

        for (int i = 0; i < out.length; i++) {
            sTempOutVec[i] /= sTempOutVec[3];
        }

        System.arraycopy(sTempOutVec, 0, out, 0, out.length);
    }
}
