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

package android.support.v13.view;


import android.graphics.Point;
import android.support.v4.view.InputDeviceCompat;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;

/**
 * DragStartHelper is a utility class for implementing drag and drop support.
 * <p>
 * It detects gestures commonly used to start drag (long click for any input source,
 * click and drag for mouse).
 * <p>
 * It also keeps track of the screen location where the drag started, and helps determining
 * the hot spot position for a drag shadow.
 * <p>
 * Implement {@link DragStartHelper.OnDragStartListener} to start the drag operation:
 * <pre>
 * DragStartHelper.OnDragStartListener listener = new DragStartHelper.OnDragStartListener {
 *     protected void onDragStart(View view, DragStartHelper helper) {
 *         View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view) {
 *             public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
 *                 super.onProvideShadowMetrics(shadowSize, shadowTouchPoint);
 *                 helper.getTouchPosition(shadowTouchPoint);
 *             }
 *         };
 *         view.startDrag(mClipData, shadowBuilder, mLocalState, mDragFlags);
 *     }
 * };
 * mDragStartHelper = new DragStartHelper(mDraggableView, listener);
 * </pre>
 * Once created, DragStartHelper can be attached to a view (this will replace existing long click
 * and touch listeners):
 * <pre>
 * mDragStartHelper.attach();
 * </pre>
 * It may also be used in combination with existing listeners:
 * <pre>
 * public boolean onTouch(View view, MotionEvent event) {
 *     if (mDragStartHelper.onTouch(view, event)) {
 *         return true;
 *     }
 *     return handleTouchEvent(view, event);
 * }
 * public boolean onLongClick(View view) {
 *     if (mDragStartHelper.onLongClick(view)) {
 *         return true;
 *     }
 *     return handleLongClickEvent(view);
 * }
 * </pre>
 */
public class DragStartHelper {
    final private View mView;
    final private OnDragStartListener mListener;

    private int mLastTouchX, mLastTouchY;
    private boolean mDragging;

    /**
     * Interface definition for a callback to be invoked when a drag start gesture is detected.
     */
    public interface OnDragStartListener {
        /**
         * Called when a drag start gesture has been detected.
         *
         * @param v The view over which the drag start gesture has been detected.
         * @param helper The DragStartHelper object which detected the gesture.
         * @return True if the listener has started the drag operation, false otherwise.
         */
        boolean onDragStart(View v, DragStartHelper helper);
    }

    /**
     * Create a DragStartHelper associated with the specified view.
     * The newly created helper is not initially attached to the view, {@link #attach} must be
     * called explicitly.
     * @param view A View
     */
    public DragStartHelper(View view, OnDragStartListener listener) {
        mView = view;
        mListener = listener;
    }

    /**
     * Attach the helper to the view.
     * <p>
     * This will replace previously existing touch and long click listeners.
     */
    public void attach() {
        mView.setOnLongClickListener(mLongClickListener);
        mView.setOnTouchListener(mTouchListener);
    }

    /**
     * Detach the helper from the view.
     * <p>
     * This will reset touch and long click listeners to {@code null}.
     */
    public void detach() {
        mView.setOnLongClickListener(null);
        mView.setOnTouchListener(null);
    }

    /**
     * Handle a touch event.
     * @param v The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *        the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    public boolean onTouch(View v, MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = x;
                mLastTouchY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!MotionEventCompat.isFromSource(event, InputDeviceCompat.SOURCE_MOUSE)
                        || (event.getButtonState()
                                & MotionEvent.BUTTON_PRIMARY) == 0) {
                    break;
                }
                if (mDragging) {
                    // Ignore ACTION_MOVE events once the drag operation is in progress.
                    break;
                }
                if (mLastTouchX == x && mLastTouchY == y) {
                    // Do not call the listener unless the pointer position has actually changed.
                    break;
                }
                mLastTouchX = x;
                mLastTouchY = y;
                mDragging = mListener.onDragStart(v, this);
                return mDragging;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                break;
        }
        return false;
    }

    /**
     * Handle a long click event.
     * @param v The view that was clicked and held.
     * @return true if the callback consumed the long click, false otherwise.
     */
    public boolean onLongClick(View v) {
        return mListener.onDragStart(v, this);
    }

    /**
     * Compute the position of the touch event that started the drag operation.
     * @param point The position of the touch event that started the drag operation.
     */
    public void getTouchPosition(Point point) {
        point.set(mLastTouchX, mLastTouchY);
    }

    private final View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return DragStartHelper.this.onLongClick(v);
        }
    };

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return DragStartHelper.this.onTouch(v, event);
        }
    };
}

