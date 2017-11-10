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

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.view.View;

import androidx.recyclerview.selection.SelectionHelper.SelectionPredicate;

/**
 * RecyclerView backed {@link BandSelectionHelper.BandHost}.
 */
final class DefaultBandHost<K> extends GridModel.GridHost<K> {

    private static final Rect NILL_RECT = new Rect(0, 0, 0, 0);

    private final RecyclerView mRecView;
    private final Drawable mBand;
    private final ItemKeyProvider<K> mKeyProvider;
    private final SelectionPredicate<K> mSelectionPredicate;

    DefaultBandHost(
            RecyclerView recView,
            @DrawableRes int bandOverlayId,
            ItemKeyProvider<K> keyProvider,
            SelectionPredicate<K> selectionPredicate) {

        checkArgument(recView != null);

        mRecView = recView;
        mBand = mRecView.getContext().getResources().getDrawable(bandOverlayId);

        checkArgument(mBand != null);
        checkArgument(keyProvider != null);
        checkArgument(selectionPredicate != null);

        mKeyProvider = keyProvider;
        mSelectionPredicate = selectionPredicate;

        mRecView.addItemDecoration(
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
        return mRecView.getChildAdapterPosition(mRecView.getChildAt(index));
    }

    @Override
    void addOnScrollListener(RecyclerView.OnScrollListener listener) {
        mRecView.addOnScrollListener(listener);
    }

    @Override
    void removeOnScrollListener(RecyclerView.OnScrollListener listener) {
        mRecView.removeOnScrollListener(listener);
    }

    @Override
    Point createAbsolutePoint(Point relativePoint) {
        return new Point(relativePoint.x + mRecView.computeHorizontalScrollOffset(),
                relativePoint.y + mRecView.computeVerticalScrollOffset());
    }

    @Override
    Rect getAbsoluteRectForChildViewAt(int index) {
        final View child = mRecView.getChildAt(index);
        final Rect childRect = new Rect();
        child.getHitRect(childRect);
        childRect.left += mRecView.computeHorizontalScrollOffset();
        childRect.right += mRecView.computeHorizontalScrollOffset();
        childRect.top += mRecView.computeVerticalScrollOffset();
        childRect.bottom += mRecView.computeVerticalScrollOffset();
        return childRect;
    }

    @Override
    int getVisibleChildCount() {
        return mRecView.getChildCount();
    }

    @Override
    int getColumnCount() {
        RecyclerView.LayoutManager layoutManager = mRecView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount();
        }

        // Otherwise, it is a list with 1 column.
        return 1;
    }

    @Override
    void showBand(Rect rect) {
        mBand.setBounds(rect);
        // TODO: mRecView.invalidateItemDecorations() should work, but it isn't currently.
        // NOTE: That without invalidating rv, the band only gets updated
        // when the pointer moves off a the item view into "NO_POSITION" territory.
        mRecView.invalidate();
    }

    @Override
    void hideBand() {
        mBand.setBounds(NILL_RECT);
        // TODO: mRecView.invalidateItemDecorations() should work, but it isn't currently.
        mRecView.invalidate();
    }

    private void onDrawBand(Canvas c) {
        mBand.draw(c);
    }

    @Override
    boolean hasView(int pos) {
        return mRecView.findViewHolderForAdapterPosition(pos) != null;
    }
}
