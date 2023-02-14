/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.motion.key;

import androidx.constraintlayout.core.motion.CustomVariable;
import androidx.constraintlayout.core.motion.utils.KeyCycleOscillator;
import androidx.constraintlayout.core.motion.utils.Oscillator;
import androidx.constraintlayout.core.motion.utils.SplineSet;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.motion.utils.Utils;

import java.util.HashMap;
import java.util.HashSet;

public class MotionKeyCycle extends MotionKey {
    private static final String TAG = "KeyCycle";
    static final String NAME = "KeyCycle";
    public static final String WAVE_PERIOD = "wavePeriod";
    public static final String WAVE_OFFSET = "waveOffset";
    public static final String WAVE_PHASE = "wavePhase";
    public static final String WAVE_SHAPE = "waveShape";
    public static final int SHAPE_SIN_WAVE = Oscillator.SIN_WAVE;
    public static final int SHAPE_SQUARE_WAVE = Oscillator.SQUARE_WAVE;
    public static final int SHAPE_TRIANGLE_WAVE = Oscillator.TRIANGLE_WAVE;
    public static final int SHAPE_SAW_WAVE = Oscillator.SAW_WAVE;
    public static final int SHAPE_REVERSE_SAW_WAVE = Oscillator.REVERSE_SAW_WAVE;
    public static final int SHAPE_COS_WAVE = Oscillator.COS_WAVE;
    public static final int SHAPE_BOUNCE = Oscillator.BOUNCE;

    @SuppressWarnings("unused") private String mTransitionEasing = null;
    @SuppressWarnings("unused") private int mCurveFit = 0;
    private int mWaveShape = -1;
    private String mCustomWaveShape = null;
    private float mWavePeriod = Float.NaN;
    private float mWaveOffset = 0;
    private float mWavePhase = 0;
    private float mProgress = Float.NaN;
    private float mAlpha = Float.NaN;
    private float mElevation = Float.NaN;
    private float mRotation = Float.NaN;
    private float mTransitionPathRotate = Float.NaN;
    private float mRotationX = Float.NaN;
    private float mRotationY = Float.NaN;
    private float mScaleX = Float.NaN;
    private float mScaleY = Float.NaN;
    private float mTranslationX = Float.NaN;
    private float mTranslationY = Float.NaN;
    private float mTranslationZ = Float.NaN;
    public static final int KEY_TYPE = 4;

    {
        mType = KEY_TYPE;
        mCustom = new HashMap<>();
    }

    @Override
    public void getAttributeNames(HashSet<String> attributes) {
        if (!Float.isNaN(mAlpha)) {
            attributes.add(CycleType.S_ALPHA);
        }
        if (!Float.isNaN(mElevation)) {
            attributes.add(CycleType.S_ELEVATION);
        }
        if (!Float.isNaN(mRotation)) {
            attributes.add(CycleType.S_ROTATION_Z);
        }
        if (!Float.isNaN(mRotationX)) {
            attributes.add(CycleType.S_ROTATION_X);
        }
        if (!Float.isNaN(mRotationY)) {
            attributes.add(CycleType.S_ROTATION_Y);
        }
        if (!Float.isNaN(mScaleX)) {
            attributes.add(CycleType.S_SCALE_X);
        }
        if (!Float.isNaN(mScaleY)) {
            attributes.add(CycleType.S_SCALE_Y);
        }
        if (!Float.isNaN(mTransitionPathRotate)) {
            attributes.add(CycleType.S_PATH_ROTATE);
        }
        if (!Float.isNaN(mTranslationX)) {
            attributes.add(CycleType.S_TRANSLATION_X);
        }
        if (!Float.isNaN(mTranslationY)) {
            attributes.add(CycleType.S_TRANSLATION_Y);
        }
        if (!Float.isNaN(mTranslationZ)) {
            attributes.add(CycleType.S_TRANSLATION_Z);
        }
        if (mCustom.size() > 0) {
            for (String s : mCustom.keySet()) {
                attributes.add(TypedValues.S_CUSTOM + "," + s);
            }
        }
    }

