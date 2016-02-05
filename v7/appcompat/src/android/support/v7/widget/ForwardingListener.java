/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v7.widget;

import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.view.menu.ShowableListMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;


/**
 * Abstract class that forwards touch events to a {@link ShowableListMenu}.
 *
 * @hide
 */
public abstract class ForwardingListener implements View.OnTouchListener {
    /** Scaled touch slop, used for detecting movement outside bounds. */
    private final float mScaledTouchSlop;

    /** Timeout before disallowing intercept on the source's parent. */
    private final int mTapTimeout;

    /** Timeout before accepting a long-press to start forwarding. */
    private final int mLongPressTimeout;

    /** Source view from which events are forwarded. */
    private final View mSrc;

    /** Runnable used to prevent conflicts with scrolling parents. */
    private Runnable mDisallowIntercept;

    /** Runnable used to trigger forwarding on long-press. */
    private Runnable mTriggerLongPress;

    /** Whether this listener is currently forwarding touch events. */
    private boolean mForwarding;

    /** The id of the first pointer down in the current event stream. */
    private int mActivePointerId;

    /**
     * Temporary Matrix instance
     */
    private final int[] mTmpLocation = new int[2];

    public ForwardingListener(View src) {
        mSrc = src;
        mScaledTouchSlop = ViewConfiguration.get(src.getContext()).getScaledTouchSlop();
        mTapTimeout = ViewConfiguration.getTapTimeout();
        // Use a medium-press timeout. Halfway between tap and long-press.
        mLongPressTimeout = (mTapTimeout + ViewConfiguration.getLongPressTimeout()) / 2;
    }

