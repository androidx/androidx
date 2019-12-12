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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.Insets;
import androidx.core.util.ObjectsCompat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Describes a set of insets for window content.
 *
 * <p>WindowInsetsCompats are immutable and may be expanded to include more inset types in the
 * future. To adjust insets, use one of the supplied clone methods to obtain a new
 * WindowInsetsCompat instance with the adjusted properties.</p>
 */
public class WindowInsetsCompat {
    private static final String TAG = "WindowInsetsCompat";
    private final Object mInsets;

    private Insets mSystemWindowInsets;
    private Insets mStableInsets;
    private Insets mSystemGestureInsets;
    private Insets mMandatorySystemGestureInsets;
    private Insets mTappableElementInsets;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    WindowInsetsCompat(@Nullable Object insets) {
        mInsets = insets;
    }

    /**
     * Constructs a new WindowInsetsCompat, copying all values from a source WindowInsetsCompat.
     *
     * @param src source from which values are copied
     */
    public WindowInsetsCompat(@Nullable WindowInsetsCompat src) {
        if (SDK_INT >= 20) {
            mInsets = src == null ? null : new WindowInsets((WindowInsets) src.mInsets);
        } else {
            mInsets = null;
        }
    }

    /**
     * Returns the left system window inset in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code 0}.
     *
     * @return The left system window inset
     */
    public int getSystemWindowInsetLeft() {
        if (SDK_INT >= 20) {
            return ((WindowInsets) mInsets).getSystemWindowInsetLeft();
        } else {
            return 0;
        }
    }

    /**
     * Returns the top system window inset in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code 0}.
     *
     * @return The top system window inset
     */
    public int getSystemWindowInsetTop() {
        if (SDK_INT >= 20) {
            return ((WindowInsets) mInsets).getSystemWindowInsetTop();
        } else {
            return 0;
        }
    }

    /**
     * Returns the right system window inset in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code 0}.
     *
     * @return The right system window inset
     */
    public int getSystemWindowInsetRight() {
        if (SDK_INT >= 20) {
            return ((WindowInsets) mInsets).getSystemWindowInsetRight();
        } else {
            return 0;
        }
    }

    /**
     * Returns the bottom system window inset in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code 0}.
     *
     * @return The bottom system window inset
     */
    public int getSystemWindowInsetBottom() {
        if (SDK_INT >= 20) {
            return ((WindowInsets) mInsets).getSystemWindowInsetBottom();
        } else {
            return 0;
        }
    }

    /**
     * Returns true if this WindowInsets has nonzero system window insets.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code false}.
     *
     * @return true if any of the system window inset values are nonzero
     */
    public boolean hasSystemWindowInsets() {
        if (SDK_INT >= 20) {
            return ((WindowInsets) mInsets).hasSystemWindowInsets();
        } else {
            return false;
        }
    }

    /**
     * Returns true if this WindowInsets has any non-zero insets.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code false}.
     *
     * @return true if any inset values are nonzero
     */
    public boolean hasInsets() {
        if (SDK_INT >= 20) {
            return ((WindowInsets) mInsets).hasInsets();
        } else {
            return false;
        }
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
        if (SDK_INT >= 21) {
            return ((WindowInsets) mInsets).isConsumed();
        } else {
            return false;
        }
    }

    /**
     * Returns true if the associated window has a round shape.
     *
     * <p>A round window's left, top, right and bottom edges reach all the way to the
     * associated edges of the window but the corners may not be visible. Views responding
     * to round insets should take care to not lay out critical elements within the corners
     * where they may not be accessible.</p>
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code false}.
     *
     * @return true if the window is round
     */
    public boolean isRound() {
        if (SDK_INT >= 20) {
            return ((WindowInsets) mInsets).isRound();
        } else {
            return false;
        }
    }

    /**
     * Returns a copy of this WindowInsets with the system window insets fully consumed.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code null}.
     *
     * @return A modified copy of this WindowInsets
     */
    @Nullable
    public WindowInsetsCompat consumeSystemWindowInsets() {
        if (SDK_INT >= 20) {
            return new WindowInsetsCompat(((WindowInsets) mInsets).consumeSystemWindowInsets());
        } else {
            return null;
        }
    }

