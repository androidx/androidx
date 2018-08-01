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

package androidx.car.widget;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

/**
 * Utility class that helps navigating in GridLayoutManager.
 *
 * <p>Assumes parameter {@code RecyclerView} uses {@link GridLayoutManager}.
 *
 * <p>Assumes the orientation of {@code GridLayoutManager} is vertical.
 */
class GridLayoutManagerUtils {
    private GridLayoutManagerUtils() {}

    /**
     * @param parent RecyclerView that uses GridLayoutManager as LayoutManager.
     * @return number of items in the first row in {@code RecyclerView}.
     */
    public static int getFirstRowItemCount(RecyclerView parent) {
        GridLayoutManager manager = (GridLayoutManager) parent.getLayoutManager();
        int itemCount = parent.getAdapter().getItemCount();
        int spanCount = manager.getSpanCount();

        int spanSum = 0;
        int pos = 0;
        while (pos < itemCount && spanSum < spanCount) {
            spanSum += manager.getSpanSizeLookup().getSpanSize(pos);
            pos += 1;
        }
        // pos will be either the first item in second row, or item count when items not fill
        // the first row.
        return pos;
    }

    /**
     * Returns the span index of an item.
     */
    public static int getSpanIndex(View item) {
        GridLayoutManager.LayoutParams layoutParams =
                ((GridLayoutManager.LayoutParams) item.getLayoutParams());
        return layoutParams.getSpanIndex();
    }

    /**
     * Returns the span size of an item. {@code item} must be already laid out.
     */
    public static int getSpanSize(View item) {
        GridLayoutManager.LayoutParams layoutParams =
                ((GridLayoutManager.LayoutParams) item.getLayoutParams());
        return layoutParams.getSpanSize();
    }

    /**
     * Returns the index of the last item that is on the same row as {@code index}.
     *
     * @param index index of child {@code View} in {@code parent}.
     * @param parent {@link RecyclerView} that contains the View {@code index} points to.
     */
    public static int getLastIndexOnSameRow(int index, RecyclerView parent) {
        int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
        int spanSum = GridLayoutManagerUtils.getSpanIndex(parent.getChildAt(index));
        for (int i = index; i < parent.getChildCount(); i++) {
            spanSum += GridLayoutManagerUtils.getSpanSize(parent.getChildAt(i));
            if (spanSum > spanCount) {
                // We have reached next row.

                // Implicit constraint by grid layout manager:
                // Initial spanSum + spanSize would not exceed spanCount, so it's safe to
                // subtract 1.
                return i - 1;
            }
        }
        // Still have not reached row end. Assuming the list only scrolls vertically, we are at
        // the last row.
        return parent.getChildCount() - 1;
    }
}
