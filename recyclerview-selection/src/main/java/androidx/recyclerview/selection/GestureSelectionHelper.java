/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static android.support.v4.util.Preconditions.checkArgument;
import static android.support.v4.util.Preconditions.checkState;

import android.graphics.Point;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnItemTouchListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * GestureSelectionHelper provides logic that interprets a combination
 * of motions and gestures in order to provide gesture driven selection support
 * when used in conjunction with RecyclerView and other classes in the ReyclerView
 * selection support package.
 */
final class GestureSelectionHelper implements OnItemTouchListener {

    private static final String TAG = "GestureSelectionHelper";

    private final SelectionHelper<?> mSelectionMgr;
    private final AutoScroller mScroller;
    private final ViewDelegate mView;
    private final ContentLock mLock;

    private int mLastStartedItemPos = -1;
    private boolean mStarted = false;
    private Point mLastInterceptedPoint;

    /**
     * See {@link #create(SelectionHelper, RecyclerView, AutoScroller, ContentLock)} for convenience
     * method.
     */
    GestureSelectionHelper(
            SelectionHelper<?> selectionHelper,
            ViewDelegate view,
            AutoScroller scroller,
            ContentLock lock) {

        checkArgument(selectionHelper != null);
        checkArgument(view != null);
        checkArgument(scroller != null);
        checkArgument(lock != null);

        mSelectionMgr = selectionHelper;
        mView = view;
        mScroller = scroller;
        mLock = lock;
    }

    /**
     * Explicitly kicks off a gesture multi-select.
     */
    void start() {
        checkState(!mStarted);
        checkState(mLastStartedItemPos > -1);

        // Partner code in MotionInputHandler ensures items
        // are selected and range established prior to
        // start being called.
        // Verify the truth of that statement here
        // to make the implicit coupling less of a time bomb.
        checkState(mSelectionMgr.isRangeActive());

        mLock.checkUnlocked();

        mStarted = true;
        mLock.block();
    }

    @Override
    /** @hide */
    public boolean onInterceptTouchEvent(RecyclerView unused, MotionEvent e) {
        if (MotionEvents.isMouseEvent(e)) {
            if (Shared.DEBUG) Log.w(TAG, "Unexpected Mouse event. Check configuration.");
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // NOTE: Unlike events with other actions, RecyclerView eats
                // "DOWN" events. So even if we return true here we'll
                // never see an event w/ ACTION_DOWN passed to onTouchEvent.
                return handleInterceptedDownEvent(e);
            case MotionEvent.ACTION_MOVE:
                return mStarted;
        }

        return false;
    }

