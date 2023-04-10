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
package androidx.constraintlayout.core.motion;

import androidx.constraintlayout.core.motion.utils.Easing;
import androidx.constraintlayout.core.motion.utils.Rect;
import androidx.constraintlayout.core.motion.utils.SplineSet;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.motion.utils.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * All the parameter it extracts from a ConstraintSet/View
 *
 *
 */
class MotionConstrainedPoint implements Comparable<MotionConstrainedPoint> {
    public static final String TAG = "MotionPaths";
    public static final boolean DEBUG = false;

    private float mAlpha = 1;
    int mVisibilityMode = MotionWidget.VISIBILITY_MODE_NORMAL;
    int mVisibility;
    @SuppressWarnings("unused") private boolean mApplyElevation = false;
    private float mElevation = 0;
    private float mRotation = 0;
    private float mRotationX = 0;
    public float rotationY = 0;
    private float mScaleX = 1;
    private float mScaleY = 1;
    private float mPivotX = Float.NaN;
    private float mPivotY = Float.NaN;
    private float mTranslationX = 0;
    private float mTranslationY = 0;
    private float mTranslationZ = 0;
    @SuppressWarnings("unused") private Easing mKeyFrameEasing;
    @SuppressWarnings("unused") private int mDrawPath = 0;
    private float mPosition;
    private float mX;
    private float mY;
    private float mWidth;
    private float mHeight;
    private float mPathRotate = Float.NaN;
    private float mProgress = Float.NaN;
    @SuppressWarnings("unused") private int mAnimateRelativeTo = -1;

    static final int PERPENDICULAR = 1;
    static final int CARTESIAN = 2;
    static String[] sNames = {"position", "x", "y", "width", "height", "pathRotate"};

    LinkedHashMap<String, CustomVariable> mCustomVariable = new LinkedHashMap<>();
    int mMode = 0; // how was this point computed 1=perpendicular 2=deltaRelative

    MotionConstrainedPoint() {

    }

    private boolean diff(float a, float b) {
        if (Float.isNaN(a) || Float.isNaN(b)) {
            return Float.isNaN(a) != Float.isNaN(b);
        }
        return Math.abs(a - b) > 0.000001f;
    }

    /**
     * Given the start and end points define Keys that need to be built
     */
    void different(MotionConstrainedPoint points, HashSet<String> keySet) {
        if (diff(mAlpha, points.mAlpha)) {
            keySet.add(TypedValues.AttributesType.S_ALPHA);
        }
        if (diff(mElevation, points.mElevation)) {
            keySet.add(TypedValues.AttributesType.S_TRANSLATION_Z);
        }
        if (mVisibility != points.mVisibility
                && mVisibilityMode == MotionWidget.VISIBILITY_MODE_NORMAL
                && (mVisibility == MotionWidget.VISIBLE
                || points.mVisibility == MotionWidget.VISIBLE)) {
            keySet.add(TypedValues.AttributesType.S_ALPHA);
        }
        if (diff(mRotation, points.mRotation)) {
            keySet.add(TypedValues.AttributesType.S_ROTATION_Z);
        }
        if (!(Float.isNaN(mPathRotate) && Float.isNaN(points.mPathRotate))) {
            keySet.add(TypedValues.AttributesType.S_PATH_ROTATE);
        }
        if (!(Float.isNaN(mProgress) && Float.isNaN(points.mProgress))) {
            keySet.add(TypedValues.AttributesType.S_PROGRESS);
        }
        if (diff(mRotationX, points.mRotationX)) {
            keySet.add(TypedValues.AttributesType.S_ROTATION_X);
        }
        if (diff(rotationY, points.rotationY)) {
            keySet.add(TypedValues.AttributesType.S_ROTATION_Y);
        }
        if (diff(mPivotX, points.mPivotX)) {
            keySet.add(TypedValues.AttributesType.S_PIVOT_X);
        }
        if (diff(mPivotY, points.mPivotY)) {
            keySet.add(TypedValues.AttributesType.S_PIVOT_Y);
        }
        if (diff(mScaleX, points.mScaleX)) {
            keySet.add(TypedValues.AttributesType.S_SCALE_X);
        }
        if (diff(mScaleY, points.mScaleY)) {
            keySet.add(TypedValues.AttributesType.S_SCALE_Y);
        }
        if (diff(mTranslationX, points.mTranslationX)) {
            keySet.add(TypedValues.AttributesType.S_TRANSLATION_X);
        }
        if (diff(mTranslationY, points.mTranslationY)) {
            keySet.add(TypedValues.AttributesType.S_TRANSLATION_Y);
        }
        if (diff(mTranslationZ, points.mTranslationZ)) {
            keySet.add(TypedValues.AttributesType.S_TRANSLATION_Z);
        }
        if (diff(mElevation, points.mElevation)) {
            keySet.add(TypedValues.AttributesType.S_ELEVATION);
        }
    }

