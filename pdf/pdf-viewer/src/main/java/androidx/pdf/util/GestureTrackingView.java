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
import android.util.AttributeSet;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.util.GestureTracker.Gesture;

/**
 * A {@link FrameLayout} plus helper methods to reliably share gestures with its hierarchy, by:
 *
 * <ul>
 *   <li>providing a {@link GestureTracker} that will detect what gesture is happening, regardless
 *       of where it is aimed at
 *   <li>handling generic gesture detection and passing it on in simple callbacks
 *   <li>forcing a priority order of handling an event bottom up (from the actual target View up to
 *       any containing View and finally the Activity), including when intercepts trigger.
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class GestureTrackingView extends FrameLayout {

    protected final GestureTracker mGestureTracker;

    public GestureTrackingView(@NonNull Context context) {
        super(context);
    }

    public GestureTrackingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GestureTrackingView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GestureTrackingView(@NonNull Context ctx, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(ctx, attrs, defStyleAttr, defStyleRes);
    }

    {
        mGestureTracker = new GestureTracker(getContext());
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // We need to override this because when a child wants to release an event (i.e. stop
        // holding on it, by using requestDisallowInterceptTouchEvent(false)), the next in line for
        // getting this event should be its closest parent, as opposed to the highest container
        // View, amongst the set of Views in its hierarchy that would like to intercept it.
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        mGestureTracker.interrupt(disallowIntercept);
        if (!disallowIntercept) {
            // Give ourself at least one chance to get the event next time.
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    public final boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mGestureTracker.feed(ev, false) && mGestureTracker.matches(Gesture.TOUCH)) {
            // Until further notice (i.e. releaseEvent()), we want to keep tracking the gesture.
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        boolean intercept = interceptGesture(mGestureTracker);
        if (intercept && mGestureTracker.matches(Gesture.DOUBLE_TAP)) {
            mGestureTracker.handleDoubleTap(ev);
            // Can't intercept this for real, as nested views need to know this is a double-tap,
            // not a single one.
            return false;
        } else {
            return intercept;
        }
    }

    @Override
    public final boolean onTouchEvent(MotionEvent event) {
        mGestureTracker.feed(event, true);

        // We are interested in receiving further events. This matters only for ACTION_DOWN.
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
            // Support for mouse-wheel scroll events.
            mGestureTracker.feed(event, true);
        }
        return true;
    }

    /**
     * Releases any captured gesture, so that container Views can get a chance to handle it. Once a
     * gesture is released, it's not coming back to this View.
     */
    protected void releaseGesture() {
        getParent().requestDisallowInterceptTouchEvent(false);
    }

    /**
     * Hook method called during {@link #onInterceptTouchEvent} in order to determine whether the
     * current gesture should be captured by this View.
     *
     * @param gestureTracker The {@link GestureTracker} with the current gesture.
     * @return True if this View should capture the gesture, false if it doesn't bother.
     */
    protected abstract boolean interceptGesture(@NonNull GestureTracker gestureTracker);

    @NonNull
    protected OnGestureListener patchGestureListener(@NonNull OnGestureListener original) {
        return new PatchedSimpleGestureHandler(original);
    }

    /**
     * A wrapping {@link OnGestureListener} that corrects the method {@link #onScroll} so that it's
     * not called with absurd values for distanceX and distanceY.
     */
    protected static class PatchedSimpleGestureHandler implements OnGestureListener {

        private final OnGestureListener mHandler;

        /** Initial {@link #onScroll} events can have inaccurate distance values */
        private boolean mDiscardFirstScroll = true;

        private PatchedSimpleGestureHandler(OnGestureListener listener) {
            mHandler = listener;
        }

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX,
                float distanceY) {
            if (mDiscardFirstScroll) {
                mDiscardFirstScroll = false;
                return true;
            }

            return mHandler.onScroll(e1, e2, distanceX, distanceY);
        }

        protected void onEndGesture() {
            mDiscardFirstScroll = true;
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return mHandler.onDown(e);
        }

        @Override
        public void onShowPress(@NonNull MotionEvent e) {
            mHandler.onShowPress(e);
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            return mHandler.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            mHandler.onLongPress(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX,
                float velocityY) {
            return mHandler.onFling(e1, e2, velocityX, velocityY);
        }
    }
}
