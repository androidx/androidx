/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.graphics.Rect;
import android.os.Build;

/**
 * Describes a set of insets for window content.
 *
 * <p>WindowInsetsCompats are immutable and may be expanded to include more inset types in the
 * future. To adjust insets, use one of the supplied clone methods to obtain a new
 * WindowInsetsCompat instance with the adjusted properties.</p>
 */
public class WindowInsetsCompat {
    private interface WindowInsetsCompatImpl {
        int getSystemWindowInsetLeft(Object insets);
        int getSystemWindowInsetTop(Object insets);
        int getSystemWindowInsetRight(Object insets);
        int getSystemWindowInsetBottom(Object insets);
        boolean hasSystemWindowInsets(Object insets);
        boolean hasInsets(Object insets);
        boolean isConsumed(Object insets);
        boolean isRound(Object insets);
        WindowInsetsCompat consumeSystemWindowInsets(Object insets);
        WindowInsetsCompat replaceSystemWindowInsets(Object insets,
                int left, int top, int right, int bottom);
        WindowInsetsCompat replaceSystemWindowInsets(Object insets, Rect systemWindowInsets);
        int getStableInsetTop(Object insets);
        int getStableInsetLeft(Object insets);
        int getStableInsetRight(Object insets);
        int getStableInsetBottom(Object insets);
        boolean hasStableInsets(Object insets);
        WindowInsetsCompat consumeStableInsets(Object insets);
        Object getSourceWindowInsets(Object src);
    }

    private static class WindowInsetsCompatBaseImpl implements WindowInsetsCompatImpl {
        WindowInsetsCompatBaseImpl() {
        }

        @Override
        public int getSystemWindowInsetLeft(Object insets) {
            return 0;
        }

        @Override
        public int getSystemWindowInsetTop(Object insets) {
            return 0;
        }

        @Override
        public int getSystemWindowInsetRight(Object insets) {
            return 0;
        }

        @Override
        public int getSystemWindowInsetBottom(Object insets) {
            return 0;
        }

        @Override
        public boolean hasSystemWindowInsets(Object insets) {
            return false;
        }

        @Override
        public boolean hasInsets(Object insets) {
            return false;
        }

        @Override
        public boolean isConsumed(Object insets) {
            return false;
        }

        @Override
        public boolean isRound(Object insets) {
            return false;
        }

        @Override
        public WindowInsetsCompat consumeSystemWindowInsets(Object insets) {
            return null;
        }

        @Override
        public WindowInsetsCompat replaceSystemWindowInsets(Object insets, int left, int top, int right, int bottom) {
            return null;
        }

        @Override
        public WindowInsetsCompat replaceSystemWindowInsets(Object insets, Rect systemWindowInsets) {
            return null;
        }

        @Override
        public int getStableInsetTop(Object insets) {
            return 0;
        }

        @Override
        public int getStableInsetLeft(Object insets) {
            return 0;
        }

        @Override
        public int getStableInsetRight(Object insets) {
            return 0;
        }

        @Override
        public int getStableInsetBottom(Object insets) {
            return 0;
        }

        @Override
        public boolean hasStableInsets(Object insets) {
            return false;
        }

        @Override
        public WindowInsetsCompat consumeStableInsets(Object insets) {
            return null;
        }

        @Override
        public Object getSourceWindowInsets(Object src) {
            return null;
        }
    }

    private static class WindowInsetsCompatApi20Impl extends WindowInsetsCompatBaseImpl {
        WindowInsetsCompatApi20Impl() {
        }

        @Override
        public WindowInsetsCompat consumeSystemWindowInsets(Object insets) {
            return new WindowInsetsCompat(
                    WindowInsetsCompatApi20.consumeSystemWindowInsets(insets));
        }

        @Override
        public int getSystemWindowInsetBottom(Object insets) {
            return WindowInsetsCompatApi20.getSystemWindowInsetBottom(insets);
        }

        @Override
        public int getSystemWindowInsetLeft(Object insets) {
            return WindowInsetsCompatApi20.getSystemWindowInsetLeft(insets);
        }

        @Override
        public int getSystemWindowInsetRight(Object insets) {
            return WindowInsetsCompatApi20.getSystemWindowInsetRight(insets);
        }

        @Override
        public int getSystemWindowInsetTop(Object insets) {
            return WindowInsetsCompatApi20.getSystemWindowInsetTop(insets);
        }

        @Override
        public boolean hasInsets(Object insets) {
            return WindowInsetsCompatApi20.hasInsets(insets);
        }

        @Override
        public boolean hasSystemWindowInsets(Object insets) {
            return WindowInsetsCompatApi20.hasSystemWindowInsets(insets);
        }

        @Override
        public boolean isRound(Object insets) {
            return WindowInsetsCompatApi20.isRound(insets);
        }

