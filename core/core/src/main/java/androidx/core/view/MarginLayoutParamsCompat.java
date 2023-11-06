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

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Helper for accessing API features in
 * {@link ViewGroup.MarginLayoutParams MarginLayoutParams} in a backwards compatible
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
    public static int getMarginStart(@NonNull ViewGroup.MarginLayoutParams lp) {
        if (SDK_INT >= 17) {
            return Api17Impl.getMarginStart(lp);
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
    public static int getMarginEnd(@NonNull ViewGroup.MarginLayoutParams lp) {
        if (SDK_INT >= 17) {
            return Api17Impl.getMarginEnd(lp);
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
    public static void setMarginStart(@NonNull ViewGroup.MarginLayoutParams lp, int marginStart) {
        if (SDK_INT >= 17) {
            Api17Impl.setMarginStart(lp, marginStart);
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
    public static void setMarginEnd(@NonNull ViewGroup.MarginLayoutParams lp, int marginEnd) {
        if (SDK_INT >= 17) {
            Api17Impl.setMarginEnd(lp, marginEnd);
        } else {
            lp.rightMargin = marginEnd;
        }
    }

    /**
     * Check if margins are relative.
     *
     * @return true if either marginStart or marginEnd has been set.
     */
    public static boolean isMarginRelative(@NonNull ViewGroup.MarginLayoutParams lp) {
        if (SDK_INT >= 17) {
            return Api17Impl.isMarginRelative(lp);
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
    public static int getLayoutDirection(@NonNull ViewGroup.MarginLayoutParams lp) {
        int result;
        if (SDK_INT >= 17) {
            result = Api17Impl.getLayoutDirection(lp);
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
     * @param lp LayoutParameters for which to set the layout direction.
     * @param layoutDirection the layout direction.
     *        Should be either {@link ViewCompat#LAYOUT_DIRECTION_LTR}
     *                     or {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     */
    public static void setLayoutDirection(@NonNull ViewGroup.MarginLayoutParams lp,
            int layoutDirection) {
        if (SDK_INT >= 17) {
            Api17Impl.setLayoutDirection(lp, layoutDirection);
        }
    }

    /**
     * This will be called by {@link View#requestLayout()}. Left and Right margins
     * may be overridden depending on layout direction.
     */
    public static void resolveLayoutDirection(@NonNull ViewGroup.MarginLayoutParams lp,
            int layoutDirection) {
        if (SDK_INT >= 17) {
            Api17Impl.resolveLayoutDirection(lp, layoutDirection);
        }
    }

    private MarginLayoutParamsCompat() {
    }

    @RequiresApi(17)
    static class Api17Impl {
        private Api17Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static int getMarginStart(ViewGroup.MarginLayoutParams marginLayoutParams) {
            return marginLayoutParams.getMarginStart();
        }

        @DoNotInline
        static int getMarginEnd(ViewGroup.MarginLayoutParams marginLayoutParams) {
            return marginLayoutParams.getMarginEnd();
        }

        @DoNotInline
        static void setMarginStart(ViewGroup.MarginLayoutParams marginLayoutParams, int start) {
            marginLayoutParams.setMarginStart(start);
        }

        @DoNotInline
        static void setMarginEnd(ViewGroup.MarginLayoutParams marginLayoutParams, int end) {
            marginLayoutParams.setMarginEnd(end);
        }

        @DoNotInline
        static boolean isMarginRelative(ViewGroup.MarginLayoutParams marginLayoutParams) {
            return marginLayoutParams.isMarginRelative();
        }

        @DoNotInline
        static int getLayoutDirection(ViewGroup.MarginLayoutParams marginLayoutParams) {
            return marginLayoutParams.getLayoutDirection();
        }

        @DoNotInline
        static void setLayoutDirection(ViewGroup.MarginLayoutParams marginLayoutParams,
                int layoutDirection) {
            marginLayoutParams.setLayoutDirection(layoutDirection);
        }

        @DoNotInline
        static void resolveLayoutDirection(ViewGroup.MarginLayoutParams marginLayoutParams,
                int layoutDirection) {
            marginLayoutParams.resolveLayoutDirection(layoutDirection);
        }
    }
}
