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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.core.graphics.Insets.toCompatInsets;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.Insets;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Describes a set of insets for window content.
 *
 * <p>WindowInsetsCompats are immutable and may be expanded to include more inset types in the
 * future. To adjust insets, use one of the supplied clone methods to obtain a new
 * WindowInsetsCompat instance with the adjusted properties.</p>
 */
@SuppressWarnings("JavadocReference")
public class WindowInsetsCompat {
    private static final String TAG = "WindowInsetsCompat";

    /**
     * A {@link WindowInsetsCompat} instance for which {@link #isConsumed()} returns {@code true}.
     * <p>
     * This can be used during insets dispatch in the view hierarchy by returning this value from
     * {@code View.onApplyWindowInsets(WindowInsets)} or
     * {@link OnApplyWindowInsetsListener#onApplyWindowInsets(View, WindowInsetsCompat)} to stop
     * dispatch the insets to its children to avoid traversing the entire view hierarchy.
     * <p>
     * The application should return this instance once it has taken care of all insets on a certain
     * level in the view hierarchy, and doesn't need to dispatch to its children anymore for better
     * performance.
     *
     * @see #isConsumed()
     */
    @NonNull
    public static final WindowInsetsCompat CONSUMED;

    static {
        if (SDK_INT >= 30) {
            CONSUMED = Impl30.CONSUMED;
        } else {
            CONSUMED = Impl.CONSUMED;
        }
    }

    private final Impl mImpl;