    /**
     * Returns a copy of this WindowInsets with selected system window insets replaced
     * with new values.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code null}.
     *
     * @param left New left inset in pixels
     * @param top New top inset in pixels
     * @param right New right inset in pixels
     * @param bottom New bottom inset in pixels
     * @return A modified copy of this WindowInsets
     *
     * @deprecated use {@link WindowInsetsCompat.Builder} with
     * {@link WindowInsetsCompat.Builder#setSystemWindowInsets(Insets)} instead.
     */
    @Deprecated
    @Nullable
    public WindowInsetsCompat replaceSystemWindowInsets(int left, int top, int right, int bottom) {
        if (SDK_INT >= 20) {
            return new WindowInsetsCompat(((WindowInsets) mInsets)
                    .replaceSystemWindowInsets(left, top, right, bottom));
        } else {
            return null;
        }
    }

    /**
     * Returns a copy of this WindowInsets with selected system window insets replaced
     * with new values.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code null}.
     *
     * @param systemWindowInsets New system window insets. Each field is the inset in pixels
     *                           for that edge
     * @return A modified copy of this WindowInsets
     *
     * @deprecated use {@link WindowInsetsCompat.Builder} with
     * {@link WindowInsetsCompat.Builder#setSystemWindowInsets(Insets)} instead.
     */
    @Deprecated
    @Nullable
    public WindowInsetsCompat replaceSystemWindowInsets(@NonNull Rect systemWindowInsets) {
        if (SDK_INT >= 20) {
            return replaceSystemWindowInsets(systemWindowInsets.left, systemWindowInsets.top,
                    systemWindowInsets.right, systemWindowInsets.bottom);
        } else {
            return null;
        }
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
     * <p>When running on platforms with API 20 and below, this method always returns {@code 0}.
     *
     * @return The top stable inset
     */
    public int getStableInsetTop() {
        if (SDK_INT >= 21) {
            return ((WindowInsets) mInsets).getStableInsetTop();
        } else {
            return 0;
        }
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
     * <p>When running on platforms with API 20 and below, this method always returns {@code 0}.
     *
     * @return The left stable inset
     */
    public int getStableInsetLeft() {
        if (SDK_INT >= 21) {
            return ((WindowInsets) mInsets).getStableInsetLeft();
        } else {
            return 0;
        }
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
     * <p>When running on platforms with API 20 and below, this method always returns {@code 0}.
     *
     * @return The right stable inset
     */
    public int getStableInsetRight() {
        if (SDK_INT >= 21) {
            return ((WindowInsets) mInsets).getStableInsetRight();
        } else {
            return 0;
        }
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
     * <p>When running on platforms with API 20 and below, this method always returns {@code 0}.
     *
     * @return The bottom stable inset
     */
    public int getStableInsetBottom() {
        if (SDK_INT >= 21) {
            return ((WindowInsets) mInsets).getStableInsetBottom();
        } else {
            return 0;
        }
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
     * <p>When running on platforms with API 20 and below, this method always returns {@code false}.
     *
     * @return true if any of the stable inset values are nonzero
     */
    public boolean hasStableInsets() {
        if (SDK_INT >= 21) {
            return ((WindowInsets) mInsets).hasStableInsets();
        } else {
            return false;
        }
    }

    /**
     * Returns a copy of this WindowInsets with the stable insets fully consumed.
     *
     * <p>When running on platforms with API 20 and below, this method always returns {@code null}.
     *
     * @return A modified copy of this WindowInsetsCompat
     */
    @Nullable
    public WindowInsetsCompat consumeStableInsets() {
        if (SDK_INT >= 21) {
            return new WindowInsetsCompat(((WindowInsets) mInsets).consumeStableInsets());
        } else {
            return null;
        }
    }

    /**
     * Returns the display cutout if there is one.
     *
     * <p>When running on platforms with API 27 and below, this method always returns {@code null}.
     *
     * @return the display cutout or null if there is none
     * @see DisplayCutoutCompat
     */
    @Nullable
    public DisplayCutoutCompat getDisplayCutout() {
        if (SDK_INT >= 28) {
            return DisplayCutoutCompat.wrap(((WindowInsets) mInsets).getDisplayCutout());
        } else {
            return null;
        }
    }

    /**
     * Returns a copy of this WindowInsets with the cutout fully consumed.
     *
     * <p>When running on platforms with API 27 and below, this method is a no-op.
     *
     * @return A modified copy of this WindowInsets
     */
    @Nullable
    public WindowInsetsCompat consumeDisplayCutout() {
        if (SDK_INT >= 28) {
            return new WindowInsetsCompat(((WindowInsets) mInsets).consumeDisplayCutout());
        } else {
            return this;
        }
    }

    /**
     * Returns the system window insets in pixels.
     *
     * <p>The system window inset represents the area of a full-screen window that is
     * partially or fully obscured by the status bar, navigation bar, IME or other system windows.
     * </p>
     *
     * @return The system window insets
     * @see #getSystemWindowInsetLeft()
     * @see #getSystemWindowInsetTop()
     * @see #getSystemWindowInsetRight()
     * @see #getSystemWindowInsetBottom()
     */
    @NonNull
    public Insets getSystemWindowInsets() {
        if (mSystemWindowInsets == null) {
            if (Build.VERSION.SDK_INT >= 29) {
                mSystemWindowInsets = Insets.toCompatInsets(
                        ((WindowInsets) mInsets).getSystemWindowInsets());
            } else {
                // Else we'll create a copy from the getters
                mSystemWindowInsets = Insets.of(getSystemWindowInsetLeft(),
                        getSystemWindowInsetTop(), getSystemWindowInsetRight(),
                        getSystemWindowInsetBottom());
            }
        }
        return mSystemWindowInsets;
    }

    /**
     * Returns the stable insets in pixels.
     *
     * <p>The stable inset represents the area of a full-screen window that <b>may</b> be
     * partially or fully obscured by the system UI elements.  This value does not change
     * based on the visibility state of those elements; for example, if the status bar is
     * normally shown, but temporarily hidden, the stable inset will still provide the inset
     * associated with the status bar being shown.</p>
     *
     * @return The stable insets
     * @see #getStableInsetLeft()
     * @see #getStableInsetTop()
     * @see #getStableInsetRight()
     * @see #getStableInsetBottom()
     */
    @NonNull
    public Insets getStableInsets() {
        if (mStableInsets == null) {
            if (Build.VERSION.SDK_INT >= 29) {
                mStableInsets = Insets.toCompatInsets(((WindowInsets) mInsets).getStableInsets());
            } else {
                // Else we'll create a copy from the getters
                mStableInsets = Insets.of(getStableInsetLeft(), getStableInsetTop(),
                        getStableInsetRight(), getStableInsetBottom());
            }
        }
        return mStableInsets;
    }

    /**
     * Returns the mandatory system gesture insets.
     *
     * <p>The mandatory system gesture insets represent the area of a window where mandatory system
     * gestures have priority and may consume some or all touch input, e.g. due to the a system bar
     * occupying it, or it being reserved for touch-only gestures.
     *
     * @see WindowInsets#getMandatorySystemGestureInsets
     */
    @NonNull
    public Insets getMandatorySystemGestureInsets() {
        if (mMandatorySystemGestureInsets == null) {
            if (Build.VERSION.SDK_INT >= 29) {
                mMandatorySystemGestureInsets = Insets.toCompatInsets(
                        ((WindowInsets) mInsets).getMandatorySystemGestureInsets());
            } else {
                // Before API 29, the mandatory system gesture insets == system window insets
                mMandatorySystemGestureInsets = getSystemWindowInsets();
            }
        }
        return mMandatorySystemGestureInsets;
    }

    /**
     * Returns the tappable element insets.
     *
     * <p>The tappable element insets represent how much tappable elements <b>must at least</b> be
     * inset to remain both tappable and visually unobstructed by persistent system windows.
     *
     * <p>This may be smaller than {@link #getSystemWindowInsets()} if the system window is
     * largely transparent and lets through simple taps (but not necessarily more complex gestures).
     *
     * @see WindowInsets#getTappableElementInsets
     */
    @NonNull
    public Insets getTappableElementInsets() {
        if (mTappableElementInsets == null) {
            if (Build.VERSION.SDK_INT >= 29) {
                mTappableElementInsets = Insets.toCompatInsets(
                        ((WindowInsets) mInsets).getTappableElementInsets());
            } else {
                // Before API 29,, the tappable elements insets == system window insets
                mTappableElementInsets = getSystemWindowInsets();
            }
        }
        return mTappableElementInsets;
    }

    /**
     * Returns the system gesture insets.
     *
     * <p>The system gesture insets represent the area of a window where system gestures have
     * priority and may consume some or all touch input, e.g. due to the a system bar
     * occupying it, or it being reserved for touch-only gestures.
     *
     * <p>An app can declare priority over system gestures with
     * {@link android.view.View#setSystemGestureExclusionRects} outside of the
     * {@link #getMandatorySystemGestureInsets() mandatory system gesture insets}.
     *
     * @see WindowInsets#getSystemGestureInsets
     */
    @NonNull
    public Insets getSystemGestureInsets() {
        if (mSystemGestureInsets == null) {
            if (Build.VERSION.SDK_INT >= 29) {
                mSystemGestureInsets = Insets.toCompatInsets(
                        ((WindowInsets) mInsets).getSystemGestureInsets());
            } else {
                // Before API 29,, the system gesture insets == system window insets
                mSystemGestureInsets = getSystemWindowInsets();
            }
        }
        return mSystemGestureInsets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WindowInsetsCompat)) {
            return false;
        }
        WindowInsetsCompat other = (WindowInsetsCompat) o;
        return ObjectsCompat.equals(mInsets, other.mInsets);
    }

    @Override
    public int hashCode() {
        return mInsets == null ? 0 : mInsets.hashCode();
    }

    /**
     * Return the source {@link WindowInsets} instance used in this {@link WindowInsetsCompat}.
     *
     * @return the wrapped WindowInsets instance
     */
    @Nullable
    @RequiresApi(20)
    public WindowInsets toWindowInsets() {
        return (WindowInsets) mInsets;
    }

    /**
     * Wrap an instance of {@link WindowInsets} into a {@link WindowInsetsCompat}.
     *
     * @param insets source insets to wrap
     * @return the wrapped instance
     */
    @NonNull
    @RequiresApi(20)
    public static WindowInsetsCompat toWindowInsetsCompat(@NonNull WindowInsets insets) {
        return new WindowInsetsCompat(Objects.requireNonNull(insets));
    }

    /**
     * Builder for {@link WindowInsetsCompat}.
     */
    public static final class Builder {
        private final BuilderImpl mImpl;

        /**
         * Creates a builder where all insets are initially consumed.
         */
        public Builder() {
            if (SDK_INT >= 29) {
                mImpl = new BuilderImpl29();
            } else if (SDK_INT >= 20) {
                mImpl = new BuilderImpl20();
            } else {
                mImpl = new BuilderImpl();
            }
        }

        /**
         * Creates a builder where all insets are initialized from {@link WindowInsetsCompat}.
         *
         * @param insets the instance to initialize from.
         */
        public Builder(@NonNull WindowInsetsCompat insets) {
            if (SDK_INT >= 29) {
                mImpl = new BuilderImpl29(insets);
            } else if (SDK_INT >= 20) {
                mImpl = new BuilderImpl20(insets);
            } else {
                mImpl = new BuilderImpl(insets);
            }
        }

        /**
         * Sets system window insets in pixels.
         *
         * <p>The system window inset represents the area of a full-screen window that is
         * partially or fully obscured by the status bar, navigation bar, IME or other system
         * windows.</p>
         *
         * @see #getSystemWindowInsets()
         * @return itself
         */
        @NonNull
        public Builder setSystemWindowInsets(@NonNull Insets insets) {
            mImpl.setSystemWindowInsets(insets);
            return this;
        }

        /**
         * Sets system gesture insets in pixels.
         *
         * <p>The system gesture insets represent the area of a window where system gestures have
         * priority and may consume some or all touch input, e.g. due to the a system bar
         * occupying it, or it being reserved for touch-only gestures.
         *
         * <p>The insets passed will only take effect when running on API 29 and above.
         *
         * @see #getSystemGestureInsets()
         * @return itself
         */
        @NonNull
        public Builder setSystemGestureInsets(@NonNull Insets insets) {
            mImpl.setSystemGestureInsets(insets);
            return this;
        }

        /**
         * Sets mandatory system gesture insets in pixels.
         *
         * <p>The mandatory system gesture insets represent the area of a window where mandatory
         * system gestures have priority and may consume some or all touch input, e.g. due to the a
         * system bar occupying it, or it being reserved for touch-only gestures.
         *
         * <p>In contrast to {@link #setSystemGestureInsets regular system gestures},
         * <b>mandatory</b> system gestures cannot be overridden by
         * {@link ViewCompat#setSystemGestureExclusionRects}.
         *
         * <p>The insets passed will only take effect when running on API 29 and above.
         *
         * @see #getMandatorySystemGestureInsets()
         * @return itself
         */
        @NonNull
        public Builder setMandatorySystemGestureInsets(@NonNull Insets insets) {
            mImpl.setMandatorySystemGestureInsets(insets);
            return this;
        }

        /**
         * Sets tappable element insets in pixels.
         *
         * <p>The tappable element insets represent how much tappable elements <b>must at least</b>
         * be inset to remain both tappable and visually unobstructed by persistent system windows.
         *
         * <p>The insets passed will only take effect when running on API 29 and above.
         *
         * @see #getTappableElementInsets()
         * @return itself
         */
        @NonNull
        public Builder setTappableElementInsets(@NonNull Insets insets) {
            mImpl.setTappableElementInsets(insets);
            return this;
        }

        /**
         * Sets the stable insets in pixels.
         *
         * <p>The stable inset represents the area of a full-screen window that <b>may</b> be
         * partially or fully obscured by the system UI elements.  This value does not change
         * based on the visibility state of those elements; for example, if the status bar is
         * normally shown, but temporarily hidden, the stable inset will still provide the inset
         * associated with the status bar being shown.</p>
         *
         * <p>The insets passed will only take effect when running on API 29 and above.
         *
         * @see #getStableInsets()
         * @return itself
         */
        @NonNull
        public Builder setStableInsets(@NonNull Insets insets) {
            mImpl.setStableInsets(insets);
            return this;
        }

        /**
         * Sets the display cutout.
         *
         * <p>The cutout passed will only take effect when running on API 29 and above.
         *
         * @see #getDisplayCutout()
         * @param displayCutout the display cutout or null if there is none
         * @return itself
         */
        @NonNull
        public Builder setDisplayCutout(@Nullable DisplayCutoutCompat displayCutout) {
            mImpl.setDisplayCutout(displayCutout);
            return this;
        }

        /**
         * Builds a {@link WindowInsetsCompat} instance.
         *
         * @return the {@link WindowInsetsCompat} instance.
         */
        @NonNull
        public WindowInsetsCompat build() {
            return mImpl.build();
        }
    }

    private static class BuilderImpl {
        private WindowInsetsCompat mInsets;

        BuilderImpl() {
            mInsets = new WindowInsetsCompat(null);
        }

        BuilderImpl(@NonNull WindowInsetsCompat insets) {
            mInsets = insets;
        }

        public void setSystemWindowInsets(@NonNull Insets insets) {}

        public void setSystemGestureInsets(@NonNull Insets insets) {}

        public void setMandatorySystemGestureInsets(@NonNull Insets insets) {}

        public void setTappableElementInsets(@NonNull Insets insets) {}

        public void setStableInsets(@NonNull Insets insets) {}

        public void setDisplayCutout(@Nullable DisplayCutoutCompat displayCutout) {}

        @NonNull
        public WindowInsetsCompat build() {
            return mInsets;
        }
    }

    @RequiresApi(api = 20)
    private static class BuilderImpl20 extends BuilderImpl {
        private static Field sConsumedField;
        private static boolean sConsumedFieldFetched = false;

        private static Constructor<WindowInsets> sConstructor;
        private static boolean sConstructorFetched = false;

        private WindowInsets mInsets;

        BuilderImpl20() {
            mInsets = createWindowInsetsInstance();
        }

        BuilderImpl20(@NonNull WindowInsetsCompat insets) {
            mInsets = insets.toWindowInsets();
        }

        @Override
        public void setSystemWindowInsets(@NonNull Insets insets) {
            if (mInsets != null) {
                mInsets = mInsets.replaceSystemWindowInsets(
                        insets.left, insets.top, insets.right, insets.bottom);
            }
        }

        @Override
        @NonNull
        public WindowInsetsCompat build() {
            return WindowInsetsCompat.toWindowInsetsCompat(mInsets);
        }

        @Nullable
        @SuppressWarnings("JavaReflectionMemberAccess")
        private static WindowInsets createWindowInsetsInstance() {
            // On API 20-28, there is no public way to create an WindowInsets instance, so we
            // need to use reflection.

            // We will first try getting the WindowInsets.CONSUMED static field, and creating a
            // copy of it
            if (!sConsumedFieldFetched) {
                try {
                    sConsumedField = WindowInsets.class.getDeclaredField("CONSUMED");
                } catch (ReflectiveOperationException e) {
                    Log.i(TAG, "Could not retrieve WindowInsets.CONSUMED field", e);
                }
                sConsumedFieldFetched = true;
            }
            if (sConsumedField != null) {
                try {
                    WindowInsets consumed = (WindowInsets) sConsumedField.get(null);
                    if (consumed != null) {
                        return new WindowInsets(consumed);
                    }
                } catch (ReflectiveOperationException e) {
                    Log.i(TAG, "Could not get value from WindowInsets.CONSUMED field", e);
                }
            }

            // If we reached here, the WindowInsets.CONSUMED field did not exist. We can try
            // the hidden WindowInsets(Rect) constructor instead
            if (!sConstructorFetched) {
                try {
                    sConstructor = WindowInsets.class.getConstructor(Rect.class);
                } catch (ReflectiveOperationException e) {
                    Log.i(TAG, "Could not retrieve WindowInsets(Rect) constructor", e);
                }
                sConstructorFetched = true;
            }
            if (sConstructor != null) {
                try {
                    return sConstructor.newInstance(new Rect());
                } catch (ReflectiveOperationException e) {
                    Log.i(TAG, "Could not invoke WindowInsets(Rect) constructor", e);
                }
            }

            // If the reflective calls failed, return null
            return null;
        }
    }

    @RequiresApi(api = 29)
    private static class BuilderImpl29 extends BuilderImpl {
        private final WindowInsets.Builder mPlatBuilder;

        BuilderImpl29() {
            mPlatBuilder = new WindowInsets.Builder();
        }

        BuilderImpl29(@NonNull WindowInsetsCompat insets) {
            mPlatBuilder = new WindowInsets.Builder(insets.toWindowInsets());
        }

        @Override
        public void setSystemWindowInsets(@NonNull Insets insets) {
            mPlatBuilder.setSystemWindowInsets(insets.toPlatformInsets());
        }

        @Override
        public void setSystemGestureInsets(@NonNull Insets insets) {
            mPlatBuilder.setSystemGestureInsets(insets.toPlatformInsets());
        }

        @Override
        public void setMandatorySystemGestureInsets(@NonNull Insets insets) {
            mPlatBuilder.setMandatorySystemGestureInsets(insets.toPlatformInsets());
        }

        @Override
        public void setTappableElementInsets(@NonNull Insets insets) {
            mPlatBuilder.setTappableElementInsets(insets.toPlatformInsets());
        }

        @Override
        public void setStableInsets(@NonNull Insets insets) {
            mPlatBuilder.setStableInsets(insets.toPlatformInsets());
        }

        @Override
        public void setDisplayCutout(@Nullable DisplayCutoutCompat displayCutout) {
            mPlatBuilder.setDisplayCutout(displayCutout != null ? displayCutout.unwrap() : null);
        }

        @Override
        @NonNull
        public WindowInsetsCompat build() {
            return WindowInsetsCompat.toWindowInsetsCompat(mPlatBuilder.build());
        }
    }
}
