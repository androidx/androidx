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
 * Base class for curve fitting / interpolation
 * Curve fits must be capable of being differentiable and extend beyond the points (extrapolate)
 *
 *
 */

public abstract class CurveFit {
    public static final int SPLINE = 0;
    public static final int LINEAR = 1;
    public static final int CONSTANT = 2;

    // @TODO: add description
    public static CurveFit get(int type, double[] time, double[][] y) {
        if (time.length == 1) {
            type = CONSTANT;
        }
        switch (type) {
            case SPLINE:
                return new MonotonicCurveFit(time, y);
            case CONSTANT:
                return new Constant(time[0], y[0]);
            default:
                return new LinearCurveFit(time, y);
        }
    }

    // @TODO: add description
    public static CurveFit getArc(int[] arcModes, double[] time, double[][] y) {
        return new ArcCurveFit(arcModes, time, y);
    }

    // @TODO: add description
    public abstract void getPos(double t, double[] v);

    // @TODO: add description
    public abstract void getPos(double t, float[] v);

    // @TODO: add description
    public abstract double getPos(double t, int j);

    // @TODO: add description
    public abstract void getSlope(double t, double[] v);

    // @TODO: add description
    public abstract double getSlope(double t, int j);

    // @TODO: add description
    public abstract double[] getTimePoints();

    static class Constant extends CurveFit {
        double mTime;
        double[] mValue;

        Constant(double time, double[] value) {
            mTime = time;
            mValue = value;
        }

        @Override
        public void getPos(double t, double[] v) {
            System.arraycopy(mValue, 0, v, 0, mValue.length);
        }

        @Override
        public void getPos(double t, float[] v) {
            for (int i = 0; i < mValue.length; i++) {
                v[i] = (float) mValue[i];
            }
        }

        @Override
        public double getPos(double t, int j) {
            return mValue[j];
        }

        @Override
        public void getSlope(double t, double[] v) {
            for (int i = 0; i < mValue.length; i++) {
                v[i] = 0;
            }
        }

        @Override
        public double getSlope(double t, int j) {
            return 0;
        }

        @Override
        public double[] getTimePoints() {
            return new double[]{mTime};
        }
    }
}
