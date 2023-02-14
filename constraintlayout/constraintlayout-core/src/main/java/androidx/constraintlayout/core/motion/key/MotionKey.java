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
package androidx.constraintlayout.core.motion.key;

import androidx.constraintlayout.core.motion.CustomVariable;
import androidx.constraintlayout.core.motion.utils.SplineSet;
import androidx.constraintlayout.core.motion.utils.TypedValues;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Base class in an element in a KeyFrame
 *
 */
public abstract class MotionKey implements TypedValues {
    public static int UNSET = -1;
    public int mFramePosition = UNSET;
    int mTargetId = UNSET;
    String mTargetString = null;
    public int mType;
    public HashMap<String, CustomVariable> mCustom;

    // @TODO: add description
    public abstract void getAttributeNames(HashSet<String> attributes);

    public static final String ALPHA = "alpha";
    public static final String ELEVATION = "elevation";
    public static final String ROTATION = "rotationZ";
    public static final String ROTATION_X = "rotationX";

    public static final String TRANSITION_PATH_ROTATE = "transitionPathRotate";
    public static final String SCALE_X = "scaleX";
    public static final String SCALE_Y = "scaleY";


    public static final String TRANSLATION_X = "translationX";
    public static final String TRANSLATION_Y = "translationY";

    public static final String CUSTOM = "CUSTOM";

    public static final String VISIBILITY = "visibility";

    boolean matches(String constraintTag) {
        if (mTargetString == null || constraintTag == null) return false;
        return constraintTag.matches(mTargetString);
    }

    /**
     * Defines method to add a a view to splines derived form this key frame.
     * The values are written to the spline
     *
     * @param splines splines to write values to
     */
    public abstract void addValues(HashMap<String, SplineSet> splines);

    /**
     * Return the float given a value. If the value is a "Float" object it is casted
     *
     */
    float toFloat(Object value) {
        return (value instanceof Float) ? (Float) value : Float.parseFloat(value.toString());
    }

    /**
     * Return the int version of an object if the value is an Integer object it is casted.
     *
     *
     */
    int toInt(Object value) {
        return (value instanceof Integer) ? (Integer) value : Integer.parseInt(value.toString());
    }

    /**
     * Return the boolean version this object if the object is a Boolean it is casted.
     *
     *
     */
    boolean toBoolean(Object value) {
        return (value instanceof Boolean)
                ? (Boolean) value : Boolean.parseBoolean(value.toString());
    }

    /**
     * Key frame can specify the type of interpolation it wants on various attributes
     * For each string it set it to -1, CurveFit.LINEAR or  CurveFit.SPLINE
     */
    public void setInterpolation(HashMap<String, Integer> interpolation) {
    }

    // @TODO: add description
    public MotionKey copy(MotionKey src) {
        mFramePosition = src.mFramePosition;
        mTargetId = src.mTargetId;
        mTargetString = src.mTargetString;
        mType = src.mType;
        return this;
    }

    // @TODO: add description
    @Override
    public abstract MotionKey clone();

    // @TODO: add description
    public MotionKey setViewId(int id) {
        mTargetId = id;
        return this;
    }

    /**
     * sets the frame position
     */
    public void setFramePosition(int pos) {
        mFramePosition = pos;
    }

    /**
     * Gets the current frame position
     */
    public int getFramePosition() {
        return mFramePosition;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, int value) {

        switch (type) {
            case TypedValues.TYPE_FRAME_POSITION:
                mFramePosition = value;
                return true;
        }
        return false;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, float value) {
        return false;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, String value) {
        switch (type) {
            case TypedValues.TYPE_TARGET:
                mTargetString = value;
                return true;
        }
        return false;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, boolean value) {
        return false;
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, float value) {
        mCustom.put(name, new CustomVariable(name, type, value));
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, int value) {
        mCustom.put(name, new CustomVariable(name, type, value));
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, boolean value) {
        mCustom.put(name, new CustomVariable(name, type, value));
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, String value) {
        mCustom.put(name, new CustomVariable(name, type, value));
    }
}