        @Override
        public WindowInsetsCompat replaceSystemWindowInsets(Object insets, int left, int top,
                int right, int bottom) {
            return new WindowInsetsCompat(WindowInsetsCompatApi20.replaceSystemWindowInsets(insets,
                    left, top, right, bottom));
        }

        @Override
        public Object getSourceWindowInsets(Object src) {
            return WindowInsetsCompatApi20.getSourceWindowInsets(src);
        }
    }

    private static class WindowInsetsCompatApi21Impl extends WindowInsetsCompatApi20Impl {
        WindowInsetsCompatApi21Impl() {
        }

        @Override
        public WindowInsetsCompat consumeStableInsets(Object insets) {
            return new WindowInsetsCompat(WindowInsetsCompatApi21.consumeStableInsets(insets));
        }

        @Override
        public int getStableInsetBottom(Object insets) {
            return WindowInsetsCompatApi21.getStableInsetBottom(insets);
        }

        @Override
        public int getStableInsetLeft(Object insets) {
            return WindowInsetsCompatApi21.getStableInsetLeft(insets);
        }

        @Override
        public int getStableInsetRight(Object insets) {
            return WindowInsetsCompatApi21.getStableInsetRight(insets);
        }

        @Override
        public int getStableInsetTop(Object insets) {
            return WindowInsetsCompatApi21.getStableInsetTop(insets);
        }

        @Override
        public boolean hasStableInsets(Object insets) {
            return WindowInsetsCompatApi21.hasStableInsets(insets);
        }

        @Override
        public boolean isConsumed(Object insets) {
            return WindowInsetsCompatApi21.isConsumed(insets);
        }

        @Override
        public WindowInsetsCompat replaceSystemWindowInsets(Object insets,
                Rect systemWindowInsets) {
            return new WindowInsetsCompat(WindowInsetsCompatApi21.replaceSystemWindowInsets(insets,
                    systemWindowInsets));
        }
    }

