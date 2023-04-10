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
 * @hide
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

    // Kalman gain
    public @NonNull Matrix K;

    public KalmanFilter(int xDim, int zDim) {
        x = new Matrix(xDim, 1);
        P = Matrix.identity(xDim);
        Q = Matrix.identity(xDim);
        R = Matrix.identity(zDim);
        F = new Matrix(xDim, xDim);
        H = new Matrix(zDim, xDim);
        K = new Matrix(xDim, zDim);
    }

    /** Resets the internal state of this Kalman filter. */
    public void reset() {
        // NOTE: It is not necessary to reset Q, R, F, and H matrices.
        x.fill(0);
        Matrix.setIdentity(P);
        K.fill(0);
    }

    /**
     * Performs the prediction phase of the filter, using the state estimate to produce a new
     * estimate for the current timestep.
     */
    public void predict() {
        x = F.dot(x);
        P = F.dot(P).dotTranspose(F).plus(Q);
    }

    /** Updates the state estimate to incorporate the new observation z. */
    public void update(@NonNull Matrix z) {
        Matrix y = z.minus(H.dot(x));
        Matrix tS = H.dot(P).dotTranspose(H).plus(R);
        K = P.dotTranspose(H).dot(tS.inverse());
        x = x.plus(K.dot(y));
        P = P.minus(K.dot(H).dot(P));
    }
}
