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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

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
    // A value between 0.0 and 1.0 determining the percentage of the screen on the left-hand-side
    // where edge swipe gestures are permitted to begin.
    private static final float EDGE_SWIPE_THRESHOLD = 0.1f;
    private static final float TRANSLATION_MIN_ALPHA = 0.5f;
    private static final float DEFAULT_INTERPOLATION_FACTOR = 1.5f;

    // Cached ViewConfiguration and system-wide constant value
    private int mSlop;
    private int mMinFlingVelocity;
    private float mGestureThresholdPx;

    // Transient properties
    private int mActiveTouchId;
    private float mDownX;
    private float mDownY;
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
    private float mLastX;
    private float mDismissMinDragWidthRatio = DEFAULT_DISMISS_DRAG_WIDTH_RATIO;
    boolean mStarted;
    final int mAnimationTime;

    final DecelerateInterpolator mCancelInterpolator;
    final AccelerateInterpolator mDismissInterpolator;
    final DecelerateInterpolator mCompleteDismissGestureInterpolator;

    SwipeDismissController(Context context, DismissibleFrameLayout layout) {
        super(context, layout);

        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mGestureThresholdPx =
                Resources.getSystem().getDisplayMetrics().widthPixels * EDGE_SWIPE_THRESHOLD;
        mAnimationTime = context.getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mCancelInterpolator = new DecelerateInterpolator(DEFAULT_INTERPOLATION_FACTOR);
        mDismissInterpolator = new AccelerateInterpolator(DEFAULT_INTERPOLATION_FACTOR);
        mCompleteDismissGestureInterpolator = new DecelerateInterpolator(
                DEFAULT_INTERPOLATION_FACTOR);
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
        // offset because the view is translated during swipe
        ev.offsetLocation(mTranslationX, 0);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetSwipeDetectMembers();
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
                resetSwipeDetectMembers();
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

                if (dx != 0 && mDownX >= mGestureThresholdPx
                        && canScroll(mLayout, false, dx, x, y)) {
                    mDiscardIntercept = true;
                    break;
                }
                updateSwiping(ev);
                break;
        }

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
        if (mVelocityTracker == null) {
            return false;
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
                resetSwipeDetectMembers();
                break;

            case MotionEvent.ACTION_CANCEL:
                cancel();
                resetSwipeDetectMembers();
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
        mLayout.setTranslationX(deltaX);
        mLayout.setAlpha(1 - (deltaX / mLayout.getWidth() * TRANSLATION_MIN_ALPHA));
        mStarted = true;

        if (mDismissListener != null && deltaX >= 0) {
            mDismissListener.onDismissStarted();
        }
    }

    void dismiss() {
        mLayout.animate()
                .translationX(mLayout.getWidth())
                .alpha(0)
                .setDuration(mAnimationTime)
                .setInterpolator(
                        mStarted ? mCompleteDismissGestureInterpolator
                                : mDismissInterpolator)
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (mDismissListener != null) {
                                    mDismissListener.onDismissed();
                                }
                                resetTranslationAndAlpha();
                            }
                        });
    }

    void cancel() {
        mStarted = false;
        mLayout.animate()
                .translationX(0)
                .alpha(1)
                .setDuration(mAnimationTime)
                .setInterpolator(mCancelInterpolator)
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (mDismissListener != null) {
                                    mDismissListener.onDismissCanceled();
                                }
                                resetTranslationAndAlpha();
                            }
                        });
    }

    /**
     * Resets this view to the original state. This method cancels any pending animations on this
     * view and resets the alpha as well as x translation values.
     */
    void resetTranslationAndAlpha() {
        mLayout.animate().cancel();
        mLayout.setTranslationX(0);
        mLayout.setAlpha(1);
        mStarted = false;
    }

    /** Resets internal members when canceling or finishing a given gesture. */
    private void resetSwipeDetectMembers() {
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

    private void updateDismiss(@NonNull MotionEvent ev) {
        float deltaX = ev.getRawX() - mDownX;
        mVelocityTracker.addMovement(ev);
        mVelocityTracker.computeCurrentVelocity(1000);
        if (!mDismissed) {
            if ((deltaX > (mLayout.getWidth() * mDismissMinDragWidthRatio)
                    && ev.getRawX() >= mLastX)
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
}
