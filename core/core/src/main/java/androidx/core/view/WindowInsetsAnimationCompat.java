/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.R;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Class representing an animation of a set of windows that cause insets.
 */
public final class WindowInsetsAnimationCompat {
    private static final boolean DEBUG = false;
    private static final String TAG = "WindowInsetsAnimCompat";
    private Impl mImpl;

    /**
     * Creates a new {@link WindowInsetsAnimationCompat} object.
     * <p>
     * This should only be used for testing, as usually the system creates this object for the
     * application to listen to with {@link WindowInsetsAnimationCompat.Callback}.
     * </p>
     *
     * @param typeMask       The bitmask of {@link WindowInsetsCompat.Type}s that are animating.
     * @param interpolator   The interpolator of the animation.
     * @param durationMillis The duration of the animation in
     *                       {@link java.util.concurrent.TimeUnit#MILLISECONDS}.
     */
    public WindowInsetsAnimationCompat(
            @InsetsType int typeMask, @Nullable Interpolator interpolator,
            long durationMillis) {
        if (Build.VERSION.SDK_INT >= 30) {
            mImpl = new Impl30(typeMask, interpolator, durationMillis);
        } else {
            mImpl = new Impl21(typeMask, interpolator, durationMillis);
        }
    }

    @RequiresApi(30)
    private WindowInsetsAnimationCompat(@NonNull WindowInsetsAnimation animation) {
        this(0, null, 0);
        mImpl = new Impl30(animation);
    }

    /**
     * @return The bitmask of {@link Type} that are animating.
     */
    @InsetsType
    public int getTypeMask() {
        return mImpl.getTypeMask();
    }

    /**
     * Returns the raw fractional progress of this animation between
     * start state of the animation and the end state of the animation. Note
     * that this progress is the global progress of the animation, whereas
     * {@link WindowInsetsAnimationCompat.Callback#onProgress} will only dispatch the insets that
     * may be inset with {@link WindowInsetsCompat#inset} by parents of views in the hierarchy.
     * Progress per insets animation is global for the entire animation. One animation animates
     * all things together (in, out, ...). If they don't animate together, we'd have
     * multiple animations.
     * <p>
     * Note: In case the application is controlling the animation, the valued returned here will
     * be the same as the application passed into
     *
     * {@link WindowInsetsAnimationControllerCompat#setInsetsAndAlpha(
     * androidx.core.graphics.Insets, float, float)}.
     * </p>
     *
     * @return The current progress of this animation.
     */
    @FloatRange(from = 0f, to = 1f)
    public float getFraction() {
        return mImpl.getFraction();
    }

    /**
     * Returns the interpolated fractional progress of this animation between
     * start state of the animation and the end state of the animation. Note
     * that this progress is the global progress of the animation, whereas
     * {@link WindowInsetsAnimationCompat.Callback#onProgress} will only dispatch the
     * insets that may
     * be inset with {@link WindowInsetsCompat#inset} by parents of views in the hierarchy.
     * Progress per insets animation is global for the entire animation. One animation animates
     * all things together (in, out, ...). If they don't animate together, we'd have
     * multiple animations.
     * <p>
     * Note: In case the application is controlling the animation, the valued returned here will
     * be the same as the application passed into
     * {@link WindowInsetsAnimationControllerCompat#setInsetsAndAlpha(Insets, float, float)},
     * interpolated with the interpolator passed into
     * {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation}.
     * <p>
     * Note: For system-initiated animations, this will always return a valid value between 0
     * and 1.
     *
     * @return The current interpolated progress of this animation.
     * @see #getFraction() for raw fraction.
     */
    public float getInterpolatedFraction() {
        return mImpl.getInterpolatedFraction();
    }

    /**
     * Retrieves the interpolator used for this animation, or {@code null} if this animation
     * doesn't follow an interpolation curved. For system-initiated animations, this will never
     * return {@code null}.
     *
     * @return The interpolator used for this animation.
     */
    public @Nullable Interpolator getInterpolator() {
        return mImpl.getInterpolator();
    }

    /**
     * @return duration of animation in {@link java.util.concurrent.TimeUnit#MILLISECONDS}, or
     * -1 if the animation doesn't have a fixed duration.
     */
    public long getDurationMillis() {
        return mImpl.getDurationMillis();
    }

    /**
     * Set fraction of the progress if {@link Type} animation is controlled by the app.
     * <p>
     * Note: This should only be used for testing, as the system fills in the fraction for the
     * application or the fraction that was passed into
     * {@link WindowInsetsAnimationControllerCompat#setInsetsAndAlpha(Insets, float, float)} is
     * being used.
     *
     * @param fraction fractional progress between 0 and 1 where 0 represents hidden and
     *                 zero progress and 1 represent fully shown final state.
     * @see #getFraction()
     */
    public void setFraction(@FloatRange(from = 0f, to = 1f) float fraction) {
        mImpl.setFraction(fraction);
    }

