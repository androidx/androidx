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

/**
 * Provides the API for creating a KeyPosition Object for use in the Core
 * ConstraintLayout & MotionLayout system
 */
public class KeyPosition extends Keys {

    private String mTarget = null;
    private String mTransitionEasing = null;
    private int mFrame = 0;
    private float mPercentWidth = Float.NaN;
    private float mPercentHeight = Float.NaN;
    private float mPercentX = Float.NaN;
    private float mPercentY = Float.NaN;
    private Type mPositionType = Type.CARTESIAN;

    public enum Type {
        CARTESIAN,
        SCREEN,
        PATH
    }

    public KeyPosition(String firstTarget, int frame) {

        mTarget = firstTarget;
        mFrame = frame;
    }

    public String getTransitionEasing() {
        return mTransitionEasing;
    }

    public void setTransitionEasing(String transitionEasing) {
        mTransitionEasing = transitionEasing;
    }

    public int getFrames() {
        return mFrame;
    }

    public void setFrames(int frames) {
        mFrame = frames;
    }

    public float getPercentWidth() {
        return mPercentWidth;
    }

    public void setPercentWidth(float percentWidth) {
        mPercentWidth = percentWidth;
    }

    public float getPercentHeight() {
        return mPercentHeight;
    }

    public void setPercentHeight(float percentHeight) {
        mPercentHeight = percentHeight;
    }

    public float getPercentX() {
        return mPercentX;
    }

    public void setPercentX(float percentX) {
        mPercentX = percentX;
    }

    public float getPercentY() {
        return mPercentY;
    }

    public void setPercentY(float percentY) {
        mPercentY = percentY;
    }

    public Type getPositionType() {
        return mPositionType;
    }

    public void setPositionType(Type positionType) {
        mPositionType = positionType;
    }

    public String getTarget() {
        return mTarget;
    }

    public void setTarget(String target) {
        mTarget = target;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("KeyPositions:{\n");

        append(ret, "target", mTarget);
        ret.append("frame:").append(mFrame).append(",\n");

        if (mPositionType != null) {
            ret.append("type:'").append(mPositionType).append("',\n");
        }

        append(ret, "easing", mTransitionEasing);
        append(ret, "percentX", mPercentX);
        append(ret, "percentY", mPercentY);
        append(ret, "percentWidth", mPercentWidth);
        append(ret, "percentHeight", mPercentHeight);

        ret.append("},\n");
        return ret.toString();
    }
}
