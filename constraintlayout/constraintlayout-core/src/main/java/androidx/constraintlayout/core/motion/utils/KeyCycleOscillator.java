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

import androidx.constraintlayout.core.motion.MotionWidget;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Provide the engine for executing cycles.
 * KeyCycleOscillator
 *
 *
 */
public abstract class KeyCycleOscillator {
    private static final String TAG = "KeyCycleOscillator";
    private CurveFit mCurveFit;
    private CycleOscillator mCycleOscillator;
    private String mType;
    private int mWaveShape = 0;
    private String mWaveString = null;

    public int mVariesBy = 0; // 0 = position, 2=path
    ArrayList<WavePoint> mWavePoints = new ArrayList<>();

    // @TODO: add description
    public static KeyCycleOscillator makeWidgetCycle(String attribute) {
        if (attribute.equals(TypedValues.AttributesType.S_PATH_ROTATE)) {
            return new PathRotateSet(attribute);
        }
        return new CoreSpline(attribute);
    }

    private static class CoreSpline extends KeyCycleOscillator {
        String mType;
        int mTypeId;

        CoreSpline(String str) {
            mType = str;
            mTypeId = TypedValues.CycleType.getId(mType);
        }

        @Override
        public void setProperty(MotionWidget widget, float t) {
            widget.setValue(mTypeId, get(t));
        }
    }

    public static class PathRotateSet extends KeyCycleOscillator {
        String mType;
        int mTypeId;

        public PathRotateSet(String str) {
            mType = str;
            mTypeId = TypedValues.CycleType.getId(mType);
        }

        @Override
        public void setProperty(MotionWidget widget, float t) {
            widget.setValue(mTypeId, get(t));
        }

        // @TODO: add description
        public void setPathRotate(MotionWidget view, float t, double dx, double dy) {
            view.setRotationZ(get(t) + (float) Math.toDegrees(Math.atan2(dy, dx)));
        }
    }

    // @TODO: add description
    public boolean variesByPath() {
        return mVariesBy == 1;
    }

    static class WavePoint {
        int mPosition;
        float mValue;
        float mOffset;
        float mPeriod;
        float mPhase;

        WavePoint(int position, float period, float offset, float phase, float value) {
            mPosition = position;
            mValue = value;
            mOffset = offset;
            mPeriod = period;
            mPhase = phase;
        }
    }

    @Override
    public String toString() {
        String str = mType;
        DecimalFormat df = new DecimalFormat("##.##");
        for (WavePoint wp : mWavePoints) {
            str += "[" + wp.mPosition + " , " + df.format(wp.mValue) + "] ";
        }
        return str;
    }

    public void setType(String type) {
        mType = type;
    }

    // @TODO: add description
    public float get(float t) {
        return (float) mCycleOscillator.getValues(t);
    }

    // @TODO: add description
    public float getSlope(float position) {
        return (float) mCycleOscillator.getSlope(position);
    }

    public CurveFit getCurveFit() {
        return mCurveFit;
    }

    protected void setCustom(Object custom) {

    }

    /**
     * sets a oscillator wave point
     *
     * @param framePosition the position
     * @param variesBy      only varies by path supported for now
     * @param period        the period of the wave
     * @param offset        the offset value
     * @param value         the adder
     * @param custom        The ConstraintAttribute used to set the value
     */
    public void setPoint(int framePosition,
            int shape,
            String waveString,
            int variesBy,
            float period,
            float offset,
            float phase,
            float value,
            Object custom) {
        mWavePoints.add(new WavePoint(framePosition, period, offset, phase, value));
        if (variesBy != -1) {
            mVariesBy = variesBy;
        }
        mWaveShape = shape;
        setCustom(custom);
        mWaveString = waveString;
    }

    /**
     * sets a oscillator wave point
     *
     * @param framePosition the position
     * @param variesBy      only varies by path supported for now
     * @param period        the period of the wave
     * @param offset        the offset value
     * @param value         the adder
     */
    public void setPoint(int framePosition,
            int shape,
            String waveString,
            int variesBy,
            float period,
            float offset,
            float phase,
            float value) {
        mWavePoints.add(new WavePoint(framePosition, period, offset, phase, value));
        if (variesBy != -1) {
            mVariesBy = variesBy;
        }
        mWaveShape = shape;
        mWaveString = waveString;
    }