    /**
     * Retrieves the translucency of the windows that are animating.
     *
     * @return Alpha of windows that cause insets of type {@link Type}.
     */
    @FloatRange(from = 0f, to = 1f)
    public float getAlpha() {
        return mImpl.getAlpha();
    }

    /**
     * Sets the translucency of the windows that are animating.
     * <p>
     * Note: This should only be used for testing, as the system fills in the alpha for the
     * application or the alpha that was passed into
     * {@link WindowInsetsAnimationControllerCompat#setInsetsAndAlpha(Insets, float, float)} is
     * being used.
     *
     * @param alpha Alpha of windows that cause insets of type {@link Type}.
     * @see #getAlpha()
     */
    public void setAlpha(@FloatRange(from = 0f, to = 1f) float alpha) {
        mImpl.setAlpha(alpha);
    }

    /**
     * Class representing the range of an {@link WindowInsetsAnimationCompat}
     */
    public static final class BoundsCompat {

        private final Insets mLowerBound;
        private final Insets mUpperBound;

        public BoundsCompat(@NonNull Insets lowerBound, @NonNull Insets upperBound) {
            mLowerBound = lowerBound;
            mUpperBound = upperBound;
        }

        @RequiresApi(30)
        private BoundsCompat(WindowInsetsAnimation.@NonNull Bounds bounds) {
            mLowerBound = Impl30.getLowerBounds(bounds);
            mUpperBound = Impl30.getHigherBounds(bounds);
        }

        /**
         * Queries the lower inset bound of the animation. If the animation is about showing or
         * hiding a window that cause insets, the lower bound is {@link Insets#NONE} and the upper
         * bound is the same as {@link WindowInsetsCompat#getInsets(int)} for the fully shown
         * state. This
         * is the same as {@link WindowInsetsAnimationControllerCompat#getHiddenStateInsets} and
         * {@link WindowInsetsAnimationControllerCompat#getShownStateInsets} in case the listener
         * gets invoked because of an animation that originates from
         * {@link WindowInsetsAnimationControllerCompat}.
         * <p>
         * However, if the size of a window that causes insets is changing, these are the
         * lower/upper bounds of that size animation.
         * </p>
         * There are no overlapping animations for a specific type, but there may be multiple
         * animations running at the same time for different inset types.
         *
         * @see #getUpperBound()
         * @see WindowInsetsAnimationControllerCompat#getHiddenStateInsets
         */
        public @NonNull Insets getLowerBound() {
            return mLowerBound;
        }

        /**
         * Queries the upper inset bound of the animation. If the animation is about showing or
         * hiding a window that cause insets, the lower bound is {@link Insets#NONE} nd the upper
         * bound is the same as {@link WindowInsetsCompat#getInsets(int)} for the fully shown
         * state. This is the same as
         * {@link WindowInsetsAnimationControllerCompat#getHiddenStateInsets} and
         * {@link WindowInsetsAnimationControllerCompat#getShownStateInsets} in case the listener
         * gets invoked because of an animation that originates from
         * {@link WindowInsetsAnimationControllerCompat}.
         * <p>
         * However, if the size of a window that causes insets is changing, these are the
         * lower/upper bounds of that size animation.
         * <p>
         * There are no overlapping animations for a specific type, but there may be multiple
         * animations running at the same time for different inset types.
         *
         * @see #getLowerBound()
         * @see WindowInsetsAnimationControllerCompat#getShownStateInsets
         */
        public @NonNull Insets getUpperBound() {
            return mUpperBound;
        }

        /**
         * Insets both the lower and upper bound by the specified insets. This is to be used in
         * {@link WindowInsetsAnimationCompat.Callback#onStart} to indicate that a part of the
         * insets has been used to offset or clip its children, and the children shouldn't worry
         * about that part anymore.
         *
         * @param insets The amount to inset.
         * @return A copy of this instance inset in the given directions.
         * @see WindowInsetsCompat#inset
         * @see WindowInsetsAnimationCompat.Callback#onStart
         */
        public @NonNull BoundsCompat inset(@NonNull Insets insets) {
            return new BoundsCompat(
                    // TODO: refactor so that WindowInsets.insetInsets() is in a more appropriate
                    //  place eventually.
                    WindowInsetsCompat.insetInsets(
                            mLowerBound, insets.left, insets.top, insets.right, insets.bottom),
                    WindowInsetsCompat.insetInsets(
                            mUpperBound, insets.left, insets.top, insets.right, insets.bottom));
        }

        @Override
        public String toString() {
            return "Bounds{lower=" + mLowerBound + " upper=" + mUpperBound + "}";
        }

        /**
         * Creates a new instance of {@link WindowInsetsAnimation.Bounds} from this compat instance.
         */
        @RequiresApi(30)
        public WindowInsetsAnimation.@NonNull Bounds toBounds() {
            return Impl30.createPlatformBounds(this);
        }

