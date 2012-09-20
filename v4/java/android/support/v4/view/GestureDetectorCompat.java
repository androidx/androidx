/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v4.view;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Detects various gestures and events using the supplied {@link MotionEvent}s.
 * The {@link OnGestureListener} callback will notify users when a particular
 * motion event has occurred. This class should only be used with {@link MotionEvent}s
 * reported via touch (don't use for trackball events).
 *
 * <p>This compatibility implementation of the framework's GestureDetector guarantees
 * the newer focal point scrolling behavior from Jellybean MR1 on all platform versions.</p>
 *
 * To use this class:
 * <ul>
 *  <li>Create an instance of the {@code GestureDetectorCompat} for your {@link View}
 *  <li>In the {@link View#onTouchEvent(MotionEvent)} method ensure you call
 *          {@link #onTouchEvent(MotionEvent)}. The methods defined in your callback
 *          will be executed when the events occur.
 * </ul>
 */
public class GestureDetectorCompat {
    interface GestureDetectorCompatImpl {
        boolean isLongpressEnabled();
        boolean onTouchEvent(MotionEvent ev);
        void setIsLongpressEnabled(boolean enabled);
        void setOnDoubleTapListener(OnDoubleTapListener listener);
    }

    static class GestureDetectorCompatImplBase implements GestureDetectorCompatImpl {
        private int mTouchSlopSquare;
        private int mDoubleTapSlopSquare;
        private int mMinimumFlingVelocity;
        private int mMaximumFlingVelocity;

        private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
        private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
        private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();

        // constants for Message.what used by GestureHandler below
        private static final int SHOW_PRESS = 1;
        private static final int LONG_PRESS = 2;
        private static final int TAP = 3;

        private final Handler mHandler;
        private final OnGestureListener mListener;
        private OnDoubleTapListener mDoubleTapListener;

        private boolean mStillDown;
        private boolean mInLongPress;
        private boolean mAlwaysInTapRegion;
        private boolean mAlwaysInBiggerTapRegion;

        private MotionEvent mCurrentDownEvent;
        private MotionEvent mPreviousUpEvent;

        /**
         * True when the user is still touching for the second tap (down, move, and
         * up events). Can only be true if there is a double tap listener attached.
         */
        private boolean mIsDoubleTapping;

        private float mLastFocusX;
        private float mLastFocusY;
        private float mDownFocusX;
        private float mDownFocusY;

        private boolean mIsLongpressEnabled;

        /**
         * Determines speed during touch scrolling
         */
        private VelocityTracker mVelocityTracker;

        private class GestureHandler extends Handler {
            GestureHandler() {
                super();
            }

            GestureHandler(Handler handler) {
                super(handler.getLooper());
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case SHOW_PRESS:
                    mListener.onShowPress(mCurrentDownEvent);
                    break;

                case LONG_PRESS:
                    dispatchLongPress();
                    break;

                case TAP:
                    // If the user's finger is still down, do not count it as a tap
                    if (mDoubleTapListener != null && !mStillDown) {
                        mDoubleTapListener.onSingleTapConfirmed(mCurrentDownEvent);
                    }
                    break;

                default:
                    throw new RuntimeException("Unknown message " + msg); //never
                }
            }
        }

        /**
         * Creates a GestureDetector with the supplied listener.
         * You may only use this constructor from a UI thread (this is the usual situation).
         * @see android.os.Handler#Handler()
         *
         * @param context the application's context
         * @param listener the listener invoked for all the callbacks, this must
         * not be null.
         * @param handler the handler to use
         *
         * @throws NullPointerException if {@code listener} is null.
         */
        public GestureDetectorCompatImplBase(Context context, OnGestureListener listener,
                Handler handler) {
            if (handler != null) {
                mHandler = new GestureHandler(handler);
            } else {
                mHandler = new GestureHandler();
            }
            mListener = listener;
            if (listener instanceof OnDoubleTapListener) {
                setOnDoubleTapListener((OnDoubleTapListener) listener);
            }
            init(context);
        }

        private void init(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null");
            }
            if (mListener == null) {
                throw new IllegalArgumentException("OnGestureListener must not be null");
            }
            mIsLongpressEnabled = true;

            final ViewConfiguration configuration = ViewConfiguration.get(context);
            final int touchSlop = configuration.getScaledTouchSlop();
            final int doubleTapSlop = configuration.getScaledDoubleTapSlop();
            mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
            mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();

            mTouchSlopSquare = touchSlop * touchSlop;
            mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
        }

        /**
         * Sets the listener which will be called for double-tap and related
         * gestures.
         *
         * @param onDoubleTapListener the listener invoked for all the callbacks, or
         *        null to stop listening for double-tap gestures.
         */
        public void setOnDoubleTapListener(OnDoubleTapListener onDoubleTapListener) {
            mDoubleTapListener = onDoubleTapListener;
        }

        /**
         * Set whether longpress is enabled, if this is enabled when a user
         * presses and holds down you get a longpress event and nothing further.
         * If it's disabled the user can press and hold down and then later
         * moved their finger and you will get scroll events. By default
         * longpress is enabled.
         *
         * @param isLongpressEnabled whether longpress should be enabled.
         */
        public void setIsLongpressEnabled(boolean isLongpressEnabled) {
            mIsLongpressEnabled = isLongpressEnabled;
        }

        /**
         * @return true if longpress is enabled, else false.
         */
        public boolean isLongpressEnabled() {
            return mIsLongpressEnabled;
        }

        /**
         * Analyzes the given motion event and if applicable triggers the
         * appropriate callbacks on the {@link OnGestureListener} supplied.
         *
         * @param ev The current motion event.
         * @return true if the {@link OnGestureListener} consumed the event,
         *              else false.
         */
        public boolean onTouchEvent(MotionEvent ev) {
            final int action = ev.getAction();

            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(ev);

            final boolean pointerUp =
                    (action & MotionEventCompat.ACTION_MASK) == MotionEventCompat.ACTION_POINTER_UP;
            final int skipIndex = pointerUp ? MotionEventCompat.getActionIndex(ev) : -1;

            // Determine focal point
            float sumX = 0, sumY = 0;
            final int count = MotionEventCompat.getPointerCount(ev);
            for (int i = 0; i < count; i++) {
                if (skipIndex == i) continue;
                sumX += MotionEventCompat.getX(ev, i);
                sumY += MotionEventCompat.getY(ev, i);
            }
            final int div = pointerUp ? count - 1 : count;
            final float focusX = sumX / div;
            final float focusY = sumY / div;

            boolean handled = false;

            switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEventCompat.ACTION_POINTER_DOWN:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                // Cancel long press and taps
                cancelTaps();
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;

                // Check the dot product of current velocities.
                // If the pointer that left was opposing another velocity vector, clear.
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                final int upIndex = MotionEventCompat.getActionIndex(ev);
                final int id1 = MotionEventCompat.getPointerId(ev, upIndex);
                final float x1 = VelocityTrackerCompat.getXVelocity(mVelocityTracker, id1);
                final float y1 = VelocityTrackerCompat.getYVelocity(mVelocityTracker, id1);
                for (int i = 0; i < count; i++) {
                    if (i == upIndex) continue;

                    final int id2 = MotionEventCompat.getPointerId(ev, i);
                    final float x = x1 * VelocityTrackerCompat.getXVelocity(mVelocityTracker, id2);
                    final float y = y1 * VelocityTrackerCompat.getYVelocity(mVelocityTracker, id2);

                    final float dot = x + y;
                    if (dot < 0) {
                        mVelocityTracker.clear();
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_DOWN:
                if (mDoubleTapListener != null) {
                    boolean hadTapMessage = mHandler.hasMessages(TAP);
                    if (hadTapMessage) mHandler.removeMessages(TAP);
                    if ((mCurrentDownEvent != null) && (mPreviousUpEvent != null) && hadTapMessage &&
                            isConsideredDoubleTap(mCurrentDownEvent, mPreviousUpEvent, ev)) {
                        // This is a second tap
                        mIsDoubleTapping = true;
                        // Give a callback with the first tap of the double-tap
                        handled |= mDoubleTapListener.onDoubleTap(mCurrentDownEvent);
                        // Give a callback with down event of the double-tap
                        handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                    } else {
                        // This is a first tap
                        mHandler.sendEmptyMessageDelayed(TAP, DOUBLE_TAP_TIMEOUT);
                    }
                }

                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(ev);
                mAlwaysInTapRegion = true;
                mAlwaysInBiggerTapRegion = true;
                mStillDown = true;
                mInLongPress = false;

                if (mIsLongpressEnabled) {
                    mHandler.removeMessages(LONG_PRESS);
                    mHandler.sendEmptyMessageAtTime(LONG_PRESS, mCurrentDownEvent.getDownTime()
                            + TAP_TIMEOUT + LONGPRESS_TIMEOUT);
                }
                mHandler.sendEmptyMessageAtTime(SHOW_PRESS, mCurrentDownEvent.getDownTime() + TAP_TIMEOUT);
                handled |= mListener.onDown(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mInLongPress) {
                    break;
                }
                final float scrollX = mLastFocusX - focusX;
                final float scrollY = mLastFocusY - focusY;
                if (mIsDoubleTapping) {
                    // Give the move events of the double-tap
                    handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                } else if (mAlwaysInTapRegion) {
                    final int deltaX = (int) (focusX - mDownFocusX);
                    final int deltaY = (int) (focusY - mDownFocusY);
                    int distance = (deltaX * deltaX) + (deltaY * deltaY);
                    if (distance > mTouchSlopSquare) {
                        handled = mListener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
                        mLastFocusX = focusX;
                        mLastFocusY = focusY;
                        mAlwaysInTapRegion = false;
                        mHandler.removeMessages(TAP);
                        mHandler.removeMessages(SHOW_PRESS);
                        mHandler.removeMessages(LONG_PRESS);
                    }
                    if (distance > mTouchSlopSquare) {
                        mAlwaysInBiggerTapRegion = false;
                    }
                } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    handled = mListener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
                    mLastFocusX = focusX;
                    mLastFocusY = focusY;
                }
                break;

            case MotionEvent.ACTION_UP:
                mStillDown = false;
                MotionEvent currentUpEvent = MotionEvent.obtain(ev);
                if (mIsDoubleTapping) {
                    // Finally, give the up event of the double-tap
                    handled |= mDoubleTapListener.onDoubleTapEvent(ev);
                } else if (mInLongPress) {
                    mHandler.removeMessages(TAP);
                    mInLongPress = false;
                } else if (mAlwaysInTapRegion) {
                    handled = mListener.onSingleTapUp(ev);
                } else {

                    // A fling must travel the minimum tap distance
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    final int pointerId = MotionEventCompat.getPointerId(ev, 0);
                    velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                    final float velocityY = VelocityTrackerCompat.getYVelocity(
                            velocityTracker, pointerId);
                    final float velocityX = VelocityTrackerCompat.getXVelocity(
                            velocityTracker, pointerId);

                    if ((Math.abs(velocityY) > mMinimumFlingVelocity)
                            || (Math.abs(velocityX) > mMinimumFlingVelocity)){
                        handled = mListener.onFling(mCurrentDownEvent, ev, velocityX, velocityY);
                    }
                }
                if (mPreviousUpEvent != null) {
                    mPreviousUpEvent.recycle();
                }
                // Hold the event we obtained above - listeners may have changed the original.
                mPreviousUpEvent = currentUpEvent;
                if (mVelocityTracker != null) {
                    // This may have been cleared when we called out to the
                    // application above.
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mIsDoubleTapping = false;
                mHandler.removeMessages(SHOW_PRESS);
                mHandler.removeMessages(LONG_PRESS);
                break;

            case MotionEvent.ACTION_CANCEL:
                cancel();
                break;
            }

            return handled;
        }

        private void cancel() {
            mHandler.removeMessages(SHOW_PRESS);
            mHandler.removeMessages(LONG_PRESS);
            mHandler.removeMessages(TAP);
            mVelocityTracker.recycle();
            mVelocityTracker = null;
            mIsDoubleTapping = false;
            mStillDown = false;
            mAlwaysInTapRegion = false;
            mAlwaysInBiggerTapRegion = false;
            if (mInLongPress) {
                mInLongPress = false;
            }
        }

        private void cancelTaps() {
            mHandler.removeMessages(SHOW_PRESS);
            mHandler.removeMessages(LONG_PRESS);
            mHandler.removeMessages(TAP);
            mIsDoubleTapping = false;
            mAlwaysInTapRegion = false;
            mAlwaysInBiggerTapRegion = false;
            if (mInLongPress) {
                mInLongPress = false;
            }
        }

        private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp,
                MotionEvent secondDown) {
            if (!mAlwaysInBiggerTapRegion) {
                return false;
            }

            if (secondDown.getEventTime() - firstUp.getEventTime() > DOUBLE_TAP_TIMEOUT) {
                return false;
            }

            int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
            int deltaY = (int) firstDown.getY() - (int) secondDown.getY();
            return (deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare);
        }

        private void dispatchLongPress() {
            mHandler.removeMessages(TAP);
            mInLongPress = true;
            mListener.onLongPress(mCurrentDownEvent);
        }
    }

    static class GestureDetectorCompatImplJellybeanMr1 implements GestureDetectorCompatImpl {
        private final GestureDetector mDetector;

        public GestureDetectorCompatImplJellybeanMr1(Context context, OnGestureListener listener,
                Handler handler) {
            mDetector = new GestureDetector(context, listener, handler);
        }

        @Override
        public boolean isLongpressEnabled() {
            return mDetector.isLongpressEnabled();
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            return mDetector.onTouchEvent(ev);
        }

        @Override
        public void setIsLongpressEnabled(boolean enabled) {
            mDetector.setIsLongpressEnabled(enabled);
        }

        @Override
        public void setOnDoubleTapListener(OnDoubleTapListener listener) {
            mDetector.setOnDoubleTapListener(listener);
        }
    }

    private final GestureDetectorCompatImpl mImpl;

    /**
     * Creates a GestureDetectorCompat with the supplied listener.
     * As usual, you may only use this constructor from a UI thread.
     * @see android.os.Handler#Handler()
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     */
    public GestureDetectorCompat(Context context, OnGestureListener listener) {
        this(context, listener, null);
    }

    /**
     * Creates a GestureDetectorCompat with the supplied listener.
     * As usual, you may only use this constructor from a UI thread.
     * @see android.os.Handler#Handler()
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     * @param handler the handler that will be used for posting deferred messages
     */
    public GestureDetectorCompat(Context context, OnGestureListener listener, Handler handler) {
        if (Build.VERSION.SDK_INT >= 17) {
            mImpl = new GestureDetectorCompatImplJellybeanMr1(context, listener, handler);
        } else {
            mImpl = new GestureDetectorCompatImplBase(context, listener, handler);
        }
    }

    /**
     * @return true if longpress is enabled, else false.
     */
    public boolean isLongpressEnabled() {
        return mImpl.isLongpressEnabled();
    }

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the {@link OnGestureListener} supplied.
     *
     * @param event The current motion event.
     * @return true if the {@link OnGestureListener} consumed the event,
     *              else false.
     */
    public boolean onTouchEvent(MotionEvent event) {
        return mImpl.onTouchEvent(event);
    }

    /**
     * Set whether longpress is enabled, if this is enabled when a user
     * presses and holds down you get a longpress event and nothing further.
     * If it's disabled the user can press and hold down and then later
     * moved their finger and you will get scroll events. By default
     * longpress is enabled.
     *
     * @param enabled whether longpress should be enabled.
     */
    public void setIsLongpressEnabled(boolean enabled) {
        mImpl.setIsLongpressEnabled(enabled);
    }

    /**
     * Sets the listener which will be called for double-tap and related
     * gestures.
     *
     * @param listener the listener invoked for all the callbacks, or
     *        null to stop listening for double-tap gestures.
     */
    public void setOnDoubleTapListener(OnDoubleTapListener listener) {
        mImpl.setOnDoubleTapListener(listener);
    }
}
