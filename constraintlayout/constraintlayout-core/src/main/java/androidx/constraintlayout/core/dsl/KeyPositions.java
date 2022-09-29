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
 * Provides the API for creating a KeyPosition Object for use in the Core
 * ConstraintLayout & MotionLayout system
 * This allows multiple KeyPosition positions to defined in one object.
 */
public class KeyPositions extends Keys {

    private String[] mTarget = null;
    private String mTransitionEasing = null;
    private Type mPositionType = null;

    private int[] mFrames = null;
    private float[] mPercentWidth = null;
    private float[] mPercentHeight = null;
    private float[] mPercentX = null;
    private float[] mPercentY = null;

    public enum Type {
        CARTESIAN,
        SCREEN,
        PATH
    }

    public KeyPositions(int numOfFrames, String... targets) {
        mTarget = targets;
        mFrames = new int[numOfFrames];
        // the default is evenly spaced  1 at 50, 2 at 33 & 66, 3 at 25,50,75
        float gap = 100f / (mFrames.length + 1);
        for (int i = 0; i < mFrames.length; i++) {
            mFrames[i] = (int) (i * gap + gap);
        }
    }

    public String getTransitionEasing() {
        return mTransitionEasing;
    }

    public void setTransitionEasing(String transitionEasing) {
        mTransitionEasing = transitionEasing;
    }

    public int[] getFrames() {
        return mFrames;
    }

    public void setFrames(int... frames) {
        mFrames = frames;
    }

    public float[] getPercentWidth() {
        return mPercentWidth;
    }

    public void setPercentWidth(float... percentWidth) {
        mPercentWidth = percentWidth;
    }

    public float[] getPercentHeight() {
        return mPercentHeight;
    }

    public void setPercentHeight(float... percentHeight) {
        mPercentHeight = percentHeight;
    }

    public float[] getPercentX() {
        return mPercentX;
    }

    public void setPercentX(float... percentX) {
        mPercentX = percentX;
    }

    public float[] getPercentY() {
        return mPercentY;
    }

    public void setPercentY(float... percentY) {
        mPercentY = percentY;
    }

    public Type getPositionType() {
        return mPositionType;
    }

    public void setPositionType(Type positionType) {
        mPositionType = positionType;
    }

    public String[] getTarget() {
        return mTarget;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("KeyPositions:{\n");

        append(ret, "target", mTarget);

        ret.append("frame:").append(Arrays.toString(mFrames)).append(",\n");

        if (mPositionType != null) {
            ret.append("type:'").append(mPositionType).append("',\n");
        }

        append(ret, "easing", mTransitionEasing);
        append(ret, "percentX", mPercentX);
        append(ret, "percentX", mPercentY);
        append(ret, "percentWidth", mPercentWidth);
        append(ret, "percentHeight", mPercentHeight);

        ret.append("},\n");
        return ret.toString();
    }
}