        /**
         * Create a new insance of {@link BoundsCompat} using the provided
         * platform {@link android.view.WindowInsetsAnimation.Bounds}.
         */
        @RequiresApi(30)
        public static @NonNull BoundsCompat toBoundsCompat(
                WindowInsetsAnimation.@NonNull Bounds bounds) {
            return new BoundsCompat(bounds);
        }
    }

    @RequiresApi(30)
    static WindowInsetsAnimationCompat toWindowInsetsAnimationCompat(
            WindowInsetsAnimation windowInsetsAnimation) {
        return new WindowInsetsAnimationCompat(windowInsetsAnimation);
    }

    /**
     * Interface that allows the application to listen to animation events for windows that cause
     * insets.
     */
    public abstract static class Callback {

        /**
         * Return value for {@link #getDispatchMode()}: Dispatching of animation events should
         * stop at this level in the view hierarchy, and no animation events should be dispatch to
         * the subtree of the view hierarchy.
         */
        public static final int DISPATCH_MODE_STOP = 0;

        /**
         * Return value for {@link #getDispatchMode()}: Dispatching of animation events should
         * continue in the view hierarchy.
         */
        public static final int DISPATCH_MODE_CONTINUE_ON_SUBTREE = 1;
        WindowInsetsCompat mDispachedInsets;

        @IntDef(value = {
                DISPATCH_MODE_STOP,
                DISPATCH_MODE_CONTINUE_ON_SUBTREE
        })
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public @interface DispatchMode {
        }

        @DispatchMode
        private final int mDispatchMode;

        /**
         * Creates a new {@link WindowInsetsAnimationCompat} callback with the given
         * {@link #getDispatchMode() dispatch mode}.
         *
         * @param dispatchMode The dispatch mode for this callback. See {@link #getDispatchMode()}.
         */
        public Callback(@DispatchMode int dispatchMode) {
            mDispatchMode = dispatchMode;
        }

        /**
         * Retrieves the dispatch mode of this listener. Dispatch of the all animation events is
         * hierarchical: It will starts at the root of the view hierarchy and then traverse it and
         * invoke the callback of the specific {@link View} that is being traversed.
         * The method may return either {@link #DISPATCH_MODE_CONTINUE_ON_SUBTREE} to indicate that
         * animation events should be propagated to the subtree of the view hierarchy, or
         * {@link #DISPATCH_MODE_STOP} to stop dispatching. In that case, all animation callbacks
         * related to the animation passed in will be stopped from propagating to the subtree of the
         * hierarchy.
         * <p>
         * Also note that {@link #DISPATCH_MODE_STOP} behaves the same way as
         * returning {@link WindowInsetsCompat#CONSUMED} during the regular insets dispatch in
         * {@link View#onApplyWindowInsets}.
         *
         * @return Either {@link #DISPATCH_MODE_CONTINUE_ON_SUBTREE} to indicate that dispatching of
         * animation events will continue to the subtree of the view hierarchy, or
         * {@link #DISPATCH_MODE_STOP} to indicate that animation events will stop
         * dispatching.
         */
        @DispatchMode
        public final int getDispatchMode() {
            return mDispatchMode;
        }

        /**
         * Called when an insets animation is about to start and before the views have been
         * re-laid out due to an animation.
         * <p>
         * This ordering allows the application to inspect the end state after the animation has
         * finished, and then revert to the starting state of the animation in the first
         * {@link #onProgress} callback by using post-layout view properties like {@link View#setX}
         * and related methods.
         * <p>
         * The ordering of events during an insets animation is
         * the following:
         * <ul>
         *     <li>Application calls {@link WindowInsetsControllerCompat#hide(int)},
         *     {@link WindowInsetsControllerCompat#show(int)},
         *     {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation}</li>
         *     <li>onPrepare is called on the view hierarchy listeners</li>
         *     <li>{@link View#onApplyWindowInsets} will be called with the end state of the
         *     animation</li>
         *     <li>View hierarchy gets laid out according to the changes the application has
         *     requested due to the new insets being dispatched</li>
         *     <li>{@link #onStart} is called <em>before</em> the view
         *     hierarchy gets drawn in the new laid out state</li>
         *     <li>{@link #onProgress} is called immediately after with the animation start
         *     state</li>
         *     <li>The frame gets drawn.</li>
         * </ul>
         * <p>
         * Note: If the animation is application controlled by using
         * {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation}, the end state of
         * the animation is undefined as the application may decide on the end state only by
         * passing in {@code shown} parameter when calling
         * {@link WindowInsetsAnimationControllerCompat#finish}. In this situation, the system
         * will dispatch the insets in the opposite visibility state before the animation starts.
         * Example: When controlling the input method with
         * {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation} and the input method
         * is currently showing, {@link View#onApplyWindowInsets} will receive a
         * {@link WindowInsetsCompat} instance for which {@link WindowInsetsCompat#isVisible}
         * will return {@code false} for {@link WindowInsetsCompat.Type#ime}.
         *
         * @param animation The animation that is about to start.
         */
        public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
        }

