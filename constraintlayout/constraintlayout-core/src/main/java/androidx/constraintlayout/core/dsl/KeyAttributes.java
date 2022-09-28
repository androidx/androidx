/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

import java.util.Arrays;

/**
 * Provides the API for creating a KeyAttribute Object for use in the Core
 * ConstraintLayout & MotionLayout system
 * This allows multiple KeyAttribute positions to defined in one object.
 */
public class KeyAttributes extends Keys {
    protected String TYPE = "KeyAttributes";
    private String[] mTarget = null;
    private String mTransitionEasing;
    private Fit mCurveFit = null;
    private int[] mFrames = null;

    private Visibility[] mVisibility = null;
    private float[] mAlpha = null;
    private float[] mRotation = null;
    private float[] mRotationX = null;
    private float[] mRotationY = null;
    private float[] mPivotX = null;
    private float[] mPivotY = null;
    private float[] mTransitionPathRotate = null;
    private float[] mScaleX = null;
    private float[] mScaleY = null;
    private float[] mTranslationX = null;
    private float[] mTranslationY = null;
    private float[] mTranslationZ = null;

    public enum Fit {
        SPLINE,
        LINEAR,
    }

    public enum Visibility {
        VISIBLE,
        INVISIBLE,
        GONE
    }

    KeyAttributes(int numOfFrames, String... targets) {
        mTarget = targets;
        mFrames = new int[numOfFrames];
        // the default is evenly spaced  1 at 50, 2 at 33 & 66, 3 at 25,50,75
        float gap = 100f / (mFrames.length + 1);
        for (int i = 0; i < mFrames.length; i++) {
            mFrames[i] = (int) (i * gap + gap);
        }
    }

    public String[] getTarget() {
        return mTarget;
    }

    public void setTarget(String[] target) {
        mTarget = target;
    }

    public String getTransitionEasing() {
        return mTransitionEasing;
    }

    public void setTransitionEasing(String transitionEasing) {
        mTransitionEasing = transitionEasing;
    }

    public Fit getCurveFit() {
        return mCurveFit;
    }

    public void setCurveFit(Fit curveFit) {
        mCurveFit = curveFit;
    }

    public Visibility[] getVisibility() {
        return mVisibility;
    }

    public void setVisibility(Visibility... visibility) {
        mVisibility = visibility;
    }

    public float[] getAlpha() {
        return mAlpha;
    }

    public void setAlpha(float... alpha) {
        mAlpha = alpha;
    }

    public float[] getRotation() {
        return mRotation;
    }

    public void setRotation(float... rotation) {
        mRotation = rotation;
    }

    public float[] getRotationX() {
        return mRotationX;
    }

    public void setRotationX(float... rotationX) {
        mRotationX = rotationX;
    }

    public float[] getRotationY() {
        return mRotationY;
    }

    public void setRotationY(float... rotationY) {
        mRotationY = rotationY;
    }

    public float[] getPivotX() {
        return mPivotX;
    }

    public void setPivotX(float... pivotX) {
        mPivotX = pivotX;
    }

    public float[] getPivotY() {
        return mPivotY;
    }

    public void setPivotY(float... pivotY) {
        mPivotY = pivotY;
    }

    public float[] getTransitionPathRotate() {
        return mTransitionPathRotate;
    }

    public void setTransitionPathRotate(float... transitionPathRotate) {
        mTransitionPathRotate = transitionPathRotate;
    }

    public float[] getScaleX() {
        return mScaleX;
    }

    public void setScaleX(float[] scaleX) {
        mScaleX = scaleX;
    }

    public float[] getScaleY() {
        return mScaleY;
    }

    public void setScaleY(float[] scaleY) {
        mScaleY = scaleY;
    }

    public float[] getTranslationX() {
        return mTranslationX;
    }

    public void setTranslationX(float[] translationX) {
        mTranslationX = translationX;
    }

    public float[] getTranslationY() {
        return mTranslationY;
    }

    public void setTranslationY(float[] translationY) {
        mTranslationY = translationY;
    }

    public float[] getTranslationZ() {
        return mTranslationZ;
    }

    public void setTranslationZ(float[] translationZ) {
        mTranslationZ = translationZ;
    }


    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(TYPE);
        ret.append(":{\n");
        attributesToString(ret);

        ret.append("},\n");
        return ret.toString();
    }

    protected void attributesToString(StringBuilder builder) {
        append(builder, "target", mTarget);
        builder.append("frame:").append(Arrays.toString(mFrames)).append(",\n");

        append(builder, "easing", mTransitionEasing);
        if (mCurveFit != null) {
            builder.append("fit:'").append(mCurveFit).append("',\n");
        }
        if (mVisibility != null) {
            builder.append("visibility:'").append(Arrays.toString(mVisibility)).append("',\n");
        }
        append(builder, "alpha", mAlpha);
        append(builder, "rotationX", mRotationX);
        append(builder, "rotationY", mRotationY);
        append(builder, "rotationZ", mRotation);

        append(builder, "pivotX", mPivotX);
        append(builder, "pivotY", mPivotY);
        append(builder, "pathRotate", mTransitionPathRotate);
        append(builder, "scaleX", mScaleX);
        append(builder, "scaleY", mScaleY);
        append(builder, "translationX", mTranslationX);
        append(builder, "translationY", mTranslationY);
        append(builder, "translationZ", mTranslationZ);

    }

}
