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
 * This class contains the rules for updating the bounds of a {@link RegionDrawable}. It contains
 * four rules, one for each offset of the rectangular bound - left/top/right/bottom.
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
    public static class ValueRule {
        private final int type;
        private final float fraction;
        private final int absoluteValue;

        ValueRule(int type, int absoluteValue, float fraction) {
            this.type = type;
            this.absoluteValue = absoluteValue;
            this.fraction = fraction;
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
        result.left = calculate(rect.left, left);
        result.top = calculate(rect.top, top);
        result.right = calculate(rect.right, right);
        result.bottom = calculate(rect.bottom, bottom);
    }

    private static int calculate(int value, ValueRule rule) {
        if (rule == null) {
            return value;
        }
        switch (rule.type) {
            case INHERIT_PARENT:
                return (int) (rule.fraction * value);
            case ABSOLUTE_VALUE:
                return rule.absoluteValue;
            case INHERIT_WITH_OFFSET:
                return (int) (rule.absoluteValue + rule.fraction * value);
        }

        throw new IllegalArgumentException("Invalid ValueRule");
    }

    /** {@link ValueRule} for left attribute of {@link BoundsRule} */
    public ValueRule left;

    /** {@link ValueRule} for top attribute of {@link BoundsRule} */
    public ValueRule top;

    /** {@link ValueRule} for right attribute of {@link BoundsRule} */
    public ValueRule right;

    /** {@link ValueRule} for bottom attribute of {@link BoundsRule} */
    public ValueRule bottom;
}
