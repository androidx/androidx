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

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

/**
 * GestureSelectionHelper provides logic that interprets a combination
 * of motions and gestures in order to provide gesture driven selection support
 * when used in conjunction with RecyclerView and other classes in the ReyclerView
 * selection support package.
 */
final class GestureSelectionHelper implements OnItemTouchListener {

    private static final String TAG = "GestureSelectionHelper";

    private final SelectionTracker<?> mSelectionMgr;
    private final AutoScroller mScroller;
    private final ViewDelegate mView;
    private final OperationMonitor mLock;

    private int mLastStartedItemPos = -1;
    private boolean mStarted = false;
    private Point mLastInterceptedPoint;

    /**
     * See {@link GestureSelectionHelper#create} for convenience
     * method.
     */
    GestureSelectionHelper(
            @NonNull SelectionTracker<?> selectionTracker,
            @NonNull ViewDelegate view,
            @NonNull AutoScroller scroller,
            @NonNull OperationMonitor lock) {

        checkArgument(selectionTracker != null);
        checkArgument(view != null);
        checkArgument(scroller != null);
        checkArgument(lock != null);

        mSelectionMgr = selectionTracker;
        mView = view;
        mScroller = scroller;
        mLock = lock;
    }

    /**
     * Explicitly kicks off a gesture multi-select.
     */
    void start() {
        checkState(!mStarted);
        // See: b/70518185. It appears start() is being called via onLongPress
        // even though we never received an intial handleInterceptedDownEvent
        // where we would usually initialize mLastStartedItemPos.
        if (mLastStartedItemPos < 0) {
            Log.w(TAG, "Illegal state. Can't start without valid mLastStartedItemPos.");
            return;
        }

        // Partner code in MotionInputHandler ensures items
        // are selected and range established prior to
        // start being called.
        // Verify the truth of that statement here
        // to make the implicit coupling less of a time bomb.
        checkState(mSelectionMgr.isRangeActive());

        mLock.checkStopped();

        mStarted = true;
        mLock.start();
    }

    @Override
    /** @hide */
    public boolean onInterceptTouchEvent(@NonNull RecyclerView unused, @NonNull MotionEvent e) {
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
    public void onTouchEvent(@NonNull RecyclerView unused, @NonNull MotionEvent e) {
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
    private boolean handleInterceptedDownEvent(@NonNull MotionEvent e) {
        mLastStartedItemPos = mView.getItemUnder(e);
        return mLastStartedItemPos != RecyclerView.NO_POSITION;
    }

    // Called when ACTION_UP event is to be handled.
    // Essentially, since this means all gesture movement is over, reset everything and apply
    // provisional selection.
    private void handleUpEvent(@NonNull MotionEvent e) {
        mSelectionMgr.mergeProvisionalSelection();
        endSelection();
        if (mLastStartedItemPos > -1) {
            mSelectionMgr.startRange(mLastStartedItemPos);
        }
    }

    // Called when ACTION_CANCEL event is to be handled.
    // This means this gesture selection is aborted, so reset everything and abandon provisional
    // selection.
    private void handleCancelEvent(@NonNull MotionEvent unused) {
        mSelectionMgr.clearProvisionalSelection();
        endSelection();
    }

    private void endSelection() {
        checkState(mStarted);

        mLastStartedItemPos = -1;
        mStarted = false;
        mScroller.reset();
        mLock.stop();
    }

    // Call when an intercepted ACTION_MOVE event is passed down.
    // At this point, we are sure user wants to gesture multi-select.
    private void handleMoveEvent(@NonNull MotionEvent e) {
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
            @NonNull SelectionTracker selectionMgr,
            @NonNull RecyclerView recyclerView,
            @NonNull AutoScroller scroller,
            @NonNull OperationMonitor lock) {

        return new GestureSelectionHelper(
                selectionMgr,
                new RecyclerViewDelegate(recyclerView),
                scroller,
                lock);
    }

    @VisibleForTesting
    abstract static class ViewDelegate {
        abstract int getHeight();

        abstract int getItemUnder(@NonNull MotionEvent e);

        abstract int getLastGlidedItemPosition(@NonNull MotionEvent e);
    }

    @VisibleForTesting
    static final class RecyclerViewDelegate extends ViewDelegate {

        private final RecyclerView mRecyclerView;

        RecyclerViewDelegate(@NonNull RecyclerView recyclerView) {
            checkArgument(recyclerView != null);
            mRecyclerView = recyclerView;
        }

        @Override
        int getHeight() {
            return mRecyclerView.getHeight();
        }

        @Override
        int getItemUnder(@NonNull MotionEvent e) {
            View child = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            return child != null
                    ? mRecyclerView.getChildAdapterPosition(child)
                    : RecyclerView.NO_POSITION;
        }

        @Override
        int getLastGlidedItemPosition(@NonNull MotionEvent e) {
            // If user has moved his pointer to the bottom-right empty pane (ie. to the right of the
            // last item of the recycler view), we would want to set that as the currentItemPos
            View lastItem = mRecyclerView.getLayoutManager()
                    .getChildAt(mRecyclerView.getLayoutManager().getChildCount() - 1);
            int direction = ViewCompat.getLayoutDirection(mRecyclerView);
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
            final float inboundY = getInboundY(mRecyclerView.getHeight(), e.getY());
            return (pastLastItem) ? mRecyclerView.getAdapter().getItemCount() - 1
                    : mRecyclerView.getChildAdapterPosition(
                            mRecyclerView.findChildViewUnder(e.getX(), inboundY));
        }

        /*
         * Check to see if MotionEvent if past a particular item, i.e. to the right or to the bottom
         * of the item.
         * For RTL, it would to be to the left or to the bottom of the item.
         */
        @VisibleForTesting
        static boolean isPastLastItem(
                int top, int left, int right, @NonNull MotionEvent e, int direction) {
            if (direction == View.LAYOUT_DIRECTION_LTR) {
                return e.getX() > right && e.getY() > top;
            } else {
                return e.getX() < left && e.getY() > top;
            }
        }
    }
}
