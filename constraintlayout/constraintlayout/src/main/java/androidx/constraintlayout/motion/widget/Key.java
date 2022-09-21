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
import android.util.AttributeSet;

import androidx.constraintlayout.motion.utils.ViewSpline;
import androidx.constraintlayout.widget.ConstraintAttribute;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Base class in an element in a KeyFrame
 *
 *
 */

public abstract class Key {
    public static int UNSET = -1;
    int mFramePosition = UNSET;
    int mTargetId = UNSET;
    String mTargetString = null;
    protected int mType;

    @SuppressWarnings("HiddenAbstractMethod")
    abstract void load(Context context, AttributeSet attrs);

    HashMap<String, ConstraintAttribute> mCustomConstraints;

    @SuppressWarnings("HiddenAbstractMethod")
    abstract void getAttributeNames(HashSet<String> attributes);

    public static final String ALPHA = "alpha";
    public static final String ELEVATION = "elevation";
    public static final String ROTATION = "rotation";
    public static final String ROTATION_X = "rotationX";
    public static final String ROTATION_Y = "rotationY";
    public static final String PIVOT_X = "transformPivotX";
    public static final String PIVOT_Y = "transformPivotY";
    public static final String TRANSITION_PATH_ROTATE = "transitionPathRotate";
    public static final String SCALE_X = "scaleX";
    public static final String SCALE_Y = "scaleY";
    public static final String WAVE_PERIOD = "wavePeriod";
    public static final String WAVE_OFFSET = "waveOffset";
    public static final String WAVE_PHASE = "wavePhase";
    public static final String WAVE_VARIES_BY = "waveVariesBy";
    public static final String TRANSLATION_X = "translationX";
    public static final String TRANSLATION_Y = "translationY";
    public static final String TRANSLATION_Z = "translationZ";
    public static final String PROGRESS = "progress";
    public static final String CUSTOM = "CUSTOM";
    public static final String CURVEFIT = "curveFit";
    public static final String MOTIONPROGRESS = "motionProgress";
    public static final String TRANSITIONEASING = "transitionEasing";
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
     *
     */
    public abstract void addValues(HashMap<String, ViewSpline> splines);

    /**
     * Set the value associated with this tag
     *
     * @param tag
     * @param value
     *
     */
    public abstract void setValue(String tag, Object value);

    /**
     * Return the float given a value. If the value is a "Float" object it is casted
     *
     * @param value
     * @return
     *
     */
    float toFloat(Object value) {
        return (value instanceof Float) ? (Float) value : Float.parseFloat(value.toString());
    }

    /**
     * Return the int version of an object if the value is an Integer object it is casted.
     *
     * @param value
     * @return
     *
     */
    int toInt(Object value) {
        return (value instanceof Integer) ? (Integer) value : Integer.parseInt(value.toString());
    }

    /**
     * Return the boolean version this object if the object is a Boolean it is casted.
     *
     * @param value
     * @return
     *
     */
    boolean toBoolean(Object value) {
        return (value instanceof Boolean) ? (Boolean) value :
                Boolean.parseBoolean(value.toString());
    }

    /**
     * Key frame can specify the type of interpolation it wants on various attributes
     * For each string it set it to -1, CurveFit.LINEAR or  CurveFit.SPLINE
     *
     * @param interpolation
     */
    public void setInterpolation(HashMap<String, Integer> interpolation) {
    }

    /**
     * Return a copy of this key
     * @param src
     * @return
     */
    public Key copy(Key src) {
        mFramePosition = src.mFramePosition;
        mTargetId = src.mTargetId;
        mTargetString = src.mTargetString;
        mType = src.mType;
        mCustomConstraints = src.mCustomConstraints;
        return this;
    }

    /**
     * Return a copy of this
     * @return
     */
    @Override
    public abstract Key clone();

    /**
     * set the id of the view
     * @param id
     * @return
     */
    public Key setViewId(int id) {
        mTargetId = id;
        return this;
    }

    /**
     * sets the frame position
     *
     * @param pos
     */
    public void setFramePosition(int pos) {
        mFramePosition = pos;
    }

    /**
     * Gets the current frame position
     *
     * @return
     */
    public int getFramePosition() {
        return mFramePosition;
    }

}
