/*
 * Copyright (C) 2015 The Android Open Source Project
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


package androidx.core.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.FocusFinder;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.OverScroller;
import android.widget.ScrollView;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.R;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.DifferentialMotionFlingController;
import androidx.core.view.DifferentialMotionFlingTarget;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ScrollFeedbackProviderCompat;
import androidx.core.view.ScrollingView;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityRecordCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * NestedScrollView is just like {@link ScrollView}, but it supports acting
 * as both a nested scrolling parent and child on both new and old versions of Android.
 * Nested scrolling is enabled by default.
 */
public class NestedScrollView extends FrameLayout implements NestedScrollingParent3,
        NestedScrollingChild3, ScrollingView {
    static final int ANIMATED_SCROLL_GAP = 250;

    static final float MAX_SCROLL_FACTOR = 0.5f;

    private static final String TAG = "NestedScrollView";
    private static final int DEFAULT_SMOOTH_SCROLL_DURATION = 250;

    /**
     * The following are copied from OverScroller to determine how far a fling will go.
     */
    private static final float SCROLL_FRICTION = 0.015f;
    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
    private static final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private final float mPhysicalCoeff;

    /**
     * When flinging the stretch towards scrolling content, it should destretch quicker than the
     * fling would normally do. The visual effect of flinging the stretch looks strange as little
     * appears to happen at first and then when the stretch disappears, the content starts
     * scrolling quickly.
     */
    private static final float FLING_DESTRETCH_FACTOR = 4f;

    /**
     * Interface definition for a callback to be invoked when the scroll
     * X or Y positions of a view change.
     *
     * <p>This version of the interface works on all versions of Android, back to API v4.</p>
     *
     * @see #setOnScrollChangeListener(OnScrollChangeListener)
     */
    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         * @param v The view whose scroll position has changed.
         * @param scrollX Current horizontal scroll origin.
         * @param scrollY Current vertical scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY,
                int oldScrollX, int oldScrollY);
    }

    private long mLastScroll;

    private final Rect mTempRect = new Rect();
    private OverScroller mScroller;

    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public @NonNull EdgeEffect mEdgeGlowTop;

    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public @NonNull EdgeEffect mEdgeGlowBottom;

    @VisibleForTesting
    @Nullable ScrollFeedbackProviderCompat mScrollFeedbackProvider;

    /**
     * Position of the last motion event; only used with touch related events (usually to assist
     * in movement changes in a drag gesture).
     */
    private int mLastMotionY;

    /**
     * True when the layout has changed but the traversal has not come through yet.
     * Ideally the view hierarchy would keep track of this for us.
     */
    private boolean mIsLayoutDirty = true;
    private boolean mIsLaidOut = false;

    /**
     * The child to give focus to in the event that a child has requested focus while the
     * layout is dirty. This prevents the scroll from being wrong if the child has not been
     * laid out before requesting focus.
     */
    private View mChildToScrollTo = null;

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts their finger).
     */
    private boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * When set to true, the scroll view measure its child to make it fill the currently
     * visible area.
     */
    private boolean mFillViewport;

    /**
     * Whether arrow scrolling is animated.
     */
    private boolean mSmoothScrollingEnabled = true;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    /**
     * Used during scrolling to retrieve the new offset within the window. Saves memory by saving
     * x, y changes to this array (0 position = x, 1 position = y) vs. reallocating an x and y
     * every time.
     */
    private final int[] mScrollOffset = new int[2];

    /*
     * Used during scrolling to retrieve the new consumed offset within the window.
     * Uses same memory saving strategy as mScrollOffset.
     */
    private final int[] mScrollConsumed = new int[2];

    // Used to track the position of the touch only events relative to the container.
    private int mNestedYOffset;

    private int mLastScrollerY;

    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    private SavedState mSavedState;

    private static final AccessibilityDelegate ACCESSIBILITY_DELEGATE = new AccessibilityDelegate();

    private static final int[] SCROLLVIEW_STYLEABLE = new int[] {
            android.R.attr.fillViewport
    };

    private final NestedScrollingParentHelper mParentHelper;
    private final NestedScrollingChildHelper mChildHelper;

    private float mVerticalScrollFactor;

    private OnScrollChangeListener mOnScrollChangeListener;

    @VisibleForTesting
    final DifferentialMotionFlingTargetImpl mDifferentialMotionFlingTarget =
            new DifferentialMotionFlingTargetImpl();

    @VisibleForTesting
    DifferentialMotionFlingController mDifferentialMotionFlingController =
            new DifferentialMotionFlingController(getContext(), mDifferentialMotionFlingTarget);

    public NestedScrollView(@NonNull Context context) {
        this(context, null);
    }

    public NestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.nestedScrollViewStyle);
    }

    public NestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mEdgeGlowTop = EdgeEffectCompat.create(context, attrs);
        mEdgeGlowBottom = EdgeEffectCompat.create(context, attrs);

        final float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning

        initScrollView();

        final TypedArray a = context.obtainStyledAttributes(
                attrs, SCROLLVIEW_STYLEABLE, defStyleAttr, 0);

        setFillViewport(a.getBoolean(0, false));

        a.recycle();

        mParentHelper = new NestedScrollingParentHelper(this);
        mChildHelper = new NestedScrollingChildHelper(this);

        // ...because why else would you be using this widget?
        setNestedScrollingEnabled(true);

        ViewCompat.setAccessibilityDelegate(this, ACCESSIBILITY_DELEGATE);
    }

    // NestedScrollingChild3

    @Override
    public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int @Nullable [] offsetInWindow, int type, int @NonNull [] consumed) {
        mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type, consumed);
    }

    // NestedScrollingChild2

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int @Nullable [] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(
            int dx,
            int dy,
            int @Nullable [] consumed,
            int @Nullable [] offsetInWindow,
            int type
    ) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int @Nullable [] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int @Nullable [] consumed,
            int @Nullable [] offsetInWindow) {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    // NestedScrollingParent3

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int type, int @NonNull [] consumed) {
        onNestedScrollInternal(dyUnconsumed, type, consumed);
    }

    private void onNestedScrollInternal(int dyUnconsumed, int type, int @Nullable [] consumed) {
        final int oldScrollY = getScrollY();
        scrollBy(0, dyUnconsumed);
        final int myConsumed = getScrollY() - oldScrollY;

        if (consumed != null) {
            consumed[1] += myConsumed;
        }
        final int myUnconsumed = dyUnconsumed - myConsumed;

        mChildHelper.dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null, type, consumed);
    }

    // NestedScrollingParent2

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
            int type) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
            int type) {
        mParentHelper.onNestedScrollAccepted(child, target, axes, type);
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, type);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        mParentHelper.onStopNestedScroll(target, type);
        stopNestedScroll(type);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int type) {
        onNestedScrollInternal(dyUnconsumed, type, null);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, int @NonNull [] consumed,
            int type) {
        dispatchNestedPreScroll(dx, dy, consumed, null, type);
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(
            @NonNull View child, @NonNull View target, int axes) {
        return onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScrollAccepted(
            @NonNull View child, @NonNull View target, int axes) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target) {
        onStopNestedScroll(target, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed) {
        onNestedScrollInternal(dyUnconsumed, ViewCompat.TYPE_TOUCH, null);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, int @NonNull [] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean onNestedFling(
            @NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (!consumed) {
            dispatchNestedFling(0, velocityY, true);
            fling((int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }

    // ScrollView import

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getVerticalFadingEdgeLength();
        final int scrollY = getScrollY();
        if (scrollY < length) {
            return scrollY / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        View child = getChildAt(0);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int length = getVerticalFadingEdgeLength();
        final int bottomEdge = getHeight() - getPaddingBottom();
        final int span = child.getBottom() + lp.bottomMargin - getScrollY() - bottomEdge;
        if (span < length) {
            return span / (float) length;
        }

        return 1.0f;
    }

    /**
     * @return The maximum amount this scroll view will scroll in response to
     *   an arrow event.
     */
    public int getMaxScrollAmount() {
        return (int) (MAX_SCROLL_FACTOR * getHeight());
    }

    private void initScrollView() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public void addView(@NonNull View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index, params);
    }

    /**
     * Register a callback to be invoked when the scroll X or Y positions of
     * this view change.
     * <p>This version of the method works on all versions of Android, back to API v4.</p>
     *
     * @param l The listener to notify when the scroll X or Y position changes.
     * @see View#getScrollX()
     * @see View#getScrollY()
     */
    public void setOnScrollChangeListener(@Nullable OnScrollChangeListener l) {
        mOnScrollChangeListener = l;
    }

    /**
     * @return Returns true this ScrollView can be scrolled
     */
    private boolean canScroll() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            return childSize > parentSpace;
        }
        return false;
    }

    /**
     * Indicates whether this ScrollView's content is stretched to fill the viewport.
     *
     * @return True if the content fills the viewport, false otherwise.
     *
     * @attr name android:fillViewport
     */
    public boolean isFillViewport() {
        return mFillViewport;
    }

    /**
     * Set whether this ScrollView should stretch its content height to fill the viewport or not.
     *
     * @param fillViewport True to stretch the content's height to the viewport's
     *        boundaries, false otherwise.
     *
     * @attr name android:fillViewport
     */
    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != mFillViewport) {
            mFillViewport = fillViewport;
            requestLayout();
        }
    }

    /**
     * @return Whether arrow scrolling will animate its transition.
     */
    public boolean isSmoothScrollingEnabled() {
        return mSmoothScrollingEnabled;
    }

    /**
     * Set whether arrow scrolling will animate its transition.
     * @param smoothScrollingEnabled whether arrow scrolling will animate its transition
     */
    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener.onScrollChange(this, l, t, oldl, oldt);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mFillViewport) {
            return;
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            return;
        }

        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int childSize = child.getMeasuredHeight();
            int parentSpace = getMeasuredHeight()
                    - getPaddingTop()
                    - getPaddingBottom()
                    - lp.topMargin
                    - lp.bottomMargin;

            if (childSize < parentSpace) {
                int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                        getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                        lp.width);
                int childHeightMeasureSpec =
                        MeasureSpec.makeMeasureSpec(parentSpace, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        mTempRect.setEmpty();

        if (!canScroll()) {
            if (isFocused() && event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
                View currentFocused = findFocus();
                if (currentFocused == this) currentFocused = null;
                View nextFocused = FocusFinder.getInstance().findNextFocus(this,
                        currentFocused, View.FOCUS_DOWN);
                return nextFocused != null
                        && nextFocused != this
                        && nextFocused.requestFocus(View.FOCUS_DOWN);
            }
            return false;
        }

        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (event.isAltPressed()) {
                        handled = fullScroll(View.FOCUS_UP);
                    } else {
                        handled = arrowScroll(View.FOCUS_UP);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (event.isAltPressed()) {
                        handled = fullScroll(View.FOCUS_DOWN);
                    } else {
                        handled = arrowScroll(View.FOCUS_DOWN);
                    }
                    break;
                case KeyEvent.KEYCODE_PAGE_UP:
                    handled = fullScroll(View.FOCUS_UP);
                    break;
                case KeyEvent.KEYCODE_PAGE_DOWN:
                    handled = fullScroll(View.FOCUS_DOWN);
                    break;
                case KeyEvent.KEYCODE_SPACE:
                    pageScroll(event.isShiftPressed() ? View.FOCUS_UP : View.FOCUS_DOWN);
                    break;
                case KeyEvent.KEYCODE_MOVE_HOME:
                    pageScroll(View.FOCUS_UP);
                    break;
                case KeyEvent.KEYCODE_MOVE_END:
                    pageScroll(View.FOCUS_DOWN);
                    break;
            }
        }

        return handled;
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollY = getScrollY();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY
                    || y >= child.getBottom() - scrollY
                    || x < child.getLeft()
                    || x >= child.getRight());
        }
        return false;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
        * Shortcut the most recurring case: the user is in the dragging
        * state and they are moving their finger.  We want to intercept this
        * motion.
        */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && mIsBeingDragged) {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from their original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId
                            + " in onInterceptTouchEvent");
                    break;
                }

                final int y = (int) ev.getY(pointerIndex);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop
                        && (getNestedScrollAxes() & ViewCompat.SCROLL_AXIS_VERTICAL) == 0) {
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    mNestedYOffset = 0;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int y = (int) ev.getY();
                if (!inChild((int) ev.getX(), y)) {
                    mIsBeingDragged = stopGlowAnimations(ev) || !mScroller.isFinished();
                    recycleVelocityTracker();
                    break;
                }

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged. We also want to catch the edge glow and start dragging
                 * if one is being animated. We need to call computeScrollOffset() first so that
                 * isFinished() is correct.
                */
                mScroller.computeScrollOffset();
                mIsBeingDragged = stopGlowAnimations(ev) || !mScroller.isFinished();
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /* Release the drag */
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())) {
                    postInvalidateOnAnimation();
                }
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        /*
        * The only time we want to intercept motion events is if we are in the
        * drag mode.
        */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent motionEvent) {
        initVelocityTrackerIfNotExists();

        final int actionMasked = motionEvent.getActionMasked();

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }

        MotionEvent velocityTrackerMotionEvent = MotionEvent.obtain(motionEvent);
        velocityTrackerMotionEvent.offsetLocation(0, mNestedYOffset);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {
                    return false;
                }

                // If additional fingers touch the screen while a drag is in progress, this block
                // of code will make sure the drag isn't interrupted.
                if (mIsBeingDragged) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    abortAnimatedScroll();
                }

                initializeTouchDrag(
                        (int) motionEvent.getY(),
                        motionEvent.getPointerId(0)
                );

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int activePointerIndex = motionEvent.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int y = (int) motionEvent.getY(activePointerIndex);
                int deltaY = mLastMotionY - y;
                deltaY -= releaseVerticalGlow(deltaY, motionEvent.getX(activePointerIndex));

                // Changes to dragged state if delta is greater than the slop (and not in
                // the dragged state).
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }

                if (mIsBeingDragged) {
                    final int x = (int) motionEvent.getX(activePointerIndex);
                    int scrollOffset =
                            scrollBy(deltaY, MotionEvent.AXIS_Y, motionEvent, x,
                                    ViewCompat.TYPE_TOUCH, false);
                    // Updates the global positions (used by later move events to properly scroll).
                    mLastMotionY = y - scrollOffset;
                    mNestedYOffset += scrollOffset;
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                if ((Math.abs(initialVelocity) >= mMinimumVelocity)) {
                    if (!edgeEffectFling(initialVelocity)
                            && !dispatchNestedPreFling(0, -initialVelocity)) {
                        dispatchNestedFling(0, -initialVelocity, true);
                        fling(-initialVelocity);
                    }
                } else if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                        getScrollRange())) {
                    postInvalidateOnAnimation();
                }
                endTouchDrag();
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                            getScrollRange())) {
                        postInvalidateOnAnimation();
                    }
                }
                endTouchDrag();
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = motionEvent.getActionIndex();
                mLastMotionY = (int) motionEvent.getY(index);
                mActivePointerId = motionEvent.getPointerId(index);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                onSecondaryPointerUp(motionEvent);
                mLastMotionY =
                        (int) motionEvent.getY(motionEvent.findPointerIndex(mActivePointerId));
                break;
            }
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(velocityTrackerMotionEvent);
        }
        // Returns object back to be re-used by others.
        velocityTrackerMotionEvent.recycle();

        return true;
    }

    private void initializeTouchDrag(int lastMotionY, int activePointerId) {
        mLastMotionY = lastMotionY;
        mActivePointerId = activePointerId;
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
    }

    // Ends drag in a nested scroll.
    private void endTouchDrag() {
        mActivePointerId = INVALID_POINTER;
        mIsBeingDragged = false;

        recycleVelocityTracker();
        stopNestedScroll(ViewCompat.TYPE_TOUCH);

        mEdgeGlowTop.onRelease();
        mEdgeGlowBottom.onRelease();
    }

    /**
     * Same as {@link #scrollBy(int, int, MotionEvent, int, int, boolean)}, but with no entry for
     * the vertical motion axis as well as the {@link MotionEvent}.
     *
     * <p>Use this method (instead of the other overload) if the {@link MotionEvent} that caused
     * this scroll request is not known.
     */
    private int scrollBy(
            int verticalScrollDistance,
            int x,
            int touchType,
            boolean isSourceMouseOrKeyboard
    ) {
        return scrollBy(verticalScrollDistance, /* verticalScrollAxis= */ -1, null, x, touchType,
                isSourceMouseOrKeyboard);
    }

    /**
     * Handles scroll events for both touch and non-touch events (mouse scroll wheel,
     * rotary button, keyboard, etc.).
     *
     * Note: This function returns the total scroll offset for this scroll event which is required
     * for calculating the total scroll between multiple move events (touch). This returned value
     * is NOT needed for non-touch events since a scroll is a one time event (vs. touch where a
     * drag may be triggered multiple times with the movement of the finger).
     *
     * @param verticalScrollDistance the amount of distance (in pixels) to scroll vertically.
     * @param verticalScrollAxis the motion axis that triggered the vertical scroll. This is not
     *                           always {@link MotionEvent#AXIS_Y}, because there could be other
     *                           axes that trigger a vertical scroll on the view. For example,
     *                           generic motion events reported via {@link MotionEvent#AXIS_SCROLL}
     *                           or {@link MotionEvent#AXIS_VSCROLL}. Use {@code -1} if the vertical
     *                           scroll axis is not known.
     * @param ev the {@link MotionEvent} that caused this scroll. {@code null} if the event is not
     *           known.
     * @param x the target location on the x axis.
     * @param touchType the {@link ViewCompat.NestedScrollType} for this scroll.
     * @param isSourceMouseOrKeyboard whether or not the scroll was caused by a mouse or a keyboard.
     */
    // TODO: You should rename this to nestedScrollBy() so it is different from View.scrollBy
    @VisibleForTesting
    int scrollBy(
            int verticalScrollDistance,
            int verticalScrollAxis,
            @Nullable MotionEvent ev,
            int x,
            @ViewCompat.NestedScrollType int touchType,
            boolean isSourceMouseOrKeyboard
    ) {
        int totalScrollOffset = 0;

        /*
         * Starts nested scrolling for non-touch events (mouse scroll wheel, rotary button, etc.).
         * This is in contrast to a touch event which would trigger the start of nested scrolling
         * with a touch down event outside of this method, since for a single gesture scrollBy()
         * might be called several times for a move event for a single drag gesture.
         */
        if (touchType == ViewCompat.TYPE_NON_TOUCH) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, touchType);
        }

        // Dispatches scrolling delta amount available to parent (to consume what it needs).
        // Note: The amounts the parent consumes are saved in arrays named mScrollConsumed and
        // mScrollConsumed to save space.
        if (dispatchNestedPreScroll(
                0,
                verticalScrollDistance,
                mScrollConsumed,
                mScrollOffset,
                touchType)
        ) {
            // Deducts the scroll amount (y) consumed by the parent (x in position 0,
            // y in position 1). Nested scroll only works with Y position (so we don't use x).
            verticalScrollDistance -= mScrollConsumed[1];
            totalScrollOffset += mScrollOffset[1];
        }

        // Retrieves the scroll y position (top position of this view) and scroll Y range (how far
        // the scroll can go).
        final int initialScrollY = getScrollY();
        final int scrollRangeY = getScrollRange();

        // Overscroll is for adding animations at the top/bottom of a view when the user scrolls
        // beyond the beginning/end of the view. Overscroll is not used with a mouse.
        boolean canOverscroll = canOverScroll() && !isSourceMouseOrKeyboard;

        // Scrolls content in the current View, but clamps it if it goes too far.
        boolean hitScrollBarrier =
                overScrollByCompat(
                        0,
                        verticalScrollDistance,
                        0,
                        initialScrollY,
                        0,
                        scrollRangeY,
                        0,
                        0,
                        true
                ) && !hasNestedScrollingParent(touchType);

        // The position may have been adjusted in the previous call, so we must revise our values.
        final int scrollYDelta = getScrollY() - initialScrollY;
        if (ev != null && scrollYDelta != 0) {
            getScrollFeedbackProvider().onScrollProgress(
                    ev.getDeviceId(),  ev.getSource(), verticalScrollAxis, scrollYDelta);
        }
        final int unconsumedY = verticalScrollDistance - scrollYDelta;

        // Reset the Y consumed scroll to zero
        mScrollConsumed[1] = 0;

        //  Dispatch the unconsumed delta Y to the children to consume.
        dispatchNestedScroll(
                0,
                scrollYDelta,
                0,
                unconsumedY,
                mScrollOffset,
                touchType,
                mScrollConsumed
        );

        totalScrollOffset += mScrollOffset[1];

        // Handle overscroll of the children.
        verticalScrollDistance -= mScrollConsumed[1];
        int newScrollY = initialScrollY + verticalScrollDistance;

        if (newScrollY < 0) {
            if (canOverscroll) {
                EdgeEffectCompat.onPullDistance(
                        mEdgeGlowTop,
                        (float) -verticalScrollDistance / getHeight(),
                        (float) x / getWidth()
                );
                if (ev != null) {
                    getScrollFeedbackProvider().onScrollLimit(
                            ev.getDeviceId(), ev.getSource(), verticalScrollAxis,
                            /* isStart= */ true);
                }

                if (!mEdgeGlowBottom.isFinished()) {
                    mEdgeGlowBottom.onRelease();
                }
            }

        } else if (newScrollY > scrollRangeY) {
            if (canOverscroll) {
                EdgeEffectCompat.onPullDistance(
                        mEdgeGlowBottom,
                        (float) verticalScrollDistance / getHeight(),
                        1.f - ((float) x / getWidth())
                );
                if (ev != null) {
                    getScrollFeedbackProvider().onScrollLimit(
                            ev.getDeviceId(), ev.getSource(), verticalScrollAxis,
                            /* isStart= */ false);
                }

                if (!mEdgeGlowTop.isFinished()) {
                    mEdgeGlowTop.onRelease();
                }
            }
        }

        if (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished()) {
            postInvalidateOnAnimation();
            hitScrollBarrier = false;
        }

        if (hitScrollBarrier && (touchType == ViewCompat.TYPE_TOUCH)) {
            // Break our velocity if we hit a scroll barrier.
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }

        /*
         * Ends nested scrolling for non-touch events (mouse scroll wheel, rotary button, etc.).
         * As noted above, this is in contrast to a touch event.
         */
        if (touchType == ViewCompat.TYPE_NON_TOUCH) {
            stopNestedScroll(touchType);

            // Required for scrolling with Rotary Device stretch top/bottom to work properly
            mEdgeGlowTop.onRelease();
            mEdgeGlowBottom.onRelease();
        }

        return totalScrollOffset;
    }

    /**
     * Returns true if edgeEffect should call onAbsorb() with veclocity or false if it should
     * animate with a fling. It will animate with a fling if the velocity will remove the
     * EdgeEffect through its normal operation.
     *
     * @param edgeEffect The EdgeEffect that might absorb the velocity.
     * @param velocity The velocity of the fling motion
     * @return true if the velocity should be absorbed or false if it should be flung.
     */
    private boolean shouldAbsorb(@NonNull EdgeEffect edgeEffect, int velocity) {
        if (velocity > 0) {
            return true;
        }
        float distance = EdgeEffectCompat.getDistance(edgeEffect) * getHeight();

        // This is flinging without the spring, so let's see if it will fling past the overscroll
        float flingDistance = getSplineFlingDistance(-velocity);

        return flingDistance < distance;
    }

    /**
     * If mTopGlow or mBottomGlow is currently active and the motion will remove some of the
     * stretch, this will consume any of unconsumedY that the glow can. If the motion would
     * increase the stretch, or the EdgeEffect isn't a stretch, then nothing will be consumed.
     *
     * @param unconsumedY The vertical delta that might be consumed by the vertical EdgeEffects
     * @return The remaining unconsumed delta after the edge effects have consumed.
     */
    int consumeFlingInVerticalStretch(int unconsumedY) {
        int height = getHeight();
        if (unconsumedY > 0 && EdgeEffectCompat.getDistance(mEdgeGlowTop) != 0f) {
            float deltaDistance = -unconsumedY * FLING_DESTRETCH_FACTOR / height;
            int consumed = Math.round(-height / FLING_DESTRETCH_FACTOR
                    * EdgeEffectCompat.onPullDistance(mEdgeGlowTop, deltaDistance, 0.5f));
            if (consumed != unconsumedY) {
                mEdgeGlowTop.finish();
            }
            return unconsumedY - consumed;
        }
        if (unconsumedY < 0 && EdgeEffectCompat.getDistance(mEdgeGlowBottom) != 0f) {
            float deltaDistance = unconsumedY * FLING_DESTRETCH_FACTOR / height;
            int consumed = Math.round(height / FLING_DESTRETCH_FACTOR
                    * EdgeEffectCompat.onPullDistance(mEdgeGlowBottom, deltaDistance, 0.5f));
            if (consumed != unconsumedY) {
                mEdgeGlowBottom.finish();
            }
            return unconsumedY - consumed;
        }
        return unconsumedY;
    }

    /**
     * Copied from OverScroller, this returns the distance that a fling with the given velocity
     * will go.
     * @param velocity The velocity of the fling
     * @return The distance that will be traveled by a fling of the given velocity.
     */
    private float getSplineFlingDistance(int velocity) {
        final double l =
                Math.log(INFLEXION * Math.abs(velocity) / (SCROLL_FRICTION * mPhysicalCoeff));
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (float) (SCROLL_FRICTION * mPhysicalCoeff
                * Math.exp(DECELERATION_RATE / decelMinusOne * l));
    }

    private boolean edgeEffectFling(int velocityY) {
        boolean consumed = true;
        if (EdgeEffectCompat.getDistance(mEdgeGlowTop) != 0) {
            if (shouldAbsorb(mEdgeGlowTop, velocityY)) {
                mEdgeGlowTop.onAbsorb(velocityY);
            } else {
                fling(-velocityY);
            }
        } else if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) != 0) {
            if (shouldAbsorb(mEdgeGlowBottom, -velocityY)) {
                mEdgeGlowBottom.onAbsorb(-velocityY);
            } else {
                fling(-velocityY);
            }
        } else {
            consumed = false;
        }
        return consumed;
    }

    /**
     * This stops any edge glow animation that is currently running by applying a
     * 0 length pull at the displacement given by the provided MotionEvent. On pre-S devices,
     * this method does nothing, allowing any animating edge effect to continue animating and
     * returning <code>false</code> always.
     *
     * @param e The motion event to use to indicate the finger position for the displacement of
     *          the current pull.
     * @return <code>true</code> if any edge effect had an existing effect to be drawn ond the
     * animation was stopped or <code>false</code> if no edge effect had a value to display.
     */
    private boolean stopGlowAnimations(MotionEvent e) {
        boolean stopped = false;
        if (EdgeEffectCompat.getDistance(mEdgeGlowTop) != 0) {
            EdgeEffectCompat.onPullDistance(mEdgeGlowTop, 0, e.getX() / getWidth());
            stopped = true;
        }
        if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) != 0) {
            EdgeEffectCompat.onPullDistance(mEdgeGlowBottom, 0, 1 - e.getX() / getWidth());
            stopped = true;
        }
        return stopped;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    @Override
    public boolean onGenericMotionEvent(@NonNull MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_SCROLL && !mIsBeingDragged) {
            final float verticalScroll;
            final int x;
            final int axis;

            if (MotionEventCompat.isFromSource(motionEvent, InputDevice.SOURCE_CLASS_POINTER)) {
                verticalScroll = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL);
                x = (int) motionEvent.getX();
                axis = MotionEvent.AXIS_VSCROLL;
            } else if (
                    MotionEventCompat.isFromSource(motionEvent, InputDevice.SOURCE_ROTARY_ENCODER)
            ) {
                verticalScroll = motionEvent.getAxisValue(MotionEvent.AXIS_SCROLL);
                // Since a Wear rotary event doesn't have a true X and we want to support proper
                // overscroll animations, we put the x at the center of the screen.
                x = getWidth() / 2;
                axis = MotionEvent.AXIS_SCROLL;
            } else {
                verticalScroll = 0;
                x = 0;
                axis = 0;
            }

            if (verticalScroll != 0) {
                // Rotary and Mouse scrolls are inverted from a touch scroll.
                final int invertedDelta = (int) (verticalScroll * getVerticalScrollFactorCompat());

                final boolean isSourceMouse =
                        MotionEventCompat.isFromSource(motionEvent, InputDevice.SOURCE_MOUSE);

                scrollBy(-invertedDelta, axis, motionEvent, x, ViewCompat.TYPE_NON_TOUCH,
                        isSourceMouse);
                if (axis != 0) {
                    mDifferentialMotionFlingController.onMotionEvent(motionEvent, axis);
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the NestedScrollView supports over scroll.
     */
    private boolean canOverScroll() {
        final int mode = getOverScrollMode();
        return mode == OVER_SCROLL_ALWAYS
                || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && getScrollRange() > 0);
    }

    @VisibleForTesting
    float getVerticalScrollFactorCompat() {
        if (mVerticalScrollFactor == 0) {
            TypedValue outValue = new TypedValue();
            final Context context = getContext();
            if (!context.getTheme().resolveAttribute(
                    android.R.attr.listPreferredItemHeight, outValue, true)) {
                throw new IllegalStateException(
                        "Expected theme to define listPreferredItemHeight.");
            }
            mVerticalScrollFactor = outValue.getDimension(
                    context.getResources().getDisplayMetrics());
        }
        return mVerticalScrollFactor;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
            boolean clampedX, boolean clampedY) {
        super.scrollTo(scrollX, scrollY);
    }

    @SuppressWarnings({"SameParameterValue", "unused"})
    boolean overScrollByCompat(int deltaX, int deltaY,
            int scrollX, int scrollY,
            int scrollRangeX, int scrollRangeY,
            int maxOverScrollX, int maxOverScrollY,
            boolean isTouchEvent) {

        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
                computeVerticalScrollRange() > computeVerticalScrollExtent();

        final boolean overScrollHorizontal = overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        // Clamp values if at the limits and record
        final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        if (clampedY && !hasNestedScrollingParent(ViewCompat.TYPE_NON_TOUCH)) {
            mScroller.springBack(newScrollX, newScrollY, 0, 0, 0, getScrollRange());
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            scrollRange = Math.max(0, childSize - parentSpace);
        }
        return scrollRange;
    }

    /**
     * <p>
     * Finds the next focusable component that fits in the specified bounds.
     * </p>
     *
     * @param topFocus look for a candidate is the one at the top of the bounds
     *                 if topFocus is true, or at the bottom of the bounds if topFocus is
     *                 false
     * @param top      the top offset of the bounds in which a focusable must be
     *                 found
     * @param bottom   the bottom offset of the bounds in which a focusable must
     *                 be found
     * @return the next focusable component in the bounds or null if none can
     *         be found
     */
    private View findFocusableViewInBounds(boolean topFocus, int top, int bottom) {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;

        int count = focusables.size();
        for (int i = 0; i < count; i++) {
            View view = focusables.get(i);
            int viewTop = view.getTop();
            int viewBottom = view.getBottom();

            if (top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = (top < viewTop) && (viewBottom < bottom);

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final boolean viewIsCloserToBoundary =
                            (topFocus && viewTop < focusCandidate.getTop())
                                    || (!topFocus && viewBottom > focusCandidate.getBottom());

                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view;
                        }
                    } else {
                        if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view;
                        }
                    }
                }
            }
        }

        return focusCandidate;
    }

    /**
     * <p>Handles scrolling in response to a "page up/down" shortcut press. This
     * method will scroll the view by one page up or down and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link View#FOCUS_UP}
     *                  to go one page up or
     *                  {@link View#FOCUS_DOWN} to go one page down
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean pageScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        if (down) {
            mTempRect.top = getScrollY() + height;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                int bottom = view.getBottom() + lp.bottomMargin + getPaddingBottom();
                if (mTempRect.top + height > bottom) {
                    mTempRect.top = bottom - height;
                }
            }
        } else {
            mTempRect.top = getScrollY() - height;
            if (mTempRect.top < 0) {
                mTempRect.top = 0;
            }
        }
        mTempRect.bottom = mTempRect.top + height;

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    /**
     * <p>Handles scrolling in response to a "home/end" shortcut press. This
     * method will scroll the view to the top or bottom and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link View#FOCUS_UP}
     *                  to go the top of the view or
     *                  {@link View#FOCUS_DOWN} to go the bottom
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean fullScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        mTempRect.top = 0;
        mTempRect.bottom = height;

        if (down) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                mTempRect.bottom = view.getBottom() + lp.bottomMargin + getPaddingBottom();
                mTempRect.top = mTempRect.bottom - height;
            }
        }
        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    /**
     * <p>Scrolls the view to make the area defined by <code>top</code> and
     * <code>bottom</code> visible. This method attempts to give the focus
     * to a component visible in this area. If no component can be focused in
     * the new visible area, the focus is reclaimed by this ScrollView.</p>
     *
     * @param direction the scroll direction: {@link View#FOCUS_UP}
     *                  to go upward, {@link View#FOCUS_DOWN} to downward
     * @param top       the top offset of the new area to be made visible
     * @param bottom    the bottom offset of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private boolean scrollAndFocus(int direction, int top, int bottom) {
        boolean handled = true;

        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == View.FOCUS_UP;

        View newFocused = findFocusableViewInBounds(up, top, bottom);
        if (newFocused == null) {
            newFocused = this;
        }

        if (top >= containerTop && bottom <= containerBottom) {
            handled = false;
        } else {
            int delta = up ? (top - containerTop) : (bottom - containerBottom);
            scrollBy(delta, 0, ViewCompat.TYPE_NON_TOUCH, true);
        }

        if (newFocused != findFocus()) newFocused.requestFocus(direction);

        return handled;
    }

    /**
     * Handle scrolling in response to an up or down arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was
     *                  pressed
     * @return True if we consumed the event, false otherwise
     */
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) currentFocused = null;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

        final int maxJump = getMaxScrollAmount();

        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJump, getHeight())) {
            nextFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);

            scrollBy(scrollDelta, 0, ViewCompat.TYPE_NON_TOUCH, true);
            nextFocused.requestFocus(direction);

        } else {
            // no new focus
            int scrollDelta = maxJump;

            if (direction == View.FOCUS_UP && getScrollY() < scrollDelta) {
                scrollDelta = getScrollY();
            } else if (direction == View.FOCUS_DOWN) {
                if (getChildCount() > 0) {
                    View child = getChildAt(0);
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int daBottom = child.getBottom() + lp.bottomMargin;
                    int screenBottom = getScrollY() + getHeight() - getPaddingBottom();
                    scrollDelta = Math.min(daBottom - screenBottom, maxJump);
                }
            }
            if (scrollDelta == 0) {
                return false;
            }

            int finalScrollDelta = direction == View.FOCUS_DOWN ? scrollDelta : -scrollDelta;
            scrollBy(finalScrollDelta, 0, ViewCompat.TYPE_NON_TOUCH, true);
        }

        if (currentFocused != null && currentFocused.isFocused()
                && isOffScreen(currentFocused)) {
            // previously focused item still has focus and is off screen, give
            // it up (take it back to ourselves)
            // (also, need to temporarily force FOCUS_BEFORE_DESCENDANTS so we are
            // sure to
            // get it)
            final int descendantFocusability = getDescendantFocusability();  // save
            setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            requestFocus();
            setDescendantFocusability(descendantFocusability);  // restore
        }
        return true;
    }

    /**
     * @return whether the descendant of this scroll view is scrolled off
     *  screen.
     */
    private boolean isOffScreen(View descendant) {
        return !isWithinDeltaOfScreen(descendant, 0, getHeight());
    }

    /**
     * @return whether the descendant of this scroll view is within delta
     *  pixels of being on the screen.
     */
    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);

        return (mTempRect.bottom + delta) >= getScrollY()
                && (mTempRect.top - delta) <= (getScrollY() + height);
    }

    /**
     * Smooth scroll by a Y delta
     *
     * @param delta the number of pixels to scroll by on the Y axis
     */
    private void doScrollY(int delta) {
        if (delta != 0) {
            if (mSmoothScrollingEnabled) {
                smoothScrollBy(0, delta);
            } else {
                scrollBy(0, delta);
            }
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    public final void smoothScrollBy(int dx, int dy) {
        smoothScrollBy(dx, dy, DEFAULT_SMOOTH_SCROLL_DURATION, false);
    }

   /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     */
    public final void smoothScrollBy(int dx, int dy, int scrollDurationMs) {
        smoothScrollBy(dx, dy, scrollDurationMs, false);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    private void smoothScrollBy(int dx, int dy, int scrollDurationMs, boolean withNestedScrolling) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            View child = getChildAt(0);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            final int scrollY = getScrollY();
            final int maxY = Math.max(0, childSize - parentSpace);
            dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;
            mScroller.startScroll(getScrollX(), scrollY, 0, dy, scrollDurationMs);
            runAnimatedScroll(withNestedScrolling);
        } else {
            if (!mScroller.isFinished()) {
                abortAnimatedScroll();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, DEFAULT_SMOOTH_SCROLL_DURATION, false);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     */
    public final void smoothScrollTo(int x, int y, int scrollDurationMs) {
        smoothScrollTo(x, y, scrollDurationMs, false);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    // This should be considered private, it is package private to avoid a synthetic ancestor.
    @SuppressWarnings("SameParameterValue")
    void smoothScrollTo(int x, int y, boolean withNestedScrolling) {
        smoothScrollTo(x, y, DEFAULT_SMOOTH_SCROLL_DURATION, withNestedScrolling);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    // This should be considered private, it is package private to avoid a synthetic ancestor.
    void smoothScrollTo(int x, int y, int scrollDurationMs, boolean withNestedScrolling) {
        smoothScrollBy(x - getScrollX(), y - getScrollY(), scrollDurationMs, withNestedScrolling);
    }

    /**
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int parentSpace = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0) {
            return parentSpace;
        }

        View child = getChildAt(0);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int scrollRange = child.getBottom() + lp.bottomMargin;
        final int scrollY = getScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - parentSpace);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset();
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    @Override
    protected void measureChild(@NonNull View child, int parentWidthMeasureSpec,
            int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft()
                + getPaddingRight(), lp.width);

        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                lp.topMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void computeScroll() {

        if (mScroller.isFinished()) {
            return;
        }

        mScroller.computeScrollOffset();
        final int y = mScroller.getCurrY();
        int unconsumed = consumeFlingInVerticalStretch(y - mLastScrollerY);
        mLastScrollerY = y;

        // Nested Scrolling Pre Pass
        mScrollConsumed[1] = 0;
        dispatchNestedPreScroll(0, unconsumed, mScrollConsumed, null,
                ViewCompat.TYPE_NON_TOUCH);
        unconsumed -= mScrollConsumed[1];

        final int range = getScrollRange();

        if (Build.VERSION.SDK_INT >= 35) {
            Api35Impl.setFrameContentVelocity(NestedScrollView.this,
                    Math.abs(mScroller.getCurrVelocity()));
        }

        if (unconsumed != 0) {
            // Internal Scroll
            final int oldScrollY = getScrollY();
            overScrollByCompat(0, unconsumed, getScrollX(), oldScrollY, 0, range, 0, 0, false);
            final int scrolledByMe = getScrollY() - oldScrollY;
            unconsumed -= scrolledByMe;

            // Nested Scrolling Post Pass
            mScrollConsumed[1] = 0;
            dispatchNestedScroll(0, scrolledByMe, 0, unconsumed, mScrollOffset,
                    ViewCompat.TYPE_NON_TOUCH, mScrollConsumed);
            unconsumed -= mScrollConsumed[1];
        }

        if (unconsumed != 0) {
            final int mode = getOverScrollMode();
            final boolean canOverscroll = mode == OVER_SCROLL_ALWAYS
                    || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
            if (canOverscroll) {
                if (unconsumed < 0) {
                    if (mEdgeGlowTop.isFinished()) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                } else {
                    if (mEdgeGlowBottom.isFinished()) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }
            abortAnimatedScroll();
        }

        if (!mScroller.isFinished()) {
            postInvalidateOnAnimation();
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
    }

    /**
     * If either of the vertical edge glows are currently active, this consumes part or all of
     * deltaY on the edge glow.
     *
     * @param deltaY The pointer motion, in pixels, in the vertical direction, positive
     *                         for moving down and negative for moving up.
     * @param x The vertical position of the pointer.
     * @return The amount of <code>deltaY</code> that has been consumed by the
     * edge glow.
     */
    private int releaseVerticalGlow(int deltaY, float x) {
        // First allow releasing existing overscroll effect:
        float consumed = 0;
        float displacement = x / getWidth();
        float pullDistance = (float) deltaY / getHeight();
        if (EdgeEffectCompat.getDistance(mEdgeGlowTop) != 0) {
            consumed = -EdgeEffectCompat.onPullDistance(mEdgeGlowTop, -pullDistance, displacement);
            if (EdgeEffectCompat.getDistance(mEdgeGlowTop) == 0) {
                mEdgeGlowTop.onRelease();
            }
        } else if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) != 0) {
            consumed = EdgeEffectCompat.onPullDistance(mEdgeGlowBottom, pullDistance,
                    1 - displacement);
            if (EdgeEffectCompat.getDistance(mEdgeGlowBottom) == 0) {
                mEdgeGlowBottom.onRelease();
            }
        }
        int pixelsConsumed = Math.round(consumed * getHeight());
        if (pixelsConsumed != 0) {
            invalidate();
        }
        return pixelsConsumed;
    }

    private void runAnimatedScroll(boolean participateInNestedScrolling) {
        if (participateInNestedScrolling) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
        mLastScrollerY = getScrollY();
        postInvalidateOnAnimation();
    }

    private void abortAnimatedScroll() {
        mScroller.abortAnimation();
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
    }

    /**
     * Scrolls the view to the given child.
     *
     * @param child the View to scroll to
     */
    private void scrollToChild(View child) {
        child.getDrawingRect(mTempRect);

        /* Offset from child's local coordinates to ScrollView coordinates */
        offsetDescendantRectToMyCoords(child, mTempRect);

        int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);

        if (scrollDelta != 0) {
            scrollBy(0, scrollDelta);
        }
    }

    /**
     * If rect is off screen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     *
     * @param rect      The rectangle.
     * @param immediate True to scroll immediately without animation
     * @return true if scrolling was performed
     */
    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        final int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        final boolean scroll = delta != 0;
        if (scroll) {
            if (immediate) {
                scrollBy(0, delta);
            } else {
                smoothScrollBy(0, delta);
            }
        }
        return scroll;
    }

    /**
     * Compute the amount to scroll in the Y direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) return 0;

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;
        int actualScreenBottom = screenBottom;

        int fadingEdge = getVerticalFadingEdgeLength();

        // TODO: screenTop should be incremented by fadingEdge * getTopFadingEdgeStrength (but for
        // the target scroll distance).
        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }

        // TODO: screenBottom should be decremented by fadingEdge * getBottomFadingEdgeStrength (but
        // for the target scroll distance).
        // leave room for bottom fading edge as long as rect isn't at very bottom
        View child = getChildAt(0);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (rect.bottom < child.getHeight() + lp.topMargin + lp.bottomMargin) {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta += (rect.top - screenTop);
            } else {
                // get entire rect at bottom of screen
                scrollYDelta += (rect.bottom - screenBottom);
            }

            // make sure we aren't scrolling beyond the end of our content
            int bottom = child.getBottom() + lp.bottomMargin;
            int distanceToBottom = bottom - actualScreenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta -= (screenBottom - rect.bottom);
            } else {
                // entire rect at top
                scrollYDelta -= (screenTop - rect.top);
            }

            // make sure we aren't scrolling any further than the top our content
            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (!mIsLayoutDirty) {
            scrollToChild(focused);
        } else {
            // The child may not be laid out yet, we can't compute the scroll yet
            mChildToScrollTo = focused;
        }
        super.requestChildFocus(child, focused);
    }


    /**
     * When looking for focus in children of a scroll view, need to be a little
     * more careful not to give focus to something that is scrolled off screen.
     *
     * This is more expensive than the default {@link ViewGroup}
     * implementation, otherwise this behavior might have been made the default.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {

        // convert from forward / backward notation to up / down / left / right
        // (ugh).
        if (direction == View.FOCUS_FORWARD) {
            direction = View.FOCUS_DOWN;
        } else if (direction == View.FOCUS_BACKWARD) {
            direction = View.FOCUS_UP;
        }

        final View nextFocus = previouslyFocusedRect == null
                ? FocusFinder.getInstance().findNextFocus(this, null, direction)
                : FocusFinder.getInstance().findNextFocusFromRect(
                        this, previouslyFocusedRect, direction);

        if (nextFocus == null) {
            return false;
        }

        if (isOffScreen(nextFocus)) {
            return false;
        }

        return nextFocus.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(@NonNull View child, Rect rectangle,
            boolean immediate) {
        // offset into coordinate space of this scroll view
        rectangle.offset(child.getLeft() - child.getScrollX(),
                child.getTop() - child.getScrollY());

        return scrollToChildRect(rectangle, immediate);
    }

    @Override
    public void requestLayout() {
        mIsLayoutDirty = true;
        super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mIsLayoutDirty = false;
        // Give a child focus if it needs it
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;

        if (!mIsLaidOut) {
            // If there is a saved state, scroll to the position saved in that state.
            if (mSavedState != null) {
                scrollTo(getScrollX(), mSavedState.scrollPosition);
                mSavedState = null;
            } // mScrollY default value is "0"

            // Make sure current scrollY position falls into the scroll range.  If it doesn't,
            // scroll such that it does.
            int childSize = 0;
            if (getChildCount() > 0) {
                View child = getChildAt(0);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                childSize = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            }
            int parentSpace = b - t - getPaddingTop() - getPaddingBottom();
            int currentScrollY = getScrollY();
            int newScrollY = clamp(currentScrollY, parentSpace, childSize);
            if (newScrollY != currentScrollY) {
                scrollTo(getScrollX(), newScrollY);
            }
        }

        // Calling this with the present values causes it to re-claim them
        scrollTo(getScrollX(), getScrollY());
        mIsLaidOut = true;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mIsLaidOut = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused) {
            return;
        }

        // If the currently-focused view was visible on the screen when the
        // screen was at the old height, then scroll the screen to make that
        // view visible with the new screen height.
        if (isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollY(scrollDelta);
        }
    }

    /**
     * Return true if child is a descendant of parent, (or equal to the parent).
     */
    private static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }

        final ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityY) {
        if (getChildCount() > 0) {

            mScroller.fling(getScrollX(), getScrollY(), // start
                    0, velocityY, // velocities
                    0, 0, // x
                    Integer.MIN_VALUE, Integer.MAX_VALUE, // y
                    0, 0); // overscroll
            runAnimatedScroll(true);
            if (Build.VERSION.SDK_INT >= 35) {
                Api35Impl.setFrameContentVelocity(NestedScrollView.this,
                        Math.abs(mScroller.getCurrVelocity()));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This version also clamps the scrolling to the bounds of our child.
     */
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int parentSpaceHorizontal = getWidth() - getPaddingLeft() - getPaddingRight();
            int childSizeHorizontal = child.getWidth() + lp.leftMargin + lp.rightMargin;
            int parentSpaceVertical = getHeight() - getPaddingTop() - getPaddingBottom();
            int childSizeVertical = child.getHeight() + lp.topMargin + lp.bottomMargin;
            x = clamp(x, parentSpaceHorizontal, childSizeHorizontal);
            y = clamp(y, parentSpaceVertical, childSizeVertical);
            if (x != getScrollX() || y != getScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        final int scrollY = getScrollY();
        if (!mEdgeGlowTop.isFinished()) {
            final int restoreCount = canvas.save();
            int width = getWidth();
            int height = getHeight();
            int xTranslation = 0;
            int yTranslation = Math.min(0, scrollY);
            if (getClipToPadding()) {
                width -= getPaddingLeft() + getPaddingRight();
                xTranslation += getPaddingLeft();
                height -= getPaddingTop() + getPaddingBottom();
                yTranslation += getPaddingTop();
            }
            canvas.translate(xTranslation, yTranslation);
            mEdgeGlowTop.setSize(width, height);
            if (mEdgeGlowTop.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }
        if (!mEdgeGlowBottom.isFinished()) {
            final int restoreCount = canvas.save();
            int width = getWidth();
            int height = getHeight();
            int xTranslation = 0;
            int yTranslation = Math.max(getScrollRange(), scrollY) + height;
            if (getClipToPadding()) {
                width -= getPaddingLeft() + getPaddingRight();
                xTranslation += getPaddingLeft();
            }
            if (getClipToPadding()) {
                height -= getPaddingTop() + getPaddingBottom();
                yTranslation -= getPaddingBottom();
            }
            canvas.translate(xTranslation - width, yTranslation);
            canvas.rotate(180, width, 0);
            mEdgeGlowBottom.setSize(width, height);
            if (mEdgeGlowBottom.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }
    }

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- mScrollX --|
             */
            return 0;
        }
        if ((my + n) > child) {
            /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- mScrollX --|
             */
            return child - my;
        }
        return n;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mSavedState = ss;
        requestLayout();
    }

    @Override
    protected @NonNull Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.scrollPosition = getScrollY();
        return ss;
    }

    static class SavedState extends BaseSavedState {
        public int scrollPosition;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel source) {
            super(source);
            scrollPosition = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(scrollPosition);
        }

        @Override
        public @NonNull String toString() {
            return "HorizontalScrollView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " scrollPosition=" + scrollPosition + "}";
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    static class AccessibilityDelegate extends AccessibilityDelegateCompat {
        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
            if (super.performAccessibilityAction(host, action, arguments)) {
                return true;
            }
            final NestedScrollView nsvHost = (NestedScrollView) host;
            if (!nsvHost.isEnabled()) {
                return false;
            }
            int height = nsvHost.getHeight();
            Rect rect = new Rect();
            // Gets the visible rect on the screen except for the rotation or scale cases which
            // might affect the result.
            if (nsvHost.getMatrix().isIdentity() && nsvHost.getGlobalVisibleRect(rect)) {
                height = rect.height();
            }
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                case android.R.id.accessibilityActionScrollDown: {
                    final int viewportHeight = height - nsvHost.getPaddingBottom()
                            - nsvHost.getPaddingTop();
                    final int targetScrollY = Math.min(nsvHost.getScrollY() + viewportHeight,
                            nsvHost.getScrollRange());
                    if (targetScrollY != nsvHost.getScrollY()) {
                        nsvHost.smoothScrollTo(0, targetScrollY, true);
                        return true;
                    }
                }
                return false;
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                case android.R.id.accessibilityActionScrollUp: {
                    final int viewportHeight = height - nsvHost.getPaddingBottom()
                            - nsvHost.getPaddingTop();
                    final int targetScrollY = Math.max(nsvHost.getScrollY() - viewportHeight, 0);
                    if (targetScrollY != nsvHost.getScrollY()) {
                        nsvHost.smoothScrollTo(0, targetScrollY, true);
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            final NestedScrollView nsvHost = (NestedScrollView) host;
            info.setClassName(ScrollView.class.getName());
            if (nsvHost.isEnabled()) {
                final int scrollRange = nsvHost.getScrollRange();
                if (scrollRange > 0) {
                    info.setScrollable(true);
                    if (nsvHost.getScrollY() > 0) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_BACKWARD);
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_UP);
                    }
                    if (nsvHost.getScrollY() < scrollRange) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_FORWARD);
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_DOWN);
                    }
                }
            }
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            final NestedScrollView nsvHost = (NestedScrollView) host;
            event.setClassName(ScrollView.class.getName());
            final boolean scrollable = nsvHost.getScrollRange() > 0;
            event.setScrollable(scrollable);
            event.setScrollX(nsvHost.getScrollX());
            event.setScrollY(nsvHost.getScrollY());
            AccessibilityRecordCompat.setMaxScrollX(event, nsvHost.getScrollX());
            AccessibilityRecordCompat.setMaxScrollY(event, nsvHost.getScrollRange());
        }
    }

    private ScrollFeedbackProviderCompat getScrollFeedbackProvider() {
        if (mScrollFeedbackProvider == null) {
            mScrollFeedbackProvider = ScrollFeedbackProviderCompat.createProvider(this);
        }
        return mScrollFeedbackProvider;
    }

    class DifferentialMotionFlingTargetImpl implements DifferentialMotionFlingTarget {
        @Override
        public boolean startDifferentialMotionFling(float velocity) {
            if (velocity == 0) {
                return false;
            }
            stopDifferentialMotionFling();
            fling((int) velocity);
            return true;
        }

        @Override
        public void stopDifferentialMotionFling() {
            mScroller.abortAnimation();
        }

        @Override
        public float getScaledScrollFactor() {
            return -getVerticalScrollFactorCompat();
        }
    }

    @RequiresApi(35)
    private static final class Api35Impl {
        public static void setFrameContentVelocity(View view, float velocity) {
            try {
                view.setFrameContentVelocity(velocity);
            } catch (LinkageError e) {
                // The setFrameContentVelocity method is unavailable on this device.
            }
        }
    }
}
