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

import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.state.WidgetFrame;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

import java.util.Set;

public class MotionWidget implements TypedValues {
    WidgetFrame mWidgetFrame = new WidgetFrame();
    Motion mMotion = new Motion();
    PropertySet mPropertySet = new PropertySet();
    private float mProgress;
    float mTransitionPathRotate;

    public static final int VISIBILITY_MODE_NORMAL = 0;
    public static final int VISIBILITY_MODE_IGNORE = 1;
    @SuppressWarnings("unused") private static final int INTERNAL_MATCH_PARENT = -1;
    @SuppressWarnings("unused") private static final int INTERNAL_WRAP_CONTENT = -2;
    public static final int INVISIBLE = 0;
    public static final int VISIBLE = 4;
    @SuppressWarnings("unused") private static final int INTERNAL_MATCH_CONSTRAINT = -3;
    @SuppressWarnings("unused") private static final int INTERNAL_WRAP_CONTENT_CONSTRAINED = -4;

    public static final int ROTATE_NONE = 0;
    public static final int ROTATE_PORTRATE_OF_RIGHT = 1;
    public static final int ROTATE_PORTRATE_OF_LEFT = 2;
    public static final int ROTATE_RIGHT_OF_PORTRATE = 3;
    public static final int ROTATE_LEFT_OF_PORTRATE = 4;
    public static final int UNSET = -1;
    public static final int MATCH_CONSTRAINT = 0;
    public static final int PARENT_ID = 0;
    public static final int FILL_PARENT = -1;
    public static final int MATCH_PARENT = -1;
    public static final int WRAP_CONTENT = -2;
    public static final int GONE_UNSET = Integer.MIN_VALUE;
    public static final int MATCH_CONSTRAINT_WRAP = ConstraintWidget.MATCH_CONSTRAINT_WRAP;


    /**
     *
     */
    public static class Motion {
        public String mAnimateRelativeTo = null;
        public int mAnimateCircleAngleTo = 0;
        public String mTransitionEasing = null;
        public int mPathMotionArc = UNSET;
        public int mDrawPath = 0;
        public float mMotionStagger = Float.NaN;
        public int mPolarRelativeTo = UNSET;
        public float mPathRotate = Float.NaN;
        public float mQuantizeMotionPhase = Float.NaN;
        public int mQuantizeMotionSteps = UNSET;
        public String mQuantizeInterpolatorString = null;
        public int mQuantizeInterpolatorType = INTERPOLATOR_UNDEFINED; // undefined
        public int mQuantizeInterpolatorID = -1;
        @SuppressWarnings("unused") private static final int INTERPOLATOR_REFERENCE_ID = -2;
        @SuppressWarnings("unused") private static final int SPLINE_STRING = -1;
        private static final int INTERPOLATOR_UNDEFINED = -3;
    }

    public static class PropertySet {
        public int visibility = VISIBLE;
        public int mVisibilityMode = VISIBILITY_MODE_NORMAL;
        public float alpha = 1;
        public float mProgress = Float.NaN;
    }

    public MotionWidget() {

    }

    public MotionWidget getParent() {
        return null;
    }

    // @TODO: add description
    public MotionWidget findViewById(int mTransformPivotTarget) {
        return null;
    }

    public void setVisibility(int visibility) {
        mPropertySet.visibility = visibility;
    }

    public String getName() {
        return mWidgetFrame.getId();
    }

    // @TODO: add description
    public void layout(int l, int t, int r, int b) {
        setBounds(l, t, r, b);
    }

    // @TODO: add description
    @Override
    public String toString() {
        return mWidgetFrame.left + ", " + mWidgetFrame.top + ", "
                + mWidgetFrame.right + ", " + mWidgetFrame.bottom;
    }

    // @TODO: add description
    public void setBounds(int left, int top, int right, int bottom) {
        if (mWidgetFrame == null) {
            mWidgetFrame = new WidgetFrame((ConstraintWidget) null);
        }
        mWidgetFrame.top = top;
        mWidgetFrame.left = left;
        mWidgetFrame.right = right;
        mWidgetFrame.bottom = bottom;
    }

    public MotionWidget(WidgetFrame f) {
        mWidgetFrame = f;
    }

    /**
     * This populates the motion attributes from widgetFrame to the MotionWidget
     */
    public void updateMotion(TypedValues toUpdate) {
        if (mWidgetFrame.getMotionProperties() != null) {
            mWidgetFrame.getMotionProperties().applyDelta(toUpdate);
        }
    }

