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

package android.support.wear.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Special layout that finishes its activity when swiped away.
 *
 * <p>This is a modified copy of the internal framework class
 * com.android.internal.widget.SwipeDismissLayout.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@UiThread
class SwipeDismissLayout extends FrameLayout {
    private static final String TAG = "SwipeDismissLayout";

    public static final float DEFAULT_DISMISS_DRAG_WIDTH_RATIO = .33f;
    // A value between 0.0 and 1.0 determining the percentage of the screen on the left-hand-side
    // where edge swipe gestures are permitted to begin.
    private static final float EDGE_SWIPE_THRESHOLD = 0.1f;

    /** Called when the layout is about to consider a swipe. */
    @UiThread
    interface OnPreSwipeListener {
        /**
         * Notifies listeners that the view is now considering to start a dismiss gesture from a
         * particular point on the screen. The default implementation returns true for all
         * coordinates so that is is possible to start a swipe-to-dismiss gesture from any location.
         * If any one instance of this Callback returns false for a given set of coordinates,
         * swipe-to-dismiss will not be allowed to start in that point.
         *
         * @param xDown the x coordinate of the initial {@link android.view.MotionEvent#ACTION_DOWN}
         *              event for this motion
         * @param yDown the y coordinate of the initial {@link android.view.MotionEvent#ACTION_DOWN}
         *              event for this motion
         * @return {@code true} if these coordinates should be considered as a start of a swipe
         * gesture, {@code false} otherwise
         */
        boolean onPreSwipe(SwipeDismissLayout swipeDismissLayout, float xDown, float yDown);
    }

    /**
     * Interface enabling listeners to react to when the swipe gesture is done and the view should
     * probably be dismissed from the UI.
     */
    @UiThread
    interface OnDismissedListener {
        void onDismissed(SwipeDismissLayout layout);
    }

    /**
     * Interface enabling listeners to react to changes in the progress of the swipe-to-dismiss
     * gesture.
     */
    @UiThread
    interface OnSwipeProgressChangedListener {
        /**
         * Called when the layout has been swiped and the position of the window should change.
         *
         * @param layout    the layout associated with this listener.
         * @param progress  a number in [0, 1] representing how far to the right the window has
         *                  been swiped
         * @param translate a number in [0, w], where w is the width of the layout. This is
         *                  equivalent to progress * layout.getWidth()
         */
        void onSwipeProgressChanged(SwipeDismissLayout layout, float progress, float translate);

        /**
         * Called when the layout started to be swiped away but then the gesture was canceled.
         *
         * @param layout    the layout associated with this listener
         */
        void onSwipeCanceled(SwipeDismissLayout layout);
    }

    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private float mGestureThresholdPx;

    // Transient properties
    private int mActiveTouchId;
    private float mDownX;
    private float mDownY;
    private boolean mSwipeable;
    private boolean mSwiping;
    // This variable holds information about whether the initial move of a longer swipe
    // (consisting of multiple move events) has conformed to the definition of a horizontal
    // swipe-to-dismiss. A swipe gesture is only ever allowed to be recognized if this variable is
    // set to true. Otherwise, the motion events will be allowed to propagate to the children.
    private boolean mCanStartSwipe = true;
    private boolean mDismissed;
    private boolean mDiscardIntercept;
    private VelocityTracker mVelocityTracker;
    private float mTranslationX;
    private boolean mDisallowIntercept;

    @Nullable
    private OnPreSwipeListener mOnPreSwipeListener;
    private OnDismissedListener mDismissedListener;
    private OnSwipeProgressChangedListener mProgressListener;

    private float mLastX;
    private float mDismissMinDragWidthRatio = DEFAULT_DISMISS_DRAG_WIDTH_RATIO;

    SwipeDismissLayout(Context context) {
        this(context, null);
    }

    SwipeDismissLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    SwipeDismissLayout(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    SwipeDismissLayout(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mGestureThresholdPx =
                Resources.getSystem().getDisplayMetrics().widthPixels * EDGE_SWIPE_THRESHOLD;

        // By default, the view is swipeable.
        setSwipeable(true);
    }

    /**
     * Sets the minimum ratio of the screen after which the swipe gesture is treated as swipe-to-
     * dismiss.
     *
     * @param ratio  the ratio of the screen at which the swipe gesture is treated as
     *               swipe-to-dismiss. should be provided as a fraction of the screen
     */
    public void setDismissMinDragWidthRatio(float ratio) {
        mDismissMinDragWidthRatio = ratio;
    }

    /**
     * Returns the current ratio of te screen at which the swipe gesture is treated as
     * swipe-to-dismiss.
     *
     * @return the current ratio of te screen at which the swipe gesture is treated as
     * swipe-to-dismiss
     */
    public float getDismissMinDragWidthRatio() {
        return mDismissMinDragWidthRatio;
    }

    /**
     * Sets the layout to swipeable or not. This effectively turns the functionality of this layout
     * on or off.
     *
     * @param swipeable whether the layout should react to the swipe gesture
     */
    public void setSwipeable(boolean swipeable) {
        mSwipeable = swipeable;
    }

    /** Returns true if the layout reacts to swipe gestures. */
    public boolean isSwipeable() {
        return mSwipeable;
    }

    void setOnPreSwipeListener(@Nullable OnPreSwipeListener listener) {
        mOnPreSwipeListener = listener;
    }

    void setOnDismissedListener(@Nullable OnDismissedListener listener) {
        mDismissedListener = listener;
    }

    void setOnSwipeProgressChangedListener(@Nullable OnSwipeProgressChangedListener listener) {
        mProgressListener = listener;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mDisallowIntercept = disallowIntercept;
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mSwipeable) {
            return super.onInterceptTouchEvent(ev);
        }

        // offset because the view is translated during swipe
        ev.offsetLocation(mTranslationX, 0);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetMembers();
                mDownX = ev.getRawX();
                mDownY = ev.getRawY();
                mActiveTouchId = ev.getPointerId(0);
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(ev);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                int actionIndex = ev.getActionIndex();
                mActiveTouchId = ev.getPointerId(actionIndex);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                actionIndex = ev.getActionIndex();
                int pointerId = ev.getPointerId(actionIndex);
                if (pointerId == mActiveTouchId) {
                    // This was our active pointer going up. Choose a new active pointer.
                    int newActionIndex = actionIndex == 0 ? 1 : 0;
                    mActiveTouchId = ev.getPointerId(newActionIndex);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                resetMembers();
                break;

            case MotionEvent.ACTION_MOVE:
                if (mVelocityTracker == null || mDiscardIntercept) {
                    break;
                }

                int pointerIndex = ev.findPointerIndex(mActiveTouchId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointer index: ignoring.");
                    mDiscardIntercept = true;
                    break;
                }
                float dx = ev.getRawX() - mDownX;
                float x = ev.getX(pointerIndex);
                float y = ev.getY(pointerIndex);

                if (dx != 0 && mDownX >= mGestureThresholdPx && canScroll(this, false, dx, x, y)) {
                    mDiscardIntercept = true;
                    break;
                }
                updateSwiping(ev);
                break;
        }

        if ((mOnPreSwipeListener == null && !mDisallowIntercept)
                || mOnPreSwipeListener.onPreSwipe(this, mDownX, mDownY)) {
            return (!mDiscardIntercept && mSwiping);
        }
        return false;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        // This view can only be swiped horizontally from left to right - this means a negative
        // SCROLLING direction. We return false if the view is not visible to avoid capturing swipe
        // gestures when the view is hidden.
        return direction < 0 && isSwipeable() && getVisibility() == View.VISIBLE;
    }

    /**
     * Helper function determining if a particular move gesture was verbose enough to qualify as a
     * beginning of a swipe.
     *
     * @param dx distance traveled in the x direction, from the initial touch down
     * @param dy distance traveled in the y direction, from the initial touch down
     * @return {@code true} if the gesture was long enough to be considered a potential swipe
     */
    private boolean isPotentialSwipe(float dx, float dy) {
        return (dx * dx) + (dy * dy) > mSlop * mSlop;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mSwipeable) {
            return super.onTouchEvent(ev);
        }

