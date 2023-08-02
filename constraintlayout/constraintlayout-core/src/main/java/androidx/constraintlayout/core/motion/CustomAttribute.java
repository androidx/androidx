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
/*
 * Copyright (C) 2017 The Android Open Source Project
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


/**
 * Defines non standard Attributes
 *
 *
 */
public class CustomAttribute {
    private static final String TAG = "TransitionLayout";
    @SuppressWarnings("unused") private boolean mMethod = false;
    String mName;
    private AttributeType mType;
    private int mIntegerValue;
    private float mFloatValue;
    @SuppressWarnings("unused") private String mStringValue;
    boolean mBooleanValue;
    private int mColorValue;

    public enum AttributeType {
        INT_TYPE,
        FLOAT_TYPE,
        COLOR_TYPE,
        COLOR_DRAWABLE_TYPE,
        STRING_TYPE,
        BOOLEAN_TYPE,
        DIMENSION_TYPE,
        REFERENCE_TYPE
    }

    public AttributeType getType() {
        return mType;
    }

    /**
     * Continuous types are interpolated they are fired only at
     */
    public boolean isContinuous() {
        switch (mType) {
            case REFERENCE_TYPE:
            case BOOLEAN_TYPE:
            case STRING_TYPE:
                return false;
            default:
                return true;
        }
    }

    public void setFloatValue(float value) {
        mFloatValue = value;
    }

    public void setColorValue(int value) {
        mColorValue = value;
    }

    public void setIntValue(int value) {
        mIntegerValue = value;
    }

    public void setStringValue(String value) {
        mStringValue = value;
    }

    /**
     * The number of interpolation values that need to be interpolated
     * Typically 1 but 3 for colors.
     *
     * @return Typically 1 but 3 for colors.
     */
    public int numberOfInterpolatedValues() {
        switch (mType) {
            case COLOR_TYPE:
            case COLOR_DRAWABLE_TYPE:
                return 4;
            default:
                return 1;
        }
    }

    /**
     * Transforms value to a float for the purpose of interpolation
     *
     * @return interpolation value
     */
    public float getValueToInterpolate() {
        switch (mType) {
            case INT_TYPE:
                return mIntegerValue;
            case FLOAT_TYPE:
                return mFloatValue;
            case COLOR_TYPE:
            case COLOR_DRAWABLE_TYPE:
                throw new RuntimeException("Color does not have a single color to interpolate");
            case STRING_TYPE:
                throw new RuntimeException("Cannot interpolate String");
            case BOOLEAN_TYPE:
                return mBooleanValue ? 1 : 0;
            case DIMENSION_TYPE:
                return mFloatValue;
            default:
                return Float.NaN;
        }
    }

    // @TODO: add description
    public void getValuesToInterpolate(float[] ret) {
        switch (mType) {
            case INT_TYPE:
                ret[0] = mIntegerValue;
                break;
            case FLOAT_TYPE:
                ret[0] = mFloatValue;
                break;
            case COLOR_DRAWABLE_TYPE:
            case COLOR_TYPE:
                int a = 0xFF & (mColorValue >> 24);
                int r = 0xFF & (mColorValue >> 16);
                int g = 0xFF & (mColorValue >> 8);
                int b = 0xFF & mColorValue;
                float f_r = (float) Math.pow(r / 255.0f, 2.2);
                float f_g = (float) Math.pow(g / 255.0f, 2.2);
                float f_b = (float) Math.pow(b / 255.0f, 2.2);
                ret[0] = f_r;
                ret[1] = f_g;
                ret[2] = f_b;
                ret[3] = a / 255f;
                break;
            case STRING_TYPE:
                throw new RuntimeException("Color does not have a single color to interpolate");
            case BOOLEAN_TYPE:
                ret[0] = mBooleanValue ? 1 : 0;
                break;
            case DIMENSION_TYPE:
                ret[0] = mFloatValue;
                break;
            default:
                break;
        }
    }