        /**
         * Called when an insets animation gets started.
         * <p>
         * This ordering allows the application to inspect the end state after the animation has
         * finished, and then revert to the starting state of the animation in the first
         * {@link #onProgress} callback by using post-layout view properties like {@link View#setX}
         * and related methods.
         * <p>
         * The ordering of events during an insets animation is
         * the following:
         * <ul>
         *     <li>Application calls {@link WindowInsetsControllerCompat#hide(int)},
         *     {@link WindowInsetsControllerCompat#show(int)},
         *     {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation}</li>
         *     <li>onPrepare is called on the view hierarchy listeners</li>
         *     <li>{@link View#onApplyWindowInsets} will be called with the end state of the
         *     animation</li>
         *     <li>View hierarchy gets laid out according to the changes the application has
         *     requested due to the new insets being dispatched</li>
         *     <li>{@link #onStart} is called <em>before</em> the view
         *     hierarchy gets drawn in the new laid out state</li>
         *     <li>{@link #onProgress} is called immediately after with the animation start
         *     state</li>
         *     <li>The frame gets drawn.</li>
         * </ul>
         * <p>
         * Note that, like {@link #onProgress}, dispatch of the animation start event is
         * hierarchical: It will starts at the root of the view hierarchy and then traverse it
         * and invoke the callback of the specific {@link View} that is being traversed. The
         * method may return a modified instance of the bounds by calling
         * {@link BoundsCompat#inset} to indicate that a part of the insets
         * have been used to offset or clip its children, and the children shouldn't worry about
         * that part anymore. Furthermore, if {@link #getDispatchMode()} returns
         * {@link #DISPATCH_MODE_STOP}, children of this view will not receive the callback anymore.
         *
         * @param animation The animation that is about to start.
         * @param bounds    The bounds in which animation happens.
         * @return The animation bounds representing the part of the insets that should be
         * dispatched to
         * the subtree of the hierarchy.
         */
        public @NonNull BoundsCompat onStart(
                @NonNull WindowInsetsAnimationCompat animation,
                @NonNull BoundsCompat bounds) {
            return bounds;
        }

