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
import androidx.constraintlayout.core.motion.utils.SplineSet;
import androidx.constraintlayout.core.motion.utils.TypedValues;

import java.util.HashMap;
import java.util.HashSet;

public class MotionKeyAttributes extends MotionKey {
    static final String NAME = "KeyAttribute";
    private static final String TAG = "KeyAttributes";
    @SuppressWarnings("unused") private static final boolean DEBUG = false;
    @SuppressWarnings("unused") private String mTransitionEasing;
    private int mCurveFit = -1;
    @SuppressWarnings("unused") private int mVisibility = 0;
    private float mAlpha = Float.NaN;
    private float mElevation = Float.NaN;
    private float mRotation = Float.NaN;
    private float mRotationX = Float.NaN;
    private float mRotationY = Float.NaN;
    private float mPivotX = Float.NaN;
    private float mPivotY = Float.NaN;
    private float mTransitionPathRotate = Float.NaN;
    private float mScaleX = Float.NaN;
    private float mScaleY = Float.NaN;
    private float mTranslationX = Float.NaN;
    private float mTranslationY = Float.NaN;
    private float mTranslationZ = Float.NaN;
    private float mProgress = Float.NaN;

    public static final int KEY_TYPE = 1;

    {
        mType = KEY_TYPE;
        mCustom = new HashMap<>();
    }

    @Override
    public void getAttributeNames(HashSet<String> attributes) {

        if (!Float.isNaN(mAlpha)) {
            attributes.add(AttributesType.S_ALPHA);
        }
        if (!Float.isNaN(mElevation)) {
            attributes.add(AttributesType.S_ELEVATION);
        }
        if (!Float.isNaN(mRotation)) {
            attributes.add(AttributesType.S_ROTATION_Z);
        }
        if (!Float.isNaN(mRotationX)) {
            attributes.add(AttributesType.S_ROTATION_X);
        }
        if (!Float.isNaN(mRotationY)) {
            attributes.add(AttributesType.S_ROTATION_Y);
        }
        if (!Float.isNaN(mPivotX)) {
            attributes.add(AttributesType.S_PIVOT_X);
        }
        if (!Float.isNaN(mPivotY)) {
            attributes.add(AttributesType.S_PIVOT_Y);
        }
        if (!Float.isNaN(mTranslationX)) {
            attributes.add(AttributesType.S_TRANSLATION_X);
        }
        if (!Float.isNaN(mTranslationY)) {
            attributes.add(AttributesType.S_TRANSLATION_Y);
        }
        if (!Float.isNaN(mTranslationZ)) {
            attributes.add(AttributesType.S_TRANSLATION_Z);
        }
        if (!Float.isNaN(mTransitionPathRotate)) {
            attributes.add(AttributesType.S_PATH_ROTATE);
        }
        if (!Float.isNaN(mScaleX)) {
            attributes.add(AttributesType.S_SCALE_X);
        }
        if (!Float.isNaN(mScaleY)) {
            attributes.add(AttributesType.S_SCALE_Y);
        }
        if (!Float.isNaN(mProgress)) {
            attributes.add(AttributesType.S_PROGRESS);
        }
        if (mCustom.size() > 0) {
            for (String s : mCustom.keySet()) {
                attributes.add(TypedValues.S_CUSTOM + "," + s);
            }
        }
    }