        if (mVelocityTracker == null) {
            return super.onTouchEvent(ev);
        }

        if (mOnPreSwipeListener != null && !mOnPreSwipeListener.onPreSwipe(this, mDownX, mDownY)) {
            return super.onTouchEvent(ev);
        }

        // offset because the view is translated during swipe
        ev.offsetLocation(mTranslationX, 0);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                updateDismiss(ev);
                if (mDismissed) {
                    dismiss();
                } else if (mSwiping) {
                    cancel();
                }
                resetMembers();
                break;

            case MotionEvent.ACTION_CANCEL:
                cancel();
                resetMembers();
                break;

            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(ev);
                mLastX = ev.getRawX();
                updateSwiping(ev);
                if (mSwiping) {
                    setProgress(ev.getRawX() - mDownX);
                    break;
                }
        }
        return true;
    }

    private void setProgress(float deltaX) {
        mTranslationX = deltaX;
        if (mProgressListener != null && deltaX >= 0) {
            mProgressListener.onSwipeProgressChanged(this, deltaX / getWidth(), deltaX);
        }
    }

    private void dismiss() {
        if (mDismissedListener != null) {
            mDismissedListener.onDismissed(this);
        }
    }

    private void cancel() {
        if (mProgressListener != null) {
            mProgressListener.onSwipeCanceled(this);
        }
    }

    /** Resets internal members when canceling or finishing a given gesture. */
    private void resetMembers() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }
        mVelocityTracker = null;
        mTranslationX = 0;
        mDownX = 0;
        mDownY = 0;
        mSwiping = false;
        mDismissed = false;
        mDiscardIntercept = false;
        mCanStartSwipe = true;
        mDisallowIntercept = false;
    }

    private void updateSwiping(MotionEvent ev) {
        if (!mSwiping) {
            float deltaX = ev.getRawX() - mDownX;
            float deltaY = ev.getRawY() - mDownY;
            if (isPotentialSwipe(deltaX, deltaY)) {
                // There are three conditions on which we want want to start swiping:
                // 1. The swipe is from left to right AND
                // 2. It is horizontal AND
                // 3. We actually can start swiping
                mSwiping = mCanStartSwipe && Math.abs(deltaY) < Math.abs(deltaX) && deltaX > 0;
                mCanStartSwipe = mSwiping;
            }
        }
    }

    private void updateDismiss(MotionEvent ev) {
        float deltaX = ev.getRawX() - mDownX;
        mVelocityTracker.addMovement(ev);
        mVelocityTracker.computeCurrentVelocity(1000);
        if (!mDismissed) {
            if ((deltaX > (getWidth() * mDismissMinDragWidthRatio) && ev.getRawX() >= mLastX)
                    || mVelocityTracker.getXVelocity() >= mMinFlingVelocity) {
                mDismissed = true;
            }
        }
        // Check if the user tried to undo this.
        if (mDismissed && mSwiping) {
            // Check if the user's finger is actually flinging back to left
            if (mVelocityTracker.getXVelocity() < -mMinFlingVelocity) {
                mDismissed = false;
            }
        }
    }

    /**
     * Tests scrollability within child views of v in the direction of dx.
     *
     * @param v      view to test for horizontal scrollability
     * @param checkV whether the view v passed should itself be checked for scrollability
     *               ({@code true}), or just its children ({@code false})
     * @param dx     delta scrolled in pixels. Only the sign of this is used
     * @param x      x coordinate of the active touch point
     * @param y      y coordinate of the active touch point
     * @return {@code true} if child views of v can be scrolled by delta of dx
     */
    protected boolean canScroll(View v, boolean checkV, float dx, float x, float y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft()
                        && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop()
                        && y + scrollY < child.getBottom()
                        && canScroll(
                        child, true, dx, x + scrollX - child.getLeft(),
                        y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }

        return checkV && v.canScrollHorizontally((int) -dx);
    }
}
