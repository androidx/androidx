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

import java.util.Arrays;
import java.util.Locale;

// Based on http://androidxref.com/9.0.0_r3/xref/frameworks/opt/net/wifi/service/java/com/android/server/wifi/util/Matrix.java
/**
 * Utility for basic Matrix calculations.
 *
 */
@RestrictTo(LIBRARY)
public class Matrix {

    private final int mRows;
    private final int mCols;
    private final double[] mMem;

    /**
     * Creates a new matrix, initialized to zeros.
     *
     * @param rows number of mRows
     * @param cols number of columns
     */
    public Matrix(int rows, int cols) {
        mRows = rows;
        mCols = cols;
        mMem = new double[rows * cols];
    }

    /**
     * Creates a new matrix using the provided array of values
     *
     * <p>Values are in row-major order.
     *
     * @param stride the number of columns
     * @param values the array of values
     * @throws IllegalArgumentException if length of values array not a multiple of stride
     */
    public Matrix(int stride, @NonNull double[] values) {
        mRows = (values.length + stride - 1) / stride;
        mCols = stride;
        mMem = values;
        if (mMem.length != mRows * mCols) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.ROOT,
                            "Invalid number of elements in 'values' Expected:%d Actual:%d",
                            mMem.length,
                            (mRows & mCols)));
        }
    }

    /**
     * Creates a new matrix, and copies the contents from the given {@code src} matrix.
     *
     * @param src the matrix to copy from
     */
    public Matrix(@NonNull Matrix src) {
        mRows = src.mRows;
        mCols = src.mCols;
        mMem = new double[mRows * mCols];
        System.arraycopy(src.mMem, 0, mMem, 0, mMem.length);
    }

    /** Returns the number of rows in the matrix. */
    public int getNumRows() {
        return mRows;
    }

    /** Returns the number of columns in the matrix. */
    public int getNumCols() {
        return mCols;
    }

    /**
     * Creates an identity matrix with the given {@code width}.
     *
     * @param width the height and width of the identity matrix
     * @return newly created identity matrix
     */
    public static @NonNull Matrix identity(int width) {
        final Matrix ret = new Matrix(width, width);
        setIdentity(ret);
        return ret;
    }

    /**
     * Sets all the diagonal elements to one and everything else to zero. If this is a square
     * matrix, then it will be an identity matrix.
     *
     * @param matrix the matrix to perform the operation
     */
    public static void setIdentity(@NonNull Matrix matrix) {
        Arrays.fill(matrix.mMem, 0.);
        final int width = matrix.mRows < matrix.mCols ? matrix.mRows : matrix.mCols;
        for (int i = 0; i < width; i++) {
            matrix.put(i, i, 1);
        }
    }

    /**
     * Gets the value from row i, column j.
     *
     * @param i row number
     * @param j column number
     * @return the value at at i,j
     * @throws IndexOutOfBoundsException if an index is out of bounds
     */
    public double get(int i, int j) {
        if (!(0 <= i && i < mRows && 0 <= j && j < mCols)) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            Locale.ROOT,
                            "Invalid matrix index value. i:%d j:%d not available in %s",
                            i,
                            j,
                            shortString()));
        }
        return mMem[i * mCols + j];
    }

    /**
     * Store a value in row i, column j.
     *
     * @param i row number
     * @param j column number
     * @param v value to store at i,j
     * @throws IndexOutOfBoundsException if an index is out of bounds
     */
    public void put(int i, int j, double v) {
        if (!(0 <= i && i < mRows && 0 <= j && j < mCols)) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            Locale.ROOT,
                            "Invalid matrix index value. i:%d j:%d not available in %s",
                            i,
                            j,
                            shortString()));
        }
        mMem[i * mCols + j] = v;
    }

    /**
     * Sets all the elements to {@code value}.
     *
     * @param value the value to fill the matrix
     */
    public void fill(double value) {
        Arrays.fill(mMem, value);
    }

    /**
     * Scales every element by {@code alpha}.
     *
     * @param alpha the amount each element is multiplied by
     */
    public void scale(double alpha) {
        final int size = mRows * mCols;
        for (int i = 0; i < size; ++i) {
            mMem[i] *= alpha;
        }
    }

    /**
     * Adds all elements of this matrix with {@code that}.
     *
     * @param that the other matrix
     * @return a newly created matrix representing the sum of this and that
     * @throws IllegalArgumentException if the dimensions differ
     */
    public @NonNull Matrix plus(@NonNull Matrix that) {
        if (!(mRows == that.mRows && mCols == that.mCols)) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.ROOT,
                            "The matrix dimensions are not the same. this:%s that:%s",
                            shortString(),
                            that.shortString()));
        }
        for (int i = 0; i < mMem.length; i++) {
            mMem[i] = mMem[i] + that.mMem[i];
        }
        return this;
    }

    /**
     * Calculates the difference this matrix and {@code that}.
     *
     * @param that the other matrix
     * @return newly created matrix representing the difference of this and that
     * @throws IllegalArgumentException if the dimensions differ
     */
    public @NonNull Matrix minus(@NonNull Matrix that) {
        if (!(mRows == that.mRows && mCols == that.mCols)) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.ROOT,
                            "The matrix dimensions are not the same. this:%s that:%s",
                            shortString(),
                            that.shortString()));
        }
        for (int i = 0; i < mMem.length; i++) {
            mMem[i] = mMem[i] - that.mMem[i];
        }
        return this;
    }

    /**
     * Calculates the matrix product of this matrix and {@code that}.
     *
     * @param that the other matrix
     * @param result matrix to hold the result
     * @return result, filled with the matrix product
     * @throws IllegalArgumentException if the dimensions differ
     */
    public @NonNull Matrix dot(@NonNull Matrix that, @NonNull Matrix result) {
        if (!(mRows == result.mRows && mCols == that.mRows && that.mCols == result.mCols)) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.ROOT,
                            "The matrices dimensions are not conformant for a dot matrix "
                                    + "operation. this:%s that:%s result:%s",
                            shortString(),
                            that.shortString(),
                            result.shortString()));
        }
        for (int i = 0; i < mRows; i++) {
            for (int j = 0; j < that.mCols; j++) {
                double s = 0.0;
                for (int k = 0; k < mCols; k++) {
                    s += get(i, k) * that.get(k, j);
                }
                result.put(i, j, s);
            }
        }
        return result;
    }

    /**
     * Calculates the inverse of a square matrix
     *
     * @param scratch the matrix [rows, 2*cols] to hold the temporary information
     *
     * @return newly created matrix representing the matrix inverse
     * @throws ArithmeticException if the matrix is not invertible
     */
    public @NonNull Matrix inverse(@NonNull Matrix scratch) {
        if (!(mRows == mCols)) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "The matrix is not square. this:%s", shortString()));
        }

        if (scratch.mRows != mRows || scratch.mCols != 2 * mCols) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.ROOT,
                            "The scratch matrix size is not correct. this:%s",
                            scratch.shortString()
                    )
            );
        }

        for (int i = 0; i < mRows; i++) {
            for (int j = 0; j < mCols; j++) {
                scratch.put(i, j, get(i, j));
                scratch.put(i, mCols + j, i == j ? 1.0 : 0.0);
            }
        }

        for (int i = 0; i < mRows; i++) {
            int ibest = i;
            double vbest = Math.abs(scratch.get(ibest, ibest));
            for (int ii = i + 1; ii < mRows; ii++) {
                double v = Math.abs(scratch.get(ii, i));
                if (v > vbest) {
                    ibest = ii;
                    vbest = v;
                }
            }
            if (ibest != i) {
                for (int j = 0; j < scratch.mCols; j++) {
                    double t = scratch.get(i, j);
                    scratch.put(i, j, scratch.get(ibest, j));
                    scratch.put(ibest, j, t);
                }
            }
            double d = scratch.get(i, i);
            if (d == 0.0) {
                throw new ArithmeticException("Singular matrix");
            }
            for (int j = 0; j < scratch.mCols; j++) {
                scratch.put(i, j, scratch.get(i, j) / d);
            }
            for (int ii = i + 1; ii < mRows; ii++) {
                d = scratch.get(ii, i);
                for (int j = 0; j < scratch.mCols; j++) {
                    scratch.put(ii, j, scratch.get(ii, j) - d * scratch.get(i, j));
                }
            }
        }
        for (int i = mRows - 1; i >= 0; i--) {
            for (int ii = 0; ii < i; ii++) {
                double d = scratch.get(ii, i);
                for (int j = 0; j < scratch.mCols; j++) {
                    scratch.put(ii, j, scratch.get(ii, j) - d * scratch.get(i, j));
                }
            }
        }
        for (int i = 0; i < mRows; i++) {
            for (int j = 0; j < mCols; j++) {
                put(i, j, scratch.get(i, mCols + j));
            }
        }
        return this;
    }

    /**
     * Calculates the matrix product with the transpose of a second matrix.
     *
     * @param that the other matrix
     * @param result space to hold the result
     * @return result, filled with the matrix product of this and that.transpose()
     * @throws IllegalArgumentException if shapes are not conformant
     */
    public @NonNull Matrix dotTranspose(@NonNull Matrix that, @NonNull Matrix result) {
        if (!(mRows == result.mRows && mCols == that.mCols && that.mRows == result.mCols)) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.ROOT,
                            "The matrices dimensions are not conformant for a transpose "
                                    + "operation. this:%s that:%s result:%s",
                            shortString(),
                            that.shortString(),
                            result.shortString()));
        }
        for (int i = 0; i < mRows; i++) {
            for (int j = 0; j < that.mRows; j++) {
                double s = 0.0;
                for (int k = 0; k < mCols; k++) {
                    s += get(i, k) * that.get(j, k);
                }
                result.put(i, j, s);
            }
        }
        return result;
    }

    /** Tests for equality. */
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Matrix)) {
            return false;
        }
        Matrix other = (Matrix) that;
        if (mRows != other.mRows) {
            return false;
        }
        if (mCols != other.mCols) {
            return false;
        }
        for (int i = 0; i < mMem.length; i++) {
            if (mMem[i] != other.mMem[i]) {
                return false;
            }
        }
        return true;
    }

    /** Calculates a hash code of this matrix. */
    @Override
    public int hashCode() {
        int h = mRows * 101 + mCols;
        for (double m : mMem) {
            h = h * 37 + Double.hashCode(m);
        }
        return h;
    }

    /**
     * Returns a string representation of this matrix.
     *
     * @return string like "2x2 [a, b; c, d]"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(mRows * mCols * 8);
        sb.append(mRows).append("x").append(mCols).append(" [");
        for (int i = 0; i < mMem.length; i++) {
            if (i > 0) {
                sb.append(i % mCols == 0 ? "; " : ", ");
            }
            sb.append(mMem[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /** Returns the size of the matrix as a String. */
    private String shortString() {
        return "(" + mRows + "x" + mCols + ")";
    }
}