    @Override
    public boolean setValue(int id, int value) {
        boolean set = setValueAttributes(id, value);
        if (set) {
            return true;
        }
        return setValueMotion(id, value);
    }

    @Override
    public boolean setValue(int id, float value) {
        boolean set = setValueAttributes(id, value);
        if (set) {
            return true;
        }
        return setValueMotion(id, value);
    }

    @Override
    public boolean setValue(int id, String value) {
        if (id == MotionType.TYPE_ANIMATE_RELATIVE_TO) {
            mMotion.mAnimateRelativeTo = value;
            return true;
        }
        return setValueMotion(id, value);
    }

    @Override
    public boolean setValue(int id, boolean value) {
        return false;
    }

    // @TODO: add description
    public boolean setValueMotion(int id, int value) {
        switch (id) {
            case MotionType.TYPE_ANIMATE_CIRCLEANGLE_TO:
                mMotion.mAnimateCircleAngleTo = value;
                break;
            case MotionType.TYPE_PATHMOTION_ARC:
                mMotion.mPathMotionArc = value;
                break;
            case MotionType.TYPE_DRAW_PATH:
                mMotion.mDrawPath = value;
                break;
            case MotionType.TYPE_POLAR_RELATIVETO:
                mMotion.mPolarRelativeTo = value;
                break;
            case MotionType.TYPE_QUANTIZE_MOTIONSTEPS:
                mMotion.mQuantizeMotionSteps = value;
                break;
            case MotionType.TYPE_QUANTIZE_INTERPOLATOR_TYPE:
                mMotion.mQuantizeInterpolatorType = value;
                break; // undefined
            case MotionType.TYPE_QUANTIZE_INTERPOLATOR_ID:
                mMotion.mQuantizeInterpolatorID = value;
                break;
            default:
                return false;
        }
        return true;
    }

    // @TODO: add description
    public boolean setValueMotion(int id, String value) {
        switch (id) {

            case MotionType.TYPE_EASING:
                mMotion.mTransitionEasing = value;
                break;
            case MotionType.TYPE_QUANTIZE_INTERPOLATOR:
                mMotion.mQuantizeInterpolatorString = value;
                break;
            default:
                return false;
        }
        return true;
    }