    // @TODO: add description
    public void setup(float pathLength) {
        int count = mWavePoints.size();
        if (count == 0) {
            return;
        }
        Collections.sort(mWavePoints, new Comparator<WavePoint>() {
            @Override
            public int compare(WavePoint lhs, WavePoint rhs) {
                return Integer.compare(lhs.mPosition, rhs.mPosition);
            }
        });
        double[] time = new double[count];
        double[][] values = new double[count][3];
        mCycleOscillator = new CycleOscillator(mWaveShape, mWaveString, mVariesBy, count);
        int i = 0;
        for (WavePoint wp : mWavePoints) {
            time[i] = wp.mPeriod * 1E-2;
            values[i][0] = wp.mValue;
            values[i][1] = wp.mOffset;
            values[i][2] = wp.mPhase;
            mCycleOscillator.setPoint(i, wp.mPosition, wp.mPeriod,
                    wp.mOffset, wp.mPhase, wp.mValue);
            i++;
        }
        mCycleOscillator.setup(pathLength);
        mCurveFit = CurveFit.get(CurveFit.SPLINE, time, values);
    }

    static class CycleOscillator {
        static final int UNSET = -1; // -1 is typically used through out android to the UNSET value
        private static final String TAG = "CycleOscillator";
        @SuppressWarnings("unused") private final int mVariesBy;
        Oscillator mOscillator = new Oscillator();
        private final int mOffst = 0;
        private final int mPhase = 1;
        private final int mValue = 2;

        float[] mValues;
        double[] mPosition;
        float[] mPeriod;
        float[] mOffsetArr; // offsets will be spline interpolated
        float[] mPhaseArr; // phase will be spline interpolated
        float[] mScale; // scales will be spline interpolated
        int mWaveShape;
        CurveFit mCurveFit;
        double[] mSplineValueCache; // for the return value of the curve fit
        double[] mSplineSlopeCache; // for the return value of the curve fit
        float mPathLength;

        CycleOscillator(int waveShape, String customShape, int variesBy, int steps) {
            mWaveShape = waveShape;
            mVariesBy = variesBy;
            mOscillator.setType(waveShape, customShape);
            mValues = new float[steps];
            mPosition = new double[steps];
            mPeriod = new float[steps];
            mOffsetArr = new float[steps];
            mPhaseArr = new float[steps];
            mScale = new float[steps];
        }

        public double getValues(float time) {
            if (mCurveFit != null) {
                mCurveFit.getPos(time, mSplineValueCache);
            } else { // only one value no need to interpolate
                mSplineValueCache[mOffst] = mOffsetArr[0];
                mSplineValueCache[mPhase] = mPhaseArr[0];
                mSplineValueCache[mValue] = mValues[0];

            }
            double offset = mSplineValueCache[mOffst];
            double phase = mSplineValueCache[this.mPhase];
            double waveValue = mOscillator.getValue(time, phase);
            return offset + waveValue * mSplineValueCache[mValue];
        }

        public double getLastPhase() {
            return mSplineValueCache[1];
        }

        public double getSlope(float time) {
            if (mCurveFit != null) {
                mCurveFit.getSlope(time, mSplineSlopeCache);
                mCurveFit.getPos(time, mSplineValueCache);
            } else { // only one value no need to interpolate
                mSplineSlopeCache[mOffst] = 0;
                mSplineSlopeCache[mPhase] = 0;
                mSplineSlopeCache[mValue] = 0;
            }
            double waveValue = mOscillator.getValue(time, mSplineValueCache[mPhase]);
            double waveSlope = mOscillator.getSlope(time,
                    mSplineValueCache[mPhase], mSplineSlopeCache[mPhase]);
            return mSplineSlopeCache[mOffst] + waveValue * mSplineSlopeCache[mValue]
                    + waveSlope * mSplineValueCache[mValue];
        }

        /**
         *
         */
        public void setPoint(int index,
                int framePosition,
                float wavePeriod,
                float offset,
                float phase,
                float values) {
            mPosition[index] = framePosition / 100.0;
            mPeriod[index] = wavePeriod;
            mOffsetArr[index] = offset;
            mPhaseArr[index] = phase;
            mValues[index] = values;
        }

        public void setup(float pathLength) {
            mPathLength = pathLength;
            double[][] splineValues = new double[mPosition.length][3];
            mSplineValueCache = new double[2 + mValues.length];
            mSplineSlopeCache = new double[2 + mValues.length];
            if (mPosition[0] > 0) {
                mOscillator.addPoint(0, mPeriod[0]);
            }
            int last = mPosition.length - 1;
            if (mPosition[last] < 1.0f) {
                mOscillator.addPoint(1, mPeriod[last]);
            }

            for (int i = 0; i < splineValues.length; i++) {
                splineValues[i][mOffst] = mOffsetArr[i];
                splineValues[i][mPhase] = mPhaseArr[i];
                splineValues[i][mValue] = mValues[i];
                mOscillator.addPoint(mPosition[i], mPeriod[i]);
            }

            // TODO: add mVariesBy and get total time and path length
            mOscillator.normalize();
            if (mPosition.length > 1) {
                mCurveFit = CurveFit.get(CurveFit.SPLINE, mPosition, splineValues);
            } else {
                mCurveFit = null;
            }
        }
    }

    // @TODO: add description
    public void setProperty(MotionWidget widget, float t) {

    }

}
