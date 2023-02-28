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
import androidx.input.motionprediction.kalman.matrix.DVector2;
import androidx.input.motionprediction.kalman.matrix.Matrix;

/**
 * Class that independently applies the Kalman Filter to each axis of the pen.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class PointerKalmanFilter {
    private final KalmanFilter mXKalman;
    private final KalmanFilter mYKalman;
    private final KalmanFilter mPKalman;

    private final DVector2 mPosition = new DVector2();
    private final DVector2 mVelocity = new DVector2();
    private final DVector2 mAcceleration = new DVector2();
    private final DVector2 mJank = new DVector2();
    private double mPressure = 0;
    private double mPressureChange = 0;

    private double mSigmaProcess;
    private double mSigmaMeasurement;

    private int mNumIterations = 0;

    private final Matrix mNewX = new Matrix(1, 1);
    private final Matrix mNewY = new Matrix(1, 1);
    private final Matrix mNewP = new Matrix(1, 1);

    /**
     * @param sigmaProcess lower value = more filtering
     * @param sigmaMeasurement higher value = more filtering
     */
    public PointerKalmanFilter(double sigmaProcess, double sigmaMeasurement) {
        mSigmaProcess = sigmaProcess;
        mSigmaMeasurement = sigmaMeasurement;
        mXKalman = createAxisKalmanFilter();
        mYKalman = createAxisKalmanFilter();
        mPKalman = createAxisKalmanFilter();
    }

    /** Reset filter into a neutral state. */
    public void reset() {
        mXKalman.reset();
        mYKalman.reset();
        mPKalman.reset();
        mNumIterations = 0;
    }

    /**
     * Update internal model of pen with new measurement. The state of the model can be obtained by
     * the getPosition, getVelocity, etc methods.
     */
    public void update(float x, float y, float pressure) {
        if (mNumIterations == 0) {
            mXKalman.x.put(0, 0, x);
            mYKalman.x.put(0, 0, y);
            mPKalman.x.put(0, 0, pressure);
        } else {
            mNewX.put(0, 0, x);
            mXKalman.predict();
            mXKalman.update(mNewX);

            mNewY.put(0, 0, y);
            mYKalman.predict();
            mYKalman.update(mNewY);

            mNewP.put(0, 0, pressure);
            mPKalman.predict();
            mPKalman.update(mNewP);
        }
        mNumIterations += 1;

        mPosition.a1 = mXKalman.x.get(0, 0);
        mPosition.a2 = mYKalman.x.get(0, 0);
        mVelocity.a1 = mXKalman.x.get(1, 0);
        mVelocity.a2 = mYKalman.x.get(1, 0);
        mAcceleration.a1 = mXKalman.x.get(2, 0);
        mAcceleration.a2 = mYKalman.x.get(2, 0);
        mJank.a1 = mXKalman.x.get(3, 0);
        mJank.a2 = mYKalman.x.get(3, 0);
        mPressure = mPKalman.x.get(0, 0);
        mPressureChange = mPKalman.x.get(1, 0);
    }

    public @NonNull DVector2 getPosition() {
        return mPosition;
    }

    public @NonNull DVector2 getVelocity() {
        return mVelocity;
    }

    public @NonNull DVector2 getAcceleration() {
        return mAcceleration;
    }

    public @NonNull DVector2 getJank() {
        return mJank;
    }

    public double getPressure() {
        return mPressure;
    }

    public double getPressureChange() {
        return mPressureChange;
    }

    public int getNumIterations() {
        return mNumIterations;
    }

    private KalmanFilter createAxisKalmanFilter() {
        // We tune the filter with a normalized dt=1, then apply the actual report rate during
        // prediction.
        final double dt = 1.0;

        final KalmanFilter kalman = new KalmanFilter(4, 1);

        // State transition matrix is derived from basic physics:
        // new_x = x + v * dt + 1/2 * a * dt^2 + 1/6 * jank * dt^3
        // new_v = v + a * dt + 1/2 * jank * dt^2
        // ...
        kalman.F = new Matrix(4,
                new double[]{
                        1.0, dt, 0.5 * dt * dt, 0.16 * dt * dt * dt,
                        0.0, 1.0, dt, 0.5 * dt * dt,
                        0.0, 0.0, 1.0, dt,
                        0, 0, 0, 1.0
                });

        // We model the system noise as a noisy force on the pen.
        // The matrix G describes the impact of that noise on each state.
        final Matrix g = new Matrix(1, new double[] {0.16 * dt * dt * dt, 0.5 * dt * dt, dt, 1});
        g.dotTranspose(g, kalman.Q);
        kalman.Q.scale(mSigmaProcess);

        // Measurements only impact the location
        kalman.H = new Matrix(4, new double[] {1.0, 0.0, 0.0, 0.0});

        // Measurement noise is a 1-D normal distribution
        kalman.R.put(0, 0, mSigmaMeasurement);

        return kalman;
    }
}