    void different(MotionConstrainedPoint points, boolean[] mask, String[] custom) {
        int c = 0;
        mask[c++] |= diff(mPosition, points.mPosition);
        mask[c++] |= diff(mX, points.mX);
        mask[c++] |= diff(mY, points.mY);
        mask[c++] |= diff(mWidth, points.mWidth);
        mask[c++] |= diff(mHeight, points.mHeight);
    }

    double[] mTempValue = new double[18];
    double[] mTempDelta = new double[18];

    void fillStandard(double[] data, int[] toUse) {
        float[] set = {mPosition, mX, mY, mWidth, mHeight, mAlpha, mElevation,
                mRotation, mRotationX, rotationY, mScaleX, mScaleY, mPivotX,
                mPivotY, mTranslationX, mTranslationY, mTranslationZ, mPathRotate};
        int c = 0;
        for (int i = 0; i < toUse.length; i++) {
            if (toUse[i] < set.length) {
                data[c++] = set[toUse[i]];
            }
        }
    }

    boolean hasCustomData(String name) {
        return mCustomVariable.containsKey(name);
    }

    int getCustomDataCount(String name) {
        return mCustomVariable.get(name).numberOfInterpolatedValues();
    }

    int getCustomData(String name, double[] value, int offset) {
        CustomVariable a = mCustomVariable.get(name);
        if (a.numberOfInterpolatedValues() == 1) {
            value[offset] = a.getValueToInterpolate();
            return 1;
        } else {
            int n = a.numberOfInterpolatedValues();
            float[] f = new float[n];
            a.getValuesToInterpolate(f);
            for (int i = 0; i < n; i++) {
                value[offset++] = f[i];
            }
            return n;
        }
    }

    void setBounds(float x, float y, float w, float h) {
        this.mX = x;
        this.mY = y;
        mWidth = w;
        mHeight = h;
    }

    @Override
    public int compareTo(MotionConstrainedPoint o) {
        return Float.compare(mPosition, o.mPosition);
    }

    public void applyParameters(MotionWidget view) {

        this.mVisibility = view.getVisibility();
        this.mAlpha = (view.getVisibility() != MotionWidget.VISIBLE) ? 0.0f : view.getAlpha();
        this.mApplyElevation = false; // TODO figure a way to cache parameters

        this.mRotation = view.getRotationZ();
        this.mRotationX = view.getRotationX();
        this.rotationY = view.getRotationY();
        this.mScaleX = view.getScaleX();
        this.mScaleY = view.getScaleY();
        this.mPivotX = view.getPivotX();
        this.mPivotY = view.getPivotY();
        this.mTranslationX = view.getTranslationX();
        this.mTranslationY = view.getTranslationY();
        this.mTranslationZ = view.getTranslationZ();
        Set<String> at = view.getCustomAttributeNames();
        for (String s : at) {
            CustomVariable attr = view.getCustomAttribute(s);
            if (attr != null && attr.isContinuous()) {
                this.mCustomVariable.put(s, attr);
            }
        }


    }

