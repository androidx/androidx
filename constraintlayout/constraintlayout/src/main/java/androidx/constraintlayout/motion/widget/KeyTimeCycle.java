/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.constraintlayout.motion.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;

import androidx.constraintlayout.core.motion.utils.Oscillator;
import androidx.constraintlayout.motion.utils.ViewSpline;
import androidx.constraintlayout.motion.utils.ViewTimeCycle;
import androidx.constraintlayout.widget.ConstraintAttribute;
import androidx.constraintlayout.widget.R;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Defines container for a key frame of for storing KeyTimeCycles.
 * KeyTimeCycles change post layout values of a view.
 *
 *
 */
public class KeyTimeCycle extends Key {
    public static final String WAVE_PERIOD = "wavePeriod";
    public static final String WAVE_OFFSET = "waveOffset";
    public static final String WAVE_SHAPE = "waveShape";
    public static final int SHAPE_SIN_WAVE = Oscillator.SIN_WAVE;
    public static final int SHAPE_SQUARE_WAVE = Oscillator.SQUARE_WAVE;
    public static final int SHAPE_TRIANGLE_WAVE = Oscillator.TRIANGLE_WAVE;
    public static final int SHAPE_SAW_WAVE = Oscillator.SAW_WAVE;
    public static final int SHAPE_REVERSE_SAW_WAVE = Oscillator.REVERSE_SAW_WAVE;
    public static final int SHAPE_COS_WAVE = Oscillator.COS_WAVE;
    public static final int SHAPE_BOUNCE = Oscillator.BOUNCE;
    public static final int KEY_TYPE = 3;
    static final String NAME = "KeyTimeCycle";
    private static final String TAG = NAME;
    private String mTransitionEasing;
    private int mCurveFit = -1;
    private float mAlpha = Float.NaN;
    private float mElevation = Float.NaN;
    private float mRotation = Float.NaN;
    private float mRotationX = Float.NaN;
    private float mRotationY = Float.NaN;
    private float mTransitionPathRotate = Float.NaN;
    private float mScaleX = Float.NaN;
    private float mScaleY = Float.NaN;
    private float mTranslationX = Float.NaN;
    private float mTranslationY = Float.NaN;
    private float mTranslationZ = Float.NaN;
    private float mProgress = Float.NaN;
    private int mWaveShape = 0;
    private String mCustomWaveShape = null; // TODO add support of custom wave shapes
    private float mWavePeriod = Float.NaN;
    private float mWaveOffset = 0;

    {
        mType = KEY_TYPE;
        mCustomConstraints = new HashMap<>();
    }

