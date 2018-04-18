/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.UiThread;

import java.util.ArrayList;

/**
 * A layout enabling left-to-right swipe-to-dismiss, intended for use within an activity.
 *
 * <p>At least one listener must be {@link #addCallback(Callback) added} to act on a dismissal
 * action. A listener will typically remove a containing view or fragment from the current
 * activity.
 *
 * <p>To suppress a swipe-dismiss gesture, at least one contained view must be scrollable,
 * indicating that it would like to consume any horizontal touch gestures in that direction. In
 * this  case this view will only allow swipe-to-dismiss on the very edge of the left-hand-side of
 * the screen. If you wish to entirely disable the swipe-to-dismiss gesture,
 * {@link #setSwipeable(boolean)} can be used for more direct control over the feature.
 */
@UiThread
public class SwipeDismissFrameLayout extends SwipeDismissLayout {

    private static final String TAG = "SwipeDismissFrameLayout";

    private static final float TRANSLATION_MIN_ALPHA = 0.5f;
    private static final float DEFAULT_INTERPOLATION_FACTOR = 1.5f;

    /** Implement this callback to act on particular stages of the dismissal. */
    @UiThread
    public abstract static class Callback {
        /**
         * Notifies listeners that the view is now considering to start a dismiss gesture from a
         * particular point on the screen. The default implementation returns true for all
         * coordinates so that is is possible to start a swipe-to-dismiss gesture from any location.
         * If any one instance of this Callback returns false for a given set of coordinates,
         * swipe-to-dismiss will not be allowed to start in that point.
         *
         * @param layout The layout associated with this callback.
         * @param xDown The x coordinate of the initial {@link android.view.MotionEvent#ACTION_DOWN}
         *              event for this motion.
         * @param yDown The y coordinate of the initial {@link android.view.MotionEvent#ACTION_DOWN}
         *              event for this motion.
         * @return true if this gesture should be recognized as a swipe to dismiss gesture, false
         * otherwise.
         */
        boolean onPreSwipeStart(SwipeDismissFrameLayout layout, float xDown, float yDown) {
            return true;
        }

        /**
         * Notifies listeners that the view is now being dragged as part of a dismiss gesture.
         *
         * @param layout The layout associated with this callback.
        */
        public void onSwipeStarted(SwipeDismissFrameLayout layout) {
        }

        /**
         * Notifies listeners that the swipe gesture has ended without a dismissal.
         *
         * @param layout The layout associated with this callback.
         */
        public void onSwipeCanceled(SwipeDismissFrameLayout layout) {
        }

        /**
         * Notifies listeners the dismissal is complete and the view now off screen.
         *
         * @param layout The layout associated with this callback.
         */
        public void onDismissed(SwipeDismissFrameLayout layout) {
        }
    }

    private final OnPreSwipeListener mOnPreSwipeListener = new MyOnPreSwipeListener();
    private final OnDismissedListener mOnDismissedListener = new MyOnDismissedListener();

    private final OnSwipeProgressChangedListener mOnSwipeProgressListener =
            new MyOnSwipeProgressChangedListener();

    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private final int mAnimationTime;
    private final DecelerateInterpolator mCancelInterpolator;
    private final AccelerateInterpolator mDismissInterpolator;
    private final DecelerateInterpolator mCompleteDismissGestureInterpolator;

