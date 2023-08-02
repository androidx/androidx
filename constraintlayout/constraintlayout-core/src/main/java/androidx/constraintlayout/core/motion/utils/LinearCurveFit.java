/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.constraintlayout.core.motion.utils;

/**
 * This performs a simple linear interpolation in multiple dimensions
 *
 *
 */
public class LinearCurveFit extends CurveFit {
    private static final String TAG = "LinearCurveFit";
    private double[] mT;
    private double[][] mY;
    private double mTotalLength = Double.NaN;
    private boolean mExtrapolate = true;
    double[] mSlopeTemp;

    public LinearCurveFit(double[] time, double[][] y) {
        final int dim = y[0].length;
        mSlopeTemp = new double[dim];
        mT = time;
        mY = y;
        if (dim > 2) {
            @SuppressWarnings("unused") double sum = 0;
            double lastx = 0, lasty = 0;
            for (int i = 0; i < time.length; i++) {
                double px = y[i][0];
                double py = y[i][0];
                if (i > 0) {
                    sum += Math.hypot(px - lastx, py - lasty);
                }
                lastx = px;
                lasty = py;
            }
            mTotalLength = 0;
        }
    }

    /**
     * Calculate the length traveled by the first two parameters assuming they are x and y.
     * (Added for future work)
     *
     * @param t the point to calculate the length to
     */
    @SuppressWarnings("unused")
    private double getLength2D(double t) {
        if (Double.isNaN(mTotalLength)) {
            return 0;
        }
        final int n = mT.length;
        if (t <= mT[0]) {
            return 0;
        }
        if (t >= mT[n - 1]) {
            return mTotalLength;
        }
        double sum = 0;
        double last_x = 0, last_y = 0;

        for (int i = 0; i < n - 1; i++) {
            double px = mY[i][0];
            double py = mY[i][1];
            if (i > 0) {
                sum += Math.hypot(px - last_x, py - last_y);
            }
            last_x = px;
            last_y = py;
            if (t == mT[i]) {
                return sum;
            }
            if (t < mT[i + 1]) {
                double h = mT[i + 1] - mT[i];
                double x = (t - mT[i]) / h;
                double x1 = mY[i][0];
                double x2 = mY[i + 1][0];
                double y1 = mY[i][1];
                double y2 = mY[i + 1][1];

                py -= y1 * (1 - x) + y2 * x;
                px -= x1 * (1 - x) + x2 * x;
                sum += Math.hypot(py, px);

                return sum;
            }
        }
        return 0;
    }

    // @TODO: add description
    @Override
    public void getPos(double t, double[] v) {
        final int n = mT.length;
        final int dim = mY[0].length;
        if (mExtrapolate) {
            if (t <= mT[0]) {
                getSlope(mT[0], mSlopeTemp);
                for (int j = 0; j < dim; j++) {
                    v[j] = mY[0][j] + (t - mT[0]) * mSlopeTemp[j];
                }
                return;
            }
            if (t >= mT[n - 1]) {
                getSlope(mT[n - 1], mSlopeTemp);
                for (int j = 0; j < dim; j++) {
                    v[j] = mY[n - 1][j] + (t - mT[n - 1]) * mSlopeTemp[j];
                }
                return;
            }
        } else {
            if (t <= mT[0]) {
                for (int j = 0; j < dim; j++) {
                    v[j] = mY[0][j];
                }
                return;
            }
            if (t >= mT[n - 1]) {
                for (int j = 0; j < dim; j++) {
                    v[j] = mY[n - 1][j];
                }
                return;
            }
        }

        for (int i = 0; i < n - 1; i++) {
            if (t == mT[i]) {
                for (int j = 0; j < dim; j++) {
                    v[j] = mY[i][j];
                }
            }
            if (t < mT[i + 1]) {
                double h = mT[i + 1] - mT[i];
                double x = (t - mT[i]) / h;
                for (int j = 0; j < dim; j++) {
                    double y1 = mY[i][j];
                    double y2 = mY[i + 1][j];

                    v[j] = y1 * (1 - x) + y2 * x;
                }
                return;
            }
        }
    }

