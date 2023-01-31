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
 * Provides spline interpolation code.
 * Currently not used but it is anticipated that we will be using it in the
 * KeyMotion
 *
 *
 */

public class HyperSpline {
    int mPoints;
    Cubic[][] mCurve;
    int mDimensionality;
    double[] mCurveLength;
    double mTotalLength;
    double[][] mCtl;

    /**
     * Spline in N dimensions
     *
     * @param points [mPoints][dimensionality]
     */
    public HyperSpline(double[][] points) {
        setup(points);
    }

    public HyperSpline() {
    }

    // @TODO: add description
    public void setup(double[][] points) {
        mDimensionality = points[0].length;
        mPoints = points.length;
        mCtl = new double[mDimensionality][mPoints];
        mCurve = new Cubic[mDimensionality][];
        for (int d = 0; d < mDimensionality; d++) {
            for (int p = 0; p < mPoints; p++) {
                mCtl[d][p] = points[p][d];
            }
        }

        for (int d = 0; d < mDimensionality; d++) {
            mCurve[d] = calcNaturalCubic(mCtl[d].length, mCtl[d]);
        }

        mCurveLength = new double[mPoints - 1];
        mTotalLength = 0;
        Cubic[] temp = new Cubic[mDimensionality];
        for (int p = 0; p < mCurveLength.length; p++) {
            for (int d = 0; d < mDimensionality; d++) {

                temp[d] = mCurve[d][p];

            }
            mTotalLength += mCurveLength[p] = approxLength(temp);
        }
    }

    // @TODO: add description
    public void getVelocity(double p, double[] v) {
        double pos = p * mTotalLength;
        int k = 0;
        for (; k < mCurveLength.length - 1 && mCurveLength[k] < pos; k++) {
            pos -= mCurveLength[k];
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = mCurve[i][k].vel(pos / mCurveLength[k]);
        }
    }

    // @TODO: add description
    public void getPos(double p, double[] x) {
        double pos = p * mTotalLength;
        int k = 0;
        for (; k < mCurveLength.length - 1 && mCurveLength[k] < pos; k++) {
            pos -= mCurveLength[k];
        }
        for (int i = 0; i < x.length; i++) {
            x[i] = mCurve[i][k].eval(pos / mCurveLength[k]);
        }
    }

    // @TODO: add description
    public void getPos(double p, float[] x) {
        double pos = p * mTotalLength;
        int k = 0;
        for (; k < mCurveLength.length - 1 && mCurveLength[k] < pos; k++) {
            pos -= mCurveLength[k];
        }
        for (int i = 0; i < x.length; i++) {
            x[i] = (float) mCurve[i][k].eval(pos / mCurveLength[k]);
        }
    }

    // @TODO: add description
    public double getPos(double p, int splineNumber) {
        double pos = p * mTotalLength;
        int k = 0;
        for (; k < mCurveLength.length - 1 && mCurveLength[k] < pos; k++) {
            pos -= mCurveLength[k];
        }
        return mCurve[splineNumber][k].eval(pos / mCurveLength[k]);
    }

    // @TODO: add description
    public double approxLength(Cubic[] curve) {
        double sum = 0;

        int n = curve.length;
        double[] old = new double[n];
        for (double i = 0; i < 1; i += .1) {
            double s = 0;
            for (int j = 0; j < n; j++) {
                double tmp = old[j];
                tmp -= old[j] = curve[j].eval(i);
                s += tmp * tmp;
            }
            if (i > 0) {
                sum += Math.sqrt(s);
            }

        }
        double s = 0;
        for (int j = 0; j < n; j++) {
            double tmp = old[j];
            tmp -= old[j] = curve[j].eval(1);
            s += tmp * tmp;
        }
        sum += Math.sqrt(s);
        return sum;
    }

    static Cubic[] calcNaturalCubic(int n, double[] x) {
        double[] gamma = new double[n];
        double[] delta = new double[n];
        double[] d = new double[n];
        n -= 1;

        gamma[0] = 1.0f / 2.0f;
        for (int i = 1; i < n; i++) {
            gamma[i] = 1 / (4 - gamma[i - 1]);
        }
        gamma[n] = 1 / (2 - gamma[n - 1]);

        delta[0] = 3 * (x[1] - x[0]) * gamma[0];
        for (int i = 1; i < n; i++) {
            delta[i] = (3 * (x[i + 1] - x[i - 1]) - delta[i - 1]) * gamma[i];
        }
        delta[n] = (3 * (x[n] - x[n - 1]) - delta[n - 1]) * gamma[n];

        d[n] = delta[n];
        for (int i = n - 1; i >= 0; i--) {
            d[i] = delta[i] - gamma[i] * d[i + 1];
        }

        Cubic[] c = new Cubic[n];
        for (int i = 0; i < n; i++) {
            c[i] = new Cubic((float) x[i], d[i], 3 * (x[i + 1] - x[i]) - 2
                    * d[i] - d[i + 1], 2 * (x[i] - x[i + 1]) + d[i] + d[i + 1]);
        }
        return c;
    }

    public static class Cubic {
        double mA, mB, mC, mD;

        public Cubic(double a, double b, double c, double d) {
            mA = a;
            mB = b;
            mC = c;
            mD = d;
        }

        // @TODO: add description
        public double eval(double u) {
            return (((mD * u) + mC) * u + mB) * u + mA;
        }

        // @TODO: add description
        public double vel(double v) {
            //  (((mD * u) + mC) * u + mB) * u + mA
            //  =  "mA + u*mB + u*u*mC+u*u*u*mD" a cubic expression
            // diff with respect to u = mB + u*mC/2+ u*u*mD/3
            // made efficient (mD*u/3+mC/2)*u+mB;

            return (mD * 3 * v + mC * 2) * v + mB;
        }
    }
}