    // @TODO: add description
    public void setValue(float[] value) {
        switch (mType) {
            case REFERENCE_TYPE:
            case INT_TYPE:
                mIntegerValue = (int) value[0];
                break;
            case FLOAT_TYPE:
                mFloatValue = value[0];
                break;
            case COLOR_DRAWABLE_TYPE:
            case COLOR_TYPE:
                mColorValue = hsvToRgb(value[0], value[1], value[2]);
                mColorValue = (mColorValue & 0xFFFFFF) | (clamp((int) (0xFF * value[3])) << 24);
                break;
            case STRING_TYPE:
                throw new RuntimeException("Color does not have a single color to interpolate");
            case BOOLEAN_TYPE:
                mBooleanValue = value[0] > 0.5;
                break;
            case DIMENSION_TYPE:
                mFloatValue = value[0];
                break;
            default:
                break;
        }
    }

    // @TODO: add description
    public static int hsvToRgb(float hue, float saturation, float value) {
        int h = (int) (hue * 6);
        float f = hue * 6 - h;
        int p = (int) (0.5f + 255 * value * (1 - saturation));
        int q = (int) (0.5f + 255 * value * (1 - f * saturation));
        int t = (int) (0.5f + 255 * value * (1 - (1 - f) * saturation));
        int v = (int) (0.5f + 255 * value);
        switch (h) {
            case 0:
                return 0XFF000000 | (v << 16) + (t << 8) + p;
            case 1:
                return 0XFF000000 | (q << 16) + (v << 8) + p;
            case 2:
                return 0XFF000000 | (p << 16) + (v << 8) + t;
            case 3:
                return 0XFF000000 | (p << 16) + (q << 8) + v;
            case 4:
                return 0XFF000000 | (t << 16) + (p << 8) + v;
            case 5:
                return 0XFF000000 | (v << 16) + (p << 8) + q;
            default:
                return 0;
        }
    }

    /**
     * test if the two attributes are different
     */
    public boolean diff(CustomAttribute customAttribute) {
        if (customAttribute == null || mType != customAttribute.mType) {
            return false;
        }
        switch (mType) {
            case INT_TYPE:
            case REFERENCE_TYPE:
                return mIntegerValue == customAttribute.mIntegerValue;
            case FLOAT_TYPE:
                return mFloatValue == customAttribute.mFloatValue;
            case COLOR_TYPE:
            case COLOR_DRAWABLE_TYPE:
                return mColorValue == customAttribute.mColorValue;
            case STRING_TYPE:
                return mIntegerValue == customAttribute.mIntegerValue;
            case BOOLEAN_TYPE:
                return mBooleanValue == customAttribute.mBooleanValue;
            case DIMENSION_TYPE:
                return mFloatValue == customAttribute.mFloatValue;
            default:
                return false;
        }
    }

    public CustomAttribute(String name, AttributeType attributeType) {
        mName = name;
        mType = attributeType;
    }

    public CustomAttribute(String name, AttributeType attributeType, Object value, boolean method) {
        mName = name;
        mType = attributeType;
        mMethod = method;
        setValue(value);
    }

    public CustomAttribute(CustomAttribute source, Object value) {
        mName = source.mName;
        mType = source.mType;
        setValue(value);

    }

    // @TODO: add description
    public void setValue(Object value) {
        switch (mType) {
            case REFERENCE_TYPE:
            case INT_TYPE:
                mIntegerValue = (Integer) value;
                break;
            case FLOAT_TYPE:
                mFloatValue = (Float) value;
                break;
            case COLOR_TYPE:
            case COLOR_DRAWABLE_TYPE:
                mColorValue = (Integer) value;
                break;
            case STRING_TYPE:
                mStringValue = (String) value;
                break;
            case BOOLEAN_TYPE:
                mBooleanValue = (Boolean) value;
                break;
            case DIMENSION_TYPE:
                mFloatValue = (Float) value;
                break;
            default:
                break;
        }
    }

    private static int clamp(int c) {
        int n = 255;
        c &= ~(c >> 31);
        c -= n;
        c &= (c >> 31);
        c += n;
        return c;
    }

}