    @Override
    public void addValues(HashMap<String, SplineSet> splines) {
        for (String s : splines.keySet()) {
            SplineSet splineSet = splines.get(s);
            if (splineSet == null) {
                continue;
            }
            // TODO support custom
            if (s.startsWith(AttributesType.S_CUSTOM)) {
                String cKey = s.substring(AttributesType.S_CUSTOM.length() + 1);
                CustomVariable cValue = mCustom.get(cKey);
                if (cValue != null) {
                    ((SplineSet.CustomSpline) splineSet).setPoint(mFramePosition, cValue);
                }
                continue;
            }
            switch (s) {
                case AttributesType.S_ALPHA:
                    if (!Float.isNaN(mAlpha)) {
                        splineSet.setPoint(mFramePosition, mAlpha);
                    }
                    break;
                case AttributesType.S_ELEVATION:
                    if (!Float.isNaN(mElevation)) {
                        splineSet.setPoint(mFramePosition, mElevation);
                    }
                    break;
                case AttributesType.S_ROTATION_Z:
                    if (!Float.isNaN(mRotation)) {
                        splineSet.setPoint(mFramePosition, mRotation);
                    }
                    break;
                case AttributesType.S_ROTATION_X:
                    if (!Float.isNaN(mRotationX)) {
                        splineSet.setPoint(mFramePosition, mRotationX);
                    }
                    break;
                case AttributesType.S_ROTATION_Y:
                    if (!Float.isNaN(mRotationY)) {
                        splineSet.setPoint(mFramePosition, mRotationY);
                    }
                    break;
                case AttributesType.S_PIVOT_X:
                    if (!Float.isNaN(mRotationX)) {
                        splineSet.setPoint(mFramePosition, mPivotX);
                    }
                    break;
                case AttributesType.S_PIVOT_Y:
                    if (!Float.isNaN(mRotationY)) {
                        splineSet.setPoint(mFramePosition, mPivotY);
                    }
                    break;
                case AttributesType.S_PATH_ROTATE:
                    if (!Float.isNaN(mTransitionPathRotate)) {
                        splineSet.setPoint(mFramePosition, mTransitionPathRotate);
                    }
                    break;
                case AttributesType.S_SCALE_X:
                    if (!Float.isNaN(mScaleX)) {
                        splineSet.setPoint(mFramePosition, mScaleX);
                    }
                    break;
                case AttributesType.S_SCALE_Y:
                    if (!Float.isNaN(mScaleY)) {
                        splineSet.setPoint(mFramePosition, mScaleY);
                    }
                    break;
                case AttributesType.S_TRANSLATION_X:
                    if (!Float.isNaN(mTranslationX)) {
                        splineSet.setPoint(mFramePosition, mTranslationX);
                    }
                    break;
                case AttributesType.S_TRANSLATION_Y:
                    if (!Float.isNaN(mTranslationY)) {
                        splineSet.setPoint(mFramePosition, mTranslationY);
                    }
                    break;
                case AttributesType.S_TRANSLATION_Z:
                    if (!Float.isNaN(mTranslationZ)) {
                        splineSet.setPoint(mFramePosition, mTranslationZ);
                    }
                    break;
                case AttributesType.S_PROGRESS:
                    if (!Float.isNaN(mProgress)) {
                        splineSet.setPoint(mFramePosition, mProgress);
                    }
                    break;
                default:
                    System.err.println("not supported by KeyAttributes " + s);
            }
        }
    }

    @Override
    public MotionKey clone() {
        return null;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, int value) {

        switch (type) {
            case AttributesType.TYPE_VISIBILITY:
                mVisibility = value;
                break;
            case AttributesType.TYPE_CURVE_FIT:
                mCurveFit = value;
                break;
            case TypedValues.TYPE_FRAME_POSITION:
                mFramePosition = value;
                break;
            default:
                if (!setValue(type, value)) {
                    return super.setValue(type, value);
                }
        }
        return true;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, float value) {
        switch (type) {
            case AttributesType.TYPE_ALPHA:
                mAlpha = value;
                break;
            case AttributesType.TYPE_TRANSLATION_X:
                mTranslationX = value;
                break;
            case AttributesType.TYPE_TRANSLATION_Y:
                mTranslationY = value;
                break;
            case AttributesType.TYPE_TRANSLATION_Z:
                mTranslationZ = value;
                break;
            case AttributesType.TYPE_ELEVATION:
                mElevation = value;
                break;
            case AttributesType.TYPE_ROTATION_X:
                mRotationX = value;
                break;
            case AttributesType.TYPE_ROTATION_Y:
                mRotationY = value;
                break;
            case AttributesType.TYPE_ROTATION_Z:
                mRotation = value;
                break;
            case AttributesType.TYPE_SCALE_X:
                mScaleX = value;
                break;
            case AttributesType.TYPE_SCALE_Y:
                mScaleY = value;
                break;
            case AttributesType.TYPE_PIVOT_X:
                mPivotX = value;
                break;
            case AttributesType.TYPE_PIVOT_Y:
                mPivotY = value;
                break;
            case AttributesType.TYPE_PROGRESS:
                mProgress = value;
                break;
            case AttributesType.TYPE_PATH_ROTATE:
                mTransitionPathRotate = value;
                break;
            case TypedValues.TYPE_FRAME_POSITION:
                mTransitionPathRotate = value;
                break;
            default:
                return super.setValue(type, value);
        }
        return true;
    }