    public void addValues(HashMap<String, SplineSet> splines, int mFramePosition) {
        for (String s : splines.keySet()) {
            SplineSet ViewSpline = splines.get(s);
            if (DEBUG) {
                Utils.log(TAG, "setPoint" + mFramePosition + "  spline set = " + s);
            }
            switch (s) {
                case TypedValues.AttributesType.S_ALPHA:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mAlpha) ? 1 : mAlpha);
                    break;
                case TypedValues.AttributesType.S_ROTATION_Z:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mRotation) ? 0 : mRotation);
                    break;
                case TypedValues.AttributesType.S_ROTATION_X:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mRotationX) ? 0 : mRotationX);
                    break;
                case TypedValues.AttributesType.S_ROTATION_Y:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(rotationY) ? 0 : rotationY);
                    break;
                case TypedValues.AttributesType.S_PIVOT_X:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mPivotX) ? 0 : mPivotX);
                    break;
                case TypedValues.AttributesType.S_PIVOT_Y:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mPivotY) ? 0 : mPivotY);
                    break;
                case TypedValues.AttributesType.S_PATH_ROTATE:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mPathRotate) ? 0 : mPathRotate);
                    break;
                case TypedValues.AttributesType.S_PROGRESS:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mProgress) ? 0 : mProgress);
                    break;
                case TypedValues.AttributesType.S_SCALE_X:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mScaleX) ? 1 : mScaleX);
                    break;
                case TypedValues.AttributesType.S_SCALE_Y:
                    ViewSpline.setPoint(mFramePosition, Float.isNaN(mScaleY) ? 1 : mScaleY);
                    break;
                case TypedValues.AttributesType.S_TRANSLATION_X:
                    ViewSpline.setPoint(mFramePosition,
                            Float.isNaN(mTranslationX) ? 0 : mTranslationX);
                    break;
                case TypedValues.AttributesType.S_TRANSLATION_Y:
                    ViewSpline.setPoint(mFramePosition,
                            Float.isNaN(mTranslationY) ? 0 : mTranslationY);
                    break;
                case TypedValues.AttributesType.S_TRANSLATION_Z:
                    ViewSpline.setPoint(mFramePosition,
                            Float.isNaN(mTranslationZ) ? 0 : mTranslationZ);
                    break;
                default:
                    if (s.startsWith("CUSTOM")) {
                        String customName = s.split(",")[1];
                        if (mCustomVariable.containsKey(customName)) {
                            CustomVariable custom = mCustomVariable.get(customName);
                            if (ViewSpline instanceof SplineSet.CustomSpline) {
                                ((SplineSet.CustomSpline) ViewSpline)
                                        .setPoint(mFramePosition, custom);
                            } else {
                                Utils.loge(TAG, s + " ViewSpline not a CustomSet frame = "
                                        + mFramePosition + ", value"
                                        + custom.getValueToInterpolate() + ViewSpline);

                            }

                        }
                    } else {
                        Utils.loge(TAG, "UNKNOWN spline " + s);
                    }
            }
        }

    }

    public void setState(MotionWidget view) {
        setBounds(view.getX(), view.getY(), view.getWidth(), view.getHeight());
        applyParameters(view);
    }

    /**
     * @param rect     assumes pre rotated
     * @param rotation mode Surface.ROTATION_0,Surface.ROTATION_90...
     */
    public void setState(Rect rect, MotionWidget view, int rotation, float prevous) {
        setBounds(rect.left, rect.top, rect.width(), rect.height());
        applyParameters(view);
        mPivotX = Float.NaN;
        mPivotY = Float.NaN;

        switch (rotation) {
            case MotionWidget.ROTATE_PORTRATE_OF_LEFT:
                this.mRotation = prevous + 90;
                break;
            case MotionWidget.ROTATE_PORTRATE_OF_RIGHT:
                this.mRotation = prevous - 90;
                break;
        }
    }

//   TODO support Screen Rotation
//    /**
//     * Sets the state of the position given a rect, constraintset, rotation and viewid
//     *
//     * @param cw
//     * @param constraintSet
//     * @param rotation
//     * @param viewId
//     */
//    public void setState(Rect cw, ConstraintSet constraintSet, int rotation, int viewId) {
//        setBounds(cw.left, cw.top, cw.width(), cw.height());
//        applyParameters(constraintSet.getParameters(viewId));
//        switch (rotation) {
//            case ConstraintSet.ROTATE_PORTRATE_OF_RIGHT:
//            case ConstraintSet.ROTATE_RIGHT_OF_PORTRATE:
//                this.rotation -= 90;
//                break;
//            case ConstraintSet.ROTATE_PORTRATE_OF_LEFT:
//            case ConstraintSet.ROTATE_LEFT_OF_PORTRATE:
//                this.rotation += 90;
//                if (this.rotation > 180) this.rotation -= 360;
//                break;
//        }
//    }
}
