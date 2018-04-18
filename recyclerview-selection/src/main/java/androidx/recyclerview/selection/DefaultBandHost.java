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

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

/**
 * RecyclerView backed {@link BandSelectionHelper.BandHost}.
 */
final class DefaultBandHost<K> extends GridModel.GridHost<K> {

    private static final Rect NILL_RECT = new Rect(0, 0, 0, 0);

    private final RecyclerView mRecyclerView;
    private final Drawable mBand;
    private final ItemKeyProvider<K> mKeyProvider;
    private final SelectionPredicate<K> mSelectionPredicate;

    DefaultBandHost(
            @NonNull RecyclerView recyclerView,
            @DrawableRes int bandOverlayId,
            @NonNull ItemKeyProvider<K> keyProvider,
            @NonNull SelectionPredicate<K> selectionPredicate) {

        checkArgument(recyclerView != null);

        mRecyclerView = recyclerView;
        mBand = mRecyclerView.getContext().getResources().getDrawable(bandOverlayId);

        checkArgument(mBand != null);
        checkArgument(keyProvider != null);
        checkArgument(selectionPredicate != null);

        mKeyProvider = keyProvider;
        mSelectionPredicate = selectionPredicate;

        mRecyclerView.addItemDecoration(
                new ItemDecoration() {
                    @Override
                    public void onDrawOver(
                            Canvas canvas,
                            RecyclerView unusedParent,
                            RecyclerView.State unusedState) {
                        DefaultBandHost.this.onDrawBand(canvas);
                    }
                });
    }

    @Override
    GridModel<K> createGridModel() {
        return new GridModel<>(this, mKeyProvider, mSelectionPredicate);
    }

    @Override
    int getAdapterPositionAt(int index) {
        return mRecyclerView.getChildAdapterPosition(mRecyclerView.getChildAt(index));
    }

    @Override
    void addOnScrollListener(@NonNull OnScrollListener listener) {
        mRecyclerView.addOnScrollListener(listener);
    }

    @Override
    void removeOnScrollListener(@NonNull OnScrollListener listener) {
        mRecyclerView.removeOnScrollListener(listener);
    }

    @Override
    Point createAbsolutePoint(@NonNull Point relativePoint) {
        return new Point(relativePoint.x + mRecyclerView.computeHorizontalScrollOffset(),
                relativePoint.y + mRecyclerView.computeVerticalScrollOffset());
    }

    @Override
    Rect getAbsoluteRectForChildViewAt(int index) {
        final View child = mRecyclerView.getChildAt(index);
        final Rect childRect = new Rect();
        child.getHitRect(childRect);
        childRect.left += mRecyclerView.computeHorizontalScrollOffset();
        childRect.right += mRecyclerView.computeHorizontalScrollOffset();
        childRect.top += mRecyclerView.computeVerticalScrollOffset();
        childRect.bottom += mRecyclerView.computeVerticalScrollOffset();
        return childRect;
    }

    @Override
    int getVisibleChildCount() {
        return mRecyclerView.getChildCount();
    }

    @Override
    int getColumnCount() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount();
        }

        // Otherwise, it is a list with 1 column.
        return 1;
    }

    @Override
    void showBand(@NonNull Rect rect) {
        mBand.setBounds(rect);
        // TODO: mRecyclerView.invalidateItemDecorations() should work, but it isn't currently.
        // NOTE: That without invalidating rv, the band only gets updated
        // when the pointer moves off a the item view into "NO_POSITION" territory.
        mRecyclerView.invalidate();
    }

    @Override
    void hideBand() {
        mBand.setBounds(NILL_RECT);
        // TODO: mRecyclerView.invalidateItemDecorations() should work, but it isn't currently.
        mRecyclerView.invalidate();
    }

    private void onDrawBand(@NonNull Canvas c) {
        mBand.draw(c);
    }

    @Override
    boolean hasView(int pos) {
        return mRecyclerView.findViewHolderForAdapterPosition(pos) != null;
    }
}