    @Override
    public void addValues(HashMap<String, SplineSet> splines) {

    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, int value) {
        switch (type) {
            case CycleType.TYPE_CURVE_FIT:
                mCurveFit = value;
                return true;
            case CycleType.TYPE_WAVE_SHAPE:
                mWaveShape = value;
                return true;
            default:
                boolean ret = setValue(type, (float) value);
                if (ret) {
                    return true;
                }
                return super.setValue(type, value);
        }
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, String value) {
        switch (type) {
            case CycleType.TYPE_EASING:
                mTransitionEasing = value;
                return true;
            case CycleType.TYPE_CUSTOM_WAVE_SHAPE:
                mCustomWaveShape = value;
                return true;
            default:
                return super.setValue(type, value);
        }

    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, float value) {
        switch (type) {
            case CycleType.TYPE_ALPHA:
                mAlpha = value;
                break;
            case CycleType.TYPE_TRANSLATION_X:
                mTranslationX = value;
                break;
            case CycleType.TYPE_TRANSLATION_Y:
                mTranslationY = value;
                break;
            case CycleType.TYPE_TRANSLATION_Z:
                mTranslationZ = value;
                break;
            case CycleType.TYPE_ELEVATION:
                mElevation = value;
                break;
            case CycleType.TYPE_ROTATION_X:
                mRotationX = value;
                break;
            case CycleType.TYPE_ROTATION_Y:
                mRotationY = value;
                break;
            case CycleType.TYPE_ROTATION_Z:
                mRotation = value;
                break;
            case CycleType.TYPE_SCALE_X:
                mScaleX = value;
                break;
            case CycleType.TYPE_SCALE_Y:
                mScaleY = value;
                break;
            case CycleType.TYPE_PROGRESS:
                mProgress = value;
                break;
            case CycleType.TYPE_PATH_ROTATE:
                mTransitionPathRotate = value;
                break;
            case CycleType.TYPE_WAVE_PERIOD:
                mWavePeriod = value;
                break;
            case CycleType.TYPE_WAVE_OFFSET:
                mWaveOffset = value;
                break;
            case CycleType.TYPE_WAVE_PHASE:
                mWavePhase = value;
                break;
            default:
                return super.setValue(type, value);
        }
        return true;
    }

    // @TODO: add description
    public float getValue(String key) {
        switch (key) {
            case CycleType.S_ALPHA:
                return mAlpha;
            case CycleType.S_ELEVATION:
                return mElevation;
            case CycleType.S_ROTATION_Z:
                return mRotation;
            case CycleType.S_ROTATION_X:
                return mRotationX;
            case CycleType.S_ROTATION_Y:
                return mRotationY;
            case CycleType.S_PATH_ROTATE:
                return mTransitionPathRotate;
            case CycleType.S_SCALE_X:
                return mScaleX;
            case CycleType.S_SCALE_Y:
                return mScaleY;
            case CycleType.S_TRANSLATION_X:
                return mTranslationX;
            case CycleType.S_TRANSLATION_Y:
                return mTranslationY;
            case CycleType.S_TRANSLATION_Z:
                return mTranslationZ;
            case CycleType.S_WAVE_OFFSET:
                return mWaveOffset;
            case CycleType.S_WAVE_PHASE:
                return mWavePhase;
            case CycleType.S_PROGRESS:
                return mProgress;
            default:
                return Float.NaN;
        }
    }

    @Override
    public MotionKey clone() {
        return null;
    }

