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

package androidx.input.motionprediction.kalman;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.input.motionprediction.kalman.matrix.Matrix;

/**
 * Kalman filter implementation following http://filterpy.readthedocs.io/en/latest/
 *
 * <p>To keep a reasonable naming scheme we are not following android naming conventions in this
 * class.
 *
 * <p>To improve performance, this filter is specialized to use a 4 dimensional state, with single
 * dimension measurements.
 *
 */
@RestrictTo(LIBRARY)
public class KalmanFilter {
    // State estimate
    public @NonNull Matrix x;

    // State estimate covariance
    public @NonNull Matrix P;

    // Process noise
    public @NonNull Matrix Q;

    // Measurement noise (mZDim, mZDim)
    public @NonNull Matrix R;

    // State transition matrix
    public @NonNull Matrix F;

    // Measurement matrix
    public @NonNull Matrix H;

    // Buffers to minimize matrix allocations on every MotionEvent
    private @NonNull Matrix mBufferXDimOne;
    private @NonNull Matrix mBufferXDimXDim;
    private @NonNull Matrix mBufferXDimXDim2;
    private @NonNull Matrix mBufferXDimZDim;
    private @NonNull Matrix mBufferXDimZDim2;
    private @NonNull Matrix mBufferZDimOne;
    private @NonNull Matrix mBufferZDimXDim;
    private @NonNull Matrix mBufferZDimZDim;
    private @NonNull Matrix mBufferZDimTwiceZDim;

    public KalmanFilter(int xDim, int zDim) {
        x = new Matrix(xDim, 1);
        P = Matrix.identity(xDim);
        Q = Matrix.identity(xDim);
        R = Matrix.identity(zDim);
        F = new Matrix(xDim, xDim);
        H = new Matrix(zDim, xDim);
        mBufferXDimZDim = new Matrix(xDim, zDim);
        mBufferXDimZDim2 = new Matrix(xDim, zDim);
        mBufferXDimOne = new Matrix(xDim, 1);
        mBufferXDimXDim = new Matrix(xDim, xDim);
        mBufferXDimXDim2 = new Matrix(xDim, xDim);
        mBufferZDimOne = new Matrix(zDim, 1);
        mBufferZDimXDim = new Matrix(zDim, xDim);
        mBufferZDimZDim = new Matrix(zDim, zDim);
        mBufferZDimTwiceZDim = new Matrix(zDim, 2 * zDim);
    }

    /** Resets the internal state of this Kalman filter. */
    public void reset() {
        // NOTE: It is not necessary to reset Q, R, F, and H matrices.
        x.fill(0);
        Matrix.setIdentity(P);
    }

    /**
     * Performs the prediction phase of the filter, using the state estimate to produce a new
     * estimate for the current timestep.
     */
    public void predict() {
        Matrix originalX = x;
        x = F.dot(x, mBufferXDimOne);
        mBufferXDimOne = originalX;

        F.dot(P, mBufferXDimXDim).dotTranspose(F, P).plus(Q);
    }

    /** Updates the state estimate to incorporate the new observation z. */
    public void update(@NonNull Matrix z) {
        z.minus(H.dot(x, mBufferZDimOne));
        H.dot(P, mBufferZDimXDim)
                .dotTranspose(H, mBufferZDimZDim)
                .plus(R)
                .inverse(mBufferZDimTwiceZDim);

        P.dotTranspose(H, mBufferXDimZDim2).dot(mBufferZDimZDim, mBufferXDimZDim);

        x.plus(mBufferXDimZDim.dot(z, mBufferXDimOne));
        P.minus(mBufferXDimZDim.dot(H, mBufferXDimXDim).dot(P, mBufferXDimXDim2));
    }
}
