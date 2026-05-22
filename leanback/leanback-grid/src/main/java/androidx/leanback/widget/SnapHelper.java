/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.leanback.widget;

import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;

/**
 * Class intended to support snapping for a {@link BaseGridView} during fling.
 */
final class SnapHelper extends RecyclerView.OnFlingListener {

    BaseGridView mRecyclerView;
    private Scroller mGravityScroller;
    boolean mInFling;

    @Override
    public boolean onFling(int velocityX, int velocityY) {
        GridLayoutManager layoutManager = mRecyclerView.mLayoutManager;
        if (layoutManager == null) {
            return false;
        }
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter == null) {
            return false;
        }
        // Trigger snap to item while velocity drops to certain threshold.
        int minFlingVelocity = mRecyclerView.getMinFlingVelocity();
        return (Math.abs(velocityY) > minFlingVelocity || Math.abs(velocityX) > minFlingVelocity)
                && snapFromFling(layoutManager, velocityX, velocityY);
    }

    void attachToRecyclerView(@NonNull BaseGridView recyclerView)  {
        if (mRecyclerView == recyclerView) {
            return;
        }
        if (mRecyclerView != null) {
            mRecyclerView.setOnFlingListener(null);
            mRecyclerView = null;
        }
        if (recyclerView.getOnFlingListener() != null) {
            return;
        }
        mRecyclerView = recyclerView;
        mRecyclerView.setOnFlingListener(this);
        mGravityScroller = new Scroller(mRecyclerView.getContext(),
                new DecelerateInterpolator());
    }

    void detachFromRecyclerView() {
        if (mRecyclerView == null) {
            return;
        }
        mRecyclerView.setOnFlingListener(null);
        mRecyclerView = null;
    }

    int calculateScrollDistance(int orientation, int velocityX, int velocityY) {
        boolean isHorizontal = orientation == BaseGridView.HORIZONTAL;
        mGravityScroller.fling(0, 0, isHorizontal ? velocityX : 0, isHorizontal ? 0 : velocityY,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        return isHorizontal ? mGravityScroller.getFinalX() : mGravityScroller.getFinalY();
    }

    boolean snapFromFling(@NonNull GridLayoutManager layoutManager, int velocityX,
            int velocityY) {
        int targetPosition = findTargetSnapPosition(layoutManager, velocityX, velocityY);
        if (targetPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        layoutManager.scrollToSelection(targetPosition, 0, true, 0);
        // We are now in fling mode if there is actually scrolling happening, if not we return
        // false to have RecyclerView.onTouchEvent() to stop the scroll in order to fire child
        // selected event.
        // True flag will be later reset when GridLayoutManager detected a onScrollStateChanged(),
        // either it's interrupted by another drag or it stopped on idle.
        mInFling = mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING
                || layoutManager.isSmoothScrolling();
        return mInFling;
    }

    int findTargetSnapPosition(GridLayoutManager layoutManager,
            int velocityX, int velocityY) {
        if (layoutManager.mFocusPosition < 0) {
            return RecyclerView.NO_POSITION;
        }
        if (mRecyclerView.getChildCount() == 0) {
            return RecyclerView.NO_POSITION;
        }
        int distance = calculateScrollDistance(layoutManager.mOrientation, velocityX, velocityY);
        if (distance == 0) {
            return RecyclerView.NO_POSITION;
        }
        int absoluteDistance = distance > 0 ? distance : -distance;
        int totalSize = 0;
        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            totalSize += layoutManager.getViewPrimarySize(mRecyclerView.getChildAt(i));
        }
        int averageSize = totalSize / mRecyclerView.getChildCount();
        int absolutePositionDelta = averageSize == 0 ? 1
                : Math.round((float) absoluteDistance / averageSize);
        if (absolutePositionDelta == 0) {
            // If there is a fling, make sure it moves one position at minimal.
            absolutePositionDelta = 1;
        }
        int positionDelta = distance > 0 ? absolutePositionDelta : -absolutePositionDelta;
        return layoutManager.mGrid.getNextPositionOfSameSpan(layoutManager.mFocusPosition,
                layoutManager.getItemCount(), positionDelta);
    }
}
