/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.res.Resources;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;

/**
 * Controller that handles the swipe-to-dismiss gesture for dismiss the frame layout
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
@UiThread
class SwipeDismissController extends DismissController {
    private static final String TAG = "SwipeDismissController";
    public static final float DEFAULT_DISMISS_DRAG_WIDTH_RATIO = .33f;
    private float mDismissMinDragWidthRatio = DEFAULT_DISMISS_DRAG_WIDTH_RATIO;
    // A value between 0.0 and 1.0 determining the percentage of the screen on the left-hand-side
    // where edge swipe gestures are permitted to begin.
    private static final float EDGE_SWIPE_THRESHOLD = 0.1f;
    private static final int VELOCITY_UNIT = 1000;
    // Cached ViewConfiguration and system-wide constant value
    private final int mSlop;
    private final int mMinFlingVelocity;
    private final float mGestureThresholdPx;
    private final SwipeDismissTransitionHelper mSwipeDismissTransitionHelper;
    private int mActiveTouchId;
    private float mDownX;
    private float mDownY;
    private float mLastX;
    private boolean mSwiping;
    private boolean mDismissed;
    private boolean mDiscardIntercept;

    private boolean mBlockGesture = false;

    SwipeDismissController(Context context, DismissibleFrameLayout layout) {
        super(context, layout);

        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mGestureThresholdPx =
                Resources.getSystem().getDisplayMetrics().widthPixels * EDGE_SWIPE_THRESHOLD;

        mSwipeDismissTransitionHelper = new SwipeDismissTransitionHelper(context, layout);
    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (mLayout.getParent() != null) {
            mLayout.getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    void setDismissMinDragWidthRatio(float ratio) {
        mDismissMinDragWidthRatio = ratio;
    }

    float getDismissMinDragWidthRatio() {
        return mDismissMinDragWidthRatio;
    }

    boolean onInterceptTouchEvent(MotionEvent ev) {
        checkGesture(ev);
        if (mBlockGesture) {
            return true;
        }
        // Offset because the view is translated during swipe, match X with raw X. Active touch
        // coordinates are mostly used by the velocity tracker, so offset it to match the raw
        // coordinates which is what is primarily used elsewhere.
        float offsetX = ev.getRawX() - ev.getX();
        float offsetY = 0.0f;
        ev.offsetLocation(offsetX, offsetY);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetSwipeDetectMembers();
                mDownX = ev.getRawX();
                mDownY = ev.getRawY();
                mActiveTouchId = ev.getPointerId(0);
                mSwipeDismissTransitionHelper.obtainVelocityTracker();
                mSwipeDismissTransitionHelper.getVelocityTracker().addMovement(ev);
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
                resetSwipeDetectMembers();
                break;

            case MotionEvent.ACTION_MOVE:
                if (mSwipeDismissTransitionHelper.getVelocityTracker() == null
                        || mDiscardIntercept) {
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

                if (dx != 0 && mDownX >= mGestureThresholdPx && canScroll(mLayout, false, dx, x,
                        y)) {
                    mDiscardIntercept = true;
                    break;
                }
                updateSwiping(ev);
                break;
        }
        ev.offsetLocation(-offsetX, -offsetY);
        return (!mDiscardIntercept && mSwiping);
    }

    public boolean canScrollHorizontally(int direction) {
        // This view can only be swiped horizontally from left to right - this means a negative
        // SCROLLING direction. We return false if the view is not visible to avoid capturing swipe
        // gestures when the view is hidden.
        return direction < 0 && mLayout.getVisibility() == View.VISIBLE;
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

    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        checkGesture(ev);
        if (mBlockGesture) {
            return true;
        }

        if (mSwipeDismissTransitionHelper.getVelocityTracker() == null) {
            return false;
        }

        // Offset because the view is translated during swipe, match X with raw X. Active touch
        // coordinates are mostly used by the velocity tracker, so offset it to match the raw
        // coordinates which is what is primarily used elsewhere.
        float offsetX = ev.getRawX() - ev.getX();
        float offsetY = 0.0f;
        ev.offsetLocation(offsetX, offsetY);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                updateDismiss(ev);
                // Fall through, don't update gesture tracker with the event for ACTION_CANCEL
            case MotionEvent.ACTION_CANCEL:
                if (mDismissed) {
                    mSwipeDismissTransitionHelper.animateDismissal(mDismissListener);
                } else if (mSwiping
                        // Only trigger animation if we had a MOVE event that would shift the
                        // underlying view, otherwise the animation would be janky.
                        && mLastX != Integer.MIN_VALUE) {
                    mSwipeDismissTransitionHelper.animateRecovery(mDismissListener);
                }
                resetSwipeDetectMembers();
                break;
            case MotionEvent.ACTION_MOVE:
                mSwipeDismissTransitionHelper.getVelocityTracker().addMovement(ev);
                mLastX = ev.getRawX();
                updateSwiping(ev);
                if (mSwiping) {
                    mSwipeDismissTransitionHelper.onSwipeProgressChanged(ev.getRawX() - mDownX, ev);
                    break;
                }
        }
        ev.offsetLocation(-offsetX, -offsetY);
        return true;
    }

    /** Resets internal members when canceling or finishing a given gesture. */
    private void resetSwipeDetectMembers() {
        if (mSwipeDismissTransitionHelper.getVelocityTracker() != null) {
            mSwipeDismissTransitionHelper.getVelocityTracker().recycle();
        }
        mSwipeDismissTransitionHelper.resetVelocityTracker();
        mDownX = 0;
        mDownY = 0;
        mSwiping = false;
        mLastX = Integer.MIN_VALUE;
        mDismissed = false;
        mDiscardIntercept = false;
    }

    private void updateSwiping(MotionEvent ev) {
        if (!mSwiping) {
            float deltaX = ev.getRawX() - mDownX;
            float deltaY = ev.getRawY() - mDownY;
            if (isPotentialSwipe(deltaX, deltaY)) {
                mSwiping = deltaX > mSlop * 2
                        && Math.abs(deltaY) < Math.abs(deltaX);
            } else {
                mSwiping = false;
            }
        }
    }

    private void updateDismiss(@NonNull MotionEvent ev) {
        float deltaX = ev.getRawX() - mDownX;
        // Don't add the motion event as an UP event would clear the velocity tracker
        VelocityTracker velocityTracker = mSwipeDismissTransitionHelper.getVelocityTracker();
        velocityTracker.computeCurrentVelocity(VELOCITY_UNIT);
        float xVelocity = velocityTracker.getXVelocity();
        float yVelocity = velocityTracker.getYVelocity();
        if (mLastX == Integer.MIN_VALUE) {
            // If there's no changes to mLastX, we have only one point of data, and therefore no
            // velocity. Estimate velocity from just the up and down event in that case.
            xVelocity = deltaX / ((ev.getEventTime() - ev.getDownTime()) / 1000f);
        }

        if (!mDismissed) {
            if ((deltaX > (mLayout.getWidth() * mDismissMinDragWidthRatio)
                    && ev.getRawX() >= mLastX)
                    || (xVelocity >= mMinFlingVelocity
                    && xVelocity > Math.abs(
                    yVelocity)))  {
                mDismissed = true;
            }
        }
        // Check if the user tried to undo this.
        if (mDismissed && mSwiping) {
            // Check if the user's finger is actually flinging back to left
            if (xVelocity < -mMinFlingVelocity) {
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
    protected boolean canScroll(@NonNull View v, boolean checkV, float dx, float x, float y) {
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

    private void checkGesture(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mBlockGesture = mSwipeDismissTransitionHelper.isAnimating();
        }
    }
}