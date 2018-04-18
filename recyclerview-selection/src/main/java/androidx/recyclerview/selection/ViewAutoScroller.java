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
import static androidx.recyclerview.selection.Shared.DEBUG;
import static androidx.recyclerview.selection.Shared.VERBOSE;

import android.graphics.Point;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Provides auto-scrolling upon request when user's interaction with the application
 * introduces a natural intent to scroll. Used by BandSelectionHelper and GestureSelectionHelper,
 * to provide auto scrolling when user is performing selection operations.
 */
final class ViewAutoScroller extends AutoScroller {

    private static final String TAG = "ViewAutoScroller";

    // ratio used to calculate the top/bottom hotspot region; used with view height
    private static final float DEFAULT_SCROLL_THRESHOLD_RATIO = 0.125f;
    private static final int MAX_SCROLL_STEP = 70;

    private final float mScrollThresholdRatio;

    private final ScrollHost mHost;
    private final Runnable mRunner;

    private @Nullable Point mOrigin;
    private @Nullable Point mLastLocation;
    private boolean mPassedInitialMotionThreshold;

    ViewAutoScroller(@NonNull ScrollHost scrollHost) {
        this(scrollHost, DEFAULT_SCROLL_THRESHOLD_RATIO);
    }

    @VisibleForTesting
    ViewAutoScroller(@NonNull ScrollHost scrollHost, float scrollThresholdRatio) {

        checkArgument(scrollHost != null);

        mHost = scrollHost;
        mScrollThresholdRatio = scrollThresholdRatio;

        mRunner = new Runnable() {
            @Override
            public void run() {
                runScroll();
            }
        };
    }

    @Override
    public void reset() {
        mHost.removeCallback(mRunner);
        mOrigin = null;
        mLastLocation = null;
        mPassedInitialMotionThreshold = false;
    }

    @Override
    public void scroll(@NonNull Point location) {
        mLastLocation = location;

        // See #aboveMotionThreshold for details on how we track initial location.
        if (mOrigin == null) {
            mOrigin = location;
            if (VERBOSE) Log.v(TAG, "Origin @ " + mOrigin);
        }

        if (VERBOSE) Log.v(TAG, "Current location @ " + mLastLocation);

        mHost.runAtNextFrame(mRunner);
    }

    /**
     * Attempts to smooth-scroll the view at the given UI frame. Application should be
     * responsible to do any clean up (such as unsubscribing scrollListeners) after the run has
     * finished, and re-run this method on the next UI frame if applicable.
     */
    private void runScroll() {
        if (DEBUG) checkState(mLastLocation != null);

        if (VERBOSE) Log.v(TAG, "Running in background using event location @ " + mLastLocation);

        // Compute the number of pixels the pointer's y-coordinate is past the view.
        // Negative values mean the pointer is at or before the top of the view, and
        // positive values mean that the pointer is at or after the bottom of the view. Note
        // that top/bottom threshold is added here so that the view still scrolls when the
        // pointer are in these buffer pixels.
        int pixelsPastView = 0;

        final int verticalThreshold = (int) (mHost.getViewHeight()
                * mScrollThresholdRatio);

        if (mLastLocation.y <= verticalThreshold) {
            pixelsPastView = mLastLocation.y - verticalThreshold;
        } else if (mLastLocation.y >= mHost.getViewHeight()
                - verticalThreshold) {
            pixelsPastView = mLastLocation.y - mHost.getViewHeight()
                    + verticalThreshold;
        }

        if (pixelsPastView == 0) {
            // If the operation that started the scrolling is no longer inactive, or if it is active
            // but not at the edge of the view, no scrolling is necessary.
            return;
        }

        // We're in one of the endzones. Now determine if there's enough of a difference
        // from the orgin to take any action. Basically if a user has somehow initiated
        // selection, but is hovering at or near their initial contact point, we don't
        // scroll. This avoids a situation where the user initiates selection in an "endzone"
        // only to have scrolling start automatically.
        if (!mPassedInitialMotionThreshold && !aboveMotionThreshold(mLastLocation)) {
            if (VERBOSE) Log.v(TAG, "Ignoring event below motion threshold.");
            return;
        }
        mPassedInitialMotionThreshold = true;

        if (pixelsPastView > verticalThreshold) {
            pixelsPastView = verticalThreshold;
        }

        // Compute the number of pixels to scroll, and scroll that many pixels.
        final int numPixels = computeScrollDistance(pixelsPastView);
        mHost.scrollBy(numPixels);

        // Replace any existing scheduled jobs with the latest and greatest..
        mHost.removeCallback(mRunner);
        mHost.runAtNextFrame(mRunner);
    }