    @RequiresApi(20)
    private WindowInsetsCompat(@NonNull WindowInsets insets) {
        if (SDK_INT >= 30) {
            mImpl = new Impl30(this, insets);
        } else if (SDK_INT >= 29) {
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
            if (SDK_INT >= 30 && srcImpl instanceof Impl30) {
                mImpl = new Impl30(this, (Impl30) srcImpl);
            } else if (SDK_INT >= 29 && srcImpl instanceof Impl29) {
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
            srcImpl.copyWindowDataInto(this);
        } else {
            // Ideally src would be @NonNull, oh well.
            mImpl = new Impl(this);
        }
    }

    /**
     * Wrap an instance of {@link WindowInsets} into a {@link WindowInsetsCompat}.
     * <p>
     * This version of the function does not allow the resulting {@link WindowInsetsCompat} to be
     * aware of the root window insets and root view, meaning that the returned values for many of
     * the different inset {@link Type}s will be incorrect.
     * <p>
     * Prefer using {@link #toWindowInsetsCompat(WindowInsets, View)} instead.
     *
     * @param insets source insets to wrap
     * @return the wrapped instance
     */
    @NonNull
    @RequiresApi(20)
    public static WindowInsetsCompat toWindowInsetsCompat(@NonNull WindowInsets insets) {
        return toWindowInsetsCompat(insets, null);
    }

    /**
     * Wrap an instance of {@link WindowInsets} into a {@link WindowInsetsCompat}.
     * <p>
     * This version of the function allows the resulting {@link WindowInsetsCompat} to be
     * aware of the root window insets and root view through the {@code view} parameter. This is
     * required for many of the different inset {@link Type}s to return correct values when used
     * on devices running Android 10 and before.
     *
     * @param insets source insets to wrap
     * @param view view to use as an entry point for obtaining root window information. This
     *             view needs be attached to the window, otherwise it will be ignored.
     * @return the wrapped instance
     */
    @NonNull
    @RequiresApi(20)
    public static WindowInsetsCompat toWindowInsetsCompat(@NonNull WindowInsets insets,
            @Nullable View view) {
        WindowInsetsCompat wic = new WindowInsetsCompat(Preconditions.checkNotNull(insets));
        if (view != null && ViewCompat.isAttachedToWindow(view)) {
            // Pass the root window insets, which is useful if the Activity is adjustResize
            wic.setRootWindowInsets(ViewCompat.getRootWindowInsets(view));
            // Pass in the root view which allows the WIC to make of a copy of it's visible bounds
            wic.copyRootViewBounds(view.getRootView());
        }
        return wic;
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#systemBars()} instead.
     */
    @Deprecated
    public int getSystemWindowInsetLeft() {
        return mImpl.getSystemWindowInsets().left;
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#systemBars()} instead.
     */
    @Deprecated
    public int getSystemWindowInsetTop() {
        return mImpl.getSystemWindowInsets().top;
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#systemBars()} instead.
     */
    @Deprecated
    public int getSystemWindowInsetRight() {
        return mImpl.getSystemWindowInsets().right;
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#systemBars()} instead.
     */
    @Deprecated
    public int getSystemWindowInsetBottom() {
        return mImpl.getSystemWindowInsets().bottom;
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#systemBars()}
     * instead.
     */
    @Deprecated
    public boolean hasSystemWindowInsets() {
        return !mImpl.getSystemWindowInsets().equals(Insets.NONE);
    }

    /**
     * Returns true if this WindowInsets has any non-zero insets.
     *
     * <p>When running on platforms with API 19 and below, this method always returns {@code false}.
     *
     * @return true if any inset values are nonzero
     */
    public boolean hasInsets() {
        return !getInsets(Type.all()).equals(Insets.NONE)
                || !getInsetsIgnoringVisibility(Type.all() ^ Type.ime()).equals(Insets.NONE)
                || getDisplayCutout() != null;
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
     * @deprecated Consuming of different parts individually of a {@link WindowInsetsCompat}
     * instance is deprecated, since {@link WindowInsetsCompat} contains many different insets. Use
     * {@link #CONSUMED} instead to stop dispatching insets.
     */
    @Deprecated
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
     * @param left   New left inset in pixels
     * @param top    New top inset in pixels
     * @param right  New right inset in pixels
     * @param bottom New bottom inset in pixels
     * @return A modified copy of this WindowInsets
     * @deprecated use {@link WindowInsetsCompat.Builder} with
     * {@link WindowInsetsCompat.Builder#setSystemWindowInsets(Insets)} instead.
     */
    @SuppressWarnings("deprecation") // Builder.setSystemWindowInsets
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
     * @deprecated use {@link WindowInsetsCompat.Builder} with
     * {@link WindowInsetsCompat.Builder#setSystemWindowInsets(Insets)} instead.
     */
    @SuppressWarnings("deprecation")
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
     * @deprecated Use {@link #getInsetsIgnoringVisibility(int)} with {@link Type#systemBars()}
     * instead.
     */
    @Deprecated
    public int getStableInsetTop() {
        return mImpl.getStableInsets().top;
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
     * @deprecated Use {@link #getInsetsIgnoringVisibility(int)} with {@link Type#systemBars()}
     * instead.
     */
    @Deprecated
    public int getStableInsetLeft() {
        return mImpl.getStableInsets().left;
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
     * @deprecated Use {@link #getInsetsIgnoringVisibility(int)} with {@link Type#systemBars()}
     * instead.
     */
    @Deprecated
    public int getStableInsetRight() {
        return mImpl.getStableInsets().right;
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
     * @deprecated Use {@link #getInsetsIgnoringVisibility(int)} with {@link Type#systemBars()}
     * instead.
     */
    @Deprecated
    public int getStableInsetBottom() {
        return mImpl.getStableInsets().bottom;
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
     * @deprecated Use {@link #getInsetsIgnoringVisibility(int)} with {@link Type#systemBars()}
     * instead.
     */
    @Deprecated
    public boolean hasStableInsets() {
        return !mImpl.getStableInsets().equals(Insets.NONE);
    }

    /**
     * Returns a copy of this WindowInsets with the stable insets fully consumed.
     *
     * <p>When running on platforms with API 20 and below, this method always returns {@code null}.
     *
     * @return A modified copy of this WindowInsetsCompat
     * @deprecated Consuming of different parts individually of a {@link WindowInsetsCompat}
     * instance is deprecated, since {@link WindowInsetsCompat} contains many different insets. Use
     * {@link #CONSUMED} instead to stop dispatching insets.
     */
    @Deprecated
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
     * @deprecated Consuming of different parts individually of a {@link WindowInsetsCompat}
     * instance is deprecated, since {@link WindowInsetsCompat} contains many different insets. Use
     * {@link #CONSUMED} instead to stop dispatching insets.
     */
    @Deprecated
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#systemBars()} instead.
     */
    @Deprecated
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
     * @deprecated Use {@link #getInsetsIgnoringVisibility(int)} with {@link Type#systemBars()}
     * instead.
     */
    @Deprecated
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#mandatorySystemGestures()}
     * instead.
     */
    @Deprecated
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#tappableElement()}
     * instead.
     */
    @Deprecated
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
     * @deprecated Use {@link #getInsets(int)} with {@link Type#systemGestures()}
     * instead.
     */
    @Deprecated
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

    /**
     * Returns the insets of a specific set of windows causing insets, denoted by the
     * {@code typeMask} bit mask of {@link Type}s.
     *
     * When running on devices with API Level 29 and before, the returned insets are an
     * approximation based on the information available. This is especially true for the {@link
     * Type#ime IME} type, which currently only works when running on devices with SDK level 23
     * and above.
     *
     * @param typeMask Bit mask of {@link Type}s to query the insets for.
     * @return The insets.
     */
    @NonNull
    public Insets getInsets(@InsetsType int typeMask) {
        return mImpl.getInsets(typeMask);
    }

    /**
     * Returns the insets a specific set of windows can cause, denoted by the
     * {@code typeMask} bit mask of {@link Type}s, regardless of whether that type is
     * currently visible or not.
     *
     * <p>The insets represents the area of a a window that that <b>may</b> be partially
     * or fully obscured by the system window identified by {@code typeMask}. This value does not
     * change based on the visibility state of those elements. For example, if the status bar is
     * normally shown, but temporarily hidden, the inset returned here will still provide the inset
     * associated with the status bar being shown.</p>
     *
     * When running on devices with API Level 29 and before, the returned insets are an
     * approximation based on the information available. This is especially true for the {@link
     * Type#ime IME} type, which currently only works when running on devices with SDK level 23
     * and above.
     *
     * @param typeMask Bit mask of {@link Type}s to query the insets for.
     * @return The insets.
     * @throws IllegalArgumentException If the caller tries to query {@link Type#ime()}. Insets are
     *                                  not available if the IME isn't visible as the height of the
     *                                  IME is dynamic depending on the {@link EditorInfo} of the
     *                                  currently focused view, as well as the UI state of the IME.
     */
    @NonNull
    public Insets getInsetsIgnoringVisibility(@InsetsType int typeMask) {
        return mImpl.getInsetsIgnoringVisibility(typeMask);
    }

    /**
     * Returns whether a set of windows that may cause insets is currently visible on screen,
     * regardless of whether it actually overlaps with this window.
     *
     * When running on devices with API Level 29 and before, the returned value is an
     * approximation based on the information available. This is especially true for the {@link
     * Type#ime IME} type, which currently only works when running on devices with SDK level 23
     * and above.
     *
     * @param typeMask Bit mask of {@link Type}s to query visibility status.
     * @return {@code true} if and only if all windows included in {@code typeMask} are currently
     * visible on screen.
     */
    public boolean isVisible(@InsetsType int typeMask) {
        return mImpl.isVisible(typeMask);
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
        @SuppressWarnings("deprecation")
        @NonNull
        static final WindowInsetsCompat CONSUMED = new WindowInsetsCompat.Builder()
                .build()
                .consumeDisplayCutout()
                .consumeStableInsets()
                .consumeSystemWindowInsets();

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

        @NonNull
        Insets getInsets(@InsetsType int typeMask) {
            return Insets.NONE;
        }

        @NonNull
        Insets getInsetsIgnoringVisibility(@InsetsType int typeMask) {
            if ((typeMask & Type.IME) != 0) {
                throw new IllegalArgumentException("Unable to query the maximum insets for IME");
            }
            return Insets.NONE;
        }

        boolean isVisible(@InsetsType int typeMask) {
            return true;
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

        void setRootWindowInsets(@Nullable WindowInsetsCompat rootWindowInsets) {
        }

        void setRootViewData(@NonNull Insets visibleInsets) {
        }

        void copyRootViewBounds(@NonNull View rootView) {
        }

        void copyWindowDataInto(@NonNull WindowInsetsCompat other) {
        }

        public void setOverriddenInsets(Insets[] insetsTypeMask) {
        }

        public void setStableInsets(Insets stableInsets) {
        }
    }

    @RequiresApi(20)
    private static class Impl20 extends Impl {

        private static boolean sVisibleRectReflectionFetched = false;
        private static Method sGetViewRootImplMethod;
        private static Class<?> sAttachInfoClass;
        private static Field sVisibleInsetsField;
        private static Field sAttachInfoField;

        @NonNull
        final WindowInsets mPlatformInsets;

        // TODO(175859616) save all insets in the array
        private Insets[] mOverriddenInsets;

        // Used to cache the wrapped value
        private Insets mSystemWindowInsets = null;

        private WindowInsetsCompat mRootWindowInsets;
        Insets mRootViewVisibleInsets;

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

        @NonNull
        @Override
        public Insets getInsets(int typeMask) {
            return getInsets(typeMask, false);
        }

        @NonNull
        @Override
        public Insets getInsetsIgnoringVisibility(int typeMask) {
            return getInsets(typeMask, true);
        }

        @Override
        @SuppressLint("WrongConstant")
        boolean isVisible(final int typeMask) {
            for (int i = Type.FIRST; i <= Type.LAST; i = i << 1) {
                if ((typeMask & i) == 0) {
                    continue;
                }
                if (!isTypeVisible(i)) {
                    return false;
                }
            }
            return true;
        }

        @SuppressLint("WrongConstant")
        @NonNull
        private Insets getInsets(final int typeMask, final boolean ignoreVisibility) {
            Insets result = Insets.NONE;
            for (int i = Type.FIRST; i <= Type.LAST; i = i << 1) {
                if ((typeMask & i) == 0) {
                    continue;
                }
                result = Insets.max(result, getInsetsForType(i, ignoreVisibility));
            }
            return result;
        }

        @SuppressWarnings("deprecation")
        @NonNull
        protected Insets getInsetsForType(@InsetsType int type, boolean ignoreVisibility) {
            switch (type) {
                case Type.STATUS_BARS: {
                    if (ignoreVisibility) {
                        final Insets rootStable = getRootStableInsets();
                        return Insets.of(0,
                                Math.max(rootStable.top, getSystemWindowInsets().top), 0, 0);
                    } else {
                        return Insets.of(0, getSystemWindowInsets().top, 0, 0);
                    }
                }
                case Type.NAVIGATION_BARS: {
                    if (ignoreVisibility) {
                        final Insets rootStable = getRootStableInsets();
                        final Insets stable = getStableInsets();
                        return Insets.of(
                                Math.max(rootStable.left, stable.left),
                                0, /* zero top inset (== status bar) */
                                Math.max(rootStable.right, stable.right),
                                Math.max(rootStable.bottom, stable.bottom)
                        );
                    } else {
                        final Insets systemWindow = getSystemWindowInsets();
                        final Insets rootStable = mRootWindowInsets != null
                                ? mRootWindowInsets.getStableInsets()
                                : null;

                        int bottom = systemWindow.bottom;
                        if (rootStable != null) {
                            // If we have root stable insets, and its bottom is less than the
                            // system window bottom, it's likely that the IME is visible. In this
                            // case, we want to only return the stable bottom.
                            bottom = Math.min(bottom, rootStable.bottom);
                        }
                        return Insets.of(
                                systemWindow.left,
                                0, /* zero top inset (== status bar) */
                                systemWindow.right,
                                bottom
                        );
                    }
                }
                case Type.IME: {
                    Insets overriddenInsets = mOverriddenInsets != null
                            ? mOverriddenInsets[Type.indexOf(Type.IME)] : null;
                    if (overriddenInsets != null) {
                        return overriddenInsets;
                    }
                    final Insets systemWindow = getSystemWindowInsets();
                    final Insets rootStable = getRootStableInsets();

                    if (systemWindow.bottom > rootStable.bottom) {
                        // This handles the adjustResize case on < API 30, since
                        // systemWindow.bottom is probably going to be the IME
                        return Insets.of(0, 0, 0, systemWindow.bottom);
                    } else if (mRootViewVisibleInsets != null
                            && !mRootViewVisibleInsets.equals(Insets.NONE)) {
                        // This handles the adjustPan case on < API 30. We look at the root view's
                        // visible rect and check it's bottom against the root stable insets
                        if (mRootViewVisibleInsets.bottom > rootStable.bottom) {
                            return Insets.of(0, 0, 0, mRootViewVisibleInsets.bottom);
                        }
                    }
                    return Insets.NONE;
                }
                case Type.SYSTEM_GESTURES: {
                    // Visibility does not affect this type of inset
                    return getSystemGestureInsets();
                }
                case Type.MANDATORY_SYSTEM_GESTURES: {
                    // Visibility does not affect this type of inset
                    return getMandatorySystemGestureInsets();
                }
                case Type.TAPPABLE_ELEMENT: {
                    // Visibility does not affect this type of inset
                    return getTappableElementInsets();
                }
                case Type.DISPLAY_CUTOUT: {
                    // Visibility does not affect this type of inset
                    final DisplayCutoutCompat cutout = mRootWindowInsets != null
                            ? mRootWindowInsets.getDisplayCutout()
                            : getDisplayCutout();
                    if (cutout != null) {
                        return Insets.of(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(),
                                cutout.getSafeInsetRight(), cutout.getSafeInsetBottom());
                    } else {
                        return Insets.NONE;
                    }
                }
                default:
                    return Insets.NONE;
            }
        }

        protected boolean isTypeVisible(@InsetsType final int type) {
            switch (type) {
                case Type.STATUS_BARS:
                case Type.NAVIGATION_BARS:
                case Type.IME:
                case Type.DISPLAY_CUTOUT:
                    return !getInsetsForType(type, false).equals(Insets.NONE);
                case Type.CAPTION_BAR:
                    // Caption bar is always false on API < 30
                    return false;
                default:
                    return true;
            }
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
        @SuppressWarnings("deprecation")
        WindowInsetsCompat inset(int left, int top, int right, int bottom) {
            Builder b = new Builder(toWindowInsetsCompat(mPlatformInsets));
            b.setSystemWindowInsets(insetInsets(getSystemWindowInsets(), left, top, right, bottom));
            b.setStableInsets(insetInsets(getStableInsets(), left, top, right, bottom));
            return b.build();
        }

        @Override
        void copyWindowDataInto(@NonNull WindowInsetsCompat other) {
            other.setRootWindowInsets(mRootWindowInsets);
            other.setRootViewData(mRootViewVisibleInsets);
        }

        @Override
        void setRootWindowInsets(@Nullable WindowInsetsCompat rootWindowInsets) {
            mRootWindowInsets = rootWindowInsets;
        }

        @Override
        void setRootViewData(@NonNull Insets visibleInsets) {
            mRootViewVisibleInsets = visibleInsets;
        }

        @SuppressWarnings("deprecation")
        private Insets getRootStableInsets() {
            if (mRootWindowInsets != null) {
                return mRootWindowInsets.getStableInsets();
            } else {
                return Insets.NONE;
            }
        }

        @Override
        void copyRootViewBounds(@NonNull View rootView) {
            Insets visibleInsets = getVisibleInsets(rootView);
            if (visibleInsets == null) {
                visibleInsets = Insets.NONE;
            }
            setRootViewData(visibleInsets);
        }


        /**
         * Attempt to get a copy of the visible rect from this rootView's AttachInfo.
         *
         * @return a copy of the provided view's AttachInfo.mVisibleRect or null if anything fails
         */
        @Nullable
        private Insets getVisibleInsets(@NonNull View rootView) {
            if (SDK_INT >= 30) {
                throw new UnsupportedOperationException("getVisibleInsets() should not be called "
                        + "on API >= 30. Use WindowInsets.isVisible() instead.");
            } else {
                if (!sVisibleRectReflectionFetched) {
                    loadReflectionField();
                }

                if (sGetViewRootImplMethod == null
                        || sAttachInfoClass == null
                        || sVisibleInsetsField == null) {
                    return null;
                }

                try {
                    Object viewRootImpl = sGetViewRootImplMethod.invoke(rootView);
                    if (viewRootImpl == null) {
                        Log.w(TAG, "Failed to get visible insets. getViewRootImpl() returned null"
                                        + " from the provided view. This means that the view is "
                                        + "either not attached or the method has been overridden",
                                new NullPointerException());
                        return null;
                    } else {
                        Object mAttachInfo = sAttachInfoField.get(viewRootImpl);
                        Rect visibleRect = (Rect) sVisibleInsetsField.get(mAttachInfo);
                        return visibleRect != null ? Insets.of(visibleRect) : null;
                    }
                } catch (ReflectiveOperationException e) {
                    Log.e(TAG,
                            "Failed to get visible insets. (Reflection error). " + e.getMessage(),
                            e);
                }
            }
            return null;
        }

        @Override
        public void setOverriddenInsets(Insets[] insetsTypeMask) {
            mOverriddenInsets = insetsTypeMask;
        }

        @SuppressWarnings("JavaReflectionMemberAccess") // Reflection on private method
        @SuppressLint("PrivateApi")
        private static void loadReflectionField() {
            try {
                sGetViewRootImplMethod = View.class.getDeclaredMethod("getViewRootImpl");
                sAttachInfoClass = Class.forName("android.view.View$AttachInfo");
                sVisibleInsetsField = sAttachInfoClass.getDeclaredField("mVisibleInsets");
                Class<?> viewRootImplClass = Class.forName("android.view.ViewRootImpl");
                sAttachInfoField = viewRootImplClass.getDeclaredField("mAttachInfo");
                sVisibleInsetsField.setAccessible(true);
                sAttachInfoField.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                Log.e(TAG, "Failed to get visible insets. (Reflection error). " + e.getMessage(),
                        e);
            }
            sVisibleRectReflectionFetched = true;
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) return false;
            Impl20 impl20 = (Impl20) o;
            return Objects.equals(mRootViewVisibleInsets, impl20.mRootViewVisibleInsets);
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
            mStableInsets = other.mStableInsets;
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

        @Override
        public void setStableInsets(@Nullable Insets stableInsets) {
            mStableInsets = stableInsets;
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
            return Objects.equals(mPlatformInsets, otherImpl28.mPlatformInsets)
                    && Objects.equals(mRootViewVisibleInsets, otherImpl28.mRootViewVisibleInsets);
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

        @Override
        public void setStableInsets(@Nullable Insets stableInsets) {
            //no-op already in mPlatformInsets
        }
    }

    static Insets insetInsets(@NonNull Insets insets, int left, int top, int right, int bottom) {
        int newLeft = Math.max(0, insets.left - left);
        int newTop = Math.max(0, insets.top - top);
        int newRight = Math.max(0, insets.right - right);
        int newBottom = Math.max(0, insets.bottom - bottom);
        if (newLeft == left && newTop == top && newRight == right && newBottom == bottom) {
            return insets;
        }
        return Insets.of(newLeft, newTop, newRight, newBottom);
    }

    @RequiresApi(30)
    private static class Impl30 extends Impl29 {
        @NonNull
        static final WindowInsetsCompat CONSUMED = toWindowInsetsCompat(WindowInsets.CONSUMED);

        Impl30(@NonNull WindowInsetsCompat host, @NonNull WindowInsets insets) {
            super(host, insets);
        }

        Impl30(@NonNull WindowInsetsCompat host, @NonNull Impl30 other) {
            super(host, other);
        }

        @NonNull
        @Override
        public Insets getInsets(int typeMask) {
            return toCompatInsets(
                    mPlatformInsets.getInsets(TypeImpl30.toPlatformType(typeMask))
            );
        }

        @NonNull
        @Override
        public Insets getInsetsIgnoringVisibility(int typeMask) {
            return toCompatInsets(
                    mPlatformInsets.getInsetsIgnoringVisibility(TypeImpl30.toPlatformType(typeMask))
            );
        }

        @Override
        public boolean isVisible(int typeMask) {
            return mPlatformInsets.isVisible(TypeImpl30.toPlatformType(typeMask));
        }

        @Override
        final void copyRootViewBounds(@NonNull View rootView) {
            // This is only used to copy the root view visible insets which is
            // then only used to get the visibility of the IME on API < 30.
            // Overriding this avoid to go through the code path to get the visible insets via
            // reflection.
        }
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
            if (SDK_INT >= 30) {
                mImpl = new BuilderImpl30();
            } else if (SDK_INT >= 29) {
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
            if (SDK_INT >= 30) {
                mImpl = new BuilderImpl30(insets);
            } else if (SDK_INT >= 29) {
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
         * @return itself
         * @see #getSystemWindowInsets()
         * @deprecated Use {@link #setInsets(int, Insets)} with {@link Type#systemBars()}.
         */
        @Deprecated
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
         * @return itself
         * @see #getSystemGestureInsets()
         * @deprecated Use {@link #setInsets(int, Insets)} with {@link Type#systemGestures()}.
         */
        @Deprecated
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
         * @return itself
         * @see #getMandatorySystemGestureInsets()
         * @deprecated Use {@link #setInsets(int, Insets)} with
         * {@link Type#mandatorySystemGestures()}.
         */
        @Deprecated
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
         * @return itself
         * @see #getTappableElementInsets()
         * @deprecated Use {@link #setInsets(int, Insets)} with {@link Type#tappableElement()}.
         */
        @Deprecated
        @NonNull
        public Builder setTappableElementInsets(@NonNull Insets insets) {
            mImpl.setTappableElementInsets(insets);
            return this;
        }

        /**
         * Sets the insets of a specific window type in pixels.
         *
         * <p>The insets represents the area of a a window that is partially or fully obscured by
         * the system windows identified by {@code typeMask}.
         * </p>
         *
         * @param typeMask The bitmask of {@link Type} to set the insets for.
         * @param insets   The insets to set.
         * @return itself
         * @see #getInsets(int)
         */
        @NonNull
        public Builder setInsets(@InsetsType int typeMask, @NonNull Insets insets) {
            mImpl.setInsets(typeMask, insets);
            return this;
        }

        /**
         * Sets the insets a specific window type in pixels, while ignoring its visibility state.
         *
         * <p>The insets represents the area of a a window that that <b>may</b> be partially
         * or fully obscured by the system window identified by {@code typeMask}. This value does
         * not change based on the visibility state of those elements. For example, if the status
         * bar is normally shown, but temporarily hidden, the inset returned here will still
         * provide the inset associated with the status bar being shown.</p>
         *
         * @param typeMask The bitmask of {@link Type} to set the insets for.
         * @param insets   The insets to set.
         * @return itself
         * @throws IllegalArgumentException If {@code typeMask} contains {@link Type#ime()}. Maximum
         *                                  insets are not available for this type as the height of
         *                                  the IME is dynamic depending on the {@link EditorInfo}
         *                                  of the currently focused view, as well as the UI
         *                                  state of the IME.
         * @see #getInsetsIgnoringVisibility(int)
         */
        @NonNull
        public Builder setInsetsIgnoringVisibility(@InsetsType int typeMask,
                @NonNull Insets insets) {
            mImpl.setInsetsIgnoringVisibility(typeMask, insets);
            return this;
        }

        /**
         * Sets whether windows that can cause insets are currently visible on screen.
         *
         * @param typeMask The bitmask of {@link Type} to set the visibility for.
         * @param visible  Whether to mark the windows as visible or not.
         * @return itself
         * @see #isVisible(int)
         */
        @NonNull
        public Builder setVisible(@InsetsType int typeMask, boolean visible) {
            mImpl.setVisible(typeMask, visible);
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
         * @return itself
         * @see #getStableInsets()
         * @deprecated Use {@link #setInsetsIgnoringVisibility(int, Insets)} with
         * {@link Type#systemBars()}.
         */
        @Deprecated
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
         * @param displayCutout the display cutout or null if there is none
         * @return itself
         * @see #getDisplayCutout()
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

        Insets[] mInsetsTypeMask;

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

        @SuppressWarnings("WrongConstant")
        void setInsets(int typeMask, @NonNull Insets insets) {
            if (mInsetsTypeMask == null) {
                mInsetsTypeMask = new Insets[Type.SIZE];
            }
            for (int i = Type.FIRST; i <= Type.LAST; i = i << 1) {
                if ((typeMask & i) == 0) {
                    continue;
                }
                mInsetsTypeMask[Type.indexOf(i)] = insets;
            }
        }

        void setInsetsIgnoringVisibility(int typeMask, @NonNull Insets insets) {
            if (typeMask == Type.IME) {
                // Not strictly necessary but means we will throw an exception on all platforms,
                // rather than just R, which is clearer for all users
                throw new IllegalArgumentException("Ignoring visibility inset not available"
                        + " for IME");
            }
        }

        void setVisible(int typeMask, boolean visible) {}

        /**
         * This method tries to apply any insets set via {@link #setInsets(int, Insets)} to
         * the insets builder. This function will be a no-op on API 30 since
         * {@link BuilderImpl30#setInsets(int, Insets)} does not update the array.
         */
        protected final void applyInsetTypes() {
            if (mInsetsTypeMask != null) {
                Insets statusBars = mInsetsTypeMask[Type.indexOf(Type.STATUS_BARS)];
                Insets navigationBars = mInsetsTypeMask[Type.indexOf(Type.NAVIGATION_BARS)];

                // If the insets are not set in the builder, default to the insets passed in
                // the builder parameter to avoid accidentally setting them to 0
                if (navigationBars == null) {
                    navigationBars = mInsets.getInsets(Type.NAVIGATION_BARS);
                }
                if (statusBars == null) {
                    statusBars = mInsets.getInsets(Type.STATUS_BARS);
                }

                setSystemWindowInsets(Insets.max(statusBars, navigationBars));

                Insets i = mInsetsTypeMask[Type.indexOf(Type.SYSTEM_GESTURES)];
                if (i != null) setSystemGestureInsets(i);

                i = mInsetsTypeMask[Type.indexOf(Type.MANDATORY_SYSTEM_GESTURES)];
                if (i != null) setMandatorySystemGestureInsets(i);

                i = mInsetsTypeMask[Type.indexOf(Type.TAPPABLE_ELEMENT)];
                if (i != null) setTappableElementInsets(i);
            }
        }

        @NonNull
        WindowInsetsCompat build() {
            applyInsetTypes();
            return mInsets;
        }
    }

    void setOverriddenInsets(Insets[] insetsTypeMask) {
        mImpl.setOverriddenInsets(insetsTypeMask);
    }

    @RequiresApi(api = 20)
    private static class BuilderImpl20 extends BuilderImpl {
        private static Field sConsumedField;
        private static boolean sConsumedFieldFetched = false;

        private static Constructor<WindowInsets> sConstructor;
        private static boolean sConstructorFetched = false;

        private WindowInsets mPlatformInsets;
        private Insets mStableInsets;

        BuilderImpl20() {
            mPlatformInsets = createWindowInsetsInstance();
        }

        BuilderImpl20(@NonNull WindowInsetsCompat insets) {
            super(insets);
            mPlatformInsets = insets.toWindowInsets();
        }

        @Override
        void setSystemWindowInsets(@NonNull Insets insets) {
            if (mPlatformInsets != null) {
                mPlatformInsets = mPlatformInsets.replaceSystemWindowInsets(
                        insets.left, insets.top, insets.right, insets.bottom);
            }
        }

        @Override
        void setStableInsets(@Nullable Insets insets) {
            mStableInsets = insets;
        }

        @Override
        @NonNull
        WindowInsetsCompat build() {
            applyInsetTypes();
            WindowInsetsCompat windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
                    mPlatformInsets);
            windowInsetsCompat.setOverriddenInsets(this.mInsetsTypeMask);
            windowInsetsCompat.setStableInsets(mStableInsets);
            return windowInsetsCompat;
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

    void setStableInsets(@Nullable Insets stableInsets) {
        mImpl.setStableInsets(stableInsets);
    }

    @RequiresApi(api = 29)
    private static class BuilderImpl29 extends BuilderImpl {
        final WindowInsets.Builder mPlatBuilder;

        BuilderImpl29() {
            super();
            mPlatBuilder = new WindowInsets.Builder();
        }

        BuilderImpl29(@NonNull WindowInsetsCompat insets) {
            super(insets);
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
            applyInsetTypes();
            WindowInsetsCompat windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
                    mPlatBuilder.build());
            windowInsetsCompat.setOverriddenInsets(mInsetsTypeMask);
            return windowInsetsCompat;
        }
    }

    @RequiresApi(30)
    private static class BuilderImpl30 extends BuilderImpl29 {
        BuilderImpl30() {
            super();
        }

        BuilderImpl30(@NonNull WindowInsetsCompat insets) {
            super(insets);
        }

        @Override
        void setInsets(int typeMask, @NonNull Insets insets) {
            mPlatBuilder.setInsets(
                    TypeImpl30.toPlatformType(typeMask),
                    insets.toPlatformInsets()
            );
        }

        @Override
        void setInsetsIgnoringVisibility(int typeMask, @NonNull Insets insets) {
            mPlatBuilder.setInsetsIgnoringVisibility(
                    TypeImpl30.toPlatformType(typeMask),
                    insets.toPlatformInsets()
            );
        }

        @Override
        void setVisible(int typeMask, boolean visible) {
            mPlatBuilder.setVisible(TypeImpl30.toPlatformType(typeMask), visible);
        }
    }

    /**
     * Class that defines different types of sources causing window insets.
     */
    public static final class Type {
        static final int FIRST = 1;
        static final int STATUS_BARS = FIRST;
        static final int NAVIGATION_BARS = 1 << 1;
        static final int CAPTION_BAR = 1 << 2;

        static final int IME = 1 << 3;

        static final int SYSTEM_GESTURES = 1 << 4;
        static final int MANDATORY_SYSTEM_GESTURES = 1 << 5;
        static final int TAPPABLE_ELEMENT = 1 << 6;

        static final int DISPLAY_CUTOUT = 1 << 7;

        static final int LAST = 1 << 8;
        static final int SIZE = 9;
        static final int WINDOW_DECOR = LAST;

        private Type() {}

        /**
         * @return An insets type representing any system bars for displaying status.
         */
        @InsetsType
        public static int statusBars() {
            return STATUS_BARS;
        }

        /**
         * @return An insets type representing any system bars for navigation.
         */
        @InsetsType
        public static int navigationBars() {
            return NAVIGATION_BARS;
        }

        /**
         * @return An insets type representing the window of a caption bar.
         */
        @InsetsType
        public static int captionBar() {
            return CAPTION_BAR;
        }

        /**
         * @return An insets type representing the window of an {@link InputMethod}.
         */
        @InsetsType
        public static int ime() {
            return IME;
        }

        /**
         * Returns an insets type representing the system gesture insets.
         *
         * <p>The system gesture insets represent the area of a window where system gestures have
         * priority and may consume some or all touch input, e.g. due to the a system bar
         * occupying it, or it being reserved for touch-only gestures.
         *
         * <p>Simple taps are guaranteed to reach the window even within the system gesture insets,
         * as long as they are outside the {@link #getSystemWindowInsets() system window insets}.
         *
         * <p>When {@link View#SYSTEM_UI_FLAG_LAYOUT_STABLE} is requested, an inset will be returned
         * even when the system gestures are inactive due to
         * {@link View#SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN} or
         * {@link View#SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION}.
         *
         * @see #getSystemGestureInsets()
         */
        @InsetsType
        public static int systemGestures() {
            return SYSTEM_GESTURES;
        }

        /**
         * @see #getMandatorySystemGestureInsets
         */
        @InsetsType
        public static int mandatorySystemGestures() {
            return MANDATORY_SYSTEM_GESTURES;
        }

        /**
         * @see #getTappableElementInsets
         */
        @InsetsType
        public static int tappableElement() {
            return TAPPABLE_ELEMENT;
        }

        /**
         * Returns an insets type representing the area that used by {@link DisplayCutoutCompat}.
         *
         * <p>This is equivalent to the safe insets on {@link #getDisplayCutout()}.</p>
         *
         * @see DisplayCutoutCompat#getSafeInsetLeft()
         * @see DisplayCutoutCompat#getSafeInsetTop()
         * @see DisplayCutoutCompat#getSafeInsetRight()
         * @see DisplayCutoutCompat#getSafeInsetBottom()
         */
        @InsetsType
        public static int displayCutout() {
            return DISPLAY_CUTOUT;
        }

        /**
         * @return All system bars. Includes {@link #statusBars()}, {@link #captionBar()} as well as
         * {@link #navigationBars()}, but not {@link #ime()}.
         */
        @InsetsType
        public static int systemBars() {
            return STATUS_BARS | NAVIGATION_BARS | CAPTION_BAR;
        }

        /**
         * @return All inset types combined.
         * @hide
         */
        @InsetsType
        @RestrictTo(LIBRARY_GROUP)
        @SuppressLint("WrongConstant")
        static int all() {
            return 0xFFFFFFFF;
        }

        static int indexOf(@InsetsType int type) {
            switch (type) {
                case STATUS_BARS:
                    return 0;
                case NAVIGATION_BARS:
                    return 1;
                case CAPTION_BAR:
                    return 2;
                case IME:
                    return 3;
                case SYSTEM_GESTURES:
                    return 4;
                case MANDATORY_SYSTEM_GESTURES:
                    return 5;
                case TAPPABLE_ELEMENT:
                    return 6;
                case DISPLAY_CUTOUT:
                    return 7;
                case WINDOW_DECOR:
                    return 8;
                default:
                    throw new IllegalArgumentException("type needs to be >= FIRST and <= LAST,"
                            + " type=" + type);
            }
        }

        /** @hide */
        @RestrictTo(LIBRARY_GROUP)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(flag = true, value = {STATUS_BARS, NAVIGATION_BARS, CAPTION_BAR, IME, WINDOW_DECOR,
                SYSTEM_GESTURES, MANDATORY_SYSTEM_GESTURES, TAPPABLE_ELEMENT, DISPLAY_CUTOUT})
        public @interface InsetsType {
        }
    }

    @RequiresApi(30)
    private static final class TypeImpl30 {
        private TypeImpl30() {}

        /**
         * Maps from our internal type mask constants to the platform's. Ideally we will keep the
         * constant values in sync, but this allows the platform to return different constants in
         * the future without breaking the logic in this class.
         */
        static int toPlatformType(@InsetsType final int typeMask) {
            int result = 0;
            for (int i = Type.FIRST; i <= Type.LAST; i = i << 1) {
                if ((typeMask & i) != 0) {
                    switch (i) {
                        case Type.STATUS_BARS:
                            result |= WindowInsets.Type.statusBars();
                            break;
                        case Type.NAVIGATION_BARS:
                            result |= WindowInsets.Type.navigationBars();
                            break;
                        case Type.CAPTION_BAR:
                            result |= WindowInsets.Type.captionBar();
                            break;
                        case Type.IME:
                            result |= WindowInsets.Type.ime();
                            break;
                        case Type.SYSTEM_GESTURES:
                            result |= WindowInsets.Type.systemGestures();
                            break;
                        case Type.MANDATORY_SYSTEM_GESTURES:
                            result |= WindowInsets.Type.mandatorySystemGestures();
                            break;
                        case Type.TAPPABLE_ELEMENT:
                            result |= WindowInsets.Type.tappableElement();
                            break;
                        case Type.DISPLAY_CUTOUT:
                            result |= WindowInsets.Type.displayCutout();
                            break;
                    }
                }
            }
            return result;
        }
    }

    void setRootWindowInsets(@Nullable WindowInsetsCompat rootWindowInsets) {
        mImpl.setRootWindowInsets(rootWindowInsets);
    }

    void setRootViewData(@NonNull Insets visibleInsets) {
        mImpl.setRootViewData(visibleInsets);
    }

    void copyRootViewBounds(@NonNull View rootView) {
        mImpl.copyRootViewBounds(rootView);
    }

    @SuppressWarnings("JavaReflectionMemberAccess") // Reflection on private field
    @SuppressLint("SoonBlockedPrivateApi") // mAttachInfo is only accessed on SDK 21 and 22
    @RequiresApi(21)
    static class Api21ReflectionHolder {

        private Api21ReflectionHolder() {
            // This class is not instantiable.
        }

        private static Field sViewAttachInfoField; // Only accessed on SDK 21 and 222
        private static Field sStableInsets;
        private static Field sContentInsets;
        private static boolean sReflectionSucceeded;

        static {
            try {
                sViewAttachInfoField = View.class.getDeclaredField("mAttachInfo");
                sViewAttachInfoField.setAccessible(true);
                Class<?> sAttachInfoClass = Class.forName("android.view.View$AttachInfo");
                sStableInsets = sAttachInfoClass.getDeclaredField("mStableInsets");
                sStableInsets.setAccessible(true);
                sContentInsets = sAttachInfoClass.getDeclaredField("mContentInsets");
                sContentInsets.setAccessible(true);
                sReflectionSucceeded = true;
            } catch (ReflectiveOperationException e) {
                Log.w(TAG, "Failed to get visible insets from AttachInfo " + e.getMessage(), e);
            }
        }

        // Only called on SDK 21 and 22
        @SuppressWarnings("deprecation")
        @Nullable
        public static WindowInsetsCompat getRootWindowInsets(@NonNull View v) {
            if (!sReflectionSucceeded || !v.isAttachedToWindow()) {
                return null;
            }

            View rootView = v.getRootView();
            try {
                Object attachInfo = sViewAttachInfoField.get(rootView);
                if (attachInfo != null) {
                    Rect stableInsets = (Rect) sStableInsets.get(attachInfo);
                    Rect visibleInsets = (Rect) sContentInsets.get(attachInfo);
                    if (stableInsets != null && visibleInsets != null) {
                        WindowInsetsCompat insets = new Builder()
                                .setStableInsets(Insets.of(stableInsets))
                                .setSystemWindowInsets(Insets.of(visibleInsets))
                                .build();

                        // The WindowInsetsCompat instance still needs to know about
                        // what the root window insets, and the root view visible bounds are
                        insets.setRootWindowInsets(insets);
                        insets.copyRootViewBounds(v.getRootView());
                        return insets;
                    }
                }
            } catch (IllegalAccessException e) {
                Log.w(TAG, "Failed to get insets from AttachInfo. " + e.getMessage(), e);
            }
            return null;
        }
    }
}