    private static final WindowInsetsCompatImpl IMPL;
    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 21) {
            IMPL = new WindowInsetsCompatApi21Impl();
        } else if (version >= 20) {
            IMPL = new WindowInsetsCompatApi20Impl();
        } else {
            IMPL = new WindowInsetsCompatBaseImpl();
        }
    }

    private final Object mInsets;

    WindowInsetsCompat(Object insets) {
        mInsets = insets;
    }

    /**
     * Constructs a new WindowInsetsCompat, copying all values from a source WindowInsetsCompat.
     *
     * @param src source from which values are copied
     */
    public WindowInsetsCompat(WindowInsetsCompat src) {
        mInsets = src == null ? null : IMPL.getSourceWindowInsets(src.mInsets);
    }

    /**
     * Returns the left system window inset in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     * </p>
     *
     * @return The left system window inset
     */
    public int getSystemWindowInsetLeft() {
        return IMPL.getSystemWindowInsetLeft(mInsets);
    }

    /**
     * Returns the top system window inset in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     * </p>
     *
     * @return The top system window inset
     */
    public int getSystemWindowInsetTop() {
        return IMPL.getSystemWindowInsetTop(mInsets);
    }

    /**
     * Returns the right system window inset in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     * </p>
     *
     * @return The right system window inset
     */
    public int getSystemWindowInsetRight() {
        return IMPL.getSystemWindowInsetRight(mInsets);
    }

    /**
     * Returns the bottom system window inset in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     * </p>
     *
     * @return The bottom system window inset
     */
    public int getSystemWindowInsetBottom() {
        return IMPL.getSystemWindowInsetBottom(mInsets);
    }

    /**
     * Returns true if this WindowInsets has nonzero system window insets.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     * </p>
     *
     * @return true if any of the system window inset values are nonzero
     */
    public boolean hasSystemWindowInsets() {
        return IMPL.hasSystemWindowInsets(mInsets);
    }

    /**
     * Returns true if this WindowInsets has any nonzero insets.
     *
     * @return true if any inset values are nonzero
     */
    public boolean hasInsets() {
        return IMPL.hasInsets(mInsets);
    }

    /**
     * Check if these insets have been fully consumed.
     *
     * <p>Insets are considered "consumed" if the applicable <code>consume*</code> methods
     * have been called such that all insets have been set to zero. This affects propagation of
     * insets through the view hierarchy; insets that have not been fully consumed will continue
     * to propagate down to child views.</p>
     *
     * <p>The result of this method is equivalent to the return value of
     * {@link android.view.View#fitSystemWindows(android.graphics.Rect)}.</p>
     *
     * @return true if the insets have been fully consumed.
     */
    public boolean isConsumed() {
        return IMPL.isConsumed(mInsets);
    }

    /**
     * Returns true if the associated window has a round shape.
     *
     * <p>A round window's left, top, right and bottom edges reach all the way to the
     * associated edges of the window but the corners may not be visible. Views responding
     * to round insets should take care to not lay out critical elements within the corners
     * where they may not be accessible.</p>
     *
     * @return True if the window is round
     */
    public boolean isRound() {
        return IMPL.isRound(mInsets);
    }

    /**
     * Returns a copy of this WindowInsets with the system window insets fully consumed.
     *
     * @return A modified copy of this WindowInsets
     */
    public WindowInsetsCompat consumeSystemWindowInsets() {
        return IMPL.consumeSystemWindowInsets(mInsets);
    }

    /**
     * Returns a copy of this WindowInsets with selected system window insets replaced
     * with new values.
     *
     * @param left New left inset in pixels
     * @param top New top inset in pixels
     * @param right New right inset in pixels
     * @param bottom New bottom inset in pixels
     * @return A modified copy of this WindowInsets
     */
    public WindowInsetsCompat replaceSystemWindowInsets(int left, int top, int right, int bottom) {
        return IMPL.replaceSystemWindowInsets(mInsets, left, top, right, bottom);
    }

    /**
     * Returns a copy of this WindowInsets with selected system window insets replaced
     * with new values.
     *
     * @param systemWindowInsets New system window insets. Each field is the inset in pixels
     *                           for that edge
     * @return A modified copy of this WindowInsets
     */
    public WindowInsetsCompat replaceSystemWindowInsets(Rect systemWindowInsets) {
        return IMPL.replaceSystemWindowInsets(mInsets, systemWindowInsets);
    }

    /**
     * Returns the top stable inset in pixels.
     *
     * <p>The stable inset represents the area of a full-screen window that <b>may</b> be
     * partially or fully obscured by the system UI elements.  This value does not change
     * based on the visibility state of those elements; for example, if the status bar is
     * normally shown, but temporarily hidden, the stable inset will still provide the inset
     * associated with the status bar being shown.</p>
     *
     * @return The top stable inset
     */
    public int getStableInsetTop() {
        return IMPL.getStableInsetTop(mInsets);
    }


    /**
     * Returns the left stable inset in pixels.
     *
     * <p>The stable inset represents the area of a full-screen window that <b>may</b> be
     * partially or fully obscured by the system UI elements.  This value does not change
     * based on the visibility state of those elements; for example, if the status bar is
     * normally shown, but temporarily hidden, the stable inset will still provide the inset
     * associated with the status bar being shown.</p>
     *
     * @return The left stable inset
     */
    public int getStableInsetLeft() {
        return IMPL.getStableInsetLeft(mInsets);
    }

    /**
     * Returns the right stable inset in pixels.
     *
     * <p>The stable inset represents the area of a full-screen window that <b>may</b> be
     * partially or fully obscured by the system UI elements.  This value does not change
     * based on the visibility state of those elements; for example, if the status bar is
     * normally shown, but temporarily hidden, the stable inset will still provide the inset
     * associated with the status bar being shown.</p>
     *
     * @return The right stable inset
     */
    public int getStableInsetRight() {
        return IMPL.getStableInsetRight(mInsets);
    }


    /**
     * Returns the bottom stable inset in pixels.
     *
     * <p>The stable inset represents the area of a full-screen window that <b>may</b> be
     * partially or fully obscured by the system UI elements.  This value does not change
     * based on the visibility state of those elements; for example, if the status bar is
     * normally shown, but temporarily hidden, the stable inset will still provide the inset
     * associated with the status bar being shown.</p>
     *
     * @return The bottom stable inset
     */
    public int getStableInsetBottom() {
        return IMPL.getStableInsetBottom(mInsets);
    }

    /**
     * Returns true if this WindowInsets has nonzero stable insets.
     *
     * <p>The stable inset represents the area of a full-screen window that <b>may</b> be
     * partially or fully obscured by the system UI elements.  This value does not change
     * based on the visibility state of those elements; for example, if the status bar is
     * normally shown, but temporarily hidden, the stable inset will still provide the inset
     * associated with the status bar being shown.</p>
     *
     * @return true if any of the stable inset values are nonzero
     */
    public boolean hasStableInsets() {
        return IMPL.hasStableInsets(mInsets);
    }

    /**
     * Returns a copy of this WindowInsets with the stable insets fully consumed.
     *
     * @return A modified copy of this WindowInsetsCompat
     */
    public WindowInsetsCompat consumeStableInsets() {
        return IMPL.consumeStableInsets(mInsets);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WindowInsetsCompat other = (WindowInsetsCompat) o;
        return mInsets == null ? other.mInsets == null : mInsets.equals(other.mInsets);
    }

    @Override
    public int hashCode() {
        return mInsets == null ? 0 : mInsets.hashCode();
    }

    static WindowInsetsCompat wrap(Object insets) {
        return insets == null ? null : new WindowInsetsCompat(insets);
    }

    static Object unwrap(WindowInsetsCompat insets) {
        return insets == null ? null : insets.mInsets;
    }
}
