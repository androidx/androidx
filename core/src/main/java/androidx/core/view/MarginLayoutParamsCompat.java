/*
 * Copyright 2018 The Android Open Source Project
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


package androidx.core.view;

import static android.os.Build.VERSION.SDK_INT;

import android.view.ViewGroup;

/**
 * Helper for accessing API features in
 * {@link android.view.ViewGroup.MarginLayoutParams MarginLayoutParams} in a backwards compatible
 * way.
 */
public final class MarginLayoutParamsCompat {
    /**
     * Get the relative starting margin that was set.
     *
     * <p>On platform versions supporting bidirectional text and layouts
     * this value will be resolved into the LayoutParams object's left or right
     * margin as appropriate when the associated View is attached to a window
     * or when the layout direction of that view changes.</p>
     *
     * @param lp LayoutParams to query
     * @return the margin along the starting edge in pixels
     */
    public static int getMarginStart(ViewGroup.MarginLayoutParams lp) {
        if (SDK_INT >= 17) {
            return lp.getMarginStart();
        } else {
            return lp.leftMargin;
        }
    }

    /**
     * Get the relative ending margin that was set.
     *
     * <p>On platform versions supporting bidirectional text and layouts
     * this value will be resolved into the LayoutParams object's left or right
     * margin as appropriate when the associated View is attached to a window
     * or when the layout direction of that view changes.</p>
     *
     * @param lp LayoutParams to query
     * @return the margin along the ending edge in pixels
     */
    public static int getMarginEnd(ViewGroup.MarginLayoutParams lp) {
        if (SDK_INT >= 17) {
            return lp.getMarginEnd();
        } else {
            return lp.rightMargin;
        }
    }

    /**
     * Set the relative start margin.
     *
     * <p>On platform versions supporting bidirectional text and layouts
     * this value will be resolved into the LayoutParams object's left or right
     * margin as appropriate when the associated View is attached to a window
     * or when the layout direction of that view changes.</p>
     *
     * @param lp LayoutParams to query
     * @param marginStart the desired start margin in pixels
     */
    public static void setMarginStart(ViewGroup.MarginLayoutParams lp, int marginStart) {
        if (SDK_INT >= 17) {
            lp.setMarginStart(marginStart);
        } else {
            lp.leftMargin = marginStart;
        }
    }

    /**
     * Set the relative end margin.
     *
     * <p>On platform versions supporting bidirectional text and layouts
     * this value will be resolved into the LayoutParams object's left or right
     * margin as appropriate when the associated View is attached to a window
     * or when the layout direction of that view changes.</p>
     *
     * @param lp LayoutParams to query
     * @param marginEnd the desired end margin in pixels
     */
    public static void setMarginEnd(ViewGroup.MarginLayoutParams lp, int marginEnd) {
        if (SDK_INT >= 17) {
            lp.setMarginEnd(marginEnd);
        } else {
            lp.rightMargin = marginEnd;
        }
    }

    /**
     * Check if margins are relative.
     *
     * @return true if either marginStart or marginEnd has been set.
     */
    public static boolean isMarginRelative(ViewGroup.MarginLayoutParams lp) {
        if (SDK_INT >= 17) {
            return lp.isMarginRelative();
        } else {
            return false;
        }
    }

    /**
     * Returns the layout direction. Can be either {@link ViewCompat#LAYOUT_DIRECTION_LTR} or
     * {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     *
     * @return the layout direction.
     */
    public static int getLayoutDirection(ViewGroup.MarginLayoutParams lp) {
        int result;
        if (SDK_INT >= 17) {
            result = lp.getLayoutDirection();
        } else {
            result = ViewCompat.LAYOUT_DIRECTION_LTR;
        }

        if ((result != ViewCompat.LAYOUT_DIRECTION_LTR)
                && (result != ViewCompat.LAYOUT_DIRECTION_RTL)) {
            // This can happen on older platform releases where the default (unset) layout direction
            // is -1
            result = ViewCompat.LAYOUT_DIRECTION_LTR;
        }
        return result;
    }

    /**
     * Set the layout direction.
     *
     * @param layoutDirection the layout direction.
     *        Should be either {@link ViewCompat#LAYOUT_DIRECTION_LTR}
     *                     or {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     */
    public static void setLayoutDirection(ViewGroup.MarginLayoutParams lp, int layoutDirection) {
        if (SDK_INT >= 17) {
            lp.setLayoutDirection(layoutDirection);
        }
    }

    /**
     * This will be called by {@link android.view.View#requestLayout()}. Left and Right margins
     * may be overridden depending on layout direction.
     */
    public static void resolveLayoutDirection(ViewGroup.MarginLayoutParams lp,
            int layoutDirection) {
        if (SDK_INT >= 17) {
            lp.resolveLayoutDirection(layoutDirection);
        }
    }

    private MarginLayoutParamsCompat() {}
}