    @Override
    /** @hide */
    public void onTouchEvent(RecyclerView unused, MotionEvent e) {
        checkState(mStarted);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                handleMoveEvent(e);
                break;
            case MotionEvent.ACTION_UP:
                handleUpEvent(e);
                break;
            case MotionEvent.ACTION_CANCEL:
                handleCancelEvent(e);
                break;
        }
    }

    @Override
    /** @hide */
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    // Called when an ACTION_DOWN event is intercepted.
    // If down event happens on an item, we mark that item's position as last started.
    private boolean handleInterceptedDownEvent(MotionEvent e) {
        mLastStartedItemPos = mView.getItemUnder(e);
        return mLastStartedItemPos != RecyclerView.NO_POSITION;
    }

    // Called when ACTION_UP event is to be handled.
    // Essentially, since this means all gesture movement is over, reset everything and apply
    // provisional selection.
    private void handleUpEvent(MotionEvent e) {
        mSelectionMgr.mergeProvisionalSelection();
        endSelection();
        if (mLastStartedItemPos > -1) {
            mSelectionMgr.startRange(mLastStartedItemPos);
        }
    }

    // Called when ACTION_CANCEL event is to be handled.
    // This means this gesture selection is aborted, so reset everything and abandon provisional
    // selection.
    private void handleCancelEvent(MotionEvent unused) {
        mSelectionMgr.clearProvisionalSelection();
        endSelection();
    }

    private void endSelection() {
        checkState(mStarted);

        mLastStartedItemPos = -1;
        mStarted = false;
        mScroller.reset();
        mLock.unblock();
    }

    // Call when an intercepted ACTION_MOVE event is passed down.
    // At this point, we are sure user wants to gesture multi-select.
    private void handleMoveEvent(MotionEvent e) {
        mLastInterceptedPoint = MotionEvents.getOrigin(e);

        int lastGlidedItemPos = mView.getLastGlidedItemPosition(e);
        if (lastGlidedItemPos != RecyclerView.NO_POSITION) {
            extendSelection(lastGlidedItemPos);
        }

        mScroller.scroll(mLastInterceptedPoint);
    }

    // It's possible for events to go over the top/bottom of the RecyclerView.
    // We want to get a Y-coordinate within the RecyclerView so we can find the childView underneath
    // correctly.
    private static float getInboundY(float max, float y) {
        if (y < 0f) {
            return 0f;
        } else if (y > max) {
            return max;
        }
        return y;
    }

    /* Given the end position, select everything in-between.
     * @param endPos  The adapter position of the end item.
     */
    private void extendSelection(int endPos) {
        mSelectionMgr.extendProvisionalRange(endPos);
    }

    /**
     * Returns a new instance of GestureSelectionHelper.
     */
    static GestureSelectionHelper create(
            SelectionHelper selectionMgr,
            RecyclerView recView,
            AutoScroller scroller,
            ContentLock lock) {

        return new GestureSelectionHelper(
                selectionMgr,
                new RecyclerViewDelegate(recView),
                scroller,
                lock);
    }

    @VisibleForTesting
    abstract static class ViewDelegate {
        abstract int getHeight();

        abstract int getItemUnder(MotionEvent e);

        abstract int getLastGlidedItemPosition(MotionEvent e);
    }

    @VisibleForTesting
    static final class RecyclerViewDelegate extends ViewDelegate {

        private final RecyclerView mRecView;

        RecyclerViewDelegate(RecyclerView view) {
            checkArgument(view != null);
            mRecView = view;
        }

        @Override
        int getHeight() {
            return mRecView.getHeight();
        }

        @Override
        int getItemUnder(MotionEvent e) {
            View child = mRecView.findChildViewUnder(e.getX(), e.getY());
            return child != null
                    ? mRecView.getChildAdapterPosition(child)
                    : RecyclerView.NO_POSITION;
        }

        @Override
        int getLastGlidedItemPosition(MotionEvent e) {
            // If user has moved his pointer to the bottom-right empty pane (ie. to the right of the
            // last item of the recycler view), we would want to set that as the currentItemPos
            View lastItem = mRecView.getLayoutManager()
                    .getChildAt(mRecView.getLayoutManager().getChildCount() - 1);
            int direction = ViewCompat.getLayoutDirection(mRecView);
            final boolean pastLastItem = isPastLastItem(lastItem.getTop(),
                    lastItem.getLeft(),
                    lastItem.getRight(),
                    e,
                    direction);

            // Since views get attached & detached from RecyclerView,
            // {@link LayoutManager#getChildCount} can return a different number from the actual
            // number
            // of items in the adapter. Using the adapter is the for sure way to get the actual last
            // item position.
            final float inboundY = getInboundY(mRecView.getHeight(), e.getY());
            return (pastLastItem) ? mRecView.getAdapter().getItemCount() - 1
                    : mRecView.getChildAdapterPosition(
                            mRecView.findChildViewUnder(e.getX(), inboundY));
        }

        /*
         * Check to see if MotionEvent if past a particular item, i.e. to the right or to the bottom
         * of the item.
         * For RTL, it would to be to the left or to the bottom of the item.
         */
        @VisibleForTesting
        static boolean isPastLastItem(int top, int left, int right, MotionEvent e, int direction) {
            if (direction == View.LAYOUT_DIRECTION_LTR) {
                return e.getX() > right && e.getY() > top;
            } else {
                return e.getX() < left && e.getY() > top;
            }
        }
    }
}