    // @TODO: add description
    public boolean setValueMotion(int id, float value) {
        switch (id) {
            case MotionType.TYPE_STAGGER:
                mMotion.mMotionStagger = value;
                break;
            case MotionType.TYPE_PATH_ROTATE:
                mMotion.mPathRotate = value;
                break;
            case MotionType.TYPE_QUANTIZE_MOTION_PHASE:
                mMotion.mQuantizeMotionPhase = value;
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Sets the attributes
     */
    public boolean setValueAttributes(int id, float value) {
        switch (id) {
            case AttributesType.TYPE_ALPHA:
                mWidgetFrame.alpha = value;
                break;
            case AttributesType.TYPE_TRANSLATION_X:
                mWidgetFrame.translationX = value;
                break;
            case AttributesType.TYPE_TRANSLATION_Y:
                mWidgetFrame.translationY = value;
                break;
            case AttributesType.TYPE_TRANSLATION_Z:
                mWidgetFrame.translationZ = value;
                break;
            case AttributesType.TYPE_ROTATION_X:
                mWidgetFrame.rotationX = value;
                break;
            case AttributesType.TYPE_ROTATION_Y:
                mWidgetFrame.rotationY = value;
                break;
            case AttributesType.TYPE_ROTATION_Z:
                mWidgetFrame.rotationZ = value;
                break;
            case AttributesType.TYPE_SCALE_X:
                mWidgetFrame.scaleX = value;
                break;
            case AttributesType.TYPE_SCALE_Y:
                mWidgetFrame.scaleY = value;
                break;
            case AttributesType.TYPE_PIVOT_X:
                mWidgetFrame.pivotX = value;
                break;
            case AttributesType.TYPE_PIVOT_Y:
                mWidgetFrame.pivotY = value;
                break;
            case AttributesType.TYPE_PROGRESS:
                mProgress = value;
                break;
            case AttributesType.TYPE_PATH_ROTATE:
                mTransitionPathRotate = value;
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Sets the attributes
     */
    public float getValueAttributes(int id) {
        switch (id) {
            case AttributesType.TYPE_ALPHA:
                return mWidgetFrame.alpha;
            case AttributesType.TYPE_TRANSLATION_X:
                return mWidgetFrame.translationX;
            case AttributesType.TYPE_TRANSLATION_Y:
                return mWidgetFrame.translationY;
            case AttributesType.TYPE_TRANSLATION_Z:
                return mWidgetFrame.translationZ;
            case AttributesType.TYPE_ROTATION_X:
                return mWidgetFrame.rotationX;
            case AttributesType.TYPE_ROTATION_Y:
                return mWidgetFrame.rotationY;
            case AttributesType.TYPE_ROTATION_Z:
                return mWidgetFrame.rotationZ;
            case AttributesType.TYPE_SCALE_X:
                return mWidgetFrame.scaleX;
            case AttributesType.TYPE_SCALE_Y:
                return mWidgetFrame.scaleY;
            case AttributesType.TYPE_PIVOT_X:
                return mWidgetFrame.pivotX;
            case AttributesType.TYPE_PIVOT_Y:
                return mWidgetFrame.pivotY;
            case AttributesType.TYPE_PROGRESS:
                return mProgress;
            case AttributesType.TYPE_PATH_ROTATE:
                return mTransitionPathRotate;
            default:
                return Float.NaN;
        }

    }

    @Override
    public int getId(String name) {
        int ret = AttributesType.getId(name);
        if (ret != -1) {
            return ret;
        }
        return MotionType.getId(name);
    }

    public int getTop() {
        return mWidgetFrame.top;
    }

    public int getLeft() {
        return mWidgetFrame.left;
    }

    public int getBottom() {
        return mWidgetFrame.bottom;
    }

    public int getRight() {
        return mWidgetFrame.right;
    }

    public void setPivotX(float px) {
        mWidgetFrame.pivotX = px;
    }

    public void setPivotY(float py) {
        mWidgetFrame.pivotY = py;
    }

    public float getRotationX() {
        return mWidgetFrame.rotationX;
    }

    public void setRotationX(float rotationX) {
        mWidgetFrame.rotationX = rotationX;
    }

    public float getRotationY() {
        return mWidgetFrame.rotationY;
    }

    public void setRotationY(float rotationY) {
        mWidgetFrame.rotationY = rotationY;
    }

    public float getRotationZ() {
        return mWidgetFrame.rotationZ;
    }

    public void setRotationZ(float rotationZ) {
        mWidgetFrame.rotationZ = rotationZ;
    }

    public float getTranslationX() {
        return mWidgetFrame.translationX;
    }

    public void setTranslationX(float translationX) {
        mWidgetFrame.translationX = translationX;
    }

    public float getTranslationY() {
        return mWidgetFrame.translationY;
    }

    public void setTranslationY(float translationY) {
        mWidgetFrame.translationY = translationY;
    }

    public void setTranslationZ(float tz) {
        mWidgetFrame.translationZ = tz;
    }

    public float getTranslationZ() {
        return mWidgetFrame.translationZ;
    }

    public float getScaleX() {
        return mWidgetFrame.scaleX;
    }

    public void setScaleX(float scaleX) {
        mWidgetFrame.scaleX = scaleX;
    }

    public float getScaleY() {
        return mWidgetFrame.scaleY;
    }

    public void setScaleY(float scaleY) {
        mWidgetFrame.scaleY = scaleY;
    }

    public int getVisibility() {
        return mPropertySet.visibility;
    }

    public float getPivotX() {
        return mWidgetFrame.pivotX;
    }

    public float getPivotY() {
        return mWidgetFrame.pivotY;
    }

    public float getAlpha() {
        return mWidgetFrame.alpha;
    }

    public int getX() {
        return mWidgetFrame.left;
    }

    public int getY() {
        return mWidgetFrame.top;
    }

    public int getWidth() {
        return mWidgetFrame.right - mWidgetFrame.left;
    }

    public int getHeight() {
        return mWidgetFrame.bottom - mWidgetFrame.top;
    }

    public WidgetFrame getWidgetFrame() {
        return mWidgetFrame;
    }

    public Set<String> getCustomAttributeNames() {
        return mWidgetFrame.getCustomAttributeNames();
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, float value) {
        mWidgetFrame.setCustomAttribute(name, type, value);
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, int value) {
        mWidgetFrame.setCustomAttribute(name, type, value);
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, boolean value) {
        mWidgetFrame.setCustomAttribute(name, type, value);
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, String value) {
        mWidgetFrame.setCustomAttribute(name, type, value);
    }

    // @TODO: add description
    public CustomVariable getCustomAttribute(String name) {
        return mWidgetFrame.getCustomAttribute(name);
    }

    // @TODO: add description
    public void setInterpolatedValue(CustomAttribute attribute, float[] mCache) {
        mWidgetFrame.setCustomAttribute(attribute.mName, TypedValues.Custom.TYPE_FLOAT, mCache[0]);
    }

}
