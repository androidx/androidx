/*
 * Copyright (C) 2013 The Android Open Source Project
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


package android.support.v4.view;

import android.os.Build;
import android.view.ViewGroup;

/**
 * Helper for accessing API features in
 * {@link android.view.ViewGroup.MarginLayoutParams MarginLayoutParams} added after API 4.
 */
public class MarginLayoutParamsCompat {
    interface MarginLayoutParamsCompatImpl {
        int getMarginStart(ViewGroup.MarginLayoutParams lp);
        int getMarginEnd(ViewGroup.MarginLayoutParams lp);
        void setMarginStart(ViewGroup.MarginLayoutParams lp, int marginStart);
        void setMarginEnd(ViewGroup.MarginLayoutParams lp, int marginEnd);
        boolean isMarginRelative(ViewGroup.MarginLayoutParams lp);
        int getLayoutDirection(ViewGroup.MarginLayoutParams lp);
        void setLayoutDirection(ViewGroup.MarginLayoutParams lp, int layoutDirection);
        void resolveLayoutDirection(ViewGroup.MarginLayoutParams lp, int layoutDirection);
    }

    static class MarginLayoutParamsCompatImplBase implements MarginLayoutParamsCompatImpl {

        @Override
        public int getMarginStart(ViewGroup.MarginLayoutParams lp) {
            return lp.leftMargin;
        }

        @Override
        public int getMarginEnd(ViewGroup.MarginLayoutParams lp) {
            return lp.rightMargin;
        }

        @Override
        public void setMarginStart(ViewGroup.MarginLayoutParams lp, int marginStart) {
            lp.leftMargin = marginStart;
        }

        @Override
        public void setMarginEnd(ViewGroup.MarginLayoutParams lp, int marginEnd) {
            lp.rightMargin = marginEnd;
        }

        @Override
        public boolean isMarginRelative(ViewGroup.MarginLayoutParams lp) {
            return false;
        }

        @Override
        public int getLayoutDirection(ViewGroup.MarginLayoutParams lp) {
            return ViewCompat.LAYOUT_DIRECTION_LTR;
        }

        @Override
        public void setLayoutDirection(ViewGroup.MarginLayoutParams lp, int layoutDirection) {
            // No-op
        }

        @Override
        public void resolveLayoutDirection(ViewGroup.MarginLayoutParams lp, int layoutDirection) {
            // No-op
        }
    }

    static class MarginLayoutParamsCompatImplJbMr1 implements MarginLayoutParamsCompatImpl {

        @Override
        public int getMarginStart(ViewGroup.MarginLayoutParams lp) {
            return MarginLayoutParamsCompatJellybeanMr1.getMarginStart(lp);
        }

        @Override
        public int getMarginEnd(ViewGroup.MarginLayoutParams lp) {
            return MarginLayoutParamsCompatJellybeanMr1.getMarginEnd(lp);
        }

        @Override
        public void setMarginStart(ViewGroup.MarginLayoutParams lp, int marginStart) {
            MarginLayoutParamsCompatJellybeanMr1.setMarginStart(lp, marginStart);
        }

        @Override
        public void setMarginEnd(ViewGroup.MarginLayoutParams lp, int marginEnd) {
            MarginLayoutParamsCompatJellybeanMr1.setMarginEnd(lp, marginEnd);
        }

        @Override
        public boolean isMarginRelative(ViewGroup.MarginLayoutParams lp) {
            return MarginLayoutParamsCompatJellybeanMr1.isMarginRelative(lp);
        }

        @Override
        public int getLayoutDirection(ViewGroup.MarginLayoutParams lp) {
            return MarginLayoutParamsCompatJellybeanMr1.getLayoutDirection(lp);
        }

        @Override
        public void setLayoutDirection(ViewGroup.MarginLayoutParams lp, int layoutDirection) {
            MarginLayoutParamsCompatJellybeanMr1.setLayoutDirection(lp, layoutDirection);
        }

        @Override
        public void resolveLayoutDirection(ViewGroup.MarginLayoutParams lp, int layoutDirection) {
            MarginLayoutParamsCompatJellybeanMr1.resolveLayoutDirection(lp, layoutDirection);
        }
    }

    static final MarginLayoutParamsCompatImpl IMPL;
    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 17) { // jb-mr1
            IMPL = new MarginLayoutParamsCompatImplJbMr1();
        } else {
            IMPL = new MarginLayoutParamsCompatImplBase();
        }
    }

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
        return IMPL.getMarginStart(lp);
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
        return IMPL.getMarginEnd(lp);
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
        IMPL.setMarginStart(lp, marginStart);
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
        IMPL.setMarginEnd(lp, marginEnd);
    }

    /**
     * Check if margins are relative.
     *
     * @return true if either marginStart or marginEnd has been set.
     */
    public static boolean isMarginRelative(ViewGroup.MarginLayoutParams lp) {
        return IMPL.isMarginRelative(lp);
    }

    /**
     * Retuns the layout direction. Can be either {@link ViewCompat#LAYOUT_DIRECTION_LTR} or
     * {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     *
     * @return the layout direction.
     */
    public static int getLayoutDirection(ViewGroup.MarginLayoutParams lp) {
        return IMPL.getLayoutDirection(lp);
    }

    /**
     * Set the layout direction.
     *
     * @param layoutDirection the layout direction.
     *        Should be either {@link ViewCompat#LAYOUT_DIRECTION_LTR}
     *                     or {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     */
    public static void setLayoutDirection(ViewGroup.MarginLayoutParams lp, int layoutDirection) {
        IMPL.setLayoutDirection(lp, layoutDirection);
    }

    /**
     * This will be called by {@link android.view.View#requestLayout()}. Left and Right margins
     * may be overridden depending on layout direction.
     */
    public static void resolveLayoutDirection(ViewGroup.MarginLayoutParams lp,
            int layoutDirection) {
        IMPL.resolveLayoutDirection(lp, layoutDirection);
    }
}
