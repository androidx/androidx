/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.car.widget.itemdecorators;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.annotation.RestrictTo;
import androidx.car.R;
import androidx.car.widget.GridLayoutManagerUtils;
import androidx.car.widget.PagedListView.DividerVisibilityManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A {@link RecyclerView.ItemDecoration} that will draw a dividing line between each item in the
 * RecyclerView that it is added to.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class DividerDecoration extends RecyclerView.ItemDecoration {
    public static final int INVALID_RESOURCE_ID = -1;

    private final Context mContext;
    private final Paint mPaint;
    private final int mDividerHeight;
    private final int mDividerStartMargin;
    private final int mDividerEndMargin;
    @IdRes private final int mDividerStartId;
    @IdRes private final int mDividerEndId;
    @ColorRes private int mListDividerColor;
    private DividerVisibilityManager mVisibilityManager;

    /**
     * @param dividerStartMargin The start offset of the dividing line. This offset will be
     *     relative to {@code dividerStartId} if that value is given.
     * @param dividerStartId A child view id whose starting edge will be used as the starting
     *     edge of the dividing line. If this value is {@link #INVALID_RESOURCE_ID}, the top
     *     container of each child view will be used.
     * @param dividerEndId A child view id whose ending edge will be used as the starting edge
     *     of the dividing lin.e If this value is {@link #INVALID_RESOURCE_ID}, then the top
     *     container view of each child will be used.
     */
    public DividerDecoration(Context context, int dividerStartMargin,
            int dividerEndMargin, @IdRes int dividerStartId, @IdRes int dividerEndId,
            @ColorRes int listDividerColor) {
        mContext = context;
        mDividerStartMargin = dividerStartMargin;
        mDividerEndMargin = dividerEndMargin;
        mDividerStartId = dividerStartId;
        mDividerEndId = dividerEndId;
        mListDividerColor = listDividerColor;

        mPaint = new Paint();
        mPaint.setColor(mContext.getColor(listDividerColor));
        mDividerHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.car_list_divider_height);
    }

    /** Sets the color for the dividers. */
    public void setDividerColor(@ColorRes int dividerColor) {
        mListDividerColor = dividerColor;
        updateDividerColor();
    }

    /** Updates the list divider color which may have changed due to a day night transition. */
    public void updateDividerColor() {
        mPaint.setColor(mContext.getColor(mListDividerColor));
    }

    /** Sets {@link DividerVisibilityManager} on the DividerDecoration.*/
    public void setVisibilityManager(DividerVisibilityManager dvm) {
        mVisibilityManager = dvm;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        boolean usesGridLayoutManager = parent.getLayoutManager() instanceof GridLayoutManager;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View container = parent.getChildAt(i);
            int itemPosition = parent.getChildAdapterPosition(container);

            if (!showDividerForAdapterPosition(itemPosition)) {
                continue;
            }

            View nextVerticalContainer;
            if (usesGridLayoutManager) {
                // Find an item in next row to calculate vertical space.
                int lastItem = GridLayoutManagerUtils.getLastIndexOnSameRow(i, parent);
                nextVerticalContainer = parent.getChildAt(lastItem + 1);
            } else {
                nextVerticalContainer = parent.getChildAt(i + 1);
            }

            if (nextVerticalContainer == null) {
                // Skip drawing divider for the last row in GridLayoutManager, or the last
                // item (presumably in LinearLayoutManager).
                continue;
            }

            int spacing = nextVerticalContainer.getTop() - container.getBottom();

            // Sometimes during refresh, the nextVerticalContainer can still exist, but is
            // not positioned in its corresponding position in the list (i.e. it has been pushed
            // off-screen). This will result in a negative value for spacing. Do not draw a
            // divider in this case to avoid the divider appearing in the wrong position.
            if (spacing >= 0) {
                drawDivider(c, container, spacing);
            }
        }
    }

    /**
     * Draws a divider under {@code container}.
     *
     * @param spacing between {@code container} and next view.
     */
    private void drawDivider(Canvas c, View container, int spacing) {
        View startChild =
                mDividerStartId != INVALID_RESOURCE_ID
                        ? container.findViewById(mDividerStartId)
                        : container;

        View endChild =
                mDividerEndId != INVALID_RESOURCE_ID
                        ? container.findViewById(mDividerEndId)
                        : container;

        if (startChild == null || endChild == null) {
            return;
        }

        Rect containerRect = new Rect();
        container.getGlobalVisibleRect(containerRect);

        Rect startRect = new Rect();
        startChild.getGlobalVisibleRect(startRect);

        Rect endRect = new Rect();
        endChild.getGlobalVisibleRect(endRect);

        int left = container.getLeft() + mDividerStartMargin
                + (startRect.left - containerRect.left);
        int right = container.getRight()  - mDividerEndMargin
                - (endRect.right - containerRect.right);
        // "(spacing + divider height) / 2" aligns the center of divider to that of spacing
        // between two items.
        // When spacing is an odd value (e.g. created by other decoration), space under divider
        // is greater by 1dp.
        int bottom = container.getBottom() + (spacing + mDividerHeight) / 2;
        int top = bottom - mDividerHeight;

        c.drawRect(left, top, right, bottom, mPaint);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int pos = parent.getChildAdapterPosition(view);
        if (!showDividerForAdapterPosition(pos)) {
            return;
        }
        // Add an bottom offset to all items that should have divider, even when divider is not
        // drawn for the bottom item(s).
        // With GridLayoutManager it's difficult to tell whether a view is in the last row.
        // This is to keep expected behavior consistent.
        outRect.bottom = mDividerHeight;
    }

    private boolean showDividerForAdapterPosition(int position) {
        // If visibility manager is not set, default to show dividers.
        return mVisibilityManager == null || mVisibilityManager.getShowDivider(position);
    }
}
