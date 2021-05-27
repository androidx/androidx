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
public class SwipeDismissFrameLayout extends DismissibleFrameLayout {

    public static final float DEFAULT_DISMISS_DRAG_WIDTH_RATIO = .33f;


    /** Implement this callback to act on particular stages of the dismissal. */
    @UiThread
    public abstract static class Callback {

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
         * Notifies listeners that the dismissal is complete and the view is now off screen.
         * @param layout The layout associated with this callback.
         */
        public void onDismissed(SwipeDismissFrameLayout layout) {
        }
    }

    final ArrayList<Callback> mCallbacksCompat = new ArrayList<>();

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
     * @param defStyleRes It allows a style resource to be specified when creating the view.
     */
    public SwipeDismissFrameLayout(Context context, AttributeSet attrs, int defStyle,
            int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
    }

    /** Adds a callback for dismissal. */
    public void addCallback(Callback callback) {
        if (callback == null) {
            throw new NullPointerException("addCallback called with null callback");
        }

        mCallbacksCompat.add(callback);
    }

    /** Removes a callback that was added with {@link #addCallback(Callback)}. */
    public void removeCallback(Callback callback) {
        if (callback == null) {
            throw new NullPointerException("removeCallback called with null callback");
        }
        if (!mCallbacksCompat.remove(callback)) {
            throw new IllegalStateException("removeCallback called with nonexistent callback");
        }
    }

    /**
     * Set the layout to be dismissible by swipe or not.
     * @param swipeable Whether the layout should react to the swipe gesture.
     */
    public void setSwipeable(boolean swipeable) {
        super.setSwipeDismissible(swipeable);
    }

    /** Returns true if the frame layout can be dismissed by swipe gestures. */
    public boolean isSwipeable() {
        return super.isDismissableBySwipe();
    }

    /**
     * Sets the minimum ratio of the screen after which the swipe gesture is treated as
     * swipe-to-dismiss.
     *
     * @param ratio the ratio of the screen at which the swipe gesture is treated as
     *              swipe-to-dismiss. should be provided as a fraction of the screen
     */
    public void setDismissMinDragWidthRatio(float ratio) {
        if (isSwipeable()) {
            getSwipeDismissController().setDismissMinDragWidthRatio(ratio);
        }
    }

    /**
     * Gets the minimum ratio of the screen after which the swipe gesture is treated as
     * swipe-to-dismiss.
     */
    public float getDismissMinDragWidthRatio() {
        if (isSwipeable()) {
            return getSwipeDismissController().getDismissMinDragWidthRatio();
        }
        return DEFAULT_DISMISS_DRAG_WIDTH_RATIO;
    }

    @Override
    protected void performDismissFinishedCallbacks() {
        super.performDismissFinishedCallbacks();
        for (int i = mCallbacksCompat.size() - 1; i >= 0; i--) {
            mCallbacksCompat.get(i).onDismissed(this);
        }
    }

    @Override
    protected void performDismissStartedCallbacks() {
        super.performDismissStartedCallbacks();
        for (int i = mCallbacksCompat.size() - 1; i >= 0; i--) {
            mCallbacksCompat.get(i).onSwipeStarted(this);
        }
    }

    @Override
    protected void performDismissCanceledCallbacks() {
        super.performDismissCanceledCallbacks();
        for (int i = mCallbacksCompat.size() - 1; i >= 0; i--) {
            mCallbacksCompat.get(i).onSwipeCanceled(this);
        }
    }
}