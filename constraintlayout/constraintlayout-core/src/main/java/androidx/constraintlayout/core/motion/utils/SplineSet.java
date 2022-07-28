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

import androidx.constraintlayout.core.motion.CustomAttribute;
import androidx.constraintlayout.core.motion.CustomVariable;
import androidx.constraintlayout.core.motion.MotionWidget;
import androidx.constraintlayout.core.state.WidgetFrame;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * This engine allows manipulation of attributes by Curves
 *
 *
 */

public abstract class SplineSet {
    private static final String TAG = "SplineSet";
    protected CurveFit mCurveFit;
    protected int[] mTimePoints = new int[10];
    protected float[] mValues = new float[10];
    private int mCount;
    private String mType;

    // @TODO: add description
    public void setProperty(TypedValues widget, float t) {
        widget.setValue(TypedValues.AttributesType.getId(mType), get(t));
    }

    @Override
    public String toString() {
        String str = mType;
        DecimalFormat df = new DecimalFormat("##.##");
        for (int i = 0; i < mCount; i++) {
            str += "[" + mTimePoints[i] + " , " + df.format(mValues[i]) + "] ";

        }
        return str;
    }

    public void setType(String type) {
        mType = type;
    }

    // @TODO: add description
    public float get(float t) {
        return (float) mCurveFit.getPos(t, 0);
    }

    // @TODO: add description
    public float getSlope(float t) {
        return (float) mCurveFit.getSlope(t, 0);
    }

    public CurveFit getCurveFit() {
        return mCurveFit;
    }

    // @TODO: add description
    public void setPoint(int position, float value) {
        if (mTimePoints.length < mCount + 1) {
            mTimePoints = Arrays.copyOf(mTimePoints, mTimePoints.length * 2);
            mValues = Arrays.copyOf(mValues, mValues.length * 2);
        }
        mTimePoints[mCount] = position;
        mValues[mCount] = value;
        mCount++;
    }

    // @TODO: add description
    public void setup(int curveType) {
        if (mCount == 0) {
            return;
        }

        Sort.doubleQuickSort(mTimePoints, mValues, 0, mCount - 1);

        int unique = 1;

        for (int i = 1; i < mCount; i++) {
            if (mTimePoints[i - 1] != mTimePoints[i]) {
                unique++;
            }
        }

        double[] time = new double[unique];
        double[][] values = new double[unique][1];
        int k = 0;
        for (int i = 0; i < mCount; i++) {
            if (i > 0 && mTimePoints[i] == mTimePoints[i - 1]) {
                continue;
            }

            time[k] = mTimePoints[i] * 1E-2;
            values[k][0] = mValues[i];
            k++;
        }
        mCurveFit = CurveFit.get(curveType, time, values);
    }

    // @TODO: add description
    public static SplineSet makeCustomSpline(String str, KeyFrameArray.CustomArray attrList) {
        return new CustomSet(str, attrList);
    }

    // @TODO: add description
    public static SplineSet makeCustomSplineSet(String str, KeyFrameArray.CustomVar attrList) {
        return new CustomSpline(str, attrList);
    }

    // @TODO: add description
    public static SplineSet makeSpline(String str, long currentTime) {

        return new CoreSpline(str, currentTime);
    }

    private static class Sort {

        static void doubleQuickSort(int[] key, float[] value, int low, int hi) {
            int[] stack = new int[key.length + 10];
            int count = 0;
            stack[count++] = hi;
            stack[count++] = low;
            while (count > 0) {
                low = stack[--count];
                hi = stack[--count];
                if (low < hi) {
                    int p = partition(key, value, low, hi);
                    stack[count++] = p - 1;
                    stack[count++] = low;
                    stack[count++] = hi;
                    stack[count++] = p + 1;
                }
            }
        }

        private static int partition(int[] array, float[] value, int low, int hi) {
            int pivot = array[hi];
            int i = low;
            for (int j = low; j < hi; j++) {
                if (array[j] <= pivot) {
                    swap(array, value, i, j);
                    i++;
                }
            }
            swap(array, value, i, hi);
            return i;
        }