    private boolean aboveMotionThreshold(@NonNull Point location) {
        // We reuse the scroll threshold to calculate a much smaller area
        // in which we ignore motion initially.
        int motionThreshold =
                (int) ((mHost.getViewHeight() * mScrollThresholdRatio)
                        * (mScrollThresholdRatio * 2));
        return Math.abs(mOrigin.y - location.y) >= motionThreshold;
    }

    /**
     * Computes the number of pixels to scroll based on how far the pointer is past the end
     * of the region. Roughly based on ItemTouchHelper's algorithm for computing the number of
     * pixels to scroll when an item is dragged to the end of a view.
     * @return
     */
    @VisibleForTesting
    int computeScrollDistance(int pixelsPastView) {
        final int topBottomThreshold =
                (int) (mHost.getViewHeight() * mScrollThresholdRatio);

        final int direction = (int) Math.signum(pixelsPastView);
        final int absPastView = Math.abs(pixelsPastView);

        // Calculate the ratio of how far out of the view the pointer currently resides to
        // the top/bottom scrolling hotspot of the view.
        final float outOfBoundsRatio = Math.min(
                1.0f, (float) absPastView / topBottomThreshold);
        // Interpolate this ratio and use it to compute the maximum scroll that should be
        // possible for this step.
        final int cappedScrollStep =
                (int) (direction * MAX_SCROLL_STEP * smoothOutOfBoundsRatio(outOfBoundsRatio));

        // If the final number of pixels to scroll ends up being 0, the view should still
        // scroll at least one pixel.
        return cappedScrollStep != 0 ? cappedScrollStep : direction;
    }

    /**
     * Interpolates the given out of bounds ratio on a curve which starts at (0,0) and ends
     * at (1,1) and quickly approaches 1 near the start of that interval. This ensures that
     * drags that are at the edge or barely past the edge of the threshold does little to no
     * scrolling, while drags that are near the edge of the view does a lot of
     * scrolling. The equation y=x^10 is used, but this could also be tweaked if
     * needed.
     * @param ratio A ratio which is in the range [0, 1].
     * @return A "smoothed" value, also in the range [0, 1].
     */
    private float smoothOutOfBoundsRatio(float ratio) {
        return (float) Math.pow(ratio, 10);
    }

    /**
     * Used by to calculate the proper amount of pixels to scroll given time passed
     * since scroll started, and to properly scroll / proper listener clean up if necessary.
     *
     * Callback used by scroller to perform UI tasks, such as scrolling and rerunning at next UI
     * cycle.
     */
    abstract static class ScrollHost {
        /**
         * @return height of the view.
         */
        abstract int getViewHeight();

        /**
         * @param dy distance to scroll.
         */
        abstract void scrollBy(int dy);

        /**
         * @param r schedule runnable to be run at next convenient time.
         */
        abstract void runAtNextFrame(@NonNull Runnable r);

        /**
         * @param r remove runnable from being run.
         */
        abstract void removeCallback(@NonNull Runnable r);
    }

    static ScrollHost createScrollHost(final RecyclerView recyclerView) {
        return new RuntimeHost(recyclerView);
    }

    /**
     * Tracks location of last surface contact as reported by RecyclerView.
     */
    private static final class RuntimeHost extends ScrollHost {

        private final RecyclerView mRecyclerView;

        RuntimeHost(@NonNull RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

        @Override
        void runAtNextFrame(@NonNull Runnable r) {
            ViewCompat.postOnAnimation(mRecyclerView, r);
        }

        @Override
        void removeCallback(@NonNull Runnable r) {
            mRecyclerView.removeCallbacks(r);
        }

        @Override
        void scrollBy(int dy) {
            if (VERBOSE) Log.v(TAG, "Scrolling view by: " + dy);
            mRecyclerView.scrollBy(0, dy);
        }

        @Override
        int getViewHeight() {
            return mRecyclerView.getHeight();
        }
    }
}
