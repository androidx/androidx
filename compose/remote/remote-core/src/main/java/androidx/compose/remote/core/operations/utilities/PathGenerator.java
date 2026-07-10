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

/**
 * This is designed to algorithmically generate a path from a set of points or expressions that
 * describe the points
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PathGenerator {
    private Linear mLinear;
    private Monotonic mMonotonic;
    private Spline mSpline;
    private final AnimatedFloatExpression mExpression = new AnimatedFloatExpression();
    public static final int SPLINE = 0;
    public static final int MONOTONIC = 2;
    public static final int LINEAR = 4;
    float[] mXData = new float[0];
    float[] mYData = new float[0];

    /**
     * Build a Path2D from the given points. If loop==true, the last point connects back to the
     * first.
     */
    private static class Path {
        float[] mPath = new float[10];
        int mSize = 0;
        int mMaxSize = 10;
        float mCx = 0;
        float mCy = 0;
        public static final int MOVE = 10;
        public static final int CUBIC = 14;
        public static final int CLOSE = 15;
        public static final float MOVE_NAN = Utils.asNan(MOVE);
        public static final float CUBIC_NAN = Utils.asNan(CUBIC);
        public static final float CLOSE_NAN = Utils.asNan(CLOSE);

        Path(int bufferSize) {
            mPath = new float[mMaxSize = bufferSize];
        }

        public int copyPoints(float[] dest) {
            if (dest == null) {
                throw new IllegalArgumentException("points null");
            }
            int n = dest.length;
            if (n < mSize) {
                throw new IllegalArgumentException("points too small " + n + " < " + mSize);
            }
            System.arraycopy(mPath, 0, dest, 0, mSize);
            return mSize;
        }

        public void moveTo(float x, float y) {
            mPath[mSize++] = MOVE_NAN;
            mPath[mSize++] = x;
            mPath[mSize++] = y;
            mCx = x;
            mCy = y;
        }

        public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            mPath[mSize++] = CUBIC_NAN;
            mPath[mSize++] = mCx;
            mPath[mSize++] = mCy;
            mPath[mSize++] = x1;
            mPath[mSize++] = y1;
            mPath[mSize++] = x2;
            mPath[mSize++] = y2;
            mPath[mSize++] = x3;
            mPath[mSize++] = y3;
            mCx = x3;
            mCy = y3;
        }

        public void closePath() {
            mPath[mSize++] = CLOSE_NAN;
        }

        public void reset() {
            mSize = 0;
        }
    }

    /**
     * Calculate the length of the path that will be returned
     *
     * @param len the number of point in the input
     * @param loop The input is looped
     * @return the length of the path
     */
    public int getReturnLength(int len, boolean loop) {
        int ret = 3; // move to
        ret += loop ? len * 9 + 1 : (len - 1) * 9;
        return ret;
    }

    /**
     * Build a path from the given points. If loop==true, the last point connects back to the first.
     *
     * @param dest the destination buffer
     * @param x the x coordinates
     * @param y the y coordinates
     * @param mode 0=spline, 1=monotonic, 2=linear ,
     * @param loop if true, the last point connects back to the first
     * @return the number of float in the path
     */
    public int getPath(
            float @NonNull [] dest,
            float @NonNull [] x,
            float @NonNull [] y,
            int mode,
            boolean loop) {
        switch (mode) {
            case LINEAR:
            case MONOTONIC:
                if (mMonotonic == null) {
                    mMonotonic = new Monotonic();
                }
                return mMonotonic.asPath(x, y, loop).copyPoints(dest);
            default:
                if (mSpline == null) {
                    mSpline = new Spline();
                }

                return mSpline.asPath(x, y, loop).copyPoints(dest);
        }
    }

    /**
     * Build a path from Float expressions
     *
     * @param dest the destination buffer
     * @param expressionX the x expression
     * @param expressionY the y expression
     * @param min the min value of the expression
     * @param max the max value of the expression
     * @param count the number of points in the expression
     * @param mode 0=spline, 1=monotonic, 2=linear ,
     * @param loop if true, the last point connects back to the first
     * @param ca the collection access object
     * @return the number of float in the path
     */
    public int getPath(
            float @NonNull [] dest,
            float @NonNull [] expressionX,
            float @NonNull [] expressionY,
            float min,
            float max,
            int count,
            int mode,
            boolean loop,
            @Nullable CollectionsAccess ca) {
        if (mXData.length != count) {
            mXData = new float[count];
            mYData = new float[count];
        }
        float gap = max - min;
        float step = loop ? (gap / (float) count) : (gap / (float) (count - 1));
        if (ca == null) {
            for (int i = 0; i < mXData.length; i++) {
                float val = min + i * step;
                mXData[i] = mExpression.eval(expressionX, expressionX.length, val);
                mYData[i] = mExpression.eval(expressionY, expressionY.length, val);
            }
        } else {
            for (int i = 0; i < mXData.length; i++) {
                float val = min + i * step;
                mXData[i] = mExpression.eval(ca, expressionX, expressionX.length, val);
                mYData[i] = mExpression.eval(ca, expressionY, expressionY.length, val);
            }
        }

        switch (mode) {
            case LINEAR:
                if (mLinear == null) {
                    mLinear = new Linear();
                }
                return mLinear.asPath(mXData, mYData, loop).copyPoints(dest);
            case MONOTONIC:
                if (mMonotonic == null) {
                    mMonotonic = new Monotonic();
                }
                return mMonotonic.asPath(mXData, mYData, loop).copyPoints(dest);

            default:
                if (mSpline == null) {
                    mSpline = new Spline();
                }
                int len = mSpline.asPath(mXData, mYData, loop).copyPoints(dest);
                return len;
        }
    }

    /**
     * generate a polar path
     *
     * @param dest the destination buffer to be filled with the path
     * @param expressionRad the expression for the radius
     * @param coord the center of the path
     * @param start the start angle
     * @param end the end angle
     * @param count the number of points in the path
     * @param mode 0=spline, 1=monotonic, 2=linear ,
     * @param loop if true, the last point connects back to the first
     * @param ca the collection access object
     * @return the number of float in the path
     */
    public int getPolarPath(
            float @NonNull [] dest,
            float @NonNull [] expressionRad,
            float @NonNull [] coord,
            float start,
            float end,
            int count,
            int mode,
            boolean loop,
            @Nullable CollectionsAccess ca) {
        if (mXData.length != count) {
            mXData = new float[count];
            mYData = new float[count];
        }
        float gap = end - start;
        float step = loop ? (gap / (float) count) : (gap / (float) (count - 1));
        if (ca == null) {
            for (int i = 0; i < mXData.length; i++) {
                float val = start + i * step;
                float r = mExpression.eval(expressionRad, expressionRad.length, val);
                mXData[i] = coord[0] + r * (float) Math.cos(val);
                mYData[i] = coord[1] + r * (float) Math.sin(val);
            }
        } else {
            for (int i = 0; i < mXData.length; i++) {
                float val = start + i * step;
                float r = mExpression.eval(ca, expressionRad, expressionRad.length, val);
                mXData[i] = coord[0] + r * (float) Math.cos(val);
                mYData[i] = coord[1] + r * (float) Math.sin(val);
            }
        }

        switch (mode) {
            case LINEAR:
                if (mLinear == null) {
                    mLinear = new Linear();
                }
                return mLinear.asPath(mXData, mYData, loop).copyPoints(dest);
            case MONOTONIC:
                if (mMonotonic == null) {
                    mMonotonic = new Monotonic();
                }
                return mMonotonic.asPath(mXData, mYData, loop).copyPoints(dest);

            default:
                if (mSpline == null) {
                    mSpline = new Spline();
                }
                int len = mSpline.asPath(mXData, mYData, loop).copyPoints(dest);
                return len;
        }
    }

    /** Monotonic spline path generator */
    private static class Monotonic {
        float[] mH = new float[0];
        float[] mDxSeg = new float[0];
        float[] mDySeg = new float[0];
        float[] mDxTan = new float[0];
        float[] mDyTan = new float[0];
        Path mPath = new Path(233);

        public Path asPath(float[] x, float[] y, boolean loop) {
            if (x == null || y == null) {
                throw new IllegalArgumentException("x/y null");
            }
            if (x.length != y.length) {
                throw new IllegalArgumentException("x/y length mismatch");
            }
            int n = x.length;

            final int segs = loop ? n : n - 1;

            // Segment lengths (h) and normalized slopes (delta) per coordinate
            if (segs != mH.length) {
                mPath = new Path(segs * 10);
                mH = new float[segs];
                mDxSeg = new float[segs];
                mDySeg = new float[segs];
                final int tans = loop ? segs : segs + 1;
                mDxTan = new float[tans];
                mDyTan = new float[tans];
            }
            mPath.reset();
            if (n == 0) return mPath;
            mPath.moveTo(x[0], y[0]);
            if (n == 1) return mPath;

            for (int i0 = 0; i0 < segs; i0++) {
                int i1 = (i0 + 1) % n;
                float sx = x[i1] - x[i0];
                float sy = y[i1] - y[i0];
                float dist = (float) Math.hypot(sx, sy);
                if (dist == 0) dist = 1e-12f; // avoid divide by zero
                mH[i0] = dist;
                mDxSeg[i0] = sx / dist; // slope wrt param t
                mDySeg[i0] = sy / dist;
            }

            monotoneTangents(mDxTan, mDxSeg, mH, loop); // tangents for x wrt t
            monotoneTangents(mDyTan, mDySeg, mH, loop); // tangents for y wrt t

            // Convert Hermite (Pi, Pi+1, Di, Di+1) to cubic Bézier and feed into Path2D
            for (int i0 = 0; i0 < segs; i0++) {

                int i1 = (i0 + 1) % n;
                double hi = mH[i0];

                double c1x = x[i0] + mDxTan[i0] * hi / 3.0;
                double c1y = y[i0] + mDyTan[i0] * hi / 3.0;
                double c2x = x[i1] - mDxTan[i1] * hi / 3.0;
                double c2y = y[i1] - mDyTan[i1] * hi / 3.0;

                mPath.cubicTo((float) c1x, (float) c1y, (float) c2x, (float) c2y, x[i1], y[i1]);
            }

            if (loop) {
                mPath.closePath();
            }
            return mPath;
        }

        /**
         * Compute monotone (Fritsch–Carlson) tangents for one coordinate. delta[i] = slope on
         * segment i, h[i] = segment length. Returns D[i] per point.
         */
        private void monotoneTangents(float[] d, float[] delta, float[] h, boolean loop) {
            final int segs = delta.length;
            final int n = loop ? segs : segs + 1;

            // 1) Initial (unfiltered) guesses
            for (int i = 0; i < n; i++) {
                int prev = (i - 1 + segs) % segs;
                int next = i % segs;

                if (!loop && i == 0) {
                    d[i] = delta[0];
                } else if (!loop && i == n - 1) {
                    d[i] = delta[segs - 1];
                } else {
                    float dp = delta[prev];
                    float dn = delta[next];
                    if (dp == 0.0 || dn == 0.0 || Math.signum(dp) != Math.signum(dn)) {
                        d[i] = 0.0f;
                    } else {
                        float w1 = 2 * h[next] + h[prev];
                        float w2 = h[next] + 2 * h[prev];
                        d[i] = (w1 + w2) / (w1 / dp + w2 / dn);
                    }
                }
            }

            // 2) Fritsch–Carlson "Hyman filter" to prevent overshoot
            for (int i = 0; i < segs; i++) {
                if (delta[i] == 0.0) {
                    d[i] = 0.0f;
                    d[(i + 1) % n] = 0.0f;
                } else {
                    float a = d[i] / delta[i];
                    float b = d[(i + 1) % n] / delta[i];
                    float s = a * a + b * b;
                    if (s > 9.0) {
                        float t = 3.0f / (float) Math.sqrt(s);
                        d[i] = t * a * delta[i];
                        d[(i + 1) % n] = t * b * delta[i];
                    }
                }
            }
        }
    }

    /** Spline path generator */
    private static class Spline {
        float[] mH = new float[0]; // chord lengths
        float[] mDxSeg = new float[0]; // per-seg slopes wrt param t
        float[] mDySeg = new float[0];
        float[] mDxTan = new float[0];
        float[] mDyTan = new float[0];
        Path mPath = new Path(2);

        /** Build a cubic-spline Path2D. If loop==true, last point connects to first. */
        public Path asPath(float[] x, float[] y, boolean loop) {
            if (x == null || y == null) throw new IllegalArgumentException("x/y null");
            if (x.length != y.length) throw new IllegalArgumentException("x/y length mismatch");
            int n = x.length;
            mPath.reset();
            final int segs = loop ? n : n - 1;
            if (segs != mH.length) {
                mPath = new Path(x.length * 10);
                mH = new float[segs]; // chord lengths
                mDxSeg = new float[segs]; // per-seg slopes wrt param t
                mDySeg = new float[segs];
                final int tans = loop ? segs : segs + 1;
                mDxTan = new float[tans];
                mDyTan = new float[tans];
            }
            if (n == 0) return mPath;
            mPath.moveTo(x[0], y[0]);
            if (n == 1) return mPath;

            for (int i0 = 0; i0 < segs; i0++) {

                int i1 = (i0 + 1) % n;
                float sx = x[i1] - x[i0];
                float sy = y[i1] - y[i0];
                float dist = (float) Math.hypot(sx, sy);
                if (dist == 0) dist = 1e-12f;
                mH[i0] = dist;
                mDxSeg[i0] = sx / dist;
                mDySeg[i0] = sy / dist;
            }

            smoothTangents(mDxTan, mDxSeg, mH, loop);
            smoothTangents(mDyTan, mDySeg, mH, loop);

            for (int i0 = 0; i0 < segs; i0++) {
                int i1 = (i0 + 1) % n;
                float hi = mH[i0];

                float c1x = x[i0] + mDxTan[i0] * hi / 3.0f;
                float c1y = y[i0] + mDyTan[i0] * hi / 3.0f;
                float c2x = x[i1] - mDxTan[i1] * hi / 3.0f;
                float c2y = y[i1] - mDyTan[i1] * hi / 3.0f;

                mPath.cubicTo((float) c1x, (float) c1y, (float) c2x, (float) c2y, x[i1], y[i1]);
            }

            if (loop) mPath.closePath();
            return mPath;
        }

        /** Simple C1 spline tangents: weighted average of adjacent segment slopes. */
        private void smoothTangents(float[] d, float[] delta, float[] h, boolean loop) {
            final int segs = delta.length;
            final int n = loop ? segs : segs + 1;

            if (loop) {
                for (int i = 0; i < n; i++) {
                    int im1 = (i - 1 + segs) % segs;
                    int ip0 = i % segs;
                    // weighted average by segment lengths
                    d[i] = (h[im1] * delta[ip0] + h[ip0] * delta[im1]) / (h[im1] + h[ip0]);
                }
            } else {
                d[0] = delta[0];
                d[n - 1] = delta[segs - 1];
                for (int i = 1; i < n - 1; i++) {
                    float hm1 = h[i - 1];
                    float hi = h[i];
                    d[i] = (hm1 * delta[i] + hi * delta[i - 1]) / (hm1 + hi);
                }
            }
        }
    }

    /** Linear path generator */
    private static class Linear {
        Path mPath = new Path(2);

        /** Build a cubic-spline Path2D. If loop==true, last point connects to first. */
        public Path asPath(float[] x, float[] y, boolean loop) {
            if (x == null || y == null) throw new IllegalArgumentException("x/y null");
            if (x.length != y.length) throw new IllegalArgumentException("x/y length mismatch");
            int n = x.length;
            mPath.reset();
            final int segs = loop ? n : n - 1;
            if (x.length * 10 != mPath.mMaxSize) {
                mPath = new Path(x.length * 10);
            }
            if (n == 0) return mPath;
            mPath.moveTo(x[0], y[0]);
            if (n == 1) return mPath;

            for (int i0 = 0; i0 < segs; i0++) {
                int i1 = (i0 + 1) % n;

                float c1x = x[i0];
                float c1y = y[i0];
                float c2x = x[i1];
                float c2y = y[i1];

                mPath.cubicTo(c1x, c1y, c2x, c2y, x[i1], y[i1]);
            }

            if (loop) mPath.closePath();
            return mPath;
        }
    }
}
