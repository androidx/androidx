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

import android.view.View;
import android.view.animation.Interpolator;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Class representing an animation of a set of windows that cause insets.
 */
public final class WindowInsetsAnimationCompat {

    @InsetsType
    private final int mTypeMask;
    private float mFraction;
    @Nullable
    private final Interpolator mInterpolator;
    private final long mDurationMillis;
    private float mAlpha;

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
        mTypeMask = typeMask;
        mInterpolator = interpolator;
        mDurationMillis = durationMillis;
    }

    /**
     * @return The bitmask of {@link Type} that are animating.
     */
    @InsetsType
    public int getTypeMask() {
        return mTypeMask;
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
        return mFraction;
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
        if (mInterpolator != null) {
            return mInterpolator.getInterpolation(mFraction);
        }
        return mFraction;
    }

    /**
     * Retrieves the interpolator used for this animation, or {@code null} if this animation
     * doesn't follow an interpolation curved. For system-initiated animations, this will never
     * return {@code null}.
     *
     * @return The interpolator used for this animation.
     */
    @Nullable
    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    /**
     * @return duration of animation in {@link java.util.concurrent.TimeUnit#MILLISECONDS}, or
     * -1 if the animation doesn't have a fixed duration.
     */
    public long getDurationMillis() {
        return mDurationMillis;
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
        mFraction = fraction;
    }

    /**
     * Retrieves the translucency of the windows that are animating.
     *
     * @return Alpha of windows that cause insets of type {@link Type}.
     */
    @FloatRange(from = 0f, to = 1f)
    public float getAlpha() {
        return mAlpha;
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
        mAlpha = alpha;
    }

    /**
     * Class representing the range of an {@link WindowInsetsAnimationCompat}
     */
    public static final class Bounds {

        private final Insets mLowerBound;
        private final Insets mUpperBound;

        public Bounds(@NonNull Insets lowerBound, @NonNull Insets upperBound) {
            mLowerBound = lowerBound;
            mUpperBound = upperBound;
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
        @NonNull
        public Insets getLowerBound() {
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
        @NonNull
        public Insets getUpperBound() {
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
        @NonNull
        public Bounds inset(@NonNull Insets insets) {
            return new Bounds(
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

        /** @hide */
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
         * {@link WindowInsetsAnimationCompat.Bounds#inset} to indicate that a part of the insets
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
        @NonNull
        public WindowInsetsAnimationCompat.Bounds onStart(
                @NonNull WindowInsetsAnimationCompat animation,
                @NonNull WindowInsetsAnimationCompat.Bounds bounds) {
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
        @NonNull
        public abstract WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets,
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
}
