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
 * the hot spot position for drag shadow.
 * <p>
 * Implement {@link #onDragStart(View)} method to start the drag operation:
 * <pre>
 * mDragStartHelper = new DragStartHelper(mDraggableView) {
 *     protected void onDragStart(View v) {
 *         v.startDrag(mClipData, getShadowBuilder(view), mLocalState, mDragFlags);
 *     }
 * };
 * </pre>
 * Once created, DragStartHelper can be attached to a view (this will replace existing long click
 * and touch listeners):
 * <pre>
 * mDragStartHelper.attach();
 * </pre>
 * It may also be used in combination with existing listeners:
 * <pre>
 * public boolean onTouch(View view, MotionEvent event) {
 *     return mDragStartHelper.handleTouch(view, event) || handleTouchEvent(view, event);
 * }
 * public boolean onLongClick(View view) {
 *     return mDragStartHelper.handleLongClick(view) || handleLongClickEvent(view);
 * }
 * </pre>
 */
public abstract class DragStartHelper implements View.OnLongClickListener, View.OnTouchListener {
    final private View mView;
    private float mLastTouchRawX, mLastTouchRawY;

    /**
     * Create a DragStartHelper associated with the specified view.
     * The newly created helper is not initally attached to the view, {@link #attach} must be
     * called explicitly.
     * @param view A View
     */
    public DragStartHelper(View view) {
        mView = view;
    }

    /**
     * Attach the helper to the view.
     * <p>
     * This will replace previously existing touch and long click listeners.
     */
    public void attach() {
        mView.setOnLongClickListener(this);
        mView.setOnTouchListener(this);
    }

    /**
     * Detach the helper from the view.
     * <p>
     * This will reset touch and long click listeners to {@code null}.
     */
    public void remove() {
        mView.setOnLongClickListener(null);
        mView.setOnTouchListener(null);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return handleTouch(v, event);
    }

    @Override
    public boolean onLongClick(View v) {
        return handleLongClick(v);
    }

    /**
     * Handle a touch event.
     * @param v The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *        the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    public boolean handleTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {
            mLastTouchRawX = event.getRawX();
            mLastTouchRawY = event.getRawY();
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE &&
                MotionEventCompat.isFromSource(event, InputDeviceCompat.SOURCE_MOUSE) &&
                (MotionEventCompat.getButtonState(event) & MotionEventCompat.BUTTON_PRIMARY) != 0) {
            onDragStart(v);
            return true;
        }
        return false;
    }

    /**
     * Handle a long click event.
     * @param v The view that was clicked and held.
     * @return true if the callback consumed the long click, false otherwise.
     */
    public boolean handleLongClick(View v) {
        onDragStart(v);
        return true;
    }

    /**
     * Compute the position of the touch event that started the drag operation.
     * @param v The view relative to which the position should be computed.
     * @param point The position of the touch event that started the drag operation.
     */
    public void getTouchPosition(View v, Point point) {
        int [] viewLocation = new int[2];
        v.getLocationOnScreen(viewLocation);
        point.set((int) mLastTouchRawX - viewLocation[0], (int) mLastTouchRawY - viewLocation[1]);
    }

    /**
     * Create a {@link View.DragShadowBuilder} which will build a drag shadow with the same
     * appearance and dimensions as the specified view and with the hot spot at the location of
     * the touch event that started the drag operation.
     * @param view A view
     * @return  {@link View.DragShadowBuilder}
     */
    public View.DragShadowBuilder getShadowBuilder(View view) {
        return new ShadowBuilder(view);
    }

    /**
     * A utility class that builds a drag shadow with the same appearance and dimensions as the
     * specified view and with the hot spot at the location of the touch event that started the
     * drag operation.
     * <p>
     * At the start of the drag operation a drag shadow built this way will be positioned directly
     * over the specified view.
     */
    public class ShadowBuilder extends View.DragShadowBuilder {
        /**
         * Constructs a shadow image builder based on a View. By default, the resulting drag
         * shadow will have the same appearance and dimensions as the View, with the touch point
         * at the location of the touch event that started the drag operation.
         * @param view A view.
         */
        public ShadowBuilder(View view) {
            super(view);
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            super.onProvideShadowMetrics(shadowSize, shadowTouchPoint);
            getTouchPosition(getView(), shadowTouchPoint);
        }
    }

    /**
     * Called when the drag start gesture has been detected.
     * @param v A view on which the drag start gesture has been detected.
     */
    protected abstract void onDragStart(View v);
}