    // @TODO: add description
    @Override
    public void setInterpolation(HashMap<String, Integer> interpolation) {
        if (!Float.isNaN(mAlpha)) {
            interpolation.put(AttributesType.S_ALPHA, mCurveFit);
        }
        if (!Float.isNaN(mElevation)) {
            interpolation.put(AttributesType.S_ELEVATION, mCurveFit);
        }
        if (!Float.isNaN(mRotation)) {
            interpolation.put(AttributesType.S_ROTATION_Z, mCurveFit);
        }
        if (!Float.isNaN(mRotationX)) {
            interpolation.put(AttributesType.S_ROTATION_X, mCurveFit);
        }
        if (!Float.isNaN(mRotationY)) {
            interpolation.put(AttributesType.S_ROTATION_Y, mCurveFit);
        }
        if (!Float.isNaN(mPivotX)) {
            interpolation.put(AttributesType.S_PIVOT_X, mCurveFit);
        }
        if (!Float.isNaN(mPivotY)) {
            interpolation.put(AttributesType.S_PIVOT_Y, mCurveFit);
        }
        if (!Float.isNaN(mTranslationX)) {
            interpolation.put(AttributesType.S_TRANSLATION_X, mCurveFit);
        }
        if (!Float.isNaN(mTranslationY)) {
            interpolation.put(AttributesType.S_TRANSLATION_Y, mCurveFit);
        }
        if (!Float.isNaN(mTranslationZ)) {
            interpolation.put(AttributesType.S_TRANSLATION_Z, mCurveFit);
        }
        if (!Float.isNaN(mTransitionPathRotate)) {
            interpolation.put(AttributesType.S_PATH_ROTATE, mCurveFit);
        }
        if (!Float.isNaN(mScaleX)) {
            interpolation.put(AttributesType.S_SCALE_X, mCurveFit);
        }
        if (!Float.isNaN(mScaleY)) {
            interpolation.put(AttributesType.S_SCALE_Y, mCurveFit);
        }
        if (!Float.isNaN(mProgress)) {
            interpolation.put(AttributesType.S_PROGRESS, mCurveFit);
        }
        if (mCustom.size() > 0) {
            for (String s : mCustom.keySet()) {
                interpolation.put(AttributesType.S_CUSTOM + "," + s, mCurveFit);
            }
        }
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, String value) {
        switch (type) {
            case AttributesType.TYPE_EASING:
                mTransitionEasing = value;
                break;

            case TypedValues.TYPE_TARGET:
                mTargetString = value;
                break;
            default:
                return super.setValue(type, value);
        }
        return true;
    }

    @Override
    public int getId(String name) {
        return AttributesType.getId(name);
    }

    public int getCurveFit() {
        return mCurveFit;
    }

    // @TODO: add description
    public void printAttributes() {
        HashSet<String> nameSet = new HashSet<>();
        getAttributeNames(nameSet);

        System.out.println(" ------------- " + mFramePosition + " -------------");
        String[] names = nameSet.toArray(new String[0]);
        for (int i = 0; i < names.length; i++) {
            int id = AttributesType.getId(names[i]);
            System.out.println(names[i] + ":" + getFloatValue(id));
        }
    }

    private float getFloatValue(int id) {
        switch (id) {
            case AttributesType.TYPE_ALPHA:
                return mAlpha;
            case AttributesType.TYPE_TRANSLATION_X:
                return mTranslationX;
            case AttributesType.TYPE_TRANSLATION_Y:
                return mTranslationY;
            case AttributesType.TYPE_TRANSLATION_Z:
                return mTranslationZ;
            case AttributesType.TYPE_ELEVATION:
                return mElevation;
            case AttributesType.TYPE_ROTATION_X:
                return mRotationX;
            case AttributesType.TYPE_ROTATION_Y:
                return mRotationY;
            case AttributesType.TYPE_ROTATION_Z:
                return mRotation;
            case AttributesType.TYPE_SCALE_X:
                return mScaleX;
            case AttributesType.TYPE_SCALE_Y:
                return mScaleY;
            case AttributesType.TYPE_PIVOT_X:
                return mPivotX;
            case AttributesType.TYPE_PIVOT_Y:
                return mPivotY;
            case AttributesType.TYPE_PROGRESS:
                return mProgress;
            case AttributesType.TYPE_PATH_ROTATE:
                return mTransitionPathRotate;
            case TypedValues.TYPE_FRAME_POSITION:
                return mFramePosition;
            default:
                return Float.NaN;
        }
    }
}