    @Override
    public int getId(String name) {
        switch (name) {
            case CycleType.S_CURVE_FIT:
                return CycleType.TYPE_CURVE_FIT;
            case CycleType.S_VISIBILITY:
                return CycleType.TYPE_VISIBILITY;
            case CycleType.S_ALPHA:
                return CycleType.TYPE_ALPHA;
            case CycleType.S_TRANSLATION_X:
                return CycleType.TYPE_TRANSLATION_X;
            case CycleType.S_TRANSLATION_Y:
                return CycleType.TYPE_TRANSLATION_Y;
            case CycleType.S_TRANSLATION_Z:
                return CycleType.TYPE_TRANSLATION_Z;
            case CycleType.S_ROTATION_X:
                return CycleType.TYPE_ROTATION_X;
            case CycleType.S_ROTATION_Y:
                return CycleType.TYPE_ROTATION_Y;
            case CycleType.S_ROTATION_Z:
                return CycleType.TYPE_ROTATION_Z;
            case CycleType.S_SCALE_X:
                return CycleType.TYPE_SCALE_X;
            case CycleType.S_SCALE_Y:
                return CycleType.TYPE_SCALE_Y;
            case CycleType.S_PIVOT_X:
                return CycleType.TYPE_PIVOT_X;
            case CycleType.S_PIVOT_Y:
                return CycleType.TYPE_PIVOT_Y;
            case CycleType.S_PROGRESS:
                return CycleType.TYPE_PROGRESS;
            case CycleType.S_PATH_ROTATE:
                return CycleType.TYPE_PATH_ROTATE;
            case CycleType.S_EASING:
                return CycleType.TYPE_EASING;
            case CycleType.S_WAVE_PERIOD:
                return CycleType.TYPE_WAVE_PERIOD;
            case CycleType.S_WAVE_SHAPE:
                return CycleType.TYPE_WAVE_SHAPE;
            case CycleType.S_WAVE_PHASE:
                return CycleType.TYPE_WAVE_PHASE;
            case CycleType.S_WAVE_OFFSET:
                return CycleType.TYPE_WAVE_OFFSET;
            case CycleType.S_CUSTOM_WAVE_SHAPE:
                return CycleType.TYPE_CUSTOM_WAVE_SHAPE;

        }
        return -1;
    }

    // @TODO: add description
    public void addCycleValues(HashMap<String, KeyCycleOscillator> oscSet) {

        for (String key : oscSet.keySet()) {
            if (key.startsWith(TypedValues.S_CUSTOM)) {
                String customKey = key.substring(TypedValues.S_CUSTOM.length() + 1);
                CustomVariable cValue = mCustom.get(customKey);
                if (cValue == null || cValue.getType() != Custom.TYPE_FLOAT) {
                    continue;
                }

                KeyCycleOscillator osc = oscSet.get(key);
                if (osc == null) {
                    continue;
                }

                osc.setPoint(mFramePosition, mWaveShape, mCustomWaveShape, -1, mWavePeriod,
                        mWaveOffset, mWavePhase / 360, cValue.getValueToInterpolate(), cValue);
                continue;
            }
            float value = getValue(key);
            if (Float.isNaN(value)) {
                continue;
            }

            KeyCycleOscillator osc = oscSet.get(key);
            if (osc == null) {
                continue;
            }

            osc.setPoint(mFramePosition, mWaveShape, mCustomWaveShape,
                    -1, mWavePeriod, mWaveOffset, mWavePhase / 360, value);
        }
    }


    // @TODO: add description
    public void dump() {
        System.out.println("MotionKeyCycle{"
                + "mWaveShape=" + mWaveShape
                + ", mWavePeriod=" + mWavePeriod
                + ", mWaveOffset=" + mWaveOffset
                + ", mWavePhase=" + mWavePhase
                + ", mRotation=" + mRotation
                + '}');
    }

    // @TODO: add description
    public void printAttributes() {
        HashSet<String> nameSet = new HashSet<>();
        getAttributeNames(nameSet);

        Utils.log(" ------------- " + mFramePosition + " -------------");
        Utils.log("MotionKeyCycle{"
                + "Shape=" + mWaveShape
                + ", Period=" + mWavePeriod
                + ", Offset=" + mWaveOffset
                + ", Phase=" + mWavePhase
                + '}');
        String[] names = nameSet.toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            Utils.log(names[i] + ":" + getValue(names[i]));
        }
    }


}
