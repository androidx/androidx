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
import static androidx.recyclerview.selection.Shared.DEBUG;
import static androidx.recyclerview.selection.Shared.VERBOSE;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import org.jspecify.annotations.NonNull;

/**
 * GestureSelectionHelper provides logic that interprets a combination
 * of motions and gestures in order to provide gesture driven selection support
 * when used in conjunction with RecyclerView and other classes in the ReyclerView
 * selection support package.
 */
final class GestureSelectionHelper implements OnItemTouchListener, Resettable {

    private static final String TAG = "GestureSelectionHelper";

    private final SelectionTracker<?> mSelectionMgr;
    private final SelectionTracker.SelectionPredicate<?> mSelectionPredicate;
    private final AutoScroller mScroller;
    private final ViewDelegate mView;
    private final OperationMonitor mLock;

    private boolean mStarted = false;

    /**
     * See {@link GestureSelectionHelper#create} for convenience
     * method.
     */
    GestureSelectionHelper(
            @NonNull SelectionTracker<?> selectionTracker,
            @NonNull SelectionPredicate<?> selectionPredicate,
            @NonNull ViewDelegate view,
            @NonNull AutoScroller scroller,
            @NonNull OperationMonitor lock) {

        checkArgument(selectionTracker != null);
        checkArgument(selectionPredicate != null);
        checkArgument(view != null);
        checkArgument(scroller != null);
        checkArgument(lock != null);

        mSelectionMgr = selectionTracker;
        mSelectionPredicate = selectionPredicate;
        mView = view;
        mScroller = scroller;
        mLock = lock;
    }

    /**
     * Explicitly kicks off a gesture multi-select.
     */
    void start() {
        // Partner code in MotionInputHandler ensures items
        // are selected and range anchor initialized prior to
        // start being called.
        // Verify the truth of that statement here
        // to make the implicit coupling less of a time bomb.
        if (mStarted) {
            if (DEBUG) {
                Log.e(TAG, "Attempting to start, but state is already=started.");
                throw new IllegalStateException(
                        "Attempting to start, but state is already=started.");
            }
            return;
        }

        mStarted = true;
        mLock.start();
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView unused, @NonNull MotionEvent e) {
        // MotionEvents that aren't ACTION_DOWN are only ever passed to either onInterceptTouchEvent
        // or onTouchEvent; never to both, so events delivered to this method are effectively
        // lost if we don't act on them in this method.
        //
        // TODO(b/132447183): For some reason we're not receiving an ACTION_UP
        // event after a > long-press NOT followed by a ACTION_MOVE < event.
        if (mStarted) {
            onTouchEvent(unused, e);
        }

        // ACTION_CANCEL is associated with "TOOL_TYPE_UNKNOWN" and
        // is handled in ResetManager.
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                return mStarted;
            default:
                return false;
        }
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView unused, @NonNull MotionEvent e) {
        if (!mStarted) {
            if (VERBOSE) Log.i(TAG, "Ignoring input event. Not started.");
            return;
        }

        if (!mSelectionMgr.isRangeActive()) {
            Log.e(TAG,
                    "Internal state of GestureSelectionHelper out of sync w/ SelectionTracker "
                            + "(isRangeActive is false). Ignoring event and resetting state.");
            endSelection();
        }

        // ACTION_CANCEL is associated with "TOOL_TYPE_UNKNOWN" and
        // is handled in ResetManager.
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                handleMoveEvent(e);
                break;
            case MotionEvent.ACTION_UP:
                handleUpEvent();
                break;
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    // Called when ACTION_UP event is to be handled.
    // Essentially, since this means all gesture movement is over, reset everything and apply
    // provisional selection.
    private void handleUpEvent() {
        mSelectionMgr.mergeProvisionalSelection();
        endSelection();
    }

    /**
     * Immediately "Stops" active gesture selection, and resets all related state.
     */
    @Override
    public void reset() {
        if (DEBUG) Log.d(TAG, "Received reset request.");
        mStarted = false;
        mScroller.reset();
    }

    @Override
    public boolean isResetRequired() {
        return mStarted;
    }

    private void endSelection() {
        mStarted = false;
        mScroller.reset();
        mLock.stop();
    }

    // Call when an intercepted ACTION_MOVE event is passed down.
    // At this point, we are sure user wants to gesture multi-select.
    private void handleMoveEvent(@NonNull MotionEvent e) {
        if (!mStarted) {
            Log.e(TAG, "Received event while not started.");
            if (DEBUG) throw new IllegalStateException("Received event while not started.");
        }

        int lastGlidedItemPos = mView.getLastGlidedItemPosition(e);
        if (mSelectionPredicate.canSetStateAtPosition(lastGlidedItemPos, true)) {
            extendSelection(lastGlidedItemPos);
        }

        mScroller.scroll(MotionEvents.getOrigin(e));
    }

    // It's possible for events to go over the top/bottom of the RecyclerView.
    // We want to get a Y-coordinate within the RecyclerView so we can find the childView underneath
    // correctly.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static float getInboundY(float max, float y) {
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
            @NonNull SelectionTracker<?> selectionMgr,
            @NonNull SelectionPredicate<?> selectionPredicate,
            @NonNull RecyclerView recyclerView,
            @NonNull AutoScroller scroller,
            @NonNull OperationMonitor lock) {

        return new GestureSelectionHelper(
                selectionMgr,
                selectionPredicate,
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
            // If user has moved their pointer to the bottom-right empty pane (ie. to the
            // right of the last item of the recycler view), we would want to set that as
            // the currentItemPos
            View lastItem = mRecyclerView.getLayoutManager()
                    .getChildAt(mRecyclerView.getLayoutManager().getChildCount() - 1);
            int direction = mRecyclerView.getLayoutDirection();
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
            return pastLastItem ? mRecyclerView.getAdapter().getItemCount() - 1
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
