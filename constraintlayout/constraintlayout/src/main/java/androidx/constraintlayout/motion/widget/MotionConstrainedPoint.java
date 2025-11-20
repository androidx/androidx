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

import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.core.motion.utils.Easing;
import androidx.constraintlayout.motion.utils.ViewSpline;
import androidx.constraintlayout.widget.ConstraintAttribute;
import androidx.constraintlayout.widget.ConstraintSet;

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
    static final int PERPENDICULAR = 1;
    static final int CARTESIAN = 2;
    static String[] sNames = {"position", "x", "y", "width", "height", "pathRotate"};
    public float rotationY = 0;
    int mVisibilityMode = ConstraintSet.VISIBILITY_MODE_NORMAL;
    int mVisibility;
    LinkedHashMap<String, ConstraintAttribute> mAttributes = new LinkedHashMap<>();
    int mMode = 0; // how was this point computed 1=perpendicular 2=deltaRelative
    double[] mTempValue = new double[18];
    double[] mTempDelta = new double[18];
    private float mAlpha = 1;
    @SuppressWarnings("unused")
    private boolean mApplyElevation = false;
    private float mElevation = 0;
    private float mRotation = 0;
    private float mRotationX = 0;
    private float mScaleX = 1;
    private float mScaleY = 1;
    private float mPivotX = Float.NaN;
    private float mPivotY = Float.NaN;
    private float mTranslationX = 0;
    private float mTranslationY = 0;
    private float mTranslationZ = 0;
    @SuppressWarnings("unused")
    private Easing mKeyFrameEasing;
    @SuppressWarnings("unused")
    private int mDrawPath = 0;
    @SuppressWarnings("unused")
    private float mPosition;
    private float mX;
    private float mY;
    private float mWidth;
    private float mHeight;
    private float mPathRotate = Float.NaN;
    private float mProgress = Float.NaN;
    @SuppressWarnings("unused")
    private int mAnimateRelativeTo = -1;

    private boolean diff(float a, float b) {
        if (Float.isNaN(a) || Float.isNaN(b)) {
            return Float.isNaN(a) != Float.isNaN(b);
        }
        return Math.abs(a - b) > 0.000001f;
    }

    /**
     * Given the start and end points define Keys that need to be built
     *
     * @param points
     * @param keySet
     */
    void different(MotionConstrainedPoint points, HashSet<String> keySet) {
        if (diff(mAlpha, points.mAlpha)) {
            keySet.add(Key.ALPHA);
        }
        if (diff(mElevation, points.mElevation)) {
            keySet.add(Key.ELEVATION);
        }
        if (mVisibility != points.mVisibility
                && mVisibilityMode == ConstraintSet.VISIBILITY_MODE_NORMAL
                && (mVisibility == ConstraintSet.VISIBLE
                || points.mVisibility == ConstraintSet.VISIBLE)) {
            keySet.add(Key.ALPHA);
        }
        if (diff(mRotation, points.mRotation)) {
            keySet.add(Key.ROTATION);
        }
        if (!(Float.isNaN(mPathRotate) && Float.isNaN(points.mPathRotate))) {
            keySet.add(Key.TRANSITION_PATH_ROTATE);
        }
        if (!(Float.isNaN(mProgress) && Float.isNaN(points.mProgress))) {
            keySet.add(Key.PROGRESS);
        }
        if (diff(mRotationX, points.mRotationX)) {
            keySet.add(Key.ROTATION_X);
        }
        if (diff(rotationY, points.rotationY)) {
            keySet.add(Key.ROTATION_Y);
        }
        if (diff(mPivotX, points.mPivotX)) {
            keySet.add(Key.PIVOT_X);
        }
        if (diff(mPivotY, points.mPivotY)) {
            keySet.add(Key.PIVOT_Y);
        }
        if (diff(mScaleX, points.mScaleX)) {
            keySet.add(Key.SCALE_X);
        }
        if (diff(mScaleY, points.mScaleY)) {
            keySet.add(Key.SCALE_Y);
        }
        if (diff(mTranslationX, points.mTranslationX)) {
            keySet.add(Key.TRANSLATION_X);
        }
        if (diff(mTranslationY, points.mTranslationY)) {
            keySet.add(Key.TRANSLATION_Y);
        }
        if (diff(mTranslationZ, points.mTranslationZ)) {
            keySet.add(Key.TRANSLATION_Z);
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

    void fillStandard(double[] data, int[] toUse) {
        float[] set = {mPosition, mX, mY, mWidth, mHeight, mAlpha, mElevation,
                mRotation, mRotationX, rotationY,
                mScaleX, mScaleY, mPivotX, mPivotY,
                mTranslationX, mTranslationY, mTranslationZ, mPathRotate};
        int c = 0;
        for (int i = 0; i < toUse.length; i++) {
            if (toUse[i] < set.length) {
                data[c++] = set[toUse[i]];
            }
        }
    }

    boolean hasCustomData(String name) {
        return mAttributes.containsKey(name);
    }

    int getCustomDataCount(String name) {
        return mAttributes.get(name).numberOfInterpolatedValues();
    }

    int getCustomData(String name, double[] value, int offset) {
        ConstraintAttribute a = mAttributes.get(name);
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

    public void applyParameters(View view) {

        this.mVisibility = view.getVisibility();
        this.mAlpha = (view.getVisibility() != View.VISIBLE) ? 0.0f : view.getAlpha();
        this.mApplyElevation = false; // TODO figure a way to cache parameters
        this.mElevation = view.getElevation();
        this.mRotation = view.getRotation();
        this.mRotationX = view.getRotationX();
        this.rotationY = view.getRotationY();
        this.mScaleX = view.getScaleX();
        this.mScaleY = view.getScaleY();
        this.mPivotX = view.getPivotX();
        this.mPivotY = view.getPivotY();
        this.mTranslationX = view.getTranslationX();
        this.mTranslationY = view.getTranslationY();
        this.mTranslationZ = view.getTranslationZ();
    }

    public void applyParameters(ConstraintSet.Constraint c) {
        this.mVisibilityMode = c.propertySet.mVisibilityMode;
        this.mVisibility = c.propertySet.visibility;
        this.mAlpha = (c.propertySet.visibility != ConstraintSet.VISIBLE
                && mVisibilityMode == ConstraintSet.VISIBILITY_MODE_NORMAL)
                ? 0.0f : c.propertySet.alpha;
        this.mApplyElevation = c.transform.applyElevation;
        this.mElevation = c.transform.elevation;
        this.mRotation = c.transform.rotation;
        this.mRotationX = c.transform.rotationX;
        this.rotationY = c.transform.rotationY;
        this.mScaleX = c.transform.scaleX;
        this.mScaleY = c.transform.scaleY;
        this.mPivotX = c.transform.transformPivotX;
        this.mPivotY = c.transform.transformPivotY;
        this.mTranslationX = c.transform.translationX;
        this.mTranslationY = c.transform.translationY;
        this.mTranslationZ = c.transform.translationZ;

        this.mKeyFrameEasing = Easing.getInterpolator(c.motion.mTransitionEasing);
        this.mPathRotate = c.motion.mPathRotate;
        this.mDrawPath = c.motion.mDrawPath;
        this.mAnimateRelativeTo = c.motion.mAnimateRelativeTo;
        this.mProgress = c.propertySet.mProgress;
        Set<String> at = c.mCustomConstraints.keySet();
        for (String s : at) {
            ConstraintAttribute attr = c.mCustomConstraints.get(s);
            if (attr.isContinuous()) {
                this.mAttributes.put(s, attr);
            }
        }
    }

    public void addValues(HashMap<String, ViewSpline> splines, int mFramePosition) {
        for (String s : splines.keySet()) {
            ViewSpline viewSpline = splines.get(s);
            if (viewSpline == null) {
                continue;
            }
            if (DEBUG) {
                Log.v(TAG, "setPoint" + mFramePosition + "  spline set = " + s);
            }
            switch (s) {
                case Key.ALPHA:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mAlpha) ? 1 : mAlpha);
                    break;
                case Key.ELEVATION:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mElevation) ? 0 : mElevation);
                    break;
                case Key.ROTATION:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mRotation) ? 0 : mRotation);
                    break;
                case Key.ROTATION_X:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mRotationX) ? 0 : mRotationX);
                    break;
                case Key.ROTATION_Y:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(rotationY) ? 0 : rotationY);
                    break;
                case Key.PIVOT_X:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mPivotX) ? 0 : mPivotX);
                    break;
                case Key.PIVOT_Y:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mPivotY) ? 0 : mPivotY);
                    break;
                case Key.TRANSITION_PATH_ROTATE:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mPathRotate) ? 0 : mPathRotate);
                    break;
                case Key.PROGRESS:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mProgress) ? 0 : mProgress);
                    break;
                case Key.SCALE_X:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mScaleX) ? 1 : mScaleX);
                    break;
                case Key.SCALE_Y:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mScaleY) ? 1 : mScaleY);
                    break;
                case Key.TRANSLATION_X:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mTranslationX)
                            ? 0 : mTranslationX);
                    break;
                case Key.TRANSLATION_Y:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mTranslationY)
                            ? 0 : mTranslationY);
                    break;
                case Key.TRANSLATION_Z:
                    viewSpline.setPoint(mFramePosition, Float.isNaN(mTranslationZ)
                            ? 0 : mTranslationZ);
                    break;
                default:
                    if (s.startsWith("CUSTOM")) {
                        String customName = s.split(",")[1];
                        if (mAttributes.containsKey(customName)) {
                            ConstraintAttribute custom = mAttributes.get(customName);
                            if (viewSpline instanceof ViewSpline.CustomSet) {
                                ((ViewSpline.CustomSet) viewSpline)
                                        .setPoint(mFramePosition, custom);
                            } else {
                                Log.e(TAG, s + " ViewSpline not a CustomSet frame = "
                                        + mFramePosition
                                        + ", value" + custom.getValueToInterpolate()
                                        + viewSpline);

                            }

                        }
                    } else {
                        Log.e(TAG, "UNKNOWN spline " + s);
                    }
            }
        }

    }


    public void setState(View view) {
        setBounds(view.getX(), view.getY(), view.getWidth(), view.getHeight());
        applyParameters(view);
    }

    /**
     * @param rect     assumes pre rotated
     * @param view
     * @param rotation mode Surface.ROTATION_0,Surface.ROTATION_90...
     */
    public void setState(Rect rect, View view, int rotation, float prevous) {
        setBounds(rect.left, rect.top, rect.width(), rect.height());
        applyParameters(view);
        mPivotX = Float.NaN;
        mPivotY = Float.NaN;

        switch (rotation) {
            case ConstraintSet.ROTATE_PORTRATE_OF_LEFT:
                this.mRotation = prevous + 90;
                break;
            case ConstraintSet.ROTATE_PORTRATE_OF_RIGHT:
                this.mRotation = prevous - 90;
                break;
        }
    }

    /**
     * Sets the state of the position given a rect, constraintset, rotation and viewid
     *
     * @param cw
     * @param constraintSet
     * @param rotation
     * @param viewId
     */
    public void setState(Rect cw, ConstraintSet constraintSet, int rotation, int viewId) {
        setBounds(cw.left, cw.top, cw.width(), cw.height());
        applyParameters(constraintSet.getParameters(viewId));
        switch (rotation) {
            case ConstraintSet.ROTATE_PORTRATE_OF_RIGHT:
            case ConstraintSet.ROTATE_RIGHT_OF_PORTRATE:
                this.mRotation -= 90;
                break;
            case ConstraintSet.ROTATE_PORTRATE_OF_LEFT:
            case ConstraintSet.ROTATE_LEFT_OF_PORTRATE:
                this.mRotation += 90;
                if (this.mRotation > 180) this.mRotation -= 360;
                break;
        }
    }
}
