/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.graphics;

import android.graphics.Rect;

/**
 * This class contains the rules for updating the bounds of a
 * {@link CompositeDrawable.ChildDrawable}. It contains four rules, one for each value of the
 * rectangular bound - left/top/right/bottom.
 * @hide
 */
public class BoundsRule {
    static final int INHERIT_PARENT = 0;
    static final int ABSOLUTE_VALUE = 1;
    static final int INHERIT_WITH_OFFSET = 2;

    /**
     * This class represents individual rules for updating the bounds. Currently we support
     * 3 different rule types -
     *
     * <ul>
     *     <li>inheritFromParent: it applies a percentage to the parent property to compute
     *     the final value </li>
     *     <li>absoluteValue: it always used the supplied absolute value</li>
     *     <li>inheritFromParentWithOffset: this uses a combination of INHERIT_PARENT
     *     and ABSOLUTE_VALUE. First it applies the percentage on the parent and then adds the
     *     offset to compute the final value</li>
     * </ul>
     */
    public final static class ValueRule {
        private final int type;
        private float fraction;
        private int absoluteValue;

        ValueRule(int type, int absoluteValue, float fraction) {
            this.type = type;
            this.absoluteValue = absoluteValue;
            this.fraction = fraction;
        }

        ValueRule(ValueRule rule) {
            this.type = rule.type;
            this.fraction = rule.fraction;
            this.absoluteValue = rule.absoluteValue;
        }

        /**
         * Sets the fractional value (percentage of parent) for this rule.
         */
        public void setFraction(float fraction) {
            this.fraction = fraction;
        }

        /**
         * Returns the current fractional value.
         */
        public float getFraction() {
            return fraction;
        }

        /**
         * Sets the absolute value for this rule.
         */
        public void setAbsoluteValue(int absoluteValue) {
            this.absoluteValue = absoluteValue;
        }

        /**
         * Returns the current absolute value.
         */
        public int getAbsoluteValue() {
            return absoluteValue;
        }
    }

    /**
     * Factory method for creating ValueRule of type INHERIT_FROM_PARENT.
     */
    public static ValueRule inheritFromParent(float fraction) {
        return new ValueRule(INHERIT_PARENT, 0, fraction);
    }

    /**
     * Factory method for creating ValueRule of type ABSOLUTE_VALUE.
     */
    public static ValueRule absoluteValue(int value) {
        return new ValueRule(ABSOLUTE_VALUE, value, 0);
    }

    /**
     * Factory method for creating ValueRule of type INHERIT_WITH_OFFSET.
     */
    public static ValueRule inheritFromParentWithOffset(float fraction, int value) {
        return new ValueRule(INHERIT_WITH_OFFSET, value, fraction);
    }

    /**
     * Takes in the current bounds and sets the final values based on the individual rules in the
     * result object.
     *
     * @param rect Represents the current bounds.
     * @param result Represents the final bounds.
     */
    public void calculateBounds(Rect rect, Rect result) {
        if (mLeft == null) {
            result.left = rect.left;
        } else {
            result.left = doCalculate(rect.left, mLeft, rect.width());
        }

        if (mRight == null) {
            result.right = rect.right;
        } else {
            result.right = doCalculate(rect.left, mRight, rect.width());
        }

        if (mTop == null) {
            result.top = rect.top;
        } else {
            result.top = doCalculate(rect.top, mTop, rect.height());
        }

        if (mBottom == null) {
            result.bottom = rect.bottom;
        } else {
            result.bottom = doCalculate(rect.top, mBottom, rect.height());
        }
    }

    public BoundsRule() {}

    public BoundsRule(BoundsRule boundsRule) {
        this.mLeft = boundsRule.mLeft != null ? new ValueRule(boundsRule.mLeft) : null;
        this.mRight = boundsRule.mRight != null ? new ValueRule(boundsRule.mRight) : null;
        this.mTop = boundsRule.mTop != null ? new ValueRule(boundsRule.mTop) : null;
        this.mBottom = boundsRule.mBottom != null ? new ValueRule(boundsRule.mBottom) : null;
    }

    private int doCalculate(int value, ValueRule rule, int size) {
        int offset = 0;
        switch(rule.type) {
            case INHERIT_WITH_OFFSET:
                offset = rule.absoluteValue;
            case INHERIT_PARENT:
                return value + offset + (int)(rule.fraction * size);
            case ABSOLUTE_VALUE:
                return rule.absoluteValue;
        }

        throw new IllegalArgumentException("Invalid type: "+rule.type);
    }

    /** {@link ValueRule} for left attribute of {@link BoundsRule} */
    public ValueRule mLeft;

    /** {@link ValueRule} for top attribute of {@link BoundsRule} */
    public ValueRule mTop;

    /** {@link ValueRule} for right attribute of {@link BoundsRule} */
    public ValueRule mRight;

    /** {@link ValueRule} for bottom attribute of {@link BoundsRule} */
    public ValueRule mBottom;
}
