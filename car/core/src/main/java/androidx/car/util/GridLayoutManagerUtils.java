/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.util;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Utility class that helps navigating in GridLayoutManager.
 *
 * <p>Assumes parameter {@code RecyclerView} uses {@link GridLayoutManager}.
 *
 * <p>Assumes the orientation of {@code GridLayoutManager} is vertical.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class GridLayoutManagerUtils {
    private GridLayoutManagerUtils() {}

    /**
     * Returns the number of items in the first row of a RecyclerView that has a
     * {@link GridLayoutManager} as its {@code LayoutManager}.
     *
     * @param recyclerView RecyclerView that uses GridLayoutManager as LayoutManager.
     * @return number of items in the first row in {@code RecyclerView}.
     */
    public static int getFirstRowItemCount(RecyclerView recyclerView) {
        GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
        int itemCount = recyclerView.getAdapter().getItemCount();
        int spanCount = manager.getSpanCount();

        int spanSum = 0;
        int numOfItems = 0;
        while (numOfItems < itemCount && spanSum < spanCount) {
            spanSum += manager.getSpanSizeLookup().getSpanSize(numOfItems);
            numOfItems++;
        }
        return numOfItems;
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

    /**
     * Returns whether or not the given view is on the last row of a {@code RecyclerView} with a
     * {@link GridLayoutManager}.
     *
     * @param view The view to inspect.
     * @param parent {@link RecyclerView} that contains the given view.
     * @return {@code true} if the given view is on the last row of the {@code RecyclerView}.
     */
    public static boolean isOnLastRow(View view, RecyclerView parent) {
        GridLayoutManager layoutManager = ((GridLayoutManager) parent.getLayoutManager());

        int lastChildPosition = parent.getAdapter().getItemCount() - 1;
        int currentChildPosition = parent.getChildAdapterPosition(view);

        // The last view is automatically on the last row.
        if (currentChildPosition == lastChildPosition) {
            return true;
        }

        GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();
        int spanSum = getSpanIndex(view) + spanSizeLookup.getSpanSize(currentChildPosition);
        int spanCount = layoutManager.getSpanCount();

        // Iterate to the end of the row starting from the current child position.
        while (spanSum < spanCount) {
            currentChildPosition++;

            // Encountered the last child on the row, meaning the given View is on the last row.
            if (currentChildPosition == lastChildPosition) {
                return true;
            }

            spanSum += spanSizeLookup.getSpanSize(currentChildPosition);
        }

        // Last child is not on the current row, meaning the current row is not the last row.
        return false;
    }
}
