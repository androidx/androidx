/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;


/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The SwipeRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and progress
 * animation, call setEnabled(false) on the view.
 *
 * <p> This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This single direct
 * child can, however, specify a specific sub-child that will be the view that does
 * the actual swipe gesture and move with the swipe motion. This specific sub-child
 * is specified using {@link #setTarget(int)}. If no specific sub-child is
 * specified, the single direct child will be used for the swipe view as well. This
 * view will be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.</p>
 */
public class SwipeRefreshLayout extends ViewGroup {
    private static final String LOG_TAG = SwipeRefreshLayout.class.getSimpleName();

    private static final long RETURN_TO_ORIGINAL_POSITION_TIMEOUT = 300;
    private static final float ACCELERATE_INTERPOLATION_FACTOR = 1.5f;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final float PROGRESS_BAR_HEIGHT = 4;
    private static final float MAX_SWIPE_DISTANCE_FACTOR = .6f;
    private static final int REFRESH_TRIGGER_DISTANCE = 120;
    private static final int INVALID_POINTER = -1;

    private SwipeProgressBar mProgressBar; // The thing that shows progress is going
    private View mScrollTarget; // The scrolling single child
    private View mTarget; // The content that gets pulled down
    private int mOriginalOffsetTop;
    private OnRefreshListener mListener;
    private int mFrom;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private float mDistanceToTriggerSync = -1;
    private int mMediumAnimationDuration;
    private float mFromPercentage = 0;
    private float mCurrPercentage = 0;
    private int mProgressBarHeight;
    private int mCurrentTargetOffsetTop;

    private float mInitialTargetY;
    private float mInitialMotionY;
    private float mLastMotionY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private final AccelerateInterpolator mAccelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[] {
        android.R.attr.enabled
    };

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            if (mFrom != mOriginalOffsetTop) {
                targetTop = (mFrom + (int)((mOriginalOffsetTop - mFrom) * interpolatedTime));
            }
            int offset = targetTop - mTarget.getTop();
            final int currentTop = mTarget.getTop();
            if (offset + currentTop < 0) {
                offset = 0 - currentTop;
            }
            setTargetOffsetTopAndBottom(offset);
        }
    };

    private Animation mShrinkTrigger = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            float percent = mFromPercentage + ((0 - mFromPercentage) * interpolatedTime);
            mProgressBar.setTriggerPercentage(percent);
        }
    };

    private final AnimationListener mReturnToStartPositionListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            // Once the target content has returned to its start position, reset
            // the target offset to 0
            mCurrentTargetOffsetTop = 0;
        }
    };

    private final AnimationListener mShrinkAnimationListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            mCurrPercentage = 0;
        }
    };

    private final Runnable mReturnToStartPosition = new Runnable() {

        @Override
        public void run() {
            mReturningToStart = true;
            animateOffsetToStartPosition();
        }

    };

    // Cancel the refresh gesture and animate everything back to its original state.
    private final Runnable mCancel = new Runnable() {

        @Override
        public void run() {
            mReturningToStart = true;
            // Timeout fired since the user last moved their finger; animate the
            // trigger to 0 and put the target back at its original position
            if (mProgressBar != null) {
                mFromPercentage = mCurrPercentage;
                mShrinkTrigger.setDuration(mMediumAnimationDuration);
                mShrinkTrigger.setAnimationListener(mShrinkAnimationListener);
                mShrinkTrigger.reset();
                mShrinkTrigger.setInterpolator(mDecelerateInterpolator);
                startAnimation(mShrinkTrigger);
            }
            animateOffsetToStartPosition();
        }

    };

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     * @param context
     */
    public SwipeRefreshLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     * @param context
     * @param attrs
     */
    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mProgressBar = new SwipeProgressBar(this);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mProgressBarHeight = (int) (metrics.density * PROGRESS_BAR_HEIGHT);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mAccelerateInterpolator = new AccelerateInterpolator(ACCELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(mCancel);
        removeCallbacks(mReturnToStartPosition);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mReturnToStartPosition);
        removeCallbacks(mCancel);
    }

    private void animateOffsetToStartPosition() {
        mFrom = mCurrentTargetOffsetTop + getPaddingTop();
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(mMediumAnimationDuration);
        mAnimateToStartPosition.setAnimationListener(mReturnToStartPositionListener);
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        mTarget.startAnimation(mAnimateToStartPosition);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Specify a separate {@link android.view.View} that should become the
     * piece of content that the user is able to drag in order to trigger the
     * refresh. This {@link android.view.View} must be a child of the
     * {@link android.support.v4.widget.SwipeRefreshLayout}. If not specified,
     * it will default to being the same single, direct child of the overall
     * {@link android.support.v4.widget.SwipeRefreshLayout}.
     *
     * @param resId Resource ID of the {@link android.view.View} that should
     *              become the new draggable section.
     */
    public void setTarget(int resId) {
        View potentialChild = findViewById(resId);
        // Only update if it's a valid child
        if (potentialChild != null) {
            mTarget = potentialChild;
        } else {
            Log.w(LOG_TAG, "View provided is not a valid child of SwipeRefreshLayout.");
        }
    }

    private void setTriggerPercentage(float percent) {
        if (percent == 0f) {
            // No-op. A null trigger means it's uninitialized, and setting it to zero-percent
            // means we're trying to reset state, so there's nothing to reset in this case.
            mCurrPercentage = 0;
            return;
        }
        mCurrPercentage = percent;
        mProgressBar.setTriggerPercentage(percent);
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (mRefreshing != refreshing) {
            ensureTarget();
            mCurrPercentage = 0;
            mRefreshing = refreshing;
            if (mRefreshing) {
                mProgressBar.start();
            } else {
                mProgressBar.stop();
            }
        }
    }

    /**
     * @deprecated Use {@link #setColorSchemeResources(int, int, int, int)}
     */
    @Deprecated
    public void setColorScheme(int colorRes1, int colorRes2, int colorRes3, int colorRes4) {
        setColorSchemeResources(colorRes1, colorRes2, colorRes3, colorRes4);
    }

    /**
     * Set the four colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     */
    public void setColorSchemeResources(int colorRes1, int colorRes2, int colorRes3,
            int colorRes4) {
        final Resources res = getResources();
        setColorSchemeColors(res.getColor(colorRes1), res.getColor(colorRes2),
                res.getColor(colorRes3), res.getColor(colorRes4));
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     */
    public void setColorSchemeColors(int color1, int color2, int color3, int color4) {
        ensureTarget();
        mProgressBar.setColorScheme(color1, color2, color3, color4);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     *         progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid out yet.

        // Scroll target is always the single child.
        if (mScrollTarget == null) {
            mScrollTarget = getSingleChild();
        }

        // Swipeable target is either already defined or defaults to the single child.
        if (mTarget == null) {
            mTarget = getSingleChild();
        }
        if (mDistanceToTriggerSync == -1) {
            if (getParent() != null && ((View)getParent()).getHeight() > 0) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDistanceToTriggerSync = (int) Math.min(
                        ((View) getParent()) .getHeight() * MAX_SWIPE_DISTANCE_FACTOR,
                        REFRESH_TRIGGER_DISTANCE * metrics.density);
            }
        }
    }

    private View getSingleChild() {
        if (getChildCount() > 1 && !isInEditMode()) {
            throw new IllegalStateException(
                    "SwipeRefreshLayout can host only one direct child");
        }
        return getChildAt(0);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mProgressBar.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width =  getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        final View child = getChildAt(0);
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        // Setting these initial value needs to happen after mTarget has received its size
        mOriginalOffsetTop = mTarget.getTop() + getPaddingTop();
        mProgressBar.setBounds(0, mOriginalOffsetTop, width,
                mOriginalOffsetTop + mProgressBarHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() > 1 && !isInEditMode()) {
            throw new IllegalStateException("SwipeRefreshLayout can host only one direct child");
        }
        if (getChildCount() > 0) {
            getChildAt(0).measure(
                    MeasureSpec.makeMeasureSpec(
                            getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(
                            getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                            MeasureSpec.EXACTLY));
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mScrollTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mScrollTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return mScrollTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mScrollTarget, -1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp()
                || ev.getY() < mTarget.getTop() || ev.getY() > mTarget.getBottom()) {
            // Fail fast if we're not in a state where a swipe is possible
            // This includes if the swipe is outside mTarget
            touchEventFailFast();
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialTargetY = mTarget.getTop();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mInitialMotionY;
                if (yDiff > mTouchSlop) {
                    mLastMotionY = y;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchEventFailFast();
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp()
                || ev.getY() < mTarget.getTop() || ev.getY() > mTarget.getBottom()) {
            // Fail fast if we're not in a state where a swipe is possible
            // This includes if the swipe it outside mTarget
            touchEventFailFast();
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialTargetY = mTarget.getTop();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mInitialMotionY;

                if (!mIsBeingDragged && yDiff > mTouchSlop) {
                    mIsBeingDragged = true;
                }

                if (mIsBeingDragged) {
                    // User velocity passed min velocity; trigger a refresh
                    if (yDiff > mDistanceToTriggerSync) {
                        // User movement passed distance; trigger a refresh
                        startRefresh();
                    } else {
                        // Just track the user's movement
                        setTriggerPercentage(
                                mAccelerateInterpolator.getInterpolation(
                                        yDiff / mDistanceToTriggerSync));
                        updateContentOffsetTop((int) (mInitialTargetY + yDiff));
                        if (mLastMotionY > y && mTarget.getTop() == getPaddingTop()) {
                            // If the user puts the view back at the top, we
                            // don't need to. This shouldn't be considered
                            // cancelling the gesture as the user can restart from the top.
                            removeCallbacks(mCancel);
                        } else {
                            updatePositionTimeout();
                        }
                    }
                    mLastMotionY = y;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchEventFailFast();
                return false;
        }

        return true;
    }

    private void touchEventFailFast() {
        mIsBeingDragged = false;
        mCurrPercentage = 0;
        mActivePointerId = INVALID_POINTER;
    }

    private void startRefresh() {
        removeCallbacks(mCancel);
        mReturnToStartPosition.run();
        setRefreshing(true);
        if (mListener != null) {
            mListener.onRefresh();
        }
    }

    private void updateContentOffsetTop(int targetTop) {
        final int currentTop = mTarget.getTop();
        if (targetTop > (currentTop + mDistanceToTriggerSync)) {
            targetTop = currentTop + ((int) mDistanceToTriggerSync);
        } else if (targetTop < 0) {
            targetTop = mTarget.getTop();
        }
        setTargetOffsetTopAndBottom(targetTop - currentTop);
    }

    private void setTargetOffsetTopAndBottom(int offset) {
        mTarget.offsetTopAndBottom(offset);
        mCurrentTargetOffsetTop = mTarget.getTop();
    }

    private void updatePositionTimeout() {
        removeCallbacks(mCancel);
        postDelayed(mCancel, RETURN_TO_ORIGINAL_POSITION_TIMEOUT);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        public void onRefresh();
    }

    /**
     * Simple AnimationListener to avoid having to implement unneeded methods in
     * AnimationListeners.
     */
    private class BaseAnimationListener implements AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
