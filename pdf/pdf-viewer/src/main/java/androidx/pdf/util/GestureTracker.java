/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Feed me events, I'll tell you what gesture is happening. This class also accepts a {@link
 * GestureHandler} delegate that will be invoked for every event that should be handled (as marked
 * in the {@link #feed} call).
 *
 * <p>It is an {@link OnTouchListener} so that it can be plugged directly into {@link
 * View#setOnTouchListener}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GestureTracker implements OnTouchListener {

    private static final String TAG = GestureTracker.class.getSimpleName();

    /**
     * The factor by which the swipe needs to be bigger horizontally than vertically to be
     * considered DRAG_X.
     */
    private static final float DRAG_X_MULTIPLIER = 1;
    /**
     * The factor by which the swipe needs to be bigger vertically than horizontally to be
     * considered DRAG_Y.
     */
    private static final float DRAG_Y_MULTIPLIER = 3;

    /** Multiplier to normalize mouse wheel scroll events. */
    private static final float MOUSE_SCROLL_MULTIPLIER = 10;
    /**
     * Negate vertical mouse wheel scroll value so scrolling up moves the content up, and down moves
     * it down, to be consistent with default behavior on Android.
     */
    private static final float VERTICAL_MOUSE_SCROLL_DIRECTION_ADJUSTMENT = -1;

    /** Identifier for a {@link MotionEvent}. Instances of MotionEvent are recycled. */
    static class EventId {
        private final long mEventTimeMs;
        private final int mEventAction;

        EventId(MotionEvent event) {
            mEventTimeMs = event.getEventTime();
            mEventAction = event.getActionMasked();
        }

        public boolean match(MotionEvent other) {
            return other != null
                    && mEventTimeMs == other.getEventTime()
                    && mEventAction == other.getActionMasked();
        }

        public static boolean matches(EventId event, MotionEvent other) {
            return event != null && event.match(other);
        }

        /** Returns the {@link MotionEvent#getEventTime()} of the event. */
        public long getEventTimeMs() {
            return mEventTimeMs;
        }

        /** Returns the {@link MotionEvent#getAction()} code for the event. */
        public int getEventAction() {
            return mEventAction;
        }
    }

    public static final int NO_AXIS = -1;

    /** A recognized user gesture. */
    public enum Gesture {
        TOUCH, // First known event, usually an ACTION_DOWN.
        FIRST_TAP, // A first tap, to be confirmed as a SINGLE_TAP, or overridden by anything else.
        SINGLE_TAP, // Confirms a FIRST_TAP into an actual tap.
        DOUBLE_TAP,
        LONG_PRESS,
        DRAG, // A move gesture that is not aligned on one axis
        DRAG_X,
        DRAG_Y,
        FLING,
        ZOOM;

        /** Whether this Gesture is a better guess than other in case of doubt between both. */
        boolean supersedes(@Nullable Gesture other) {
            if (other == this) {
                return false;
            }
            if (other == null || other == TOUCH) {
                // Every Gesture is finer than nothing or a TOUCH.
                return true;
            }
            if (other == FIRST_TAP) {
                // TAP is overridden by any other Gesture except TOUCH.
                return this != TOUCH;
            }
            if (other == DOUBLE_TAP) {
                // A Double tap is overridden by any drag while on the second tap.
                return this == DRAG || this == DRAG_X || this == DRAG_Y;
            }
            switch (this) {
                case FLING:
                case ZOOM:
                    return true;
                default:
                    return other == LONG_PRESS;
            }
        }
    }
    private final int mMoveSlop;

    private final ScaleGestureDetector mZoomDetector;
    private final GestureDetector mMoveDetector;
    private final GestureDetector mDoubleTapDetector;

    @Nullable
    private GestureHandler mDelegate;

    private final StringBuilder mLog = new StringBuilder();

    /**
     * Whether we are currently tracking a gesture in progress, i.e. between the initial ACTION_DOWN
     * and the end of the gesture.
     */
    private boolean mTracking = false;

    private boolean mInterrupted = false;
    private boolean mHandling = false;

    private final PointF mTouchDown = new PointF();

    @Nullable
    private EventId mLastEvent;

    @Nullable
    private Gesture mDetectedGesture;

    private final QuickScaleBypassDecider mQuickScaleBypassDecider;

    public GestureTracker(@NonNull Context context) {
        ViewConfiguration config = ViewConfiguration.get(context);
        mMoveSlop = config.getScaledTouchSlop();
        DetectorListener listener = new DetectorListener();
        mMoveDetector = new GestureDetector(context, listener);
        mZoomDetector = new ScaleGestureDetector(context, listener);

        // Detection of double tap on the main detector messes up with everything else, so divert it
        // on a secondary detector:
        mMoveDetector.setOnDoubleTapListener(null);
        mDoubleTapDetector = new GestureDetector(context, new SimpleOnGestureListener());
        mDoubleTapDetector.setOnDoubleTapListener(listener);
        mQuickScaleBypassDecider = new QuickScaleBypassDecider();
    }

    /**
     * Set a delegate {@link GestureHandler} that will receive all relevant event-handling
     * callbacks.
     */
    public void setDelegateHandler(@NonNull GestureHandler handler) {
        mDelegate = handler;
    }

    /**
     * Feed an event into this tracker. To be plugged in a View's onIntercept and onTouchEvent.
     *
     * @param event  The event.
     * @param handle Should this event be handled, i.e. forwarded to any delegate.
     * @return true if the event was recorded, false if it was discarded (as a duplicate).
     */
    public boolean feed(@NonNull MotionEvent event, boolean handle) {
        // If the tracking of the previous gesture was interrupted, reset tracking now.
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && mTracking && mInterrupted) {
            initTracking(event.getX(), event.getY());
        }

        if (EventId.matches(mLastEvent, event) && handle == mHandling) {
            // We have already processed this event in this way (handling or non-handling).
            return false;
        }

        if (!mTracking) {
            initTracking(event.getX(), event.getY());
        }

        if (mTracking
                && event.getActionMasked() == MotionEvent.ACTION_DOWN
                && mDetectedGesture == Gesture.DOUBLE_TAP
                && !mHandling
                && handle) {
            // If this DOWN event was just intercepted as a DOUBLE_TAP and we are receiving it a
            // second
            // time (first on intercept, second onTouch), then we need to ignore it, but still
            // process
            // that we're now handling this gesture.
            mHandling = handle;
        } else {
            // Call onGestureStart as soon as we start handling a gesture - even if we
            // missed the ACTION_DOWN part of the gesture.
            if (mDelegate != null && handle && !mHandling) {
                mDelegate.onGestureStart();
            }
            mHandling = handle;

            mLog.append(getEventTag(event));
            mMoveDetector.onTouchEvent(event);

            if (!mQuickScaleBypassDecider.shouldSkipZoomDetector(event, mLastEvent)) {
                mZoomDetector.onTouchEvent(event);
            }
            mDoubleTapDetector.onTouchEvent(event);
            // Note: This does not reliably detect double-taps: in an ImageView nested in a
            // ZoomView, the ImageView's doubleTapDetector will fail to count the second TAP as a
            // Double tap, apparently because the line: `boolean hadTapMessage = mHandler
            // .hasMessages(TAP);` returns false. Where's the TAP message? Nobody knows. We could
            // detect double-taps by  hecking for FIRST_TAP and ACTION_DOWN (close to touchDown)
            // here and get rid of the doubleTapDetector.
        }

        // Support for mouse-wheel scroll events.
        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
            mDelegate.onScroll(
                    event,
                    event,
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL) * MOUSE_SCROLL_MULTIPLIER,
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                            * MOUSE_SCROLL_MULTIPLIER
                            * VERTICAL_MOUSE_SCROLL_DIRECTION_ADJUSTMENT);
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mDetectedGesture == Gesture.DOUBLE_TAP && mHandling && mDelegate != null) {
                // Delayed from detection which happens too early.
                mDelegate.onDoubleTap(event);
            }
            if (mDetectedGesture != Gesture.FIRST_TAP) {
                // All gestures but FIRST_TAP are final, should end gesture here.
                endGesture();
            } else {
                // TAP will be followed by SINGLE_TAP if left on its own.
                mLog.append('+');
            }
        }

        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            endGesture();
        }

        mLastEvent = new EventId(event);
        return true;
    }

    /**
     * Forcefully handle a double-tap gesture.
     *
     * <p>Double-tap gestures can't be properly intercepted, because any nested view will
     * incorrectly process them as single-taps (there's no way to tell them to stop processing the
     * pending single tap, e.g. by sending a cancel event). Thus it's best for a container to not
     * actually intercept them, but it needs a way to force handling of it, which is done by
     * calling this method. It can be called on any event, and must be called on the ACTION_UP one,
     * as it will actually handle the gesture on that one.
     *
     * @param ev The intercepted event. Must be called at least on the ACTION_UP event.
     */
    public void handleDoubleTap(@NonNull MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mDetectedGesture == Gesture.DOUBLE_TAP && mDelegate != null) {
                // tracking might be false, if this happens after the regular endGesture() has
                // been called.
                mDelegate.onDoubleTap(ev);
                endGesture();
            }
        }
    }

    private void endGesture() {
        mTracking = false;
        mLog.append('/');
        if (mHandling && mDelegate != null) {
            mDelegate.onGestureEnd(mDetectedGesture);
        }
        mHandling = false;
    }

    /** Signals the stream of events is currently interrupted (or resumed). */
    public void interrupt(boolean interrupt) {
        mLog.append(interrupt ? ']' : '[');
        mInterrupted = interrupt;
        if (interrupt) {
            mHandling = false;
        }
    }

    /**
     * Returns whether the currently detected gesture matches any of the one(s) passed as argument.
     */
    public boolean matches(@NonNull Gesture... gestures) {
        for (Gesture g : gestures) {
            if (mDetectedGesture == g) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether this gesture in now finished, i.e. a final event (UP or CANCEL) has been
     * received.
     */
    public boolean isFinished() {
        return !mTracking;
    }

    /** Get a textual representation of the gesture's stream of events so far. */
    @NonNull
    public String getLog() {
        return mDetectedGesture == null ? mLog.toString() : String.format("%s: %s",
                mDetectedGesture,
                mLog);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return feed(event, true);
    }

    private float getDistance(MotionEvent event, int axis) {
        switch (axis) {
            case MotionEvent.AXIS_X:
                return Math.abs(event.getX() - mTouchDown.x);
            case MotionEvent.AXIS_Y:
                return Math.abs(event.getY() - mTouchDown.y);
            case NO_AXIS:
                float x = event.getX() - mTouchDown.x;
                float y = event.getY() - mTouchDown.y;
                return (float) Math.sqrt(x * x + y * y);
            default:
                throw new IllegalArgumentException(String.format("Wrong axis value %d", axis));
        }
    }

    private void detected(Gesture gesture) {
        if (mInterrupted) {
            return;
        }
        if (gesture.supersedes(mDetectedGesture)) {
            mQuickScaleBypassDecider.setLastGesture(mDetectedGesture);
            mDetectedGesture = gesture;
        }
    }

    private static char getEventTag(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_CANCEL:
                return 'C';
            case MotionEvent.ACTION_DOWN:
                return 'D';
            case MotionEvent.ACTION_MOVE:
                return 'M';
            case MotionEvent.ACTION_POINTER_DOWN:
                return 'P';
            case MotionEvent.ACTION_POINTER_UP:
                return 'Q';
            case MotionEvent.ACTION_UP:
                return 'U';
            case MotionEvent.ACTION_SCROLL:
                return 'S';
            case MotionEvent.ACTION_HOVER_ENTER:
                return 'e';
            case MotionEvent.ACTION_HOVER_EXIT:
                return 'x';
            case MotionEvent.ACTION_HOVER_MOVE:
                return 'm';
            default:
                return '.';
        }
    }

    private void initTracking(float x, float y) {
        mTracking = true;
        mInterrupted = false;
        mLog.setLength(0);
        mTouchDown.set(x, y);
        mDetectedGesture = Gesture.TOUCH;
    }

    /** A recipient for all gesture handling. */
    public static class GestureHandler extends SimpleOnGestureListener
            implements OnScaleGestureListener {

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        }

        /** Called at the start of any gesture, before any other callback. */
        protected void onGestureStart() {
        }

        /**
         * Called at the end of any gesture, after any other callback.
         *
         * @param gesture The detected gesture that just ended.
         */
        protected void onGestureEnd(@NonNull Gesture gesture) {
        }
    }

    /** The listener used for detecting various gestures. */
    private class DetectorListener extends SimpleOnGestureListener implements
            OnScaleGestureListener {

        @Override
        public void onShowPress(MotionEvent e) {
            if (mHandling && mDelegate != null) {
                mDelegate.onShowPress(e);
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            detected(Gesture.FIRST_TAP);
            if (mHandling && mDelegate != null) {
                mDelegate.onSingleTapUp(e);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            detected(Gesture.SINGLE_TAP);
            if (mHandling && mDelegate != null) {
                mDelegate.onSingleTapConfirmed(e);
            }
            // This comes from a delayed call from the doubleTapDetector, not an event
            endGesture();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Double-taps are only valid if the first gesture was just a FIRST_TAP, nothing else.
            if (mDetectedGesture == Gesture.FIRST_TAP) {
                detected(Gesture.DOUBLE_TAP);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float dx = getDistance(e2, MotionEvent.AXIS_X);
            float dy = getDistance(e2, MotionEvent.AXIS_Y);
            if (dx > mMoveSlop && dx > DRAG_X_MULTIPLIER * dy) {
                detected(Gesture.DRAG_X);
            } else if (dy > mMoveSlop && dy > DRAG_Y_MULTIPLIER * dx) {
                detected(Gesture.DRAG_Y);
            } else if (getDistance(e2, NO_AXIS) > mMoveSlop) {
                detected(Gesture.DRAG);
            }
            if (mHandling && mDelegate != null) {
                mDelegate.onScroll(e1, e2, distanceX, distanceY);
            }
            return false;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            detected(Gesture.LONG_PRESS);
            if (mHandling && mDelegate != null) {
                mDelegate.onLongPress(e);
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX,
                float velocityY) {
            detected(Gesture.FLING);
            if (mHandling && mDelegate != null) {
                mDelegate.onFling(e1, e2, velocityX, velocityY);
            }
            return false;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            if (mHandling && mDelegate != null) {
                mDelegate.onScale(detector);
            }
            // Return true is required to keep the gesture detector happy (and the events flowing).
            return true;
        }

        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            detected(Gesture.ZOOM);
            if (mHandling && mDelegate != null) {
                mDelegate.onScaleBegin(detector);
            }
            // Return true is required to keep the gesture detector happy (and the events flowing).
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            if (mHandling && mDelegate != null) {
                mDelegate.onScaleEnd(detector);
            }
        }
    }
}
