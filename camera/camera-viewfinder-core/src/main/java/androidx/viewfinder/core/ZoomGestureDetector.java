/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.viewfinder.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Detects scaling transformation gestures that interprets zooming events using the supplied
 * {@link MotionEvent}s.
 *
 * <p>The {@link OnZoomGestureListener} callback will notify users when a particular
 * gesture event has occurred.
 *
 * <p>This class should only be used with {@link MotionEvent}s reported via touch.
 *
 * <p>To use this class:
 * <ul>
 *  <li>Create an instance of the {@code ZoomGestureDetector} for your
 *      {@link View}
 *  <li>In the {@link View#onTouchEvent(MotionEvent)} method ensure you call
 *          {@link #onTouchEvent(MotionEvent)}. The methods defined in your
 *          callback will be executed when the events occur.
 * </ul>
 */
// TODO(b/314701735): update the documentation with examples using camera classes.
// TODO(b/314701401): convert to kotlin implementation.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ZoomGestureDetector {
    private static final String TAG = "ZoomGestureDetector";
    // The default minimum span that the detector interprets a zooming event with. It's set to 0
    // to give the most responsiveness.
    // TODO(b/314702145): define a different span if appropriate.
    private static final int DEFAULT_MIN_SPAN = 0;

    /**
     * The listener for receiving notifications when gestures occur.
     * If you want to listen for all the different gestures then implement
     * this interface.
     *
     * <p>An application will receive events in the following order:
     * <ul>
     *  <li>One {@link OnZoomGestureListener#onZoomBegin(ZoomGestureDetector)}
     *  <li>Zero or more {@link OnZoomGestureListener#onZoom(ZoomGestureDetector)}
     *  <li>One {@link OnZoomGestureListener#onZoomEnd(ZoomGestureDetector)}
     * </ul>
     */
    public interface OnZoomGestureListener {
        /**
         * Responds to zooming events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *          retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         *          as handled. If an event was not handled, the detector
         *          will continue to accumulate movement until an event is
         *          handled. This can be useful if an application, for example,
         *          only wants to update scaling factors if the change is
         *          greater than 0.01.
         */
        default boolean onZoom(@NonNull ZoomGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a zooming gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *          retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         *          this gesture. For example, if a gesture is beginning
         *          with a focal point outside of a region where it makes
         *          sense, onZoomBegin() may return false to ignore the
         *          rest of the gesture.
         */
        default boolean onZoomBegin(@NonNull ZoomGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a zoom gesture. Reported by existing
         * pointers going up.
         *
         * <p>Once a zoom has ended, {@link ZoomGestureDetector#getFocusX()}
         * and {@link ZoomGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *          retrieve extended info about event state.
         */
        default void onZoomEnd(@NonNull ZoomGestureDetector detector) {
            // Intentionally empty
        }
    }

    private final Context mContext;
    private final OnZoomGestureListener mListener;

    private float mFocusX;
    private float mFocusY;

    private boolean mQuickZoomEnabled;
    private boolean mStylusZoomEnabled;

    private float mCurrSpan;
    private float mPrevSpan;
    private float mInitialSpan;
    private float mCurrSpanX;
    private float mCurrSpanY;
    private float mPrevSpanX;
    private float mPrevSpanY;
    private long mCurrTime;
    private long mPrevTime;
    private boolean mInProgress;
    private int mSpanSlop;

    private int mMinSpan;

    private final Handler mHandler;

    private float mAnchoredZoomStartX;
    private float mAnchoredZoomStartY;
    private int mAnchoredZoomMode = ANCHORED_ZOOM_MODE_NONE;

    private static final float SCALE_FACTOR = .5f;
    private static final int ANCHORED_ZOOM_MODE_NONE = 0;
    private static final int ANCHORED_ZOOM_MODE_DOUBLE_TAP = 1;
    private static final int ANCHORED_ZOOM_MODE_STYLUS = 2;
    private GestureDetector mGestureDetector;

    private boolean mEventBeforeOrAboveStartingGestureEvent;

    /**
     * Creates a ZoomGestureDetector with the supplied listener.
     * You may only use this constructor from a {@link android.os.Looper Looper} thread.
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    public ZoomGestureDetector(@NonNull Context context,
            @NonNull OnZoomGestureListener listener) {
        this(context, null, listener);
    }

    /**
     * Creates a ZoomGestureDetector with the supplied listener.
     * @see android.os.Handler#Handler()
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     * @param handler the handler to use for running deferred listener events.
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    public ZoomGestureDetector(@NonNull Context context, @Nullable Handler handler,
            @NonNull OnZoomGestureListener listener) {
        this(context, ViewConfiguration.get(context).getScaledTouchSlop() * 2,
                DEFAULT_MIN_SPAN, handler, listener);
    }

    /**
     * Creates a ZoomGestureDetector with span slop and min span.
     *
     * @param context the application's context.
     * @param spanSlop the threshold for interpreting a touch movement as zooming.
     * @param minSpan the minimum threshold of zooming span. The span could be
     *                overridden by other usages to specify a different zooming span, for instance,
     *                if you need pinch gestures to continue closer together than the default.
     * @param listener the listener invoked for all the callbacks, this must not be null.
     * @param handler the handler to use for running deferred listener events.
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    @SuppressLint("ExecutorRegistration")
    public ZoomGestureDetector(@NonNull Context context, int spanSlop,
            int minSpan, @Nullable Handler handler,
            @NonNull OnZoomGestureListener listener) {
        mContext = context;
        mListener = listener;
        mSpanSlop = spanSlop;
        mMinSpan = minSpan;
        mHandler = handler;
        // Quick zoom is enabled by default after JB_MR2
        final int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        if (targetSdkVersion > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setQuickZoomEnabled(true);
        }
        // Stylus zoom is enabled by default after LOLLIPOP_MR1
        if (targetSdkVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            setStylusZoomEnabled(true);
        }
    }

    /**
     * Accepts MotionEvents and dispatches events to a {@link OnZoomGestureListener}
     * when appropriate.
     *
     * <p>Applications should pass a complete and consistent event stream to this method.
     * A complete and consistent event stream involves all MotionEvents from the initial
     * ACTION_DOWN to the final ACTION_UP or ACTION_CANCEL.</p>
     *
     * @param event The event to process
     * @return true if the event was processed and the detector wants to receive the
     *         rest of the MotionEvents in this event stream.
     */
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        mCurrTime = event.getEventTime();

        final int action = event.getActionMasked();

        // Forward the event to check for double tap gesture
        if (mQuickZoomEnabled) {
            mGestureDetector.onTouchEvent(event);
        }

        final int count = event.getPointerCount();
        final boolean isStylusButtonDown =
                (event.getButtonState() & MotionEvent.BUTTON_STYLUS_PRIMARY) != 0;

        final boolean anchoredZoomCancelled =
                mAnchoredZoomMode == ANCHORED_ZOOM_MODE_STYLUS && !isStylusButtonDown;
        final boolean streamComplete = action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL
                || anchoredZoomCancelled;

        if (action == MotionEvent.ACTION_DOWN || streamComplete) {
            // Reset any scale in progress with the listener.
            // If it's an ACTION_DOWN we're beginning a new event stream.
            // This means the app probably didn't give us all the events. Shame on it.
            if (mInProgress) {
                mListener.onZoomEnd(this);
                mInProgress = false;
                mInitialSpan = 0;
                mAnchoredZoomMode = ANCHORED_ZOOM_MODE_NONE;
            } else if (inAnchoredZoomMode() && streamComplete) {
                mInProgress = false;
                mInitialSpan = 0;
                mAnchoredZoomMode = ANCHORED_ZOOM_MODE_NONE;
            }

            if (streamComplete) {
                return true;
            }
        }

        if (!mInProgress && mStylusZoomEnabled && !inAnchoredZoomMode()
                && !streamComplete && isStylusButtonDown) {
            // Start of a button zoom gesture
            mAnchoredZoomStartX = event.getX();
            mAnchoredZoomStartY = event.getY();
            mAnchoredZoomMode = ANCHORED_ZOOM_MODE_STYLUS;
            mInitialSpan = 0;
        }

        final boolean configChanged = action == MotionEvent.ACTION_DOWN
                || action == MotionEvent.ACTION_POINTER_UP
                || action == MotionEvent.ACTION_POINTER_DOWN
                || anchoredZoomCancelled;

        final boolean pointerUp = action == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;

        // Determine focal point
        float sumX = 0, sumY = 0;
        final int div = pointerUp ? count - 1 : count;
        final float focusX;
        final float focusY;
        if (inAnchoredZoomMode()) {
            // In anchored scale mode, the focal pt is always where the double tap
            // or button down gesture started
            focusX = mAnchoredZoomStartX;
            focusY = mAnchoredZoomStartY;
            if (event.getY() < focusY) {
                mEventBeforeOrAboveStartingGestureEvent = true;
            } else {
                mEventBeforeOrAboveStartingGestureEvent = false;
            }
        } else {
            for (int i = 0; i < count; i++) {
                if (skipIndex == i) continue;
                sumX += event.getX(i);
                sumY += event.getY(i);
            }

            focusX = sumX / div;
            focusY = sumY / div;
        }

        // Determine average deviation from focal point
        float devSumX = 0, devSumY = 0;
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) continue;

            // Convert the resulting diameter into a radius.
            devSumX += Math.abs(event.getX(i) - focusX);
            devSumY += Math.abs(event.getY(i) - focusY);
        }
        final float devX = devSumX / div;
        final float devY = devSumY / div;

        // Span is the average distance between touch points through the focal point;
        // i.e. the diameter of the circle with a radius of the average deviation from
        // the focal point.
        final float spanX = devX * 2;
        final float spanY = devY * 2;
        final float span;
        if (inAnchoredZoomMode()) {
            span = spanY;
        } else {
            span = (float) Math.hypot(spanX, spanY);
        }

        // Dispatch begin/end events as needed.
        // If the configuration changes, notify the app to reset its current state by beginning
        // a fresh zoom event stream.
        final boolean wasInProgress = mInProgress;
        mFocusX = focusX;
        mFocusY = focusY;
        if (!inAnchoredZoomMode() && mInProgress && (span < mMinSpan || configChanged)) {
            mListener.onZoomEnd(this);
            mInProgress = false;
            mInitialSpan = span;
        }
        if (configChanged) {
            mPrevSpanX = mCurrSpanX = spanX;
            mPrevSpanY = mCurrSpanY = spanY;
            mInitialSpan = mPrevSpan = mCurrSpan = span;
        }

        final int minSpan = inAnchoredZoomMode() ? mSpanSlop : mMinSpan;
        if (!mInProgress && span >=  minSpan
                && (wasInProgress || Math.abs(span - mInitialSpan) > mSpanSlop)) {
            mPrevSpanX = mCurrSpanX = spanX;
            mPrevSpanY = mCurrSpanY = spanY;
            mPrevSpan = mCurrSpan = span;
            mPrevTime = mCurrTime;
            mInProgress = mListener.onZoomBegin(this);
        }

        // Handle motion; focal point and span/scale factor are changing.
        if (action == MotionEvent.ACTION_MOVE) {
            mCurrSpanX = spanX;
            mCurrSpanY = spanY;
            mCurrSpan = span;

            boolean updatePrev = true;

            if (mInProgress) {
                updatePrev = mListener.onZoom(this);
            }

            if (updatePrev) {
                mPrevSpanX = mCurrSpanX;
                mPrevSpanY = mCurrSpanY;
                mPrevSpan = mCurrSpan;
                mPrevTime = mCurrTime;
            }
        }

        return true;
    }

    private boolean inAnchoredZoomMode() {
        return mAnchoredZoomMode != ANCHORED_ZOOM_MODE_NONE;
    }

    /**
     * Set whether the associated {@link OnZoomGestureListener} should receive onZoom callbacks
     * when the user performs a doubleTap followed by a swipe.
     *
     * <p>If not set, this is enabled by default.
     *
     * @param enabled {@code true} to enable quick zooming, {@code false} to disable.
     */
    public void setQuickZoomEnabled(boolean enabled) {
        mQuickZoomEnabled = enabled;
        if (mQuickZoomEnabled && mGestureDetector == null) {
            GestureDetector.SimpleOnGestureListener gestureListener =
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            // Double tap: start watching for a swipe
                            mAnchoredZoomStartX = e.getX();
                            mAnchoredZoomStartY = e.getY();
                            mAnchoredZoomMode = ANCHORED_ZOOM_MODE_DOUBLE_TAP;
                            return true;
                        }
                    };
            mGestureDetector = new GestureDetector(mContext, gestureListener, mHandler);
        }
    }

    /**
     * Return whether the quick zoom gesture, in which the user performs a double tap followed by a
     * swipe, should perform zooming.
     *
     * @see #setQuickZoomEnabled(boolean)
     */
    public boolean isQuickZoomEnabled() {
        return mQuickZoomEnabled;
    }

    /**
     * Sets whether the associates {@link OnZoomGestureListener} should receive
     * onZoom callbacks when the user uses a stylus and presses the button.
     *
     * <p>If not set, this is enabled by default.
     *
     * @param enabled {@code true} to enable stylus zooming, {@code false} to disable.
     */
    public void setStylusZoomEnabled(boolean enabled) {
        mStylusZoomEnabled = enabled;
    }

    /**
     * Return whether the stylus zoom gesture, in which the user uses a stylus and presses the
     * button, should perform zooming. {@see #setStylusScaleEnabled(boolean)}
     */
    public boolean isStylusZoomEnabled() {
        return mStylusZoomEnabled;
    }

    /**
     * Returns {@code true} if a zoom gesture is in progress.
     */
    public boolean isInProgress() {
        return mInProgress;
    }

    /**
     * Get the X coordinate of the current gesture's focal point.
     * If a gesture is in progress, the focal point is between
     * each of the pointers forming the gesture.
     *
     * <p>If {@link #isInProgress()} would return false, the result of this
     * function is undefined.
     *
     * @return X coordinate of the focal point in pixels.
     */
    public float getFocusX() {
        return mFocusX;
    }

    /**
     * Get the Y coordinate of the current gesture's focal point.
     * If a gesture is in progress, the focal point is between
     * each of the pointers forming the gesture.
     *
     * <p>If {@link #isInProgress()} would return false, the result of this
     * function is undefined.
     *
     * @return Y coordinate of the focal point in pixels.
     */
    public float getFocusY() {
        return mFocusY;
    }

    /**
     * Return the average distance between each of the pointers forming the
     * gesture in progress through the focal point.
     *
     * @return Distance between pointers in pixels.
     */
    public float getCurrentSpan() {
        return mCurrSpan;
    }

    /**
     * Return the average X distance between each of the pointers forming the
     * gesture in progress through the focal point.
     *
     * @return Distance between pointers in pixels.
     */
    public float getCurrentSpanX() {
        return mCurrSpanX;
    }

    /**
     * Return the average Y distance between each of the pointers forming the
     * gesture in progress through the focal point.
     *
     * @return Distance between pointers in pixels.
     */
    public float getCurrentSpanY() {
        return mCurrSpanY;
    }

    /**
     * Return the previous average distance between each of the pointers forming the
     * gesture in progress through the focal point.
     *
     * @return Previous distance between pointers in pixels.
     */
    public float getPreviousSpan() {
        return mPrevSpan;
    }

    /**
     * Return the previous average X distance between each of the pointers forming the
     * gesture in progress through the focal point.
     *
     * @return Previous distance between pointers in pixels.
     */
    public float getPreviousSpanX() {
        return mPrevSpanX;
    }

    /**
     * Return the previous average Y distance between each of the pointers forming the
     * gesture in progress through the focal point.
     *
     * @return Previous distance between pointers in pixels.
     */
    public float getPreviousSpanY() {
        return mPrevSpanY;
    }

    /**
     * Return the scaling factor from the previous zoom event to the current
     * event. This value is defined as
     * ({@link #getCurrentSpan()} / {@link #getPreviousSpan()}).
     *
     * @return The current scaling factor.
     */
    public float getScaleFactor() {
        if (inAnchoredZoomMode()) {
            // Drag is moving up; the further away from the gesture
            // start, the smaller the span should be, the closer,
            // the larger the span, and therefore the larger the scale
            final boolean scaleUp =
                    (mEventBeforeOrAboveStartingGestureEvent
                            && (mCurrSpan < mPrevSpan))
                            || (!mEventBeforeOrAboveStartingGestureEvent
                            && (mCurrSpan > mPrevSpan));
            final float spanDiff = (Math.abs(1 - (mCurrSpan / mPrevSpan)) * SCALE_FACTOR);
            return mPrevSpan <= mSpanSlop ? 1 : scaleUp ? (1 + spanDiff) : (1 - spanDiff);
        }
        return mPrevSpan > 0 ? mCurrSpan / mPrevSpan : 1;
    }

    /**
     * Return the time difference in milliseconds between the previous
     * accepted zooming event and the current zooming event.
     *
     * @return Time difference since the last zooming event in milliseconds.
     */
    public long getTimeDelta() {
        return mCurrTime - mPrevTime;
    }

    /**
     * Return the event time of the current event being processed.
     *
     * @return Current event time in milliseconds.
     */
    public long getEventTime() {
        return mCurrTime;
    }
}