        private static void swap(int[] array, float[] value, int a, int b) {
            int tmp = array[a];
            array[a] = array[b];
            array[b] = tmp;
            float tmpv = value[a];
            value[a] = value[b];
            value[b] = tmpv;
        }
    }


    public static class CustomSet extends SplineSet {
        String mAttributeName;
        KeyFrameArray.CustomArray mConstraintAttributeList;
        float[] mTempValues;

        @SuppressWarnings("StringSplitter")
        public CustomSet(String attribute, KeyFrameArray.CustomArray attrList) {
            mAttributeName = attribute.split(",")[1];
            mConstraintAttributeList = attrList;
        }

        // @TODO: add description
        @Override
        public void setup(int curveType) {
            int size = mConstraintAttributeList.size();
            int dimensionality = mConstraintAttributeList.valueAt(0).numberOfInterpolatedValues();
            double[] time = new double[size];
            mTempValues = new float[dimensionality];
            double[][] values = new double[size][dimensionality];
            for (int i = 0; i < size; i++) {

                int key = mConstraintAttributeList.keyAt(i);
                CustomAttribute ca = mConstraintAttributeList.valueAt(i);

                time[i] = key * 1E-2;
                ca.getValuesToInterpolate(mTempValues);
                for (int k = 0; k < mTempValues.length; k++) {
                    values[i][k] = mTempValues[k];
                }

            }
            mCurveFit = CurveFit.get(curveType, time, values);
        }

        // @TODO: add description
        @Override
        public void setPoint(int position, float value) {
            throw new RuntimeException("don't call for custom "
                    + "attribute call setPoint(pos, ConstraintAttribute)");
        }

        // @TODO: add description
        public void setPoint(int position, CustomAttribute value) {
            mConstraintAttributeList.append(position, value);
        }

        // @TODO: add description
        public void setProperty(WidgetFrame view, float t) {
            mCurveFit.getPos(t, mTempValues);
            view.setCustomValue(mConstraintAttributeList.valueAt(0), mTempValues);
        }
    }


    private static class CoreSpline extends SplineSet {
        String mType;
        @SuppressWarnings("unused") long mStart;

        CoreSpline(String str, long currentTime) {
            mType = str;
            mStart = currentTime;
        }

        @Override
        public void setProperty(TypedValues widget, float t) {
            int id = widget.getId(mType);
            widget.setValue(id, get(t));
        }
    }

    public static class CustomSpline extends SplineSet {
        String mAttributeName;
        KeyFrameArray.CustomVar mConstraintAttributeList;
        float[] mTempValues;

        @SuppressWarnings("StringSplitter")
        public CustomSpline(String attribute, KeyFrameArray.CustomVar attrList) {
            mAttributeName = attribute.split(",")[1];
            mConstraintAttributeList = attrList;
        }

        // @TODO: add description
        @Override
        public void setup(int curveType) {
            int size = mConstraintAttributeList.size();
            int dimensionality = mConstraintAttributeList.valueAt(0).numberOfInterpolatedValues();
            double[] time = new double[size];
            mTempValues = new float[dimensionality];
            double[][] values = new double[size][dimensionality];
            for (int i = 0; i < size; i++) {

                int key = mConstraintAttributeList.keyAt(i);
                CustomVariable ca = mConstraintAttributeList.valueAt(i);

                time[i] = key * 1E-2;
                ca.getValuesToInterpolate(mTempValues);
                for (int k = 0; k < mTempValues.length; k++) {
                    values[i][k] = mTempValues[k];
                }

            }
            mCurveFit = CurveFit.get(curveType, time, values);
        }

        // @TODO: add description
        @Override
        public void setPoint(int position, float value) {
            throw new RuntimeException("don't call for custom attribute"
                    + " call setPoint(pos, ConstraintAttribute)");
        }

        // @TODO: add description
        @Override
        public void setProperty(TypedValues widget, float t) {
            setProperty((MotionWidget) widget, t);
        }

        // @TODO: add description
        public void setPoint(int position, CustomVariable value) {
            mConstraintAttributeList.append(position, value);
        }

        // @TODO: add description
        public void setProperty(MotionWidget view, float t) {
            mCurveFit.getPos(t, mTempValues);
            mConstraintAttributeList.valueAt(0).setInterpolatedValue(view, mTempValues);
        }
    }

}
