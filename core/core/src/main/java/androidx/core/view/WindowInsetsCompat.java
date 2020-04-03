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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.core.graphics.Insets.toCompatInsets;

import android.graphics.Rect;
import android.util.Log;
import android.view.WindowInsets;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.Insets;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

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

    /**
     * @hide we'll make this public in a future release
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final WindowInsetsCompat CONSUMED = new WindowInsetsCompat.Builder()
            .build()
            .consumeDisplayCutout()
            .consumeStableInsets()
            .consumeSystemWindowInsets();

    private final Impl mImpl;

    @RequiresApi(20)
    private WindowInsetsCompat(@NonNull WindowInsets insets) {
        if (SDK_INT >= 29) {
            mImpl = new Impl29(this, insets);
        } else if (SDK_INT >= 28) {
            mImpl = new Impl28(this, insets);
        } else if (SDK_INT >= 21) {
            mImpl = new Impl21(this, insets);
        } else if (SDK_INT >= 20) {
            mImpl = new Impl20(this, insets);
        } else {
            mImpl = new Impl(this);
        }
    }

    /**
     * Constructs a new WindowInsetsCompat, copying all values from a source WindowInsetsCompat.
     *
     * @param src source from which values are copied
     */
    public WindowInsetsCompat(@Nullable final WindowInsetsCompat src) {
        if (src != null) {
            // We'll copy over from the 'src' instance's impl
            final Impl srcImpl = src.mImpl;
            if (SDK_INT >= 29 && srcImpl instanceof Impl29) {
                mImpl = new Impl29(this, (Impl29) srcImpl);
            } else if (SDK_INT >= 28 && srcImpl instanceof Impl28) {
                mImpl = new Impl28(this, (Impl28) srcImpl);
            } else if (SDK_INT >= 21 && srcImpl instanceof Impl21) {
                mImpl = new Impl21(this, (Impl21) srcImpl);
            } else if (SDK_INT >= 20 && srcImpl instanceof Impl20) {
                mImpl = new Impl20(this, (Impl20) srcImpl);
            } else {
                mImpl = new Impl(this);
            }
        } else {
            // Ideally src would be @NonNull, oh well.
            mImpl = new Impl(this);
        }
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
        return new WindowInsetsCompat(Preconditions.checkNotNull(insets));
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
        return getSystemWindowInsets().left;
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
        return getSystemWindowInsets().top;
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
        return getSystemWindowInsets().right;
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
        return getSystemWindowInsets().bottom;
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
        return !getSystemWindowInsets().equals(Insets.NONE);
    }

    /**
     * Returns true if this WindowInsets has any non-zero insets.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code false}.
     *
     * @return true if any inset values are nonzero
     */
    public boolean hasInsets() {
        return hasSystemWindowInsets()
                || hasStableInsets()
                || getDisplayCutout() != null
                || !getSystemGestureInsets().equals(Insets.NONE)
                || !getMandatorySystemGestureInsets().equals(Insets.NONE)
                || !getTappableElementInsets().equals(Insets.NONE);
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
        return mImpl.isConsumed();
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
        return mImpl.isRound();
    }

    /**
     * Returns a copy of this WindowInsets with the system window insets fully consumed.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code null}.
     *
     * @return A modified copy of this WindowInsets
     */
    @NonNull
    public WindowInsetsCompat consumeSystemWindowInsets() {
        return mImpl.consumeSystemWindowInsets();
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
    @NonNull
    public WindowInsetsCompat replaceSystemWindowInsets(int left, int top, int right, int bottom) {
        return new Builder(this)
                .setSystemWindowInsets(Insets.of(left, top, right, bottom))
                .build();
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
    @NonNull
    public WindowInsetsCompat replaceSystemWindowInsets(@NonNull Rect systemWindowInsets) {
        return new Builder(this)
                .setSystemWindowInsets(Insets.of(systemWindowInsets))
                .build();
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
        return getStableInsets().top;
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
        return getStableInsets().left;
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
        return getStableInsets().right;
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
        return getStableInsets().bottom;
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
        return !getStableInsets().equals(Insets.NONE);
    }

    /**
     * Returns a copy of this WindowInsets with the stable insets fully consumed.
     *
     * <p>When running on platforms with API 20 and below, this method always returns {@code null}.
     *
     * @return A modified copy of this WindowInsetsCompat
     */
    @NonNull
    public WindowInsetsCompat consumeStableInsets() {
        return mImpl.consumeStableInsets();
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
        return mImpl.getDisplayCutout();
    }

    /**
     * Returns a copy of this WindowInsets with the cutout fully consumed.
     *
     * <p>When running on platforms with API 27 and below, this method is a no-op.
     *
     * @return A modified copy of this WindowInsets
     */
    @NonNull
    public WindowInsetsCompat consumeDisplayCutout() {
        return mImpl.consumeDisplayCutout();
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
        return mImpl.getSystemWindowInsets();
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
        return mImpl.getStableInsets();
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
        return mImpl.getMandatorySystemGestureInsets();
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
        return mImpl.getTappableElementInsets();
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
        return mImpl.getSystemGestureInsets();
    }

    /**
     * Returns a copy of this instance inset in the given directions.
     *
     * This is intended for dispatching insets to areas of the window that are smaller than the
     * current area.
     *
     * <p>Example:
     * <pre>
     * childView.dispatchApplyWindowInsets(insets.inset(childMargins));
     * </pre>
     *
     * @param insets the amount of insets to remove from all sides.
     *
     * @see #inset(int, int, int, int)
     */
    @NonNull
    public WindowInsetsCompat inset(@NonNull Insets insets) {
        return inset(insets.left, insets.top, insets.right, insets.bottom);
    }

    /**
     * Returns a copy of this instance inset in the given directions.
     *
     * This is intended for dispatching insets to areas of the window that are smaller than the
     * current area.
     *
     * <p>Example:
     * <pre>
     * childView.dispatchApplyWindowInsets(insets.inset(
     *         childMarginLeft, childMarginTop, childMarginBottom, childMarginRight));
     * </pre>
     *
     * @param left the amount of insets to remove from the left. Must be non-negative.
     * @param top the amount of insets to remove from the top. Must be non-negative.
     * @param right the amount of insets to remove from the right. Must be non-negative.
     * @param bottom the amount of insets to remove from the bottom. Must be non-negative.
     *
     * @return the inset insets
     */
    @NonNull
    public WindowInsetsCompat inset(@IntRange(from = 0) int left, @IntRange(from = 0) int top,
            @IntRange(from = 0) int right, @IntRange(from = 0) int bottom) {
        return mImpl.inset(left, top, right, bottom);
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
        return ObjectsCompat.equals(mImpl, other.mImpl);
    }

    @Override
    public int hashCode() {
        return mImpl == null ? 0 : mImpl.hashCode();
    }

    /**
     * Return the source {@link WindowInsets} instance used in this {@link WindowInsetsCompat}.
     *
     * @return the wrapped WindowInsets instance
     */
    @Nullable
    @RequiresApi(20)
    public WindowInsets toWindowInsets() {
        return mImpl instanceof Impl20 ? ((Impl20) mImpl).mPlatformInsets : null;
    }

    private static class Impl {
        final WindowInsetsCompat mHost;

        Impl(@NonNull WindowInsetsCompat host) {
            mHost = host;
        }

        boolean isRound() {
            return false;
        }

        boolean isConsumed() {
            return false;
        }

        @NonNull
        WindowInsetsCompat consumeSystemWindowInsets() {
            return mHost;
        }

        @NonNull
        WindowInsetsCompat consumeStableInsets() {
            return mHost;
        }

        @Nullable
        DisplayCutoutCompat getDisplayCutout() {
            return null;
        }

        @NonNull
        WindowInsetsCompat consumeDisplayCutout() {
            return mHost;
        }

        @NonNull
        Insets getSystemWindowInsets() {
            return Insets.NONE;
        }

        @NonNull
        Insets getStableInsets() {
            return Insets.NONE;
        }

        @NonNull
        Insets getSystemGestureInsets() {
            // Pre-Q return the system window insets
            return getSystemWindowInsets();
        }

        @NonNull
        Insets getMandatorySystemGestureInsets() {
            // Pre-Q return the system window insets
            return getSystemWindowInsets();
        }

        @NonNull
        Insets getTappableElementInsets() {
            // Pre-Q return the system window insets
            return getSystemWindowInsets();
        }

        @NonNull
        WindowInsetsCompat inset(int left, int top, int right, int bottom) {
            return CONSUMED;
        }

        @Override
        public boolean equals(Object o) {
            // On API < 28 we can not rely on WindowInsets.equals(), so we handle it manually
            if (this == o) return true;
            if (!(o instanceof Impl)) return false;
            final Impl impl = (Impl) o;
            return isRound() == impl.isRound()
                    && isConsumed() == impl.isConsumed()
                    && ObjectsCompat.equals(getSystemWindowInsets(), impl.getSystemWindowInsets())
                    && ObjectsCompat.equals(getStableInsets(), impl.getStableInsets())
                    && ObjectsCompat.equals(getDisplayCutout(), impl.getDisplayCutout());
        }

        @Override
        public int hashCode() {
            // On API < 28 we can not rely on WindowInsets.hashCode(), so we handle it manually
            return ObjectsCompat.hash(isRound(), isConsumed(), getSystemWindowInsets(),
                    getStableInsets(), getDisplayCutout());
        }
    }

    @RequiresApi(20)
    private static class Impl20 extends Impl {
        @NonNull
        final WindowInsets mPlatformInsets;

        // Used to cache the wrapped value
        private Insets mSystemWindowInsets = null;

        Impl20(@NonNull WindowInsetsCompat host, @NonNull WindowInsets insets) {
            super(host);
            mPlatformInsets = insets;
        }

        Impl20(@NonNull WindowInsetsCompat host, @NonNull Impl20 other) {
            this(host, new WindowInsets(other.mPlatformInsets));
        }

        @Override
        boolean isRound() {
            return mPlatformInsets.isRound();
        }

        @Override
        @NonNull
        final Insets getSystemWindowInsets() {
            if (mSystemWindowInsets == null) {
                mSystemWindowInsets = Insets.of(
                        mPlatformInsets.getSystemWindowInsetLeft(),
                        mPlatformInsets.getSystemWindowInsetTop(),
                        mPlatformInsets.getSystemWindowInsetRight(),
                        mPlatformInsets.getSystemWindowInsetBottom());
            }
            return mSystemWindowInsets;
        }

        @NonNull
        @Override
        WindowInsetsCompat inset(int left, int top, int right, int bottom) {
            Builder b = new Builder(toWindowInsetsCompat(mPlatformInsets));
            b.setSystemWindowInsets(insetInsets(getSystemWindowInsets(), left, top, right, bottom));
            b.setStableInsets(insetInsets(getStableInsets(), left, top, right, bottom));
            return b.build();
        }
    }

    @RequiresApi(21)
    private static class Impl21 extends Impl20 {
        private Insets mStableInsets = null;

        Impl21(@NonNull WindowInsetsCompat host, @NonNull WindowInsets insets) {
            super(host, insets);
        }

        Impl21(@NonNull WindowInsetsCompat host, @NonNull Impl21 other) {
            super(host, other);
        }

        @Override
        boolean isConsumed() {
            return mPlatformInsets.isConsumed();
        }

        @NonNull
        @Override
        WindowInsetsCompat consumeStableInsets() {
            return toWindowInsetsCompat(mPlatformInsets.consumeStableInsets());
        }

        @NonNull
        @Override
        WindowInsetsCompat consumeSystemWindowInsets() {
            return toWindowInsetsCompat(mPlatformInsets.consumeSystemWindowInsets());
        }

        @Override
        @NonNull
        final Insets getStableInsets() {
            if (mStableInsets == null) {
                mStableInsets = Insets.of(
                        mPlatformInsets.getStableInsetLeft(),
                        mPlatformInsets.getStableInsetTop(),
                        mPlatformInsets.getStableInsetRight(),
                        mPlatformInsets.getStableInsetBottom());
            }
            return mStableInsets;
        }
    }

    @RequiresApi(28)
    private static class Impl28 extends Impl21 {
        Impl28(@NonNull WindowInsetsCompat host, @NonNull WindowInsets insets) {
            super(host, insets);
        }

        Impl28(@NonNull WindowInsetsCompat host, @NonNull Impl28 other) {
            super(host, other);
        }

        @Nullable
        @Override
        DisplayCutoutCompat getDisplayCutout() {
            return DisplayCutoutCompat.wrap(mPlatformInsets.getDisplayCutout());
        }

        @NonNull
        @Override
        WindowInsetsCompat consumeDisplayCutout() {
            return toWindowInsetsCompat(mPlatformInsets.consumeDisplayCutout());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Impl28)) return false;
            Impl28 otherImpl28 = (Impl28) o;
            // On API 28+ we can rely on WindowInsets.equals()
            return Objects.equals(mPlatformInsets, otherImpl28.mPlatformInsets);
        }

        @Override
        public int hashCode() {
            return mPlatformInsets.hashCode();
        }
    }

    @RequiresApi(29)
    private static class Impl29 extends Impl28 {
        // Used to cache the wrapped values
        private Insets mSystemGestureInsets = null;
        private Insets mMandatorySystemGestureInsets = null;
        private Insets mTappableElementInsets = null;

        Impl29(@NonNull WindowInsetsCompat host, @NonNull WindowInsets insets) {
            super(host, insets);
        }

        Impl29(@NonNull WindowInsetsCompat host, @NonNull Impl29 other) {
            super(host, other);
        }

        @NonNull
        @Override
        Insets getSystemGestureInsets() {
            if (mSystemGestureInsets == null) {
                mSystemGestureInsets = toCompatInsets(mPlatformInsets.getSystemGestureInsets());
            }
            return mSystemGestureInsets;
        }

        @NonNull
        @Override
        Insets getMandatorySystemGestureInsets() {
            if (mMandatorySystemGestureInsets == null) {
                mMandatorySystemGestureInsets =
                        toCompatInsets(mPlatformInsets.getMandatorySystemGestureInsets());
            }
            return mMandatorySystemGestureInsets;
        }

        @NonNull
        @Override
        Insets getTappableElementInsets() {
            if (mTappableElementInsets == null) {
                mTappableElementInsets = toCompatInsets(mPlatformInsets.getTappableElementInsets());
            }
            return mTappableElementInsets;
        }

        @NonNull
        @Override
        WindowInsetsCompat inset(int left, int top, int right, int bottom) {
            return toWindowInsetsCompat(mPlatformInsets.inset(left, top, right, bottom));
        }
    }

    static Insets insetInsets(Insets insets, int left, int top, int right, int bottom) {
        int newLeft = Math.max(0, insets.left - left);
        int newTop = Math.max(0, insets.top - top);
        int newRight = Math.max(0, insets.right - right);
        int newBottom = Math.max(0, insets.bottom - bottom);
        if (newLeft == left && newTop == top && newRight == right && newBottom == bottom) {
            return insets;
        }
        return Insets.of(newLeft, newTop, newRight, newBottom);
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
        private final WindowInsetsCompat mInsets;

        BuilderImpl() {
            this(new WindowInsetsCompat((WindowInsetsCompat) null));
        }

        BuilderImpl(@NonNull WindowInsetsCompat insets) {
            mInsets = insets;
        }

        void setSystemWindowInsets(@NonNull Insets insets) {}

        void setSystemGestureInsets(@NonNull Insets insets) {}

        void setMandatorySystemGestureInsets(@NonNull Insets insets) {}

        void setTappableElementInsets(@NonNull Insets insets) {}

        void setStableInsets(@NonNull Insets insets) {}

        void setDisplayCutout(@Nullable DisplayCutoutCompat displayCutout) {}

        @NonNull
        WindowInsetsCompat build() {
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
        void setSystemWindowInsets(@NonNull Insets insets) {
            if (mInsets != null) {
                mInsets = mInsets.replaceSystemWindowInsets(
                        insets.left, insets.top, insets.right, insets.bottom);
            }
        }

        @Override
        @NonNull
        WindowInsetsCompat build() {
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
        final WindowInsets.Builder mPlatBuilder;

        BuilderImpl29() {
            mPlatBuilder = new WindowInsets.Builder();
        }

        BuilderImpl29(@NonNull WindowInsetsCompat insets) {
            final WindowInsets platInsets = insets.toWindowInsets();
            mPlatBuilder = platInsets != null
                    ? new WindowInsets.Builder(platInsets)
                    : new WindowInsets.Builder();
        }

        @Override
        void setSystemWindowInsets(@NonNull Insets insets) {
            mPlatBuilder.setSystemWindowInsets(insets.toPlatformInsets());
        }

        @Override
        void setSystemGestureInsets(@NonNull Insets insets) {
            mPlatBuilder.setSystemGestureInsets(insets.toPlatformInsets());
        }

        @Override
        void setMandatorySystemGestureInsets(@NonNull Insets insets) {
            mPlatBuilder.setMandatorySystemGestureInsets(insets.toPlatformInsets());
        }

        @Override
        void setTappableElementInsets(@NonNull Insets insets) {
            mPlatBuilder.setTappableElementInsets(insets.toPlatformInsets());
        }

        @Override
        void setStableInsets(@NonNull Insets insets) {
            mPlatBuilder.setStableInsets(insets.toPlatformInsets());
        }

        @Override
        void setDisplayCutout(@Nullable DisplayCutoutCompat displayCutout) {
            mPlatBuilder.setDisplayCutout(displayCutout != null ? displayCutout.unwrap() : null);
        }

        @Override
        @NonNull
        WindowInsetsCompat build() {
            return WindowInsetsCompat.toWindowInsetsCompat(mPlatBuilder.build());
        }
    }
}
