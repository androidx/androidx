/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.constraintlayout.core.state;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_PERCENT;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;

import androidx.constraintlayout.core.widgets.ConstraintWidget;

/**
 * Represents a dimension (width or height) of a constrained widget
 */
public class Dimension {

    public static final Object FIXED_DIMENSION = new String("FIXED_DIMENSION");
    public static final Object WRAP_DIMENSION = new String("WRAP_DIMENSION");
    public static final Object SPREAD_DIMENSION = new String("SPREAD_DIMENSION");
    public static final Object PARENT_DIMENSION = new String("PARENT_DIMENSION");
    public static final Object PERCENT_DIMENSION = new String("PERCENT_DIMENSION");
    public static final Object RATIO_DIMENSION = new String("RATIO_DIMENSION");

    private final int mWrapContent = -2;

    int mMin = 0;
    int mMax = Integer.MAX_VALUE;
    float mPercent = 1f;
    int mValue = 0;
    String mRatioString = null;
    Object mInitialValue = WRAP_DIMENSION;
    boolean mIsSuggested = false;

    /**
     * Returns true if the dimension is a fixed dimension of
     * the same given value
     */
    public boolean equalsFixedValue(int value) {
        if (mInitialValue == null
                && mValue == value) {
            return true;
        }
        return false;
    }

    public enum Type {
        FIXED,
        WRAP,
        MATCH_PARENT,
        MATCH_CONSTRAINT
    }

    private Dimension() {
    }

    private Dimension(Object type) {
        mInitialValue = type;
    }

    // @TODO: add description
    public static Dimension createSuggested(int value) {
        Dimension dimension = new Dimension();
        dimension.suggested(value);
        return dimension;
    }

    // @TODO: add description
    public static Dimension createSuggested(Object startValue) {
        Dimension dimension = new Dimension();
        dimension.suggested(startValue);
        return dimension;
    }

    // @TODO: add description
    public static Dimension createFixed(int value) {
        Dimension dimension = new Dimension(FIXED_DIMENSION);
        dimension.fixed(value);
        return dimension;
    }

    // @TODO: add description
    public static Dimension createFixed(Object value) {
        Dimension dimension = new Dimension(FIXED_DIMENSION);
        dimension.fixed(value);
        return dimension;
    }

    // @TODO: add description
    public static Dimension createPercent(Object key, float value) {
        Dimension dimension = new Dimension(PERCENT_DIMENSION);
        dimension.percent(key, value);
        return dimension;
    }

    // @TODO: add description
    public static Dimension createParent() {
        return new Dimension(PARENT_DIMENSION);
    }

    // @TODO: add description
    public static Dimension createWrap() {
        return new Dimension(WRAP_DIMENSION);
    }

    // @TODO: add description
    public static Dimension createSpread() {
        return new Dimension(SPREAD_DIMENSION);
    }

    // @TODO: add description
    public static Dimension createRatio(String ratio) {
        Dimension dimension = new Dimension(RATIO_DIMENSION);
        dimension.ratio(ratio);
        return dimension;
    }

    // @TODO: add description
    public Dimension percent(Object key, float value) {
        mPercent = value;
        return this;
    }

    // @TODO: add description
    public Dimension min(int value) {
        if (value >= 0) {
            mMin = value;
        }
        return this;
    }

    // @TODO: add description
    public Dimension min(Object value) {
        if (value == WRAP_DIMENSION) {
            mMin = mWrapContent;
        }
        return this;
    }

    // @TODO: add description
    public Dimension max(int value) {
        if (mMax >= 0) {
            mMax = value;
        }
        return this;
    }

    // @TODO: add description
    public Dimension max(Object value) {
        if (value == WRAP_DIMENSION && mIsSuggested) {
            mInitialValue = WRAP_DIMENSION;
            mMax = Integer.MAX_VALUE;
        }
        return this;
    }

    // @TODO: add description
    public Dimension suggested(int value) {
        mIsSuggested = true;
        if (value >= 0) {
            mMax = value;
        }
        return this;
    }

    // @TODO: add description
    public Dimension suggested(Object value) {
        mInitialValue = value;
        mIsSuggested = true;
        return this;
    }

    // @TODO: add description
    public Dimension fixed(Object value) {
        mInitialValue = value;
        if (value instanceof Integer) {
            mValue = (Integer) value;
            mInitialValue = null;
        }
        return this;
    }

    // @TODO: add description
    public Dimension fixed(int value) {
        mInitialValue = null;
        mValue = value;
        return this;
    }

    // @TODO: add description
    public Dimension ratio(String ratio) { // WxH ratio
        mRatioString = ratio;
        return this;
    }

    void setValue(int value) {
        mIsSuggested = false; // fixed value
        mInitialValue = null;
        mValue = value;
    }

    int getValue() {
        return mValue;
    }

    /**
     * Apply the dimension to the given constraint widget
     */
    public void apply(State state, ConstraintWidget constraintWidget, int orientation) {
        if (mRatioString != null) {
            constraintWidget.setDimensionRatio(mRatioString);
        }
        if (orientation == ConstraintWidget.HORIZONTAL) {
            if (mIsSuggested) {
                constraintWidget.setHorizontalDimensionBehaviour(
                        ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                int type = MATCH_CONSTRAINT_SPREAD;
                if (mInitialValue == WRAP_DIMENSION) {
                    type = MATCH_CONSTRAINT_WRAP;
                } else if (mInitialValue == PERCENT_DIMENSION) {
                    type = MATCH_CONSTRAINT_PERCENT;
                }
                constraintWidget.setHorizontalMatchStyle(type, mMin, mMax, mPercent);
            } else { // fixed
                if (mMin > 0) {
                    constraintWidget.setMinWidth(mMin);
                }
                if (mMax < Integer.MAX_VALUE) {
                    constraintWidget.setMaxWidth(mMax);
                }
                if (mInitialValue == WRAP_DIMENSION) {
                    constraintWidget.setHorizontalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
                } else if (mInitialValue == PARENT_DIMENSION) {
                    constraintWidget.setHorizontalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
                } else if (mInitialValue == null) {
                    constraintWidget.setHorizontalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.FIXED);
                    constraintWidget.setWidth(mValue);
                }
            }
        } else {
            if (mIsSuggested) {
                constraintWidget.setVerticalDimensionBehaviour(
                        ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                int type = MATCH_CONSTRAINT_SPREAD;
                if (mInitialValue == WRAP_DIMENSION) {
                    type = MATCH_CONSTRAINT_WRAP;
                } else if (mInitialValue == PERCENT_DIMENSION) {
                    type = MATCH_CONSTRAINT_PERCENT;
                }
                constraintWidget.setVerticalMatchStyle(type, mMin, mMax, mPercent);
            } else { // fixed
                if (mMin > 0) {
                    constraintWidget.setMinHeight(mMin);
                }
                if (mMax < Integer.MAX_VALUE) {
                    constraintWidget.setMaxHeight(mMax);
                }
                if (mInitialValue == WRAP_DIMENSION) {
                    constraintWidget.setVerticalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
                } else if (mInitialValue == PARENT_DIMENSION) {
                    constraintWidget.setVerticalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
                } else if (mInitialValue == null) {
                    constraintWidget.setVerticalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.FIXED);
                    constraintWidget.setHeight(mValue);
                }
            }
        }
    }

}
