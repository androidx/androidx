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
import static androidx.recyclerview.selection.Shared.VERBOSE;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import java.util.Set;

/**
 * Provides mouse driven band-selection support when used in conjunction with a {@link RecyclerView}
 * instance. This class is responsible for rendering a band overlay and manipulating selection
 * status of the items it intersects with.
 *
 * <p>
 * Given the recycling nature of RecyclerView items that have scrolled off-screen would not
 * be selectable with a band that itself was partially rendered off-screen. To address this,
 * BandSelectionController builds a model of the list/grid information presented by RecyclerView as
 * the user interacts with items using their pointer (and the band). Selectable items that intersect
 * with the band, both on and off screen, are selected on pointer up.
 *
 * @see SelectionTracker.Builder#withPointerTooltypes(int...) for details on the specific
 *     tooltypes routed to this helper.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
class BandSelectionHelper<K> implements OnItemTouchListener {

    static final String TAG = "BandSelectionHelper";
    static final boolean DEBUG = false;

    private final BandHost mHost;
    private final ItemKeyProvider<K> mKeyProvider;
    private final SelectionTracker<K> mSelectionTracker;
    private final BandPredicate mBandPredicate;
    private final FocusDelegate<K> mFocusDelegate;
    private final OperationMonitor mLock;
    private final AutoScroller mScroller;
    private final GridModel.SelectionObserver mGridObserver;

    private @Nullable Point mCurrentPosition;
    private @Nullable Point mOrigin;
    private @Nullable GridModel mModel;

    /**
     * See {@link BandSelectionHelper#create}.
     */
    BandSelectionHelper(
            @NonNull BandHost host,
            @NonNull AutoScroller scroller,
            @NonNull ItemKeyProvider<K> keyProvider,
            @NonNull SelectionTracker<K> selectionTracker,
            @NonNull BandPredicate bandPredicate,
            @NonNull FocusDelegate<K> focusDelegate,
            @NonNull OperationMonitor lock) {

        checkArgument(host != null);
        checkArgument(scroller != null);
        checkArgument(keyProvider != null);
        checkArgument(selectionTracker != null);
        checkArgument(bandPredicate != null);
        checkArgument(focusDelegate != null);
        checkArgument(lock != null);

        mHost = host;
        mKeyProvider = keyProvider;
        mSelectionTracker = selectionTracker;
        mBandPredicate = bandPredicate;
        mFocusDelegate = focusDelegate;
        mLock = lock;

        mHost.addOnScrollListener(
                new OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        BandSelectionHelper.this.onScrolled(recyclerView, dx, dy);
                    }
                });

        mScroller = scroller;

        mGridObserver = new GridModel.SelectionObserver<K>() {
            @Override
            public void onSelectionChanged(Set<K> updatedSelection) {
                mSelectionTracker.setProvisionalSelection(updatedSelection);
            }
        };
    }

    /**
     * Creates a new instance.
     *
     * @return new BandSelectionHelper instance.
     */
    static <K> BandSelectionHelper create(
            @NonNull RecyclerView recyclerView,
            @NonNull AutoScroller scroller,
            @DrawableRes int bandOverlayId,
            @NonNull ItemKeyProvider<K> keyProvider,
            @NonNull SelectionTracker<K> selectionTracker,
            @NonNull SelectionPredicate<K> selectionPredicate,
            @NonNull BandPredicate bandPredicate,
            @NonNull FocusDelegate<K> focusDelegate,
            @NonNull OperationMonitor lock) {

        return new BandSelectionHelper<>(
                new DefaultBandHost<>(recyclerView, bandOverlayId, keyProvider, selectionPredicate),
                scroller,
                keyProvider,
                selectionTracker,
                bandPredicate,
                focusDelegate,
                lock);
    }

    @VisibleForTesting
    boolean isActive() {
        boolean active = mModel != null;
        if (DEBUG && active) {
            mLock.checkStarted();
        }
        return active;
    }

    /**
     * Clients must call reset when there are any material changes to the layout of items
     * in RecyclerView.
     */
    void reset() {
        if (!isActive()) {
            return;
        }

        mHost.hideBand();
        if (mModel != null) {
            mModel.stopCapturing();
            mModel.onDestroy();
        }

        mModel = null;
        mOrigin = null;

        mScroller.reset();
        mLock.stop();
    }

    @VisibleForTesting
    boolean shouldStart(@NonNull MotionEvent e) {
        // b/30146357 && b/23793622. onInterceptTouchEvent does not dispatch events to onTouchEvent
        // unless the event is != ACTION_DOWN. Thus, we need to actually start band selection when
        // mouse moves.
        return MotionEvents.isPrimaryMouseButtonPressed(e)
                && MotionEvents.isActionMove(e)
                && mBandPredicate.canInitiate(e)
                && !isActive();
    }

    @VisibleForTesting
    boolean shouldStop(@NonNull MotionEvent e) {
        return isActive()
                && (MotionEvents.isActionUp(e)
                || MotionEvents.isActionPointerUp(e)
                || MotionEvents.isActionCancel(e));
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView unused, @NonNull MotionEvent e) {
        if (shouldStart(e)) {
            startBandSelect(e);
        } else if (shouldStop(e)) {
            endBandSelect();
        }

        return isActive();
    }

    /**
     * Processes a MotionEvent by starting, ending, or resizing the band select overlay.
     */
    @Override
    public void onTouchEvent(@NonNull RecyclerView unused, @NonNull MotionEvent e) {
        if (shouldStop(e)) {
            endBandSelect();
            return;
        }

        // We shouldn't get any events in this method when band select is not active,
        // but it turns some guests show up late to the party.
        // Probably happening when a re-layout is happening to the ReyclerView (ie. Pull-To-Refresh)
        if (!isActive()) {
            return;
        }

        if (DEBUG) {
            checkArgument(MotionEvents.isActionMove(e));
            checkState(mModel != null);
        }

        mCurrentPosition = MotionEvents.getOrigin(e);

        mModel.resizeSelection(mCurrentPosition);

        resizeBand();
        mScroller.scroll(mCurrentPosition);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    /**
     * Starts band select by adding the drawable to the RecyclerView's overlay.
     */
    private void startBandSelect(@NonNull MotionEvent e) {
        checkState(!isActive());

        if (!MotionEvents.isCtrlKeyPressed(e)) {
            mSelectionTracker.clearSelection();
        }

        Point origin = MotionEvents.getOrigin(e);
        if (DEBUG) Log.d(TAG, "Starting band select @ " + origin);

        mModel = mHost.createGridModel();
        mModel.addOnSelectionChangedListener(mGridObserver);

        mLock.start();
        mFocusDelegate.clearFocus();
        mOrigin = origin;
        // NOTE: Pay heed that resizeBand modifies the y coordinates
        // in onScrolled. Not sure if model expects this. If not
        // it should be defending against this.
        mModel.startCapturing(mOrigin);
    }

    /**
     * Resizes the band select rectangle by using the origin and the current pointer position as
     * two opposite corners of the selection.
     */
    private void resizeBand() {
        Rect bounds = new Rect(Math.min(mOrigin.x, mCurrentPosition.x),
                Math.min(mOrigin.y, mCurrentPosition.y),
                Math.max(mOrigin.x, mCurrentPosition.x),
                Math.max(mOrigin.y, mCurrentPosition.y));

        if (VERBOSE) Log.v(TAG, "Resizing band! " + bounds);
        mHost.showBand(bounds);
    }

    /**
     * Ends band select by removing the overlay.
     */
    private void endBandSelect() {
        if (DEBUG) {
            Log.d(TAG, "Ending band select.");
            checkState(mModel != null);
        }

        // TODO: Currently when a band select operation ends outside
        // of an item (e.g. in the empty area between items),
        // getPositionNearestOrigin may return an unselected item.
        // Since the point of this code is to establish the
        // anchor point for subsequent range operations (SHIFT+CLICK)
        // we really want to do a better job figuring out the last
        // item selected (and nearest to the cursor).
        int firstSelected = mModel.getPositionNearestOrigin();
        if (firstSelected != GridModel.NOT_SET
                && mSelectionTracker.isSelected(mKeyProvider.getKey(firstSelected))) {
            // Establish the band selection point as range anchor. This
            // allows touch and keyboard based selection activities
            // to be based on the band selection anchor point.
            mSelectionTracker.anchorRange(firstSelected);
        }

        mSelectionTracker.mergeProvisionalSelection();
        reset();
    }

    /**
     * @see OnScrollListener
     */
    private void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (!isActive()) {
            return;
        }

        // Adjust the y-coordinate of the origin the opposite number of pixels so that the
        // origin remains in the same place relative to the view's items.
        mOrigin.y -= dy;
        resizeBand();
    }

    /**
     * Provides functionality for BandController. Exists primarily to tests that are
     * fully isolated from RecyclerView.
     *
     * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
     */
    abstract static class BandHost<K> {

        /**
         * Returns a new GridModel instance.
         */
        abstract GridModel<K> createGridModel();

        /**
         * Show the band covering the bounds.
         *
         * @param bounds The boundaries of the band to show.
         */
        abstract void showBand(@NonNull Rect bounds);

        /**
         * Hide the band.
         */
        abstract void hideBand();

        /**
         * Add a listener to be notified on scroll events.
         *
         * @param listener
         */
        abstract void addOnScrollListener(@NonNull OnScrollListener listener);
    }
}