    /**
     * Returns the popup to which this listener is forwarding events.
     * <p>
     * Override this to return the correct popup. If the popup is displayed
     * asynchronously, you may also need to override
     * {@link #onForwardingStopped} to prevent premature cancelation of
     * forwarding.
     *
     * @return the popup to which this listener is forwarding events
     */
    public abstract ShowableListMenu getPopup();

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final boolean wasForwarding = mForwarding;
        final boolean forwarding;
        if (wasForwarding) {
            forwarding = onTouchForwarded(event) || !onForwardingStopped();
        } else {
            forwarding = onTouchObserved(event) && onForwardingStarted();

            if (forwarding) {
                // Make sure we cancel any ongoing source event stream.
                final long now = SystemClock.uptimeMillis();
                final MotionEvent e = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL,
                        0.0f, 0.0f, 0);
                mSrc.onTouchEvent(e);
                e.recycle();
            }
        }

        mForwarding = forwarding;
        return forwarding || wasForwarding;
    }

    /**
     * Called when forwarding would like to start.
     * <p>
     * By default, this will show the popup returned by {@link #getPopup()}.
     * It may be overridden to perform another action, like clicking the
     * source view or preparing the popup before showing it.
     *
     * @return true to start forwarding, false otherwise
     */
    protected boolean onForwardingStarted() {
        final ShowableListMenu popup = getPopup();
        if (popup != null && !popup.isShowing()) {
            popup.show();
        }
        return true;
    }

    /**
     * Called when forwarding would like to stop.
     * <p>
     * By default, this will dismiss the popup returned by
     * {@link #getPopup()}. It may be overridden to perform some other
     * action.
     *
     * @return true to stop forwarding, false otherwise
     */
    protected boolean onForwardingStopped() {
        final ShowableListMenu popup = getPopup();
        if (popup != null && popup.isShowing()) {
            popup.dismiss();
        }
        return true;
    }

    /**
     * Observes motion events and determines when to start forwarding.
     *
     * @param srcEvent motion event in source view coordinates
     * @return true to start forwarding motion events, false otherwise
     */
    private boolean onTouchObserved(MotionEvent srcEvent) {
        final View src = mSrc;
        if (!src.isEnabled()) {
            return false;
        }

        final int actionMasked = MotionEventCompat.getActionMasked(srcEvent);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = srcEvent.getPointerId(0);

                if (mDisallowIntercept == null) {
                    mDisallowIntercept = new DisallowIntercept();
                }
                src.postDelayed(mDisallowIntercept, mTapTimeout);

                if (mTriggerLongPress == null) {
                    mTriggerLongPress = new TriggerLongPress();
                }
                src.postDelayed(mTriggerLongPress, mLongPressTimeout);
                break;
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = srcEvent.findPointerIndex(mActivePointerId);
                if (activePointerIndex >= 0) {
                    final float x = srcEvent.getX(activePointerIndex);
                    final float y = srcEvent.getY(activePointerIndex);

                    // Has the pointer moved outside of the view?
                    if (!pointInView(src, x, y, mScaledTouchSlop)) {
                        clearCallbacks();

                        // Don't let the parent intercept our events.
                        src.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                clearCallbacks();
                break;
        }

        return false;
    }

    private void clearCallbacks() {
        if (mTriggerLongPress != null) {
            mSrc.removeCallbacks(mTriggerLongPress);
        }

        if (mDisallowIntercept != null) {
            mSrc.removeCallbacks(mDisallowIntercept);
        }
    }

    private void onLongPress() {
        clearCallbacks();

        final View src = mSrc;
        if (!src.isEnabled() || src.isLongClickable()) {
            // Ignore long-press if the view is disabled or has its own
            // handler.
            return;
        }

        if (!onForwardingStarted()) {
            return;
        }

        // Don't let the parent intercept our events.
        src.getParent().requestDisallowInterceptTouchEvent(true);

        // Make sure we cancel any ongoing source event stream.
        final long now = SystemClock.uptimeMillis();
        final MotionEvent e = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        src.onTouchEvent(e);
        e.recycle();

        mForwarding = true;
    }

    /**
     * Handles forwarded motion events and determines when to stop
     * forwarding.
     *
     * @param srcEvent motion event in source view coordinates
     * @return true to continue forwarding motion events, false to cancel
     */
    private boolean onTouchForwarded(MotionEvent srcEvent) {
        final View src = mSrc;
        final ShowableListMenu popup = getPopup();
        if (popup == null || !popup.isShowing()) {
            return false;
        }

        final DropDownListView dst = (DropDownListView) popup.getListView();
        if (dst == null || !dst.isShown()) {
            return false;
        }

        // Convert event to destination-local coordinates.
        final MotionEvent dstEvent = MotionEvent.obtainNoHistory(srcEvent);
        toGlobalMotionEvent(src, dstEvent);
        toLocalMotionEvent(dst, dstEvent);

        // Forward converted event to destination view, then recycle it.
        final boolean handled = dst.onForwardedEvent(dstEvent, mActivePointerId);
        dstEvent.recycle();

        // Always cancel forwarding when the touch stream ends.
        final int action = MotionEventCompat.getActionMasked(srcEvent);
        final boolean keepForwarding = action != MotionEvent.ACTION_UP
                && action != MotionEvent.ACTION_CANCEL;

        return handled && keepForwarding;
    }

    private static boolean pointInView(View view, float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop &&
                localX < ((view.getRight() - view.getLeft()) + slop) &&
                localY < ((view.getBottom() - view.getTop()) + slop);
    }

    /**
     * Emulates View.toLocalMotionEvent(). This implementation does not handle transformations
     * (scaleX, scaleY, etc).
     */
    private boolean toLocalMotionEvent(View view, MotionEvent event) {
        final int[] loc = mTmpLocation;
        view.getLocationOnScreen(loc);
        event.offsetLocation(-loc[0], -loc[1]);
        return true;
    }

    /**
     * Emulates View.toGlobalMotionEvent(). This implementation does not handle transformations
     * (scaleX, scaleY, etc).
     */
    private boolean toGlobalMotionEvent(View view, MotionEvent event) {
        final int[] loc = mTmpLocation;
        view.getLocationOnScreen(loc);
        event.offsetLocation(loc[0], loc[1]);
        return true;
    }

    private class DisallowIntercept implements Runnable {
        @Override
        public void run() {
            final ViewParent parent = mSrc.getParent();
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private class TriggerLongPress implements Runnable {
        @Override
        public void run() {
            onLongPress();
        }
    }
}