        /**
         * Called when the insets change as part of running an animation. Note that even if multiple
         * animations for different types are running, there will only be one progress callback per
         * frame. The {@code insets} passed as an argument represents the overall state and will
         * include all types, regardless of whether they are animating or not.
         * <p>
         * Note that insets dispatch is hierarchical: It will start at the root of the view
         * hierarchy, and then traverse it and invoke the callback of the specific {@link View}
         * being traversed. The method may return a modified instance by calling
         * {@link WindowInsetsCompat#inset(int, int, int, int)} to indicate that a part of the
         * insets have been used to offset or clip its children, and the children shouldn't worry
         * about that part anymore. Furthermore, if {@link #getDispatchMode()} returns
         * {@link #DISPATCH_MODE_STOP}, children of this view will not receive the callback anymore.
         *
         * @param insets            The current insets.
         * @param runningAnimations The currently running animations.
         * @return The insets to dispatch to the subtree of the hierarchy.
         */
        public abstract @NonNull WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets,
                @NonNull List<WindowInsetsAnimationCompat> runningAnimations);

        /**
         * Called when an insets animation has ended.
         *
         * @param animation The animation that has ended. This will be the same instance
         *                  as passed into {@link #onStart}
         */
        public void onEnd(@NonNull WindowInsetsAnimationCompat animation) {
        }
    }

    static void setCallback(@NonNull View view, @Nullable Callback callback) {
        if (Build.VERSION.SDK_INT >= 30) {
            Impl30.setCallback(view, callback);
        } else {
            Impl21.setCallback(view, callback);
        }
        // Do nothing pre 21
    }

    private static class Impl {
        @InsetsType
        private final int mTypeMask;
        private float mFraction;
        private final @Nullable Interpolator mInterpolator;
        private final long mDurationMillis;
        private float mAlpha = 1f;

        Impl(int typeMask, @Nullable Interpolator interpolator, long durationMillis) {
            mTypeMask = typeMask;
            mInterpolator = interpolator;
            mDurationMillis = durationMillis;
        }

        public int getTypeMask() {
            return mTypeMask;
        }

        public float getFraction() {
            return mFraction;
        }

        public float getInterpolatedFraction() {
            if (mInterpolator != null) {
                return mInterpolator.getInterpolation(mFraction);
            }
            return mFraction;
        }

        public @Nullable Interpolator getInterpolator() {
            return mInterpolator;
        }

        public long getDurationMillis() {
            return mDurationMillis;
        }

        public float getAlpha() {
            return mAlpha;
        }

        public void setFraction(float fraction) {
            mFraction = fraction;
        }

        public void setAlpha(float alpha) {
            mAlpha = alpha;
        }

    }

    private static class Impl21 extends Impl {

        /**
         * A fixed interpolator to use when simulating the window insets animation for showing the
         * IME.
         *
         * This interpolator was picked via experimentation to subjectively improve the end result.
         */
        private static final Interpolator SHOW_IME_INTERPOLATOR =
                new PathInterpolator(0, 1.1f, 0f, 1f);

        /**
         * A fixed interpolator to use when simulating the window insets animation for hiding the
         * IME.
         */
        private static final Interpolator HIDE_IME_INTERPOLATOR =
                new FastOutLinearInInterpolator();

        /**
         * A fixed interpolator to use when simulating the window insets animation for showing
         * system bars.
         *
         * This interpolator and the factor was to align with the legacy animation described in
         * dock_[side]_enter.xml before API 30.
         */
        private static final Interpolator SHOW_SYSTEM_BAR_INTERPOLATOR =
                new DecelerateInterpolator(1.5f /* factor */);

        /**
         * A fixed interpolator to use when simulating the window insets animation for hiding
         * system bars.
         *
         * This interpolator and the factor was to align with the legacy animation described in
         * dock_[side]_exit.xml before API 30.
         */
        private static final Interpolator HIDE_SYSTEM_BAR_INTERPOLATOR =
                new AccelerateInterpolator(1.5f /* factor */);

        Impl21(int typeMask, @Nullable Interpolator interpolator, long durationMillis) {
            super(typeMask, interpolator, durationMillis);
        }

        static void setCallback(final @NonNull View view,
                final @Nullable Callback callback) {
            final View.OnApplyWindowInsetsListener proxyListener = callback != null
                    ? createProxyListener(view, callback)
                    : null;
            view.setTag(R.id.tag_window_insets_animation_callback, proxyListener);

            // We rely on View.OnApplyWindowInsetsListener, but one might already be set by the
            // library, so we only register it on the view if none is set yet.
            // If any of them is set via ViewGroupCompat#installCompatInsetsDispatch or
            // ViewCompat.setOnApplyWindowInsetsListener, this Callback will be called by their
            // listener.
            if (view.getTag(R.id.tag_compat_insets_dispatch) == null
                    && view.getTag(R.id.tag_on_apply_window_listener) == null) {
                view.setOnApplyWindowInsetsListener(proxyListener);
            }
        }

        private static View.@NonNull OnApplyWindowInsetsListener createProxyListener(
                @NonNull View view, final @NonNull Callback callback) {
            return new Impl21OnApplyWindowInsetsListener(view, callback);
        }

        static @NonNull BoundsCompat computeAnimationBounds(
                @NonNull WindowInsetsCompat targetInsets,
                @NonNull WindowInsetsCompat startingInsets, int mask) {
            Insets targetInsetsInsets = targetInsets.getInsets(mask);
            Insets startingInsetsInsets = startingInsets.getInsets(mask);
            final Insets lowerBound = Insets.of(
                    Math.min(targetInsetsInsets.left, startingInsetsInsets.left),
                    Math.min(targetInsetsInsets.top, startingInsetsInsets.top),
                    Math.min(targetInsetsInsets.right, startingInsetsInsets.right),
                    Math.min(targetInsetsInsets.bottom, startingInsetsInsets.bottom)
            );
            final Insets upperBound = Insets.of(
                    Math.max(targetInsetsInsets.left, startingInsetsInsets.left),
                    Math.max(targetInsetsInsets.top, startingInsetsInsets.top),
                    Math.max(targetInsetsInsets.right, startingInsetsInsets.right),
                    Math.max(targetInsetsInsets.bottom, startingInsetsInsets.bottom)
            );
            return new BoundsCompat(lowerBound, upperBound);
        }

        @SuppressLint("WrongConstant") // We iterate over all the constants.
        static void buildAnimationMask(@NonNull WindowInsetsCompat targetInsets,
                @NonNull WindowInsetsCompat currentInsets, int[] showingTypes, int[] hidingTypes) {
            for (int i = WindowInsetsCompat.Type.FIRST; i <= WindowInsetsCompat.Type.LAST;
                    i = i << 1) {
                final Insets target = targetInsets.getInsets(i);
                final Insets current = currentInsets.getInsets(i);
                final boolean showing = target.left > current.left
                        || target.top > current.top
                        || target.right > current.right
                        || target.bottom > current.bottom;
                final boolean hiding = target.left < current.left
                        || target.top < current.top
                        || target.right < current.right
                        || target.bottom < current.bottom;
                // If both showing and hiding are true, it can be the side change of navigation bar.
                // Don't consider that it is playing an animation.
                if (showing != hiding) {
                    if (showing) {
                        showingTypes[0] |= i;
                    } else {
                        hidingTypes[0] |= i;
                    }
                }
            }
        }

        /**
         * Determine which interpolator to use based on which insets are being animated.
         *
         * This allows for a smoother animation especially in the common case of showing and hiding
         * the IME.
         */
        static @Nullable Interpolator createInsetInterpolator(int showingTypes, int hidingTypes) {
            if ((showingTypes & WindowInsetsCompat.Type.ime()) != 0) {
                return SHOW_IME_INTERPOLATOR;
            } else if ((hidingTypes & WindowInsetsCompat.Type.ime()) != 0) {
                return HIDE_IME_INTERPOLATOR;
            } else if ((showingTypes & WindowInsetsCompat.Type.systemBars()) != 0) {
                return SHOW_SYSTEM_BAR_INTERPOLATOR;
            } else if ((hidingTypes & WindowInsetsCompat.Type.systemBars()) != 0) {
                return HIDE_SYSTEM_BAR_INTERPOLATOR;
            }
            return null;
        }

        @SuppressLint("WrongConstant")
        static WindowInsetsCompat interpolateInsets(
                WindowInsetsCompat target, WindowInsetsCompat starting,
                float fraction, int typeMask) {
            WindowInsetsCompat.Builder builder = new WindowInsetsCompat.Builder(target);
            for (int i = WindowInsetsCompat.Type.FIRST; i <= WindowInsetsCompat.Type.LAST;
                    i = i << 1) {
                if ((typeMask & i) == 0) {
                    builder.setInsets(i, target.getInsets(i));
                    continue;
                }
                Insets targetInsets = target.getInsets(i);
                Insets startingInsets = starting.getInsets(i);
                Insets interpolatedInsets = WindowInsetsCompat.insetInsets(
                        targetInsets,
                        (int) (0.5 + (targetInsets.left - startingInsets.left) * (1 - fraction)),
                        (int) (0.5 + (targetInsets.top - startingInsets.top) * (1 - fraction)),
                        (int) (0.5 + (targetInsets.right - startingInsets.right) * (1 - fraction)),
                        (int) (0.5 + (targetInsets.bottom - startingInsets.bottom) * (1 - fraction))

                );
                builder.setInsets(i, interpolatedInsets);
            }

            return builder.build();
        }

        /**
         * Wrapper class around a {@link Callback} that will trigger the callback when
         * {@link View#onApplyWindowInsets(WindowInsets)} is called
         */
        private static class Impl21OnApplyWindowInsetsListener implements
                View.OnApplyWindowInsetsListener {

            private static final int COMPAT_ANIMATION_DURATION_IME = 160;
            private static final int COMPAT_ANIMATION_DURATION_SYSTEM_BAR = 250;

            final Callback mCallback;
            // We save the last insets to compute the starting insets for the animation.
            private WindowInsetsCompat mLastInsets;

            Impl21OnApplyWindowInsetsListener(@NonNull View view, @NonNull Callback callback) {
                mCallback = callback;
                WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(view);
                mLastInsets = rootWindowInsets != null
                        // Insets are not immutable on SDK < 26 so we make copy to ensure it's not
                        // changed until we need them.
                        ? new WindowInsetsCompat.Builder(rootWindowInsets).build()
                        : null;
            }

            @Override
            public @NonNull WindowInsets onApplyWindowInsets(final View v,
                    @NonNull WindowInsets insets) {
                // We cannot rely on the compat insets value until the view is laid out.
                if (!v.isLaidOut()) {
                    mLastInsets = toWindowInsetsCompat(insets, v);
                    return forwardToViewIfNeeded(v, insets);
                }

                final WindowInsetsCompat targetInsets = toWindowInsetsCompat(insets, v);

                if (mLastInsets == null) {
                    mLastInsets = ViewCompat.getRootWindowInsets(v);
                }

                if (mLastInsets == null) {
                    if (DEBUG) {
                        Log.d(TAG, "Couldn't initialize last insets");
                    }
                    mLastInsets = targetInsets;
                    return forwardToViewIfNeeded(v, insets);
                }

                if (DEBUG) {
                    int allTypes = WindowInsetsCompat.Type.all();
                    Log.d(TAG, String.format("lastInsets:   %s\ntargetInsets: %s",
                            mLastInsets.getInsets(allTypes),
                            targetInsets.getInsets(allTypes)));
                }

                // When we start dispatching the insets animation, we save the instance of insets
                // that have been dispatched first as a marker to avoid dispatching the callback
                // in children.
                Callback callback = getCallback(v);
                if (callback != null && Objects.equals(callback.mDispachedInsets, targetInsets)) {
                    return forwardToViewIfNeeded(v, insets);
                }

                // We only run the animation when the some insets are animating
                final int[] showingTypes = new int[1];
                final int[] hidingTypes = new int[1];
                buildAnimationMask(targetInsets, mLastInsets, showingTypes, hidingTypes);
                final int animationMask = showingTypes[0] | hidingTypes[0];

                if (animationMask == 0) {
                    if (DEBUG) {
                        Log.d(TAG, "Insets applied but no window animation to run");
                    }
                    mLastInsets = targetInsets;
                    return forwardToViewIfNeeded(v, insets);
                }

                final WindowInsetsCompat startingInsets = this.mLastInsets;

                final Interpolator interpolator = createInsetInterpolator(
                        showingTypes[0], hidingTypes[0]);

                final WindowInsetsAnimationCompat anim =
                        new WindowInsetsAnimationCompat(animationMask, interpolator,
                                (animationMask & WindowInsetsCompat.Type.ime()) != 0
                                        ? COMPAT_ANIMATION_DURATION_IME
                                        : COMPAT_ANIMATION_DURATION_SYSTEM_BAR);
                anim.setFraction(0);

                final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f).setDuration(
                        anim.getDurationMillis());

                // Compute the bounds of the animation
                final BoundsCompat animationBounds = computeAnimationBounds(targetInsets,
                        startingInsets, animationMask
                );

                dispatchOnPrepare(v, anim, targetInsets, false);

                animator.addUpdateListener(
                        new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                anim.setFraction(animator.getAnimatedFraction());
                                WindowInsetsCompat interpolateInsets = interpolateInsets(
                                        targetInsets,
                                        startingInsets,
                                        anim.getInterpolatedFraction(), animationMask);
                                List<WindowInsetsAnimationCompat> runningAnimations =
                                        Collections.singletonList(anim);
                                dispatchOnProgress(v, interpolateInsets, runningAnimations);
                            }
                        });

                animator.addListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        anim.setFraction(1);
                        dispatchOnEnd(v, anim);
                    }
                });

                // We need to call onStart and start the animator before the next draw
                // to ensure the animation starts before the relayout caused by the change of
                // insets.
                OneShotPreDrawListener.add(v, new Runnable() {
                    @Override
                    public void run() {
                        dispatchOnStart(v, anim, animationBounds);
                        animator.start();
                    }
                });
                this.mLastInsets = targetInsets;

                return forwardToViewIfNeeded(v, insets);
            }
        }

        /**
         * Forward the call to view.onApplyWindowInsets if there is no other listener attached to
         * the view.
         */
        static @NonNull WindowInsets forwardToViewIfNeeded(@NonNull View v,
                @NonNull WindowInsets insets) {
            // If the app set an on apply window listener, it will be called after this
            // and will decide whether to call the view's onApplyWindowInsets.
            if (v.getTag(R.id.tag_on_apply_window_listener) != null) {
                return insets;
            }
            return v.onApplyWindowInsets(insets);
        }

        static void dispatchOnPrepare(View v, WindowInsetsAnimationCompat anim,
                WindowInsetsCompat insets, boolean stopDispatch) {
            final Callback callback = getCallback(v);
            if (callback != null) {
                callback.mDispachedInsets = insets;
                if (!stopDispatch) {
                    callback.onPrepare(anim);
                    stopDispatch = callback.getDispatchMode() == Callback.DISPATCH_MODE_STOP;
                }
            }
            // When stopDispatch is true, we don't call onPrepare but we still need to propagate
            // the dispatched insets to the children to mark them with the latest dispatched
            // insets so their compat callback in not called when onApplyWindowInsets is called.
            if (v instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) v;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    dispatchOnPrepare(child, anim, insets, stopDispatch);
                }
            }
        }

        static void dispatchOnStart(View v,
                WindowInsetsAnimationCompat anim,
                BoundsCompat animationBounds) {
            final Callback callback = getCallback(v);
            if (callback != null) {
                callback.onStart(anim, animationBounds);
                if (callback.getDispatchMode() == Callback.DISPATCH_MODE_STOP) {
                    return;
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) v;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    dispatchOnStart(child, anim, animationBounds);
                }
            }
        }

        static void dispatchOnProgress(@NonNull View v,
                @NonNull WindowInsetsCompat interpolateInsets,
                @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
            final Callback callback = getCallback(v);
            WindowInsetsCompat insets = interpolateInsets;
            if (callback != null) {
                insets = callback.onProgress(insets, runningAnimations);
                if (callback.getDispatchMode() == Callback.DISPATCH_MODE_STOP) {
                    return;
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) v;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    dispatchOnProgress(child, insets, runningAnimations);
                }
            }
        }

        static void dispatchOnEnd(@NonNull View v,
                @NonNull WindowInsetsAnimationCompat anim) {
            final Callback callback = getCallback(v);
            if (callback != null) {
                callback.onEnd(anim);
                if (callback.getDispatchMode() == Callback.DISPATCH_MODE_STOP) {
                    return;
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) v;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    dispatchOnEnd(child, anim);
                }
            }
        }

        static @Nullable Callback getCallback(View child) {
            Object listener = child.getTag(
                    R.id.tag_window_insets_animation_callback);
            Callback callback = null;
            if (listener instanceof Impl21OnApplyWindowInsetsListener) {
                callback = ((Impl21OnApplyWindowInsetsListener) listener).mCallback;
            }
            return callback;
        }
    }

    @RequiresApi(30)
    private static class Impl30 extends Impl {

        private final @NonNull WindowInsetsAnimation mWrapped;

        Impl30(@NonNull WindowInsetsAnimation wrapped) {
            super(0, null, 0);
            mWrapped = wrapped;
        }

        Impl30(int typeMask, Interpolator interpolator, long durationMillis) {
            this(new WindowInsetsAnimation(typeMask, interpolator, durationMillis));
        }

        @Override
        public int getTypeMask() {
            return mWrapped.getTypeMask();
        }

        @Override
        public @Nullable Interpolator getInterpolator() {
            return mWrapped.getInterpolator();
        }

        @Override
        public long getDurationMillis() {
            return mWrapped.getDurationMillis();
        }

        @Override
        public float getFraction() {
            return mWrapped.getFraction();
        }

        @Override
        public void setFraction(float fraction) {
            mWrapped.setFraction(fraction);
        }

        @Override
        public float getInterpolatedFraction() {
            return mWrapped.getInterpolatedFraction();
        }

        @Override
        public float getAlpha() {
            return mWrapped.getAlpha();
        }

        @Override
        public void setAlpha(float alpha) {
            mWrapped.setAlpha(alpha);
        }

        @RequiresApi(30)
        private static class ProxyCallback extends WindowInsetsAnimation.Callback {

            private final Callback mCompat;

            ProxyCallback(final WindowInsetsAnimationCompat.@NonNull Callback compat) {
                super(compat.getDispatchMode());
                mCompat = compat;
            }

            private List<WindowInsetsAnimationCompat> mRORunningAnimations;
            private ArrayList<WindowInsetsAnimationCompat> mTmpRunningAnimations;
            private final HashMap<WindowInsetsAnimation, WindowInsetsAnimationCompat>
                    mAnimations = new HashMap<>();

            private @NonNull WindowInsetsAnimationCompat getWindowInsetsAnimationCompat(
                    @NonNull WindowInsetsAnimation animation) {
                WindowInsetsAnimationCompat animationCompat = mAnimations.get(
                        animation);
                if (animationCompat == null) {
                    animationCompat = toWindowInsetsAnimationCompat(animation);
                    mAnimations.put(animation, animationCompat);
                }
                return animationCompat;
            }

            @Override
            public void onPrepare(@NonNull WindowInsetsAnimation animation) {
                mCompat.onPrepare(getWindowInsetsAnimationCompat(animation));
            }

            @Override
            public WindowInsetsAnimation.@NonNull Bounds onStart(
                    @NonNull WindowInsetsAnimation animation,
                    WindowInsetsAnimation.@NonNull Bounds bounds) {
                return mCompat.onStart(
                        getWindowInsetsAnimationCompat(animation),
                        BoundsCompat.toBoundsCompat(bounds)).toBounds();
            }

            @Override
            public @NonNull WindowInsets onProgress(@NonNull WindowInsets insets,
                    @NonNull List<WindowInsetsAnimation> runningAnimations) {
                if (mTmpRunningAnimations == null) {
                    mTmpRunningAnimations = new ArrayList<>(runningAnimations.size());
                    mRORunningAnimations = Collections.unmodifiableList(mTmpRunningAnimations);
                } else {
                    mTmpRunningAnimations.clear();
                }

                for (int i = runningAnimations.size() - 1; i >= 0; i--) {
                    WindowInsetsAnimation animation = runningAnimations.get(i);
                    WindowInsetsAnimationCompat animationCompat =
                            getWindowInsetsAnimationCompat(animation);
                    animationCompat.setFraction(animation.getFraction());
                    mTmpRunningAnimations.add(animationCompat);
                }
                return mCompat.onProgress(
                        WindowInsetsCompat.toWindowInsetsCompat(insets),
                        mRORunningAnimations).toWindowInsets();
            }

            @Override
            public void onEnd(@NonNull WindowInsetsAnimation animation) {
                mCompat.onEnd(getWindowInsetsAnimationCompat(animation));
                mAnimations.remove(animation);
            }
        }

        public static void setCallback(@NonNull View view, @Nullable Callback callback) {
            WindowInsetsAnimation.Callback platformCallback =
                    callback != null ? new ProxyCallback(callback) : null;
            view.setWindowInsetsAnimationCallback(platformCallback);
        }

        public static WindowInsetsAnimation.@NonNull Bounds createPlatformBounds(
                @NonNull BoundsCompat bounds) {
            return new WindowInsetsAnimation.Bounds(bounds.getLowerBound().toPlatformInsets(),
                    bounds.getUpperBound().toPlatformInsets());
        }

        public static @NonNull Insets getLowerBounds(WindowInsetsAnimation.@NonNull Bounds bounds) {
            return Insets.toCompatInsets(bounds.getLowerBound());
        }

        public static @NonNull Insets getHigherBounds(
                WindowInsetsAnimation.@NonNull Bounds bounds) {
            return Insets.toCompatInsets(bounds.getUpperBound());
        }
    }
}