    private boolean mStarted;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The {@link Context} the view is running in, through which it can access the
     *                current theme, resources, etc.
     */
    public SwipeDismissFrameLayout(Context context) {
        this(context, null, 0);
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called when a view is
     * being constructed from an XML file, supplying attributes that were specified in the XML file.
     * This version uses a default style of 0, so the only attribute values applied are those in the
     * Context's Theme and the given AttributeSet.
     *
     * <p>
     *
     * <p>The method onFinishInflate() will be called after all children have been added.
     *
     * @param context The {@link Context} the view is running in, through which it can access the
     *                current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public SwipeDismissFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * This constructor allows subclasses to use their own base style when they are inflating.
     *
     * @param context  The {@link Context} the view is running in, through which it can access the
     *                 current theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     *                 resource that supplies default values for the view. Can be 0 to not look for
     *                 defaults.
     */
    public SwipeDismissFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * This constructor allows subclasses to use their own base style when they are inflating.
     *
     * @param context  The {@link Context} the view is running in, through which it can access the
     *                 current theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     *                 resource that supplies default values for the view. Can be 0 to not look for
     *                 defaults.
     * @param defStyleRes This corresponds to the fourth argument
     *                    of {@link View#View(Context, AttributeSet, int, int)}. It allows a style
     *                    resource to be specified when creating the view.
     */
    public SwipeDismissFrameLayout(Context context, AttributeSet attrs, int defStyle,
            int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        setOnPreSwipeListener(mOnPreSwipeListener);
        setOnDismissedListener(mOnDismissedListener);
        setOnSwipeProgressChangedListener(mOnSwipeProgressListener);
        mAnimationTime = getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mCancelInterpolator = new DecelerateInterpolator(DEFAULT_INTERPOLATION_FACTOR);
        mDismissInterpolator = new AccelerateInterpolator(DEFAULT_INTERPOLATION_FACTOR);
        mCompleteDismissGestureInterpolator = new DecelerateInterpolator(
                DEFAULT_INTERPOLATION_FACTOR);
    }

    /** Adds a callback for dismissal. */
    public void addCallback(Callback callback) {
        if (callback == null) {
            throw new NullPointerException("addCallback called with null callback");
        }
        mCallbacks.add(callback);
    }

    /** Removes a callback that was added with {@link #addCallback(Callback)}. */
    public void removeCallback(Callback callback) {
        if (callback == null) {
            throw new NullPointerException("removeCallback called with null callback");
        }
        if (!mCallbacks.remove(callback)) {
            throw new IllegalStateException("removeCallback called with nonexistent callback");
        }
    }

    /**
     * Resets this view to the original state. This method cancels any pending animations on this
     * view and resets the alpha as well as x translation values.
     */
    private void resetTranslationAndAlpha() {
        animate().cancel();
        setTranslationX(0);
        setAlpha(1);
        mStarted = false;
    }

    private final class MyOnPreSwipeListener implements OnPreSwipeListener {

        @Override
        public boolean onPreSwipe(SwipeDismissLayout layout, float xDown, float yDown) {
            for (Callback callback : mCallbacks) {
                if (!callback.onPreSwipeStart(SwipeDismissFrameLayout.this, xDown, yDown)) {
                    return false;
                }
            }
            return true;
        }
    }

    private final class MyOnDismissedListener implements OnDismissedListener {

        @Override
        public void onDismissed(SwipeDismissLayout layout) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onDismissed()");
            }
            animate()
                    .translationX(getWidth())
                    .alpha(0)
                    .setDuration(mAnimationTime)
                    .setInterpolator(
                            mStarted ? mCompleteDismissGestureInterpolator : mDismissInterpolator)
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                                        Callback callbacks = mCallbacks.get(i);
                                        callbacks.onDismissed(SwipeDismissFrameLayout.this);
                                    }
                                    resetTranslationAndAlpha();
                                }
                            });
        }
    }

    private final class MyOnSwipeProgressChangedListener implements OnSwipeProgressChangedListener {

        @Override
        public void onSwipeProgressChanged(SwipeDismissLayout layout, float progress,
                float translate) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSwipeProgressChanged() - " + translate);
            }
            setTranslationX(translate);
            setAlpha(1 - (progress * TRANSLATION_MIN_ALPHA));
            if (!mStarted) {
                for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                    Callback callbacks = mCallbacks.get(i);
                    callbacks.onSwipeStarted(SwipeDismissFrameLayout.this);
                }
                mStarted = true;
            }
        }

        @Override
        public void onSwipeCanceled(SwipeDismissLayout layout) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSwipeCanceled() run swipe cancel animation");
            }
            mStarted = false;
            animate()
                    .translationX(0)
                    .alpha(1)
                    .setDuration(mAnimationTime)
                    .setInterpolator(mCancelInterpolator)
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                                        Callback callbacks = mCallbacks.get(i);
                                        callbacks.onSwipeCanceled(SwipeDismissFrameLayout.this);
                                    }
                                    resetTranslationAndAlpha();
                                }
                            });
        }
    }
}