    // @TODO: add description
    @Override
    public void getPos(double t, float[] v) {
        final int n = mT.length;
        final int dim = mY[0].length;
        if (mExtrapolate) {
            if (t <= mT[0]) {
                getSlope(mT[0], mSlopeTemp);
                for (int j = 0; j < dim; j++) {
                    v[j] = (float) (mY[0][j] + (t - mT[0]) * mSlopeTemp[j]);
                }
                return;
            }
            if (t >= mT[n - 1]) {
                getSlope(mT[n - 1], mSlopeTemp);
                for (int j = 0; j < dim; j++) {
                    v[j] = (float) (mY[n - 1][j] + (t - mT[n - 1]) * mSlopeTemp[j]);
                }
                return;
            }
        } else {
            if (t <= mT[0]) {
                for (int j = 0; j < dim; j++) {
                    v[j] = (float) mY[0][j];
                }
                return;
            }
            if (t >= mT[n - 1]) {
                for (int j = 0; j < dim; j++) {
                    v[j] = (float) mY[n - 1][j];
                }
                return;
            }
        }

        for (int i = 0; i < n - 1; i++) {
            if (t == mT[i]) {
                for (int j = 0; j < dim; j++) {
                    v[j] = (float) mY[i][j];
                }
            }
            if (t < mT[i + 1]) {
                double h = mT[i + 1] - mT[i];
                double x = (t - mT[i]) / h;
                for (int j = 0; j < dim; j++) {
                    double y1 = mY[i][j];
                    double y2 = mY[i + 1][j];

                    v[j] = (float) (y1 * (1 - x) + y2 * x);
                }
                return;
            }
        }
    }

    // @TODO: add description
    @Override
    public double getPos(double t, int j) {
        final int n = mT.length;
        if (mExtrapolate) {
            if (t <= mT[0]) {
                return mY[0][j] + (t - mT[0]) * getSlope(mT[0], j);
            }
            if (t >= mT[n - 1]) {
                return mY[n - 1][j] + (t - mT[n - 1]) * getSlope(mT[n - 1], j);
            }
        } else {
            if (t <= mT[0]) {
                return mY[0][j];
            }
            if (t >= mT[n - 1]) {
                return mY[n - 1][j];
            }
        }

        for (int i = 0; i < n - 1; i++) {
            if (t == mT[i]) {
                return mY[i][j];
            }
            if (t < mT[i + 1]) {
                double h = mT[i + 1] - mT[i];
                double x = (t - mT[i]) / h;
                double y1 = mY[i][j];
                double y2 = mY[i + 1][j];
                return (y1 * (1 - x) + y2 * x);

            }
        }
        return 0; // should never reach here
    }

    // @TODO: add description
    @Override
    public void getSlope(double t, double[] v) {
        final int n = mT.length;
        int dim = mY[0].length;
        if (t <= mT[0]) {
            t = mT[0];
        } else if (t >= mT[n - 1]) {
            t = mT[n - 1];
        }

        for (int i = 0; i < n - 1; i++) {
            if (t <= mT[i + 1]) {
                double h = mT[i + 1] - mT[i];
                for (int j = 0; j < dim; j++) {
                    double y1 = mY[i][j];
                    double y2 = mY[i + 1][j];

                    v[j] = (y2 - y1) / h;
                }
                break;
            }
        }
        return;
    }

    // @TODO: add description
    @Override
    public double getSlope(double t, int j) {
        final int n = mT.length;

        if (t < mT[0]) {
            t = mT[0];
        } else if (t >= mT[n - 1]) {
            t = mT[n - 1];
        }
        for (int i = 0; i < n - 1; i++) {
            if (t <= mT[i + 1]) {
                double h = mT[i + 1] - mT[i];
                double y1 = mY[i][j];
                double y2 = mY[i + 1][j];
                return (y2 - y1) / h;
            }
        }
        return 0; // should never reach here
    }

    @Override
    public double[] getTimePoints() {
        return mT;
    }
}
