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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

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
@RestrictTo(LIBRARY_GROUP_PREFIX)
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
     * Returns whether or not the given view is on the last row of a {@code RecyclerView} with a
     * {@link GridLayoutManager}.
     *
     * @param view The view to inspect.
     * @param parent {@link RecyclerView} that contains the given view.
     * @return {@code true} if the given view is on the last row of the {@code RecyclerView}.
     */
    public static boolean isOnLastRow(View view, RecyclerView parent) {
        return getLastItemPositionOnSameRow(view, parent) == parent.getAdapter().getItemCount() - 1;
    }

    /**
     * Returns the position of the last item that is on the same row as input {@code view}.
     *
     * @param view The view to inspect.
     * @param parent {@link RecyclerView} that contains the given view.
     */
    public static int getLastItemPositionOnSameRow(View view, RecyclerView parent) {
        GridLayoutManager layoutManager = ((GridLayoutManager) parent.getLayoutManager());

        GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();
        int spanCount = layoutManager.getSpanCount();
        int lastItemPosition = parent.getAdapter().getItemCount() - 1;

        int currentChildPosition = parent.getChildAdapterPosition(view);
        int spanSum = getSpanIndex(view) + spanSizeLookup.getSpanSize(currentChildPosition);
        // Iterate to the end of the row starting from the current child position.
        while (currentChildPosition <= lastItemPosition && spanSum <= spanCount) {
            spanSum += spanSizeLookup.getSpanSize(currentChildPosition + 1);
            if (spanSum > spanCount) {
                return currentChildPosition;
            }
            currentChildPosition++;
        }
        return lastItemPosition;
    }
}