    @Override
    public void load(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyTimeCycle);
        Loader.read(this, a);
    }

    /**
     * Gets the curve fit type this drives the interpolation
     */

    @Override
    public void getAttributeNames(HashSet<String> attributes) {
        if (!Float.isNaN(mAlpha)) {
            attributes.add(Key.ALPHA);
        }
        if (!Float.isNaN(mElevation)) {
            attributes.add(Key.ELEVATION);
        }
        if (!Float.isNaN(mRotation)) {
            attributes.add(Key.ROTATION);
        }
        if (!Float.isNaN(mRotationX)) {
            attributes.add(Key.ROTATION_X);
        }
        if (!Float.isNaN(mRotationY)) {
            attributes.add(Key.ROTATION_Y);
        }
        if (!Float.isNaN(mTranslationX)) {
            attributes.add(Key.TRANSLATION_X);
        }
        if (!Float.isNaN(mTranslationY)) {
            attributes.add(Key.TRANSLATION_Y);
        }
        if (!Float.isNaN(mTranslationZ)) {
            attributes.add(Key.TRANSLATION_Z);
        }
        if (!Float.isNaN(mTransitionPathRotate)) {
            attributes.add(Key.TRANSITION_PATH_ROTATE);
        }
        if (!Float.isNaN(mScaleX)) {
            attributes.add(Key.SCALE_X);
        }
        if (!Float.isNaN(mScaleY)) {
            attributes.add(Key.SCALE_Y);
        }
        if (!Float.isNaN(mProgress)) {
            attributes.add(Key.PROGRESS);
        }
        if (mCustomConstraints.size() > 0) {
            for (String s : mCustomConstraints.keySet()) {
                attributes.add(Key.CUSTOM + "," + s);
            }
        }
    }

    /**
     * put key and position into the interpolation map
     *
     * @param interpolation
     */
    @Override
    public void setInterpolation(HashMap<String, Integer> interpolation) {
        if (mCurveFit == -1) {
            return;
        }
        if (!Float.isNaN(mAlpha)) {
            interpolation.put(Key.ALPHA, mCurveFit);
        }
        if (!Float.isNaN(mElevation)) {
            interpolation.put(Key.ELEVATION, mCurveFit);
        }
        if (!Float.isNaN(mRotation)) {
            interpolation.put(Key.ROTATION, mCurveFit);
        }
        if (!Float.isNaN(mRotationX)) {
            interpolation.put(Key.ROTATION_X, mCurveFit);
        }
        if (!Float.isNaN(mRotationY)) {
            interpolation.put(Key.ROTATION_Y, mCurveFit);
        }
        if (!Float.isNaN(mTranslationX)) {
            interpolation.put(Key.TRANSLATION_X, mCurveFit);
        }
        if (!Float.isNaN(mTranslationY)) {
            interpolation.put(Key.TRANSLATION_Y, mCurveFit);
        }
        if (!Float.isNaN(mTranslationZ)) {
            interpolation.put(Key.TRANSLATION_Z, mCurveFit);
        }
        if (!Float.isNaN(mTransitionPathRotate)) {
            interpolation.put(Key.TRANSITION_PATH_ROTATE, mCurveFit);
        }
        if (!Float.isNaN(mScaleX)) {
            interpolation.put(Key.SCALE_X, mCurveFit);
        }
        if (!Float.isNaN(mScaleX)) {
            interpolation.put(Key.SCALE_Y, mCurveFit);
        }
        if (!Float.isNaN(mProgress)) {
            interpolation.put(Key.PROGRESS, mCurveFit);
        }
        if (mCustomConstraints.size() > 0) {
            for (String s : mCustomConstraints.keySet()) {
                interpolation.put(Key.CUSTOM + "," + s, mCurveFit);
            }
        }
    }

    @Override
    public void addValues(HashMap<String, ViewSpline> splines) {
        // This should not get called
        throw new IllegalArgumentException(" KeyTimeCycles do not support SplineSet");
    }

    /**
     * Add values to TimeCycle Map
     *
     * @param splines
     */
    public void addTimeValues(HashMap<String, ViewTimeCycle> splines) {
        for (String s : splines.keySet()) {
            ViewTimeCycle splineSet = splines.get(s);
            if (splineSet == null) {
                continue;
            }
            if (s.startsWith(Key.CUSTOM)) {
                String cKey = s.substring(Key.CUSTOM.length() + 1);
                ConstraintAttribute cValue = mCustomConstraints.get(cKey);
                if (cValue != null) {
                    ((ViewTimeCycle.CustomSet) splineSet).setPoint(mFramePosition, cValue,
                            mWavePeriod, mWaveShape, mWaveOffset);
                }
                continue;
            }
            switch (s) {
                case Key.ALPHA:
                    if (!Float.isNaN(mAlpha)) {
                        splineSet.setPoint(mFramePosition, mAlpha,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.ELEVATION:
                    if (!Float.isNaN(mElevation)) {
                        splineSet.setPoint(mFramePosition, mElevation,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.ROTATION:
                    if (!Float.isNaN(mRotation)) {
                        splineSet.setPoint(mFramePosition, mRotation,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.ROTATION_X:
                    if (!Float.isNaN(mRotationX)) {
                        splineSet.setPoint(mFramePosition, mRotationX,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.ROTATION_Y:
                    if (!Float.isNaN(mRotationY)) {
                        splineSet.setPoint(mFramePosition, mRotationY,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.TRANSITION_PATH_ROTATE:
                    if (!Float.isNaN(mTransitionPathRotate)) {
                        splineSet.setPoint(mFramePosition, mTransitionPathRotate,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.SCALE_X:
                    if (!Float.isNaN(mScaleX)) {
                        splineSet.setPoint(mFramePosition, mScaleX,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.SCALE_Y:
                    if (!Float.isNaN(mScaleY)) {
                        splineSet.setPoint(mFramePosition, mScaleY,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.TRANSLATION_X:
                    if (!Float.isNaN(mTranslationX)) {
                        splineSet.setPoint(mFramePosition, mTranslationX,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.TRANSLATION_Y:
                    if (!Float.isNaN(mTranslationY)) {
                        splineSet.setPoint(mFramePosition, mTranslationY,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.TRANSLATION_Z:
                    if (!Float.isNaN(mTranslationZ)) {
                        splineSet.setPoint(mFramePosition, mTranslationZ,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                case Key.PROGRESS:
                    if (!Float.isNaN(mProgress)) {
                        splineSet.setPoint(mFramePosition, mProgress,
                                mWavePeriod, mWaveShape, mWaveOffset);
                    }
                    break;
                default:
                    Log.e("KeyTimeCycles", "UNKNOWN addValues \"" + s + "\"");
            }
        }
    }

    @Override
    public void setValue(String tag, Object value) {
        switch (tag) {
            case Key.ALPHA:
                mAlpha = toFloat(value);
                break;
            case CURVEFIT:
                mCurveFit = toInt(value);
                break;
            case ELEVATION:
                mElevation = toFloat(value);
                break;
            case MOTIONPROGRESS:
                mProgress = toFloat(value);
                break;
            case ROTATION:
                mRotation = toFloat(value);
                break;
            case ROTATION_X:
                mRotationX = toFloat(value);
                break;
            case ROTATION_Y:
                mRotationY = toFloat(value);
                break;
            case SCALE_X:
                mScaleX = toFloat(value);
                break;
            case SCALE_Y:
                mScaleY = toFloat(value);
                break;
            case TRANSITIONEASING:
                mTransitionEasing = value.toString();
                break;
            case TRANSITION_PATH_ROTATE:
                mTransitionPathRotate = toFloat(value);
                break;
            case TRANSLATION_X:
                mTranslationX = toFloat(value);
                break;
            case TRANSLATION_Y:
                mTranslationY = toFloat(value);
                break;
            case TRANSLATION_Z:
                mTranslationZ = toFloat(value);
                break;
            case WAVE_PERIOD:
                mWavePeriod = toFloat(value);
                break;
            case WAVE_OFFSET:
                mWaveOffset = toFloat(value);
                break;
            case WAVE_SHAPE:
                if (value instanceof Integer) {
                    mWaveShape = toInt(value);
                } else {
                    mWaveShape = Oscillator.CUSTOM;
                    mCustomWaveShape = value.toString();
                }
                break;
        }

    }

    /**
     * Copy the key
     *
     * @param src to be copied
     * @return self
     */
    @Override
    public Key copy(Key src) {
        super.copy(src);
        KeyTimeCycle k = (KeyTimeCycle) src;
        mTransitionEasing = k.mTransitionEasing;
        mCurveFit = k.mCurveFit;
        mWaveShape = k.mWaveShape;
        mWavePeriod = k.mWavePeriod;
        mWaveOffset = k.mWaveOffset;
        mProgress = k.mProgress;
        mAlpha = k.mAlpha;
        mElevation = k.mElevation;
        mRotation = k.mRotation;
        mTransitionPathRotate = k.mTransitionPathRotate;
        mRotationX = k.mRotationX;
        mRotationY = k.mRotationY;
        mScaleX = k.mScaleX;
        mScaleY = k.mScaleY;
        mTranslationX = k.mTranslationX;
        mTranslationY = k.mTranslationY;
        mTranslationZ = k.mTranslationZ;
        mCustomWaveShape = k.mCustomWaveShape;
        return this;
    }

    /**
     * Clone this KeyAttributes
     *
     * @return
     */
    @Override
    public Key clone() {
        return new KeyTimeCycle().copy(this);
    }

    private static class Loader {
        private static final int ANDROID_ALPHA = 1;
        private static final int ANDROID_ELEVATION = 2;
        private static final int ANDROID_ROTATION = 4;
        private static final int ANDROID_ROTATION_X = 5;
        private static final int ANDROID_ROTATION_Y = 6;
        private static final int TRANSITION_PATH_ROTATE = 8;
        private static final int ANDROID_SCALE_X = 7;
        private static final int TRANSITION_EASING = 9;
        private static final int TARGET_ID = 10;
        private static final int FRAME_POSITION = 12;
        private static final int CURVE_FIT = 13;
        private static final int ANDROID_SCALE_Y = 14;
        private static final int ANDROID_TRANSLATION_X = 15;
        private static final int ANDROID_TRANSLATION_Y = 16;
        private static final int ANDROID_TRANSLATION_Z = 17;
        private static final int PROGRESS = 18;
        private static final int WAVE_SHAPE = 19;
        private static final int WAVE_PERIOD = 20;
        private static final int WAVE_OFFSET = 21;
        private static SparseIntArray sAttrMap = new SparseIntArray();

        static {
            sAttrMap.append(R.styleable.KeyTimeCycle_android_alpha, ANDROID_ALPHA);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_elevation, ANDROID_ELEVATION);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_rotation, ANDROID_ROTATION);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_rotationX, ANDROID_ROTATION_X);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_rotationY, ANDROID_ROTATION_Y);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_scaleX, ANDROID_SCALE_X);
            sAttrMap.append(R.styleable.KeyTimeCycle_transitionPathRotate, TRANSITION_PATH_ROTATE);
            sAttrMap.append(R.styleable.KeyTimeCycle_transitionEasing, TRANSITION_EASING);
            sAttrMap.append(R.styleable.KeyTimeCycle_motionTarget, TARGET_ID);
            sAttrMap.append(R.styleable.KeyTimeCycle_framePosition, FRAME_POSITION);
            sAttrMap.append(R.styleable.KeyTimeCycle_curveFit, CURVE_FIT);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_scaleY, ANDROID_SCALE_Y);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_translationX, ANDROID_TRANSLATION_X);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_translationY, ANDROID_TRANSLATION_Y);
            sAttrMap.append(R.styleable.KeyTimeCycle_android_translationZ, ANDROID_TRANSLATION_Z);
            sAttrMap.append(R.styleable.KeyTimeCycle_motionProgress, PROGRESS);
            sAttrMap.append(R.styleable.KeyTimeCycle_wavePeriod, WAVE_PERIOD);
            sAttrMap.append(R.styleable.KeyTimeCycle_waveOffset, WAVE_OFFSET);
            sAttrMap.append(R.styleable.KeyTimeCycle_waveShape, WAVE_SHAPE);
        }

        public static void read(KeyTimeCycle c, TypedArray a) {
            final int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                switch (sAttrMap.get(attr)) {
                    case TARGET_ID:
                        if (MotionLayout.IS_IN_EDIT_MODE) {
                            c.mTargetId = a.getResourceId(attr, c.mTargetId);
                            if (c.mTargetId == -1) {
                                c.mTargetString = a.getString(attr);
                            }
                        } else {
                            if (a.peekValue(attr).type == TypedValue.TYPE_STRING) {
                                c.mTargetString = a.getString(attr);
                            } else {
                                c.mTargetId = a.getResourceId(attr, c.mTargetId);
                            }
                        }
                        break;
                    case FRAME_POSITION:
                        c.mFramePosition = a.getInt(attr, c.mFramePosition);
                        break;
                    case ANDROID_ALPHA:
                        c.mAlpha = a.getFloat(attr, c.mAlpha);
                        break;
                    case ANDROID_ELEVATION:
                        c.mElevation = a.getDimension(attr, c.mElevation);
                        break;
                    case ANDROID_ROTATION:
                        c.mRotation = a.getFloat(attr, c.mRotation);
                        break;
                    case CURVE_FIT:
                        c.mCurveFit = a.getInteger(attr, c.mCurveFit);
                        break;
                    case WAVE_SHAPE:
                        if (a.peekValue(attr).type == TypedValue.TYPE_STRING) {
                            c.mCustomWaveShape = a.getString(attr);
                            c.mWaveShape = Oscillator.CUSTOM;
                        } else {
                            c.mWaveShape = a.getInt(attr, c.mWaveShape);
                        }
                        break;
                    case WAVE_PERIOD:
                        c.mWavePeriod = a.getFloat(attr, c.mWavePeriod);
                        break;
                    case WAVE_OFFSET:
                        TypedValue type = a.peekValue(attr);
                        if (type.type == TypedValue.TYPE_DIMENSION) {
                            c.mWaveOffset = a.getDimension(attr, c.mWaveOffset);
                        } else {
                            c.mWaveOffset = a.getFloat(attr, c.mWaveOffset);
                        }
                        break;
                    case ANDROID_SCALE_X:
                        c.mScaleX = a.getFloat(attr, c.mScaleX);
                        break;
                    case ANDROID_ROTATION_X:
                        c.mRotationX = a.getFloat(attr, c.mRotationX);
                        break;
                    case ANDROID_ROTATION_Y:
                        c.mRotationY = a.getFloat(attr, c.mRotationY);
                        break;
                    case TRANSITION_EASING:
                        c.mTransitionEasing = a.getString(attr);
                        break;
                    case ANDROID_SCALE_Y:
                        c.mScaleY = a.getFloat(attr, c.mScaleY);
                        break;
                    case TRANSITION_PATH_ROTATE:
                        c.mTransitionPathRotate = a.getFloat(attr, c.mTransitionPathRotate);
                        break;
                    case ANDROID_TRANSLATION_X:
                        c.mTranslationX = a.getDimension(attr, c.mTranslationX);
                        break;
                    case ANDROID_TRANSLATION_Y:
                        c.mTranslationY = a.getDimension(attr, c.mTranslationY);
                        break;
                    case ANDROID_TRANSLATION_Z:
                        c.mTranslationZ = a.getDimension(attr, c.mTranslationZ);
                        break;
                    case PROGRESS:
                        c.mProgress = a.getFloat(attr, c.mProgress);
                        break;
                    default:
                        Log.e(NAME, "unused attribute 0x" + Integer.toHexString(attr)
                                + "   " + sAttrMap.get(attr));
                        break;
                }
            }
        }
    }
}